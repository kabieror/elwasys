package org.kabieror.elwasys.webportal;

import org.kabieror.elwasys.common.Location;
import org.kabieror.elwasys.common.maintenance.IClientConnection;
import org.kabieror.elwasys.common.maintenance.MaintenanceServer;

import java.io.IOException;

/**
 * Dieser Manager hält Verbindungen zu Client-Instanzen an Standorten.
 *
 * @author Oliver Kabierschke
 *
 */
public class MaintenanceConnectionManager {
    MaintenanceServer server;

    public MaintenanceConnectionManager() throws IOException {
        server =
                new MaintenanceServer(WashportalManager.instance.getConfigurationManager().getMaintenancePort(), 50000);
    }

    /**
     * Gibt den Wartungs-Zugang zum Client an einem gegebenen Standort zurück.
     *
     * @param location
     *            Der Standort, dessen Wartungs-Zugang gesucht ist.
     * @return Der Wartzungs-Zugang zum gegebenen Standort.
     */
    public IClientConnection getClient(Location location) {
        return this.server.getClientConnection(location.getName());
    }
}
