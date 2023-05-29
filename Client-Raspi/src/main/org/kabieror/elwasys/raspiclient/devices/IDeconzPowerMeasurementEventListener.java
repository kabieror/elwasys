package org.kabieror.elwasys.raspiclient.devices;

public interface IDeconzPowerMeasurementEventListener {
    void onPowerMeasurementReceived(DeconzPowerMeasurementEvent event);
}
