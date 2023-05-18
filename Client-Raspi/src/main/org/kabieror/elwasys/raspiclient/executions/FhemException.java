package org.kabieror.elwasys.raspiclient.executions;

/**
 * Fehler bei der Kommunikation zum Heimautomatisierungs-Server.
 *
 * @author Oliver Kabierschke
 */
public class FhemException extends Exception {
    public FhemException(String message) {
        super(message);
    }

    public FhemException(String message, Exception inner) {
        super(message, inner);
    }
}
