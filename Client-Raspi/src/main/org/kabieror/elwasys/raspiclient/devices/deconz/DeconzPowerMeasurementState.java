package org.kabieror.elwasys.raspiclient.devices.deconz;

import java.util.Objects;

final class DeconzPowerMeasurementState {
    private Double current;
    private Double power;
    private Double voltage;

    public DeconzPowerMeasurementState(Double current, Double power, Double voltage) {
        this.current = current;
        this.power = power;
        this.voltage = voltage;
    }

    public Double current() {
        return current;
    }

    public Double power() {
        return power;
    }

    public Double voltage() {
        return voltage;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DeconzPowerMeasurementState) obj;
        return Objects.equals(this.current, that.current) &&
                Objects.equals(this.power, that.power) &&
                Objects.equals(this.voltage, that.voltage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(current, power, voltage);
    }

    @Override
    public String toString() {
        return "DeconzPowerMeasurementState[" +
                "current=" + current + ", " +
                "power=" + power + ", " +
                "voltage=" + voltage + ']';
    }


}
