package org.kabieror.elwasys.raspiclient.devices.deconz.model;

public record DeconzDeviceState(
        boolean on,
        Boolean reachable) {
    public DeconzDeviceState(boolean on) {
        this(on, null);
    }
}
