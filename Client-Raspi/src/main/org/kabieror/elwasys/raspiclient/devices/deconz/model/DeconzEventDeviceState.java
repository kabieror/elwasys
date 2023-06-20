package org.kabieror.elwasys.raspiclient.devices.deconz.model;

public record DeconzEventDeviceState(
        Boolean on,
        Double current,
        Double power,
        Double voltage) {
}
