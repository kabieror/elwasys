package org.kabieror.elwasys.raspiclient.devices;

import org.kabieror.elwasys.common.Execution;

public interface IDevicePowerMeasurementHandler {
    void onPowerMeasurementAvailable(Execution execution, double currentPowerConsumption);
}