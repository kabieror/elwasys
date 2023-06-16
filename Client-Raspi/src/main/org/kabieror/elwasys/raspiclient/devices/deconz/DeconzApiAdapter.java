package org.kabieror.elwasys.raspiclient.devices.deconz;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.function.Consumer;

class DeconzApiAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson = new Gson().newBuilder().create();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI apiBase;
    private final String username;
    private final String password;
    private String token;

    public DeconzApiAdapter(URI apiBase, String username, String password) {
        this.apiBase = apiBase.resolve("api/");
        this.username = username;
        this.password = password;
    }

    public DeconzDeviceState getDeviceState(int deconzId) throws IOException, InterruptedException {
        var response = request("lights/%s".formatted(deconzId),
                r -> r.GET());
        var device = gson.fromJson(response.body(), DeconzDevice.class);
        return device.state();
    }

    public void setDeviceState(int deviceId, boolean newState) throws IOException, InterruptedException {
        var state = new DeconzDeviceState(newState);
        request("lights/%s/state".formatted(deviceId),
                r -> r.PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(state))));
    }

    private HttpResponse<String> request(String apiPath, Consumer<HttpRequest.Builder> requestConfigureAction) throws IOException, InterruptedException {
        if (token == null) {
            authenticate();
        }

        var uri = apiBase.resolve("%s/%s".formatted(token, apiPath));
        var requestBuilder = HttpRequest.newBuilder(uri);
        requestConfigureAction.accept(requestBuilder);
        var request = requestBuilder.build();

        int tryCount = 0;
        int maxRetries = 3;
        while (tryCount <= maxRetries) {
            tryCount++;
            try {
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == HttpStatus.OK.value()) {
                    return response;
                }
                else if (response.statusCode() == HttpStatus.UNAUTHORIZED.value()) {
                    logger.warn("Got http status %s from deCONZ. Trying to re-authenticate.".formatted(response.statusCode()));
                    authenticate();
                }
                else {
                    logger.error("Got error response with status %s from deCONZ.\n%s".formatted(response.statusCode(), response.body()));
                    throw new DeconzException();
                }
            } catch (IOException e) {
                logger.warn("Request to deCONZ failed. Trying to obtain new token.");
            }
        }
        throw new DeconzException("Fehler bei der Kommunikation mit deCONZ.");
    }

    private void authenticate() throws IOException, InterruptedException {
        var body = new JsonObject();
        body.add("devicetype", new JsonPrimitive("elwaClient"));
        var bodyString = gson.toJson(body);

        var request = HttpRequest.newBuilder(apiBase)
                .header(
                        "Authorization",
                        "Basic " + getBase64AuthString())
                .POST(HttpRequest.BodyPublishers.ofString(bodyString))
                .build();

        HttpResponse<String> responseRaw = null;
        try {
            responseRaw = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var response = gson.fromJson(String.valueOf(responseRaw.body()), DeconzAuthenticationSuccessEntity[].class);
            if (response != null
                    && response.length == 1
                    && response[0].success() != null
                    && response[0].success().username() != null) {
                token = response[0].success().username();
                logger.info("Successfully authenticated at deCONZ.");
            } else {
                logger.error("Failed to authenticate at deCONZ.\n%s".formatted(responseRaw));
                throw new IOException("Anmeldung bei deCONZ fehlgeschlagen.");
            }
        } catch (ConnectException e) {
            logger.error("Failed to authenticate at deCONZ", e);
            throw new ConnectException("Konnte keine Verbindung zu deCONZ aufbauen unter %s".formatted(apiBase));
        } catch (JsonSyntaxException e) {
            logger.info("Received data from deCONZ:\n" + (responseRaw != null ? responseRaw.body() : "null"));
            logger.error("Failed to deserialize deCONZ data for URL %s".formatted(apiBase), e);
            throw e;
        }

    }

    private String getBase64AuthString() {
        return Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes());
    }

}

