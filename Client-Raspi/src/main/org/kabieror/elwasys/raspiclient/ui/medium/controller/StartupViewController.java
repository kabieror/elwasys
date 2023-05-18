package org.kabieror.elwasys.raspiclient.ui.medium.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.kabieror.elwasys.raspiclient.ui.medium.IViewController;
import org.kabieror.elwasys.raspiclient.ui.medium.MainFormController;
import org.kabieror.elwasys.raspiclient.ui.medium.state.ToolbarState;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller für die Start-Seite
 */
public class StartupViewController implements Initializable, IViewController {

    private ToolbarState toolbarState = new ToolbarState(null, null, null, null);

    @FXML
    private Pane startupPane;

    @FXML
    private Label versionLabel;

    /**
     * Initialisiert die Komponenten nachdem die Oberfläche dieser Komponente geladen ist
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    /**
     * Wird aufgerufen, sobald alle Manager geladen sind und die Benutzeroberfläche mit Daten befüllt werden soll.
     *
     * @param mainFormController Der Haupt-Controller.
     */
    public void onStart(MainFormController mainFormController) {
        this.toolbarState.setUserButtonDisabled(true);
    }

    @Override
    public void onTerminate() {

    }

    @Override
    public void onActivate() {
        this.startupPane.setVisible(true);
    }

    @Override
    public void onDeactivate() {
        this.startupPane.setVisible(false);
    }

    @Override
    public void onReturnFromError() {

    }

    @Override
    public ToolbarState getToolbarState() {
        return this.toolbarState;
    }
}
