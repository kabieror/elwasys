package org.kabieror.elwasys.raspiclient.ui.scheduler;

/**
 * TODO: Describe
 *
 * @author Oliver Kabierschke
 */
public interface IInactivityJobDoneListener {
    /**
     * Wird aufgerufen, sobald ein Job nach Inaktivität ausgeführt wurde.
     *
     * @param future Die Statusanzeige des Jobs.
     */
    void onInactivityJobDone(InactivityFuture future);
}
