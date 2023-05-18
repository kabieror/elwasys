package org.kabieror.elwasys.raspiclient.application;

/**
 * This exception is thrown if another instance of this application is already running
 */
public class AlreadyRunningException extends Exception {

    public AlreadyRunningException() {
        super();
    }

}
