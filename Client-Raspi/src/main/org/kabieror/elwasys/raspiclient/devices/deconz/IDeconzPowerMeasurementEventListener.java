package org.kabieror.elwasys.raspiclient.devices.deconz;

interface IDeconzPowerMeasurementEventListener {
    void onPowerMeasurementReceived(DeconzPowerMeasurementEvent event);
}
