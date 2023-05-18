package org.kabieror.elwasys.raspiclient.executions;

import org.kabieror.elwasys.common.Execution;

/**
 * Diese Schnittstelle wird beim Start einer Programmausführung angewendet.
 *
 * @author Oliver Kabierschke
 */
public interface IExecutionStartedListener {
    /**
     * Wird aufgerufen, sobald eine Programmausführung gestartet wurde.
     *
     * @param e Die gestartete Programmausführung.
     */
    void onExecutionStarted(Execution e);
}
