package org.kabieror.elwasys.raspiclient.devices;

import java.io.IOException;

import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.executions.FhemException;

public class DeconzDevicePowerManager implements IDevicePowerManager {

    public DeconzDevicePowerManager(WashguardConfiguration configurationManager) {
    }

    @Override
    public void setDevicePowerState(Device device, DevicePowerState newState)
            throws IOException, InterruptedException, FhemException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDevicePowerState'");
    }

    @Override
    public DevicePowerState getState(Device device) throws InterruptedException, FhemException, IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getState'");
    }

    @Override
    public void addPowerMeasurementListener(IDevicePowerMeasurementHandler handler) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addPowerMeasurementListener'");
    }
    
}
