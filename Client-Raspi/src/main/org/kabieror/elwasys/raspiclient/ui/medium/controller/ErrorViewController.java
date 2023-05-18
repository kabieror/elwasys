package org.kabieror.elwasys.raspiclient.ui.medium.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.kabieror.elwasys.raspiclient.ui.medium.IViewController;
import org.kabieror.elwasys.raspiclient.ui.medium.MainFormController;
import org.kabieror.elwasys.raspiclient.ui.medium.state.ErrorState;
import org.kabieror.elwasys.raspiclient.ui.medium.state.ToolbarState;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller für die Fehlerseite
 */
public class ErrorViewController implements Initializable, IViewController {

    private MainFormController mfc;

    private ToolbarState toolbarState;

    @FXML
    private Pane errorPane;

    @FXML
    private Label errorTitle;
    @FXML
    private Label errorDetail;

    /**
     * Initialisiert die Komponenten nachdem die Oberfläche dieser Komponente geladen ist
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    /**
     * Wird aufgerufen, sobald alle Manager geladen sind und die Benutzeroberfläche mit Daten befüllt werden soll.
     *
     * @param mfc Der Haupt-Controller.
     */
    public void onStart(MainFormController mfc) {
        this.mfc = mfc;
    }

    @Override
    public void onTerminate() {

    }

    @Override
    public void onActivate() {
        ErrorState err = mfc.getErrorState();
        this.errorTitle.setText(err.getErrorTitle());
        this.errorDetail.setText(err.getErrorMessage());

        String bTxt = null;
        String fTxt = null;
        Runnable bAct = null;
        Runnable fAct = null;
        if (err.getBackAction() != null) {
            bTxt = "Zurück";
            bAct = () -> err.getBackAction().run();
        }
        if (err.getRetryAction() != null) {
            fTxt = "Wiederholen";
            fAct = () -> err.getRetryAction().run();
        }
        this.toolbarState = new ToolbarState(bTxt, fTxt, bAct, fAct);
        this.toolbarState.setUserButtonDisabled(true);

        this.errorPane.setVisible(true);
    }

    @Override
    public void onDeactivate() {
        this.errorPane.setVisible(false);
    }

    @Override
    public void onReturnFromError() {

    }

    @Override
    public ToolbarState getToolbarState() {
        return this.toolbarState;
    }
}
