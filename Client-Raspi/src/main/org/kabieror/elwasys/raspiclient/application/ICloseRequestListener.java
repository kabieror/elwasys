package org.kabieror.elwasys.raspiclient.application;

import javafx.stage.WindowEvent;

/**
 * Dieses Interface erlaubt die Benachrichtigung im Fall einer Schließen-Anfrage
 * des Haupfensters
 * 
 * @author Oliver Kabierschke
 *
 */
public interface ICloseRequestListener {
    /**
     * Wird aufgerufen, sobald eine Schließen-Anfrage vom Haupfenster empfangen
     * wird
     * 
     * @param e
     *            Das zugehörige Window-Event
     */
    void onCloseRequest(WindowEvent e);
}
