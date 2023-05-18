package org.kabieror.elwasys.webportal.events;

import org.kabieror.elwasys.common.Program;

/**
 * Dieses Interface erlaubt es, benachrichtigt zu werden, sobald ein Programm
 * erstellt oder verändert wurde.
 * 
 * @author Oliver Kabierschke
 *
 */
public interface IProgramUpdatedEventListener {
    /**
     * Wird aufgerufen, sobald ein Programm erstellt oder verändert wurde.
     * 
     * @param p
     *            Das veränderte oder neue Programm.
     */
    void onProgramUpdated(Program p);
}
