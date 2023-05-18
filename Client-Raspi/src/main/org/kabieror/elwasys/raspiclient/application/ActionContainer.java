package org.kabieror.elwasys.raspiclient.application;

/**
 * Dieser Container beinhaltet eine ausführbare Aktion.
 * 
 * @author Oliver Kabierschke
 *
 */
public class ActionContainer {
    Runnable r;

    public ActionContainer(Runnable r) {
	this.r = r;
    }

    public ActionContainer() {

    }

    public Runnable getAction() {
	return this.r;
    }

    public void setAction(Runnable r) {
        this.r = r;
    }
}
