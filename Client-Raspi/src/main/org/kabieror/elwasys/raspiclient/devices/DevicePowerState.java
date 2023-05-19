package org.kabieror.elwasys.raspiclient.devices;

public enum DevicePowerState {
    /**
     * The device is powered on.
     */
    ON,

    /**
     * The device is powered off.
     */
    OFF,

    /**
     * The device is to be powered on.
     */
    SET_ON,

    /**
     * The device is to be powered off.
     */
    SET_OFF,

    /**
     * Unknown state
     */
    UNKNOWN,
}