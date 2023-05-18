package org.kabieror.elwasys.raspiclient.ui;

/**
 * Ein MainFormStateManager verwaltet Zustandsübergänge vom Hauptfenster
 *
 * @author Oliver Kabierschke
 */
public interface IMainFormStateManager {
    /**
     * Gibt den aktuellen Zustand des Hauptformulars zurück
     *
     * @return Den aktuellen Zustand des Hauptformulars
     */
    MainFormState getState();

    /**
     * Führt eine Zustandsänderung des Hauptformulars durch, falls erlaubt
     *
     * @param newState Neuer Zustand
     * @throws IllegalStateException
     */
    void gotoState(MainFormState newState);

    /**
     * Geht zum Zustand zurück, der vor dem aufgetretenen Fehler vorherrschte
     */
    void gotoStateBeforeError();

    /**
     * Geht nach einem Fehler zum Startzustand
     */
    void resetAfterError();
}
