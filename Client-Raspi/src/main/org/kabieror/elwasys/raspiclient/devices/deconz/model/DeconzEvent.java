package org.kabieror.elwasys.raspiclient.devices.deconz.model;


public record DeconzEvent(
        DeconzChangeType e,
        int id,
        String r,
        DeconzEventDeviceState state,
        String uniqueid) {
}
