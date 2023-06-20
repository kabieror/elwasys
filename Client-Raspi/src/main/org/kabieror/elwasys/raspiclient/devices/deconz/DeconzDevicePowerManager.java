package org.kabieror.elwasys.raspiclient.devices.deconz;

import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.raspiclient.application.ElwaManager;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.devices.DevicePowerState;
import org.kabieror.elwasys.raspiclient.devices.IDevicePowerManager;
import org.kabieror.elwasys.raspiclient.devices.IDevicePowerMeasurementHandler;
import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzConfig;
import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzEvent;
import org.kabieror.elwasys.raspiclient.executions.FhemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class DeconzDevicePowerManager implements IDevicePowerManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DeconzApiAdapter apiAdapter;
    private final DeconzService deconzService;

    private final DeconzEventListener eventListener;
    private final List<IDevicePowerMeasurementHandler> powerMeasurementListeners = new ArrayList<>();

    public DeconzDevicePowerManager(WashguardConfiguration configurationManager) throws IOException, InterruptedException {
        ElwaManager.instance.listenToCloseEvent(restart -> onClosing());

        var deconzUri = URI.create(configurationManager.getDeconzServer());
        apiAdapter = new DeconzApiAdapter(
                deconzUri,
                configurationManager.getDeconzUser(),
                configurationManager.getDeconzPassword());

        var deconzConfig = apiAdapter.parseResponse(
                apiAdapter.request("config", r -> r.GET()),
                DeconzConfig.class);

        eventListener = new DeconzEventListener(deconzUri.getHost(), deconzConfig.websocketport());
        eventListener.listenToPowerMeasurementReceived(this::onPowerMeasurementReceived);
        eventListener.start();

        deconzService = new DeconzService(apiAdapter, eventListener);
    }

    private void onPowerMeasurementReceived(DeconzEvent e) {
        this.logger.info("Received: " + e.toString());

        var execution = ElwaManager.instance.getExecutionManager()
                .getRunningExecutions().stream()
                .filter(exe -> e.uniqueid().startsWith(exe.getDevice().getDeconzUuid()))
                .findFirst();
        execution.ifPresent(value ->
                this.powerMeasurementListeners.forEach(l -> l.onPowerMeasurementAvailable(value, e.state().power()))
        );
    }

    private void onClosing() {
        eventListener.stop();
    }

    @Override
    public void setDevicePowerState(Device device, DevicePowerState newState)
            throws IOException, InterruptedException, FhemException {
        deconzService.setDeviceState(device.getDeconzUuid(),
                newState == DevicePowerState.SET_ON || newState == DevicePowerState.ON);
    }

    @Override
    public DevicePowerState getState(Device device) throws InterruptedException, FhemException, IOException {
        var isOn = deconzService.getDeviceState(device.getDeconzUuid()).on();
        return isOn ? DevicePowerState.ON : DevicePowerState.OFF;
    }

    @Override
    public void addPowerMeasurementListener(IDevicePowerMeasurementHandler handler) {
        this.powerMeasurementListeners.add(handler);
    }

}
