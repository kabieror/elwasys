package org.kabieror.elwasys.webportal.events;

import org.kabieror.elwasys.common.User;

/**
 * Dieses Interface erlaubt es, benachrichtigt zu werden, sobald ein Benutzer
 * verändert oder erstellt wurde.
 * 
 * @author Oliver Kabierschke
 *
 */
public interface IUserUpdatedEventListener {
    /**
     * Wird aufgerufen, sobald ein Benutzer verändert bzw. erstellt wurde.
     * 
     * @param u
     *            Der veränderte oder neue Benutzer.
     */
    void onUserUpdated(User u);
}
