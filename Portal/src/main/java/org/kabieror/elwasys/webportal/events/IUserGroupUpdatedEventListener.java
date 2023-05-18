package org.kabieror.elwasys.webportal.events;

import org.kabieror.elwasys.common.UserGroup;

/**
 * Dieses Interface erlaubt es, benachrichtigt zu werden, sobald eine Benutzergruppe
 * verändert oder erstellt wurde.
 *
 * @author Oliver Kabierschke
 */
public interface IUserGroupUpdatedEventListener {
    /**
     * Wird aufgerufen, sobald eine Benutzergruppe verändert bzw. erstellt wurde.
     *
     * @param g Die veränderte oder neue Benutzergruppe.
     */
    void onUserGroupUpdated(UserGroup g);
}
