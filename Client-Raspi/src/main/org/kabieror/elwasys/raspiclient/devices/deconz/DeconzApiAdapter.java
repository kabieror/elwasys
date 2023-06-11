package org.kabieror.elwasys.raspiclient.devices.deconz;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Base64;

class DeconzApiAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson = new Gson().newBuilder().create();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI apiBase;
    private String token;

    public DeconzApiAdapter(URI apiBase) {
        this.apiBase = apiBase;
    }

    public DeconzDeviceState getDeviceState(int deconzId) throws IOException, InterruptedException {
        var response = request(HttpRequest
                .newBuilder(apiBase.resolve("lights/%s".formatted(deconzId)))
                .GET());
        var device = gson.fromJson(response.body(), DeconzDevice.class);
        return device.state();
    }

    public void setDeviceState(int deviceId, boolean newState) throws IOException, InterruptedException {
        var state = new DeconzDeviceState(newState);
        request(HttpRequest
                .newBuilder(apiBase.resolve("lights/%s/state".formatted(deviceId)))
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(state))));
    }

    private HttpResponse<String> request(HttpRequest.Builder requestBuilder) throws IOException, InterruptedException {
        if (token == null) {
            authenticate();
        }

        var request = requestBuilder.build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            logger.warn("Request to deconz failed. Trying to obtain new token.");
            authenticate();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private void authenticate() throws IOException, InterruptedException {
        var body = new JsonObject();
        body.add("devicetype", new JsonPrimitive("elwaClient"));
        var bodyString = gson.toJson(body);

        var request = HttpRequest.newBuilder(apiBase)
                .header("Authorization", "Basic " + Arrays.toString(Base64.getEncoder().encode("delight:elwasys2014".getBytes())))
                .POST(HttpRequest.BodyPublishers.ofString(bodyString))
                .build();

        var responseRaw = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        var response = gson.fromJson(String.valueOf(responseRaw), DeconzAuthenticationResponse.class);
        if (response.successEntities() != null
                && response.successEntities().length == 1
                && response.successEntities()[0].success() != null
                && response.successEntities()[0].success().username() != null) {
            token = response.successEntities()[0].success().username();
        }
    }

}

