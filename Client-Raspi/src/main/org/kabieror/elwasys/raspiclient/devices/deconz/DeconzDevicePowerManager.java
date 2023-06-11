package org.kabieror.elwasys.raspiclient.devices.deconz;

import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.raspiclient.application.ElwaManager;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.devices.DevicePowerState;
import org.kabieror.elwasys.raspiclient.devices.IDevicePowerManager;
import org.kabieror.elwasys.raspiclient.devices.IDevicePowerMeasurementHandler;
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

    private final DeconzEventListener eventListener;
    private final List<IDevicePowerMeasurementHandler> powerMeasurementListeners = new ArrayList<>();

    public DeconzDevicePowerManager(WashguardConfiguration configurationManager) {
        eventListener = new DeconzEventListener();
        eventListener.listenToPowerMeasurementReceived(this::onPowerMeasurementReceived);
        eventListener.start();

        ElwaManager.instance.listenToCloseEvent(restart -> onClosing());

        apiAdapter = new DeconzApiAdapter(URI.create(configurationManager.getDeconzServer()));
    }

    private void onPowerMeasurementReceived(DeconzPowerMeasurementEvent e) {
        this.logger.info("Received: " + e.toString());
        var execution = ElwaManager.instance.getExecutionManager()
                .getRunningExecutions().stream()
                .filter(exe -> exe.getDevice().getDeconzId() == e.id())
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
        apiAdapter.setDeviceState(device.getDeconzId(), newState == DevicePowerState.SET_ON);
    }

    @Override
    public DevicePowerState getState(Device device) throws InterruptedException, FhemException, IOException {
        var isOn = apiAdapter.getDeviceState(device.getDeconzId()).on();
        return isOn ? DevicePowerState.ON : DevicePowerState.OFF;
    }

    @Override
    public void addPowerMeasurementListener(IDevicePowerMeasurementHandler handler) {
        this.powerMeasurementListeners.add(handler);
    }

}
