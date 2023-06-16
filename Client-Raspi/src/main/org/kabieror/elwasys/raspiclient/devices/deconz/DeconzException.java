package org.kabieror.elwasys.raspiclient.devices.deconz;

import java.io.IOException;

public class DeconzException extends IOException {
    public DeconzException() {
    }

    public DeconzException(String message) {
        super(message);
    }

    public DeconzException(String message, Throwable cause) {
        super(message, cause);
    }
}
