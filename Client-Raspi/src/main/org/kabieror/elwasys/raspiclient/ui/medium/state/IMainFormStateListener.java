package org.kabieror.elwasys.raspiclient.ui.medium.state;

import org.kabieror.elwasys.raspiclient.ui.MainFormState;

/**
 * Listener, welche diese Schnittstelle implementieren, werden benachrichtigt, sobald sich der Zustand des Hauptfensters geändert hat.
 *
 * @author Oliver Kabierschke
 */
public interface IMainFormStateListener {
    void onMainFormStateChanged(MainFormState oldState, MainFormState newState);
}
