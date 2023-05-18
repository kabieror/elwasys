package org.kabieror.elwasys.webportal.events;

import org.kabieror.elwasys.common.Location;

/**
 * Dieses Interface erlaubt es, benachrichtigt zu werden, sobald ein Standort
 * erstellt oder verändert wurde.
 *
 * @author Oliver Kabierschke
 */
public interface ILocationUpdatedEventListener {
    /**
     * Wird aufgerufen, sobald ein Standort erstellt oder verändert wurde.
     *
     * @param l Der veränderte oder neue Standort.
     */
    void onLocationUpdated(Location l);
}
