package org.kabieror.elwasys.webportal;

import org.kabieror.elwasys.common.ConfigurationManager;

import java.io.InputStream;

/**
 * Dieser Manager verwaltet die Konfiguration des Waschportals
 *
 * @author Oliver Kabierschke
 *
 */
public class WashportalConfiguration extends ConfigurationManager {

    private static final String DS = System.getProperty("file.separator");
    private static final String FILE_NAME = DS + "etc" + DS + "elwaportal" + DS + "elwaportal.properties";
    private static final String DEFAULTS_FILE_NAME = "/org/kabieror/elwasys/webportal/defaultconfig.properties";

    public WashportalConfiguration() throws Exception {
        super();
    }

    @Override
    public String getFileName() {
        return FILE_NAME;
    }

    @Override
    public InputStream getDefaultsFileStream() {
        return Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(DEFAULTS_FILE_NAME);
    }

    /**
     * Gibt das für den Login verwendete Passwort zurück
     *
     * @return Das für den Login verwendete Passwort
     */
    public String getAdministratorPassword() {
        return this.props.getProperty("admin.password");
    }

    /**
     * Gibt die Nummer des Ports zurück, auf welchem der Wartungsserver auf Verbindungen von Clients hören soll.
     *
     * @return Die Nummer des Wartungsports.
     */
    public int getMaintenancePort() {
        int res;
        try {
            res = Integer.parseInt(this.props.getProperty("maintenance.server.port"));
        } catch (NumberFormatException e) {
            return 3591;
        }
        return res;
    }
}
