package org.kabieror.elwasys.raspiclient.io;

/**
 * Dieser Listener wird benachrichtigt, sobald eine Karte erkannt wird
 * 
 * @author Oliver Kabierschke
 *
 */
public interface ICardDetectedEventListener {
    /**
     * Wird aufgerufen, sobald eine Karte erkannt wird
     */
    void onCardDetected(CardDetectedEvent e);
}
