package org.kabieror.elwasys.raspiclient.devices.deconz.model;


public record DeconzEvent(
        DeconzChangeType e,
        int id,
        DeconzResourceType r,
        DeconzEventDeviceState state,
        DeconzDevice light,
        String uniqueid) {
}
