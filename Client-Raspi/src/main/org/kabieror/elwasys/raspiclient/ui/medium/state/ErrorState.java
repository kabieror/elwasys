package org.kabieror.elwasys.raspiclient.ui.medium.state;

/**
 * Diese Klasse repräsentiert einen anzuzeigenden Fehlerzustand.
 */
public class ErrorState {
    private String errorTitle;
    private String errorMessage;

    private Runnable backAction;
    private Runnable retryAction;

    public ErrorState(String errorTitle, String errorMessage) {
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
    }

    public ErrorState(String errorTitle, String errorMessage, Runnable backAction, Runnable retryAction) {
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
        this.backAction = backAction;
        this.retryAction = retryAction;
    }

    public String getErrorTitle() {
        return errorTitle;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Runnable getBackAction() {
        return backAction;
    }

    public Runnable getRetryAction() {
        return retryAction;
    }
}
