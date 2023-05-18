package org.kabieror.elwasys.raspiclient.application;

/**
 * Diese Schnittstelle ermöglicht die Benachrichtigung beim Schließen-Ereignis
 * des Hauptfensters
 *
 * @author Oliver Kabierschke
 *
 */
public interface ICloseListener {
    /**
     * Wird aufgerufen, sobald das Haupfenster geschlossen wird
     */
    void onClose(boolean restart);
}
