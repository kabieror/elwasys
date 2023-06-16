package org.kabieror.elwasys.raspiclient.devices.deconz;

record DeconzResponse<TResponse>(
        TResponse success,
        DeconzError error
) { }

record DeconzError (
        String address,
        String description
) { }
