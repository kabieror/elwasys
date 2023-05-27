package org.kabieror.elwasys.raspiclient.devices;

import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.executions.FhemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeconzDevicePowerManager implements IDevicePowerManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final WebSocketClient client;
    private final WebSocketConnectionManager manager;
    private List<IDevicePowerMeasurementHandler> powerMeasurementListeners = new ArrayList<>();

    public DeconzDevicePowerManager(WashguardConfiguration configurationManager) {
        client = new StandardWebSocketClient();
        manager = new WebSocketConnectionManager(client, new DeconzSessionHandler(), "ws://192.168.0.15:8943");
        manager.setAutoStartup(true);
        manager.start();
    }

    @Override
    public void setDevicePowerState(Device device, DevicePowerState newState)
            throws IOException, InterruptedException, FhemException {
    }

    @Override
    public DevicePowerState getState(Device device) throws InterruptedException, FhemException, IOException {
        return DevicePowerState.OFF;
    }

    @Override
    public void addPowerMeasurementListener(IDevicePowerMeasurementHandler handler) {
        this.powerMeasurementListeners.add(handler);
    }
    
}
