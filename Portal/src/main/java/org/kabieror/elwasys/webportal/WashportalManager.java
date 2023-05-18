package org.kabieror.elwasys.webportal;

import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;
import org.kabieror.elwasys.common.DataManager;
import org.kabieror.elwasys.common.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Dieser Manager verbindet alle anderen Teile der Anwendung und hält u.a.
 * Datenbankverbindung und Konfiguration.
 *
 * @author Oliver Kabierschke
 *
 */
public class WashportalManager {
    public static final String VERSION = Utilities.APP_VERSION;

    /**
     * Die Instanz des Managers
     */
    public static WashportalManager instance = new WashportalManager();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private WashportalConfiguration configurationManager;

    private DataManager dataManager;

    private SessionManager sessionManager;

    private MaintenanceConnectionManager maintenanceConnectionManager;

    private WashportalUtilities utilities;

    private WashportalManager() {
        this.logger.info("----------------------------------------------------------------");
        this.logger.info("WASHPORTAL " + WashportalManager.VERSION);
        this.logger.info("Operating System: " + System.getProperty("os.name") + " "
                + System.getProperty("os.version"));
        this.logger.info("Java Runtime Environment: " + System.getProperty("java.version"));
        final Runtime rtime = Runtime.getRuntime();
        this.logger.info("Processors: " + rtime.availableProcessors());
        this.logger.info("Memory: " + rtime.totalMemory());
        this.logger.info("Working directory: " + System.getProperty("user.dir"));
        this.logger.info("----------------------------------------------------------------");

        this.logger.info("elwasys client is starting up");
    }

    /**
     * Iniziiert die notwendigen Manager, falls noch nicht geschehen
     */
    protected void initIfNecessary() {
        if (this.configurationManager != null) {
            // Bereits initiiert
            return;
        }
        this.logger.info("Initiating managers");
        try {
            this.configurationManager = new WashportalConfiguration();
            this.utilities = new WashportalUtilities(this.configurationManager);
        } catch (final Exception e) {
            this.logger.error("Could not load the configuration.", e);
            System.exit(1);
        }
        try {
            this.dataManager = new DataManager(this.configurationManager);
        } catch (final ClassNotFoundException e) {
            this.logger.error("Cannot create data manager.", e);
            System.exit(1);
        }
        try {
            this.maintenanceConnectionManager = new MaintenanceConnectionManager();
        } catch (IOException e) {
            this.logger.error("Cannot start Maintenance Server.", e);
        }
        this.sessionManager = new SessionManager();
        this.logger.debug("Managers initiated");
    }

    public WashportalConfiguration getConfigurationManager() {
        return this.configurationManager;
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    public SessionManager getSessionManager() {
        return this.sessionManager;
    }

    public MaintenanceConnectionManager getMaintenanceConnectionManager() {
        return this.maintenanceConnectionManager;
    }

    public WashportalUtilities getUtilities() {
        return this.utilities;
    }

    /**
     * Zeigt einen Datenbankfehler an
     *
     * @param e
     *            Der Datenbankfehler
     */
    public void showDatabaseError(SQLException e) {
        final Notification msg = new Notification("Datenbankfehler", e.getLocalizedMessage());
        msg.setDelayMsec(-1);
        msg.setPosition(Position.TOP_RIGHT);
        msg.setStyleName("bar failure closable");
        msg.show(Page.getCurrent());
    }

    /**
     * Zeigt einen allgemeinen Fehler an
     *
     * @param title
     *            Der Titel der Fehlermeldung
     * @param description
     *            Die Beschreibung
     */
    public void showError(String title, String description) {
        final Notification msg = new Notification(title, description);
        msg.setHtmlContentAllowed(true);
        msg.setDelayMsec(-1);
        msg.setPosition(Position.TOP_RIGHT);
        msg.setStyleName("bar error closable");
        msg.show(Page.getCurrent());
    }

    /**
     * Zeigt einen Fehler an.
     *
     * @param e1
     *            Der anzuzeigende Fehler.
     */
    public void showError(Exception e1) {
        this.showError("Interner Fehler", e1.getClass().getName() + ": " + e1.getMessage()
                + "\nFür mehr Informationen bitte die Log-Datei prüfen.");
    }

    /**
     * Zeigt eine Information an.
     *
     * @param title
     * @param message
     */
    public void showSuccessMessage(String title, String message) {
        final Notification msg = new Notification(title, message);
        msg.setHtmlContentAllowed(true);
        msg.setDelayMsec(3000);
        msg.setPosition(Position.BOTTOM_CENTER);
        msg.setStyleName("bar success closable");
        msg.show(Page.getCurrent());
    }
}
