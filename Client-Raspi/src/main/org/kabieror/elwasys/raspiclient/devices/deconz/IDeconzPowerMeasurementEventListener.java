package org.kabieror.elwasys.raspiclient.devices.deconz;

import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzEvent;

interface IDeconzPowerMeasurementEventListener {
    void onPowerMeasurementReceived(DeconzEvent event);
}
