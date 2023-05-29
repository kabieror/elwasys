package org.kabieror.elwasys.raspiclient.devices;

import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.raspiclient.application.ElwaManager;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.executions.FhemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeconzDevicePowerManager implements IDevicePowerManager {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DeconzEventListener eventListener;
    private List<IDevicePowerMeasurementHandler> powerMeasurementListeners = new ArrayList<>();

    public DeconzDevicePowerManager(WashguardConfiguration configurationManager) {
        eventListener = new DeconzEventListener();
        eventListener.listenToPowerMeasurementReceived(this::onPowerMeasurementReceived);
        eventListener.start();

        ElwaManager.instance.listenToCloseEvent(restart -> onClosing());
    }

    private void onPowerMeasurementReceived(DeconzPowerMeasurementEvent deconzPowerMeasurementEvent) {

//        this.powerMeasurementListeners.forEach(l -> l.onPowerMeasurementAvailable(execution, consumption));
    }

    private void onClosing() {
        eventListener.stop();
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
