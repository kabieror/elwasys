package org.kabieror.elwasys.raspiclient.application.fhemsimulator;

import java.util.concurrent.TimeUnit;

/**
 * TODO: Describe
 *
 * @author Oliver Kabierschke
 */
public class SwitchDevice extends SimulatedDevice {

    int communicationDelay = 30;

    boolean isSwitchable = true;

    public SwitchDevice() {
        this.params.put("state", "off");
    }

    public void switchOn() {
        if (isSwitchable) {
            this.params.put("state", "set_on");
            FhemSimulator.scheduler
                    .schedule(() -> this.params.put("state", "on"), communicationDelay, TimeUnit.MILLISECONDS);
        }
    }

    public void switchOff() {
        if (isSwitchable) {
            this.params.put("state", "set_off");
            FhemSimulator.scheduler
                    .schedule(() -> this.params.put("state", "off"), communicationDelay, TimeUnit.MILLISECONDS);
        }
    }

}
