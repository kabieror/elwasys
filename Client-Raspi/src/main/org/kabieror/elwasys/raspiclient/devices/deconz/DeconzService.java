package org.kabieror.elwasys.raspiclient.devices.deconz;

import com.google.gson.Gson;
import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzDevice;
import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzDeviceState;
import org.kabieror.elwasys.raspiclient.util.BlockingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.concurrent.TimeUnit;

class DeconzService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson = new Gson().newBuilder().create();

    private final BlockingMap<String, Boolean> deviceStateAwaitingMap = new BlockingMap<>();

    private final DeconzApiAdapter adapter;
    private final DeconzEventListener eventListener;

    public DeconzService(DeconzApiAdapter adapter, DeconzEventListener eventListener) {
        this.adapter = adapter;
        this.eventListener = eventListener;
        this.eventListener.listenToDeviceStateEvent(deviceStateAwaitingMap::put);
    }

    public DeconzDeviceState getDeviceState(String deconzUuid) throws IOException, InterruptedException {
        var response = adapter.request("lights/%s".formatted(deconzUuid),
                r -> r.GET());
        var device = adapter.parseResponse(response, DeconzDevice.class);
        return device.state();
    }

    public void setDeviceState(String deviceUuid, boolean newState) throws IOException, InterruptedException {
        var currentState = getDeviceState(deviceUuid);
        if (!currentState.reachable()) {
            throw new DeconzException("Das Gerät ist nicht erreichbar.");
        }
        if (currentState.on() == newState) {
            logger.info("Device %s already has the desired state.".formatted(deviceUuid));
            return;
        }
        // Reset device state before starting HTTP request to be sure not to miss the event.
        deviceStateAwaitingMap.clear(deviceUuid);

        var state = new DeconzDeviceState(newState);
        adapter.request("lights/%s/state".formatted(deviceUuid),
                r -> r.PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(state))));

        waitForDeviceState(deviceUuid, newState);
    }

    private void waitForDeviceState(String deviceUuid, boolean newState) throws InterruptedException, DeconzException {
        logger.info("Waiting for deCONZ device " + deviceUuid);
        Boolean result = deviceStateAwaitingMap.get(deviceUuid, 5, TimeUnit.SECONDS);
        if (result == null || !result.equals(newState)) {
            throw new DeconzException("Das Gerät ist nicht erreichbar.");
        }
    }

}
