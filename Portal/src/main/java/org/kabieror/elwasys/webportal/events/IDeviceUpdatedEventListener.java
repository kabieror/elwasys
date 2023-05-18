package org.kabieror.elwasys.webportal.events;

import org.kabieror.elwasys.common.Device;

/**
 * Dieses Interface erlaubt es, benachrichtigt zu werden, sobald ein Gerät
 * erstellt oder verändert wurde.
 * 
 * @author Oliver Kabierschke
 *
 */
public interface IDeviceUpdatedEventListener {
    /**
     * Wird aufgerufen, sobald ein Gerät erstellt oder verändert wurde.
     * 
     * @param d
     *            Das veränderte oder neue Gerät.
     */
    void onDeviceUpdated(Device d);
}
