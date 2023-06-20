package org.kabieror.elwasys.raspiclient.devices.deconz.model;

public record DeconzError(
        String address,
        String description
) {
}
