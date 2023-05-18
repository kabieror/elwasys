package org.kabieror.elwasys.raspiclient.ui.medium;

import org.kabieror.elwasys.raspiclient.ui.medium.state.ToolbarState;

/**
 * Schreibt Methoden vor, die Controller von Seiten, welche im Hauptfenster angezeigt werden, implementieren müssen.
 */
public interface IViewController {
    void onStart(MainFormController mfc);

    void onTerminate();

    void onActivate();

    void onDeactivate();

    void onReturnFromError();

    ToolbarState getToolbarState();
}
