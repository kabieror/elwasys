package org.kabieror.elwasys.raspiclient.ui.medium.controller;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.raspiclient.ui.medium.IViewController;
import org.kabieror.elwasys.raspiclient.ui.medium.MainFormController;
import org.kabieror.elwasys.raspiclient.ui.medium.state.ToolbarState;

/**
 * Controller für die Info-Seite über die elwaApp.
 *
 * @author Oliver Kabierschke
 */
public class AppInfoViewController implements IViewController {

    public Node mainPane;
    public WebView webView;
    private MainFormController mainFormController;
    private ChangeListener<User> userChangedListener = (observable, oldValue, newValue) -> {
        this.mainFormController.hideAppAdvertisement();
    };
    private ToolbarState toolbarState =
            new ToolbarState("Zurück", null, () -> this.mainFormController.hideAppAdvertisement(), null);

    @Override
    public void onStart(MainFormController mfc) {
        this.mainFormController = mfc;
        this.webView.setContextMenuEnabled(false);
    }

    @Override
    public void onTerminate() {

    }

    @Override
    public void onActivate() {
        this.mainPane.setVisible(true);
        this.mainFormController.registeredUserProperty().addListener(userChangedListener);
        WebEngine engine = this.webView.getEngine();
        engine.load("https://www.elwasys.de/ads/elwa-app");
        engine.reload();
    }

    @Override
    public void onDeactivate() {
        this.mainFormController.registeredUserProperty().removeListener(userChangedListener);
        this.mainPane.setVisible(false);
    }

    @Override
    public void onReturnFromError() {

    }

    @Override
    public ToolbarState getToolbarState() {
        return toolbarState;
    }
}

