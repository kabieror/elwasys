package org.kabieror.elwasys.raspiclient.devices;

public interface IDevicePowerMeasurementHandler {
    void onPowerMeasurementAvailable(int deviceId, double currentPowerConsumption);
}