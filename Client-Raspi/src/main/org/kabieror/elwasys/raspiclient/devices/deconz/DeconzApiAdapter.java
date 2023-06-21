package org.kabieror.elwasys.raspiclient.devices.deconz;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzAuthenticationSuccessEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.function.Consumer;

public class DeconzApiAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Gson gson = new Gson();
    private String token;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI apiBase;
    private final String username;
    private final String password;

    public DeconzApiAdapter(WashguardConfiguration configuration) {
        this.apiBase = URI.create(configuration.getDeconzServer()).resolve("api/");
        this.username = configuration.getDeconzUser();
        this.password = configuration.getDeconzPassword();
    }

    public HttpResponse<String> request(String apiPath, Consumer<HttpRequest.Builder> requestConfigureAction) throws IOException, InterruptedException {
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
                } else if (response.statusCode() == HttpStatus.UNAUTHORIZED.value()) {
                    logger.warn("Got http status %s from deCONZ. Trying to re-authenticate.".formatted(response.statusCode()));
                    authenticate();
                } else {
                    logger.error("Got error response with status %s from deCONZ.\n%s".formatted(response.statusCode(), response.body()));
                    throw new DeconzException();
                }
            } catch (IOException e) {
                logger.warn("Request to deCONZ failed. Trying to obtain new token.");
            }
        }
        throw new DeconzException("Fehler bei der Kommunikation mit deCONZ.");
    }

    public <T> T parseResponse(HttpResponse<String> response, Class<T> responseType) throws DeconzException {
        String responseBody = response.body();
        Gson gson = new Gson();
        try {
            return gson.fromJson(responseBody, responseType);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse JSON response from deCONZ.", e);
            throw new DeconzException("Failed to parse JSON response.");
        }
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