package org.kabieror.elwasys.raspiclient.ui.medium.state;

/**
 * Diese Klasse schreibt der Toolbar einen Zustand vor.
 */
public class ToolbarState {

    private final Runnable backAction;
    private final Runnable forwardAction;
    private boolean backOption;
    private boolean forwardOption;
    private boolean backOptionDisabled = false;
    private boolean forwardOptionDisabled = false;
    private String backText;
    private String forwardText;

    private boolean userButtonDisabled = false;

    public ToolbarState(String backText, String forwardText, Runnable backAction, Runnable forwardAction) {
        this.backOption = backText != null;
        this.forwardOption = forwardText != null;
        this.backText = backText != null ? backText : "Zurück";
        this.forwardText = forwardText != null ? forwardText : "";
        this.backAction = backAction;
        this.forwardAction = forwardAction;
    }

    public ToolbarState(String backText, String forwardText, Runnable backAction, Runnable forwardAction,
                        boolean backOptionDisabled, boolean forwardOptionDisabled) {
        this.backOption = backText != null;
        this.forwardOption = forwardText != null;
        this.backText = backText != null ? backText : "Zurück";
        this.forwardText = forwardText != null ? forwardText : "";
        this.backAction = backAction;
        this.forwardAction = forwardAction;
        this.backOptionDisabled = backOptionDisabled;
        this.forwardOptionDisabled = forwardOptionDisabled;
    }

    public boolean hasBackOption() {
        return backOption;
    }

    public boolean hasForwardOption() {
        return forwardOption;
    }

    public String getBackText() {
        return backText;
    }

    public String getForwardText() {
        return forwardText;
    }

    public boolean isBackOptionDisabled() {
        return backOptionDisabled;
    }

    public boolean isForwardOptionDisabled() {
        return forwardOptionDisabled;
    }

    public Runnable getBackAction() {
        return backAction;
    }

    public Runnable getForwardAction() {
        return forwardAction;
    }

    public boolean isUserButtonDisabled() {
        return userButtonDisabled;
    }

    public void setUserButtonDisabled(boolean userButtonDisabled) {
        this.userButtonDisabled = userButtonDisabled;
    }
}