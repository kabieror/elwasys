package org.kabieror.elwasys.raspiclient.devices.deconz;

import com.google.gson.Gson;
import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzConfigPariing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DeconzRegistrationService {

    private static final int PAIRING_TIME_SECONDS = 30;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private CompletableFuture<String> currentRegistrationFuture;
    private final DeconzApiAdapter apiAdapter;
    private final Gson gson = new Gson();

    public DeconzRegistrationService(DeconzApiAdapter apiAdapter, DeconzEventListener eventListener) {
        this.apiAdapter = apiAdapter;
        eventListener.listenToDeviceRegisteredEvent(uuid -> {
            if (currentRegistrationFuture != null && !currentRegistrationFuture.isDone()) {
                currentRegistrationFuture.complete(uuid);
            }
        });
    }

    public CompletableFuture<String> scanForNewDevice() {
        if (currentRegistrationFuture != null && !currentRegistrationFuture.isDone()) {
            throw new RuntimeException("Another registration process has already been started.");
        }

        if (!enablePairingForSeconds(PAIRING_TIME_SECONDS)) return null;

        currentRegistrationFuture = new CompletableFuture<>();

        return currentRegistrationFuture
                .orTimeout(PAIRING_TIME_SECONDS, TimeUnit.SECONDS);
    }

    private boolean enablePairingForSeconds(int pairingSeconds) {
        try {
            var payload = gson.toJson(new DeconzConfigPariing(pairingSeconds));
            apiAdapter.request("config", r -> r.PUT(HttpRequest.BodyPublishers.ofString(payload)));
        } catch (IOException e) {
            logger.error("Failed to configure deCONZ", e);
        } catch (InterruptedException e) {
            logger.warn("Registration cancelled while configuring deCONZ");
            return false;
        }
        return true;
    }
}
