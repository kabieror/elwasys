package org.kabieror.elwasys.raspiclient.devices.deconz;

import com.google.gson.Gson;

import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzDevice;
import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzDeviceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.Map;

class DeconzService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson = new Gson().newBuilder().create();

    private DeconzApiAdapter adapter;

    public DeconzService(DeconzApiAdapter adapter) {
        this.adapter = adapter;
    }

    public DeconzDeviceState getDeviceState(String deconzUuid) throws IOException, InterruptedException {
        var response = adapter.request("lights/%s".formatted(deconzUuid),
                r -> r.GET());
        var device = adapter.parseResponse(response, DeconzDevice.class);
        return device.state();
    }

    public void setDeviceState(String deviceUuid, boolean newState) throws IOException, InterruptedException {
        var state = new DeconzDeviceState(newState);
        adapter.request("lights/%s/state".formatted(deviceUuid),
                r -> r.PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(state))));
    }

}
