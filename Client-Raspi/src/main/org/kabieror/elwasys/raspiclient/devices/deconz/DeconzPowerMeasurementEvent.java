package org.kabieror.elwasys.raspiclient.devices.deconz;

record DeconzPowerMeasurementEvent(
        DeconzChangeType e,
        int id,
        String r,
        DeconzPowerMeasurementState state,
        String uniqueid) {
}
