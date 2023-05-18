package org.kabieror.elwasys.raspiclient.executions;

import org.kabieror.elwasys.common.Execution;

/**
 * Diese Schnittstelle wird beim Ende einer Ausführung angewendet
 * 
 * @author Oliver Kabierschke
 *
 */
public interface IExecutionFinishedListener {
    /**
     * Wird aufgerufen, sobald die Ausführung e beendet ist.
     * 
     * @param e
     *            Die beendete Ausführung
     */
    void onExecutionFinished(Execution e);
}
