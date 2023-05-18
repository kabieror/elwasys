package org.kabieror.elwasys.raspiclient.ui.medium.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.kabieror.elwasys.raspiclient.application.ElwaManager;

/**
 * Controller der Copyright-Bar
 *
 * @author Oliver Kabierschke
 */
public class CopyrightBarController {
    private StringProperty versionText = new SimpleStringProperty(ElwaManager.APP_NAME + " " + ElwaManager.VERSION);

    public String getVersionText() {
        return versionText.get();
    }

    public StringProperty versionTextProperty() {
        return versionText;
    }
}
