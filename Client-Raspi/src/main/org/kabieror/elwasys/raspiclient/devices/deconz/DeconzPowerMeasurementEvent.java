package org.kabieror.elwasys.raspiclient.devices.deconz;

import java.util.Objects;

final class DeconzPowerMeasurementEvent {
    private DeconzChangeType e;
    private int id;
    private String r;
    private DeconzPowerMeasurementState state;

    public DeconzPowerMeasurementEvent(
            DeconzChangeType e,
            int id,
            String r,
            DeconzPowerMeasurementState state) {
        this.e = e;
        this.id = id;
        this.r = r;
        this.state = state;
    }

    public DeconzChangeType e() {
        return e;
    }

    public int id() {
        return id;
    }

    public String r() {
        return r;
    }

    public DeconzPowerMeasurementState state() {
        return state;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DeconzPowerMeasurementEvent) obj;
        return Objects.equals(this.e, that.e) &&
                this.id == that.id &&
                Objects.equals(this.r, that.r) &&
                Objects.equals(this.state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(e, id, r, state);
    }

    @Override
    public String toString() {
        return "DeconzPowerMeasurementEvent[" +
                "e=" + e + ", " +
                "id=" + id + ", " +
                "r=" + r + ", " +
                "state=" + state + ']';
    }


}
