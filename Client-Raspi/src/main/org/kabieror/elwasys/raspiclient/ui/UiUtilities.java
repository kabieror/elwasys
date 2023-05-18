package org.kabieror.elwasys.raspiclient.ui;

import javafx.scene.Node;

/**
 * Hilfsmethoden für die Arbeit mit der Benutzeroberfläche
 *
 * @author Oliver Kabierschke
 */
public class UiUtilities {
    /**
     * Setzt oder entfernt eine Style-Klasse
     */
    public static void setStyleClass(Node n, String styleClass, boolean set) {
        boolean contains = n.getStyleClass().contains(styleClass);
        if (set && !contains) {
            n.getStyleClass().add(styleClass);
        } else if (!set && contains) {
            n.getStyleClass().remove(styleClass);
        }
    }
}
