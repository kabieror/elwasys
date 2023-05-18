package org.kabieror.elwasys.raspiclient.ui;

import javafx.scene.Node;

/**
 * Die Instanz einer Komponente mit dessen Controller.
 *
 * @author Oliver Kabierschke
 */
public class ComponentControlInstance<T> {
    private final Node component;
    private final T controller;

    public ComponentControlInstance(Node component, T controller) {
        this.component = component;
        this.controller = controller;
    }

    public Node getComponent() {
        return component;
    }

    public T getController() {
        return controller;
    }
}
