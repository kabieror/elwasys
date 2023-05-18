package org.kabieror.elwasys.raspiclient.application.fhemsimulator;

import java.util.HashMap;
import java.util.Map;

/**
 * Ein simuliertes Gerät
 *
 * @author Oliver Kabierschke
 */
public abstract class SimulatedDevice {

    protected Map<String, String> params = new HashMap<>();

    public String getParameterValue(String parameter) {
        return params.get(parameter);
    }
}
