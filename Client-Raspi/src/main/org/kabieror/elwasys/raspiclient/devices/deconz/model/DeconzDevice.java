package org.kabieror.elwasys.raspiclient.devices.deconz.model;

public record DeconzDevice(
        String name,
        DeconzDeviceState state,
        String uniqueid
) {
}
