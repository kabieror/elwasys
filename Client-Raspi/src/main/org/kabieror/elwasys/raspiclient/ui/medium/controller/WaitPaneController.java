package org.kabieror.elwasys.raspiclient.ui.medium.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import org.kabieror.elwasys.raspiclient.ui.medium.IViewController;
import org.kabieror.elwasys.raspiclient.ui.medium.MainFormController;
import org.kabieror.elwasys.raspiclient.ui.medium.state.ToolbarState;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller für die Warte-Seite
 */
public class WaitPaneController implements Initializable, IViewController {
    private ToolbarState toolbarState = new ToolbarState(null, null, null, null);

    @FXML
    private Pane waitPane;
    private ScheduledFuture<?> runningWaitPaneDelay;
    private MainFormController mainFormController;

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
        this.mainFormController = mainFormController;
    }

    @Override
    public void onTerminate() {

    }

    @Override
    public void onActivate() {
        this.waitPane.setVisible(true);
        this.waitPane.setOpacity(0);

        if (this.runningWaitPaneDelay != null && !this.runningWaitPaneDelay.isDone()) {
            this.runningWaitPaneDelay.cancel(false);
        }
        this.runningWaitPaneDelay = this.mainFormController.getUpdateService().scheduleAtFixedRate(() -> {
            double opa = this.waitPane.getOpacity();
            Platform.runLater(() -> this.waitPane.setOpacity(opa + 0.02));
            if (opa >= 0.85) {
                this.runningWaitPaneDelay.cancel(false);
            }
        }, 20, 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onDeactivate() {
        this.waitPane.setVisible(false);
        if (this.runningWaitPaneDelay != null && !this.runningWaitPaneDelay.isDone()) {
            this.runningWaitPaneDelay.cancel(false);
        }
    }

    @Override
    public void onReturnFromError() {

    }

    @Override
    public ToolbarState getToolbarState() {
        return toolbarState;
    }
}
