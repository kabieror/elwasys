package org.kabieror.elwasys.raspiclient.ui;

/**
 * Definiert einen Zustandsübergang des Hauptfensters
 *
 * @author Oliver Kabierschke
 */
public class MainFormStateTransition {
    private final MainFormState from;
    private final MainFormState to;

    public MainFormStateTransition(MainFormState from, MainFormState to) {
        this.from = from;
        this.to = to;
    }

    protected MainFormState getFrom() {
        return this.from;
    }

    protected MainFormState getTo() {
        return this.to;
    }

    @Override
    public int hashCode() {
        return this.from.hashCode() + this.to.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MainFormStateTransition)) {
            return false;
        }
        final MainFormStateTransition other = (MainFormStateTransition) o;
        return this.from.equals(other.from) && this.to.equals(other.to);
    }
}
