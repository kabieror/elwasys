package org.kabieror.elwasys.raspiclient.executions;

import org.kabieror.elwasys.common.Execution;

/**
 * Diese Schnittstelle wird beim Ende einer Ausführung angewendet
 * 
 * @author Oliver Kabierschke
 *
 */
public interface IExecutionErrorListener {
    /**
     * Wird aufgerufen, sobald eine Ausführung fehlgeschlagen ist.
     * 
     * @param execution
     *            Die beendete Ausführung
     * @param exception
     *            Die Ausnahme, welche bei der Ausführung aufgetreten ist.
     */
    void onExecutionFailed(Execution execution, Exception exception);
}
