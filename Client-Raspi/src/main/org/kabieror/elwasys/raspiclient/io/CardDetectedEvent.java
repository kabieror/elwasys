package org.kabieror.elwasys.raspiclient.io;

/**
 * Dieses Ereignis wird beim Erkennen einer Karte geworfen
 * 
 * @author Oliver Kabierschke
 *
 */
public class CardDetectedEvent {
    private final String cardId;

    public CardDetectedEvent(String cardId) {
	this.cardId = cardId;
    }

    /**
     * Gibt die Id der erkannten Karte zurück
     * 
     * @return Die Id der erkannten Karte
     */
    public String getCardId() {
	return this.cardId;
    }
}
