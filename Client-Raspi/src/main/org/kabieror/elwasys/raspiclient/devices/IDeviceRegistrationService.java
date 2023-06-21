package org.kabieror.elwasys.raspiclient.devices;

import org.kabieror.elwasys.common.Device;

import java.util.concurrent.Future;

public interface IDeviceRegistrationService {
    /**
     * Checks whether a device can be controlled.
     */
    boolean isDeviceRegistered(Device device);

    /**
     * Tries to find a new remote socket for the given device.
     */
    Future<Boolean> registerDevice(Device device);
}
