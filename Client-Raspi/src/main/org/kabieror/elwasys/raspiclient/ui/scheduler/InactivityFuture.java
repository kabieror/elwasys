package org.kabieror.elwasys.raspiclient.ui.scheduler;

/**
 * Eine geplante Ausführung eines Jobs.
 *
 * @author Oliver Kabierschke
 */
public class InactivityFuture {

    private boolean done;
    private boolean canceled;
    private String name;

    /**
     * Bricht die Ausführung des Jobs ab, falls diese noch nicht bereits gestartet wurde.
     */
    public void cancel() {
        synchronized (this) {
            this.canceled = true;
        }
    }

    /**
     * Ermittelt, ob der zugehörige Job abgearbeitet wurde.
     */
    public boolean isDone() {
        synchronized (this) {
            return done;
        }
    }

    /**
     * Ermittelt, ob der zugehörige Job abgebrochen wurde.
     */
    public boolean isCancelled() {
        synchronized (this) {
            return canceled;
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    void setDone() {
        this.done = true;
    }
}
