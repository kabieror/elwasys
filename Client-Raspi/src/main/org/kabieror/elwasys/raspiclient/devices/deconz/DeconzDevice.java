package org.kabieror.elwasys.raspiclient.devices.deconz;

public record DeconzDevice(
        String name,
        DeconzDeviceState state
) {
}
