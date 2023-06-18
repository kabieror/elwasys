package org.kabieror.elwasys.raspiclient.devices.deconz;

public interface IDeconzDeviceStateEventListener {
    void onDeviceStateChanged(String uuid, boolean on);
}
