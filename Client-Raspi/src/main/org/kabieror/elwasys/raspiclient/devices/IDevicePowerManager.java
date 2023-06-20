package org.kabieror.elwasys.raspiclient.devices;

import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.raspiclient.executions.FhemException;

import java.io.IOException;
import java.util.concurrent.Future;

public interface IDevicePowerManager {

    void addPowerMeasurementListener(IDevicePowerMeasurementHandler handler);

    /**
     * Switches the power of a device on.
     *
     * @param device The device to switch on.
     * @throws InterruptedException
     */
    void setDevicePowerState(Device device, DevicePowerState newState)
            throws IOException, InterruptedException, FhemException;

    /**
     * Looks up the power state of a device.
     *
     * @param device Der Gerät, dessen Status geholt werden soll.
     * @return The power state of the device.
     */
    DevicePowerState getState(Device device) throws InterruptedException, FhemException, IOException;

    /**
     * Checks whether a device can be controlled.
     */
    boolean isDeviceRegistered(Device device);

    /**
     * Tries to find a new remote socket for the given device.
     */
    Future<Boolean> registerDevice(Device device);
}