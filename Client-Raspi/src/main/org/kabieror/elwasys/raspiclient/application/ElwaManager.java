package org.kabieror.elwasys.raspiclient.application;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.StringUtils;
import org.kabieror.elwasys.common.*;
import org.kabieror.elwasys.raspiclient.configuration.LocationManager;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.devices.FhemDevicePowerManager;
import org.kabieror.elwasys.raspiclient.devices.IDevicePowerManager;
import org.kabieror.elwasys.raspiclient.devices.IDeviceRegistrationService;
import org.kabieror.elwasys.raspiclient.devices.deconz.DeconzApiAdapter;
import org.kabieror.elwasys.raspiclient.devices.deconz.DeconzDevicePowerManager;
import org.kabieror.elwasys.raspiclient.devices.deconz.DeconzEventListener;
import org.kabieror.elwasys.raspiclient.executions.ExecutionManager;
import org.kabieror.elwasys.raspiclient.executions.FhemException;
import org.kabieror.elwasys.raspiclient.io.CardReader;
import org.kabieror.elwasys.raspiclient.ui.AbstractMainFormController;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Vector;

public class ElwaManager {

    public static final String APP_NAME = "elwaClient";
    public static final String VERSION = Utilities.APP_VERSION;
    public final static ElwaManager instance = new ElwaManager();

    /**
     * Logger
     */
    private final org.slf4j.Logger logger;

    /**
     * Die Utilities-Instanz.
     */
    private final Utilities utilities;

    private final MaintenanceServerManager maintenanceServerManager;

    /**
     * Listener
     */
    private final List<ICloseRequestListener> closeRequestListeners;
    private final List<ICloseListener> closeListeners;

    /**
     * Die Anbindung an die Datenbank.
     */
    private DataManager dataManager;

    /**
     * Der Manager für die Konfiguration
     */
    private WashguardConfiguration configurationManager;

    /**
     * Der Manager für Programmausführungen
     */
    private ExecutionManager executionManager;

    /**
     * Der Manager für das freigeben und abschalten des Stroms von verwalteten
     * Geräten
     */
    private IDevicePowerManager devicePowerManager;

    private IDeviceRegistrationService deviceRegistrationService;

    /**
     * Der Manager für die Registrierung auf einen Ort.
     */
    private LocationManager locationManager;

    /**
     * Der Controller für das Hauptformular
     */
    private AbstractMainFormController mainFormController;

    /**
     * Der Kartenleser
     */
    private CardReader cardReader;

    /**
     * Das Hauptfenster
     */
    private Stage primaryStage;

    /**
     * Der Ort, an dem dieser Client stationiert ist.
     */
    private Location thisLocation;

    private LocalDateTime startupTime = LocalDateTime.now();

    private ElwaManager() {
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.logger.info("----------------------------------------------------------------");
        this.logger.info("elwaClient " + VERSION);
        this.logger.info("Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        this.logger.info("Java Runtime Environment: " + System.getProperty("java.version"));
        final Runtime rtime = Runtime.getRuntime();
        this.logger.info("Processors: " + rtime.availableProcessors());
        this.logger.info("Memory: " + rtime.totalMemory());
        this.logger.info("Working directory: " + System.getProperty("user.dir"));
        this.logger.info("----------------------------------------------------------------");

        this.logger.info("Client is starting up");

        this.closeListeners = new Vector<ICloseListener>();
        this.closeRequestListeners = new Vector<ICloseRequestListener>();
        try {
            this.configurationManager = new WashguardConfiguration();
        } catch (final Exception e) {
            this.logger.error("Could not load configuration. Terminating.");
            System.exit(1);
        }
        this.utilities = new Utilities(this.configurationManager);

        // Starte Wartungs-Server
        this.maintenanceServerManager = new MaintenanceServerManager(this);
        this.maintenanceServerManager.start();
    }

    /**
     * Initiiert die Manager
     */
    public void initiate()
            throws ClassNotFoundException, SQLException, IOException, InterruptedException,
            LocationOccupiedException, FhemException, NoDataFoundException, AlreadyRunningException {
        DeconzEventListener deconzEventListener = null;
        try {
            this.logger.info("Starting up managers");
            SingleInstanceManager.instance.start(this.configurationManager.getSingleInstancePort());
            this.dataManager = new DataManager(this.configurationManager);
            this.locationManager = new LocationManager(this.configurationManager);

            // Lade Ort
            this.thisLocation = this.dataManager.getLocation(this.configurationManager.getLocationName());

            if (StringUtils.isNotBlank(this.configurationManager.getDeconzServer())) {
                this.logger.info("Using deCONZ as gateway.");
                var apiAdapter = new DeconzApiAdapter(this.configurationManager);
                deconzEventListener = new DeconzEventListener(this.configurationManager, apiAdapter);
                deconzEventListener.start();
                this.devicePowerManager = new DeconzDevicePowerManager(apiAdapter, deconzEventListener);
            } else if (StringUtils.isNotBlank(this.configurationManager.getFhemConnectionString())) {
                this.logger.info("Using fhem as gateway.");
                this.devicePowerManager = new FhemDevicePowerManager(this.configurationManager);
            } else {
                this.logger.error("Application configuration is invalid. Could not find any device power gateway to use. " +
                        "You must either provide a value for deconz.server or fhem.server");
                System.exit(1);
            }
            this.executionManager = new ExecutionManager(this.devicePowerManager);
            this.mainFormController.initiate();

            // Setze unterbrochene Ausführungen fort
            for (Device d : this.dataManager.getDevices()) {
                Execution e = this.dataManager.getRunningExecution(d);
                if (e != null) {
                    // Unterbrochene Ausführung gefunden
                    this.executionManager.startExecution(e);
                }
            }
        } catch (Exception e) {
            if (deconzEventListener != null) {
                deconzEventListener.stop();
            }
        }
    }

    /**
     * Gibt den Konfigurationsmanager zurück
     *
     * @return Den Konfigurationsmanager
     */
    public WashguardConfiguration getConfigurationManager() {
        return this.configurationManager;
    }

    /**
     * Gibt den Controller des Hauptformulars zurück
     *
     * @return Den Controller des Haupformulars
     */
    public AbstractMainFormController getMainFormController() {
        return this.mainFormController;
    }

    /**
     * Setzt den Controller des Haupformulars
     *
     * @param c Der Controller des Haupformulars
     */
    public void setMainFormController(AbstractMainFormController c) {
        this.mainFormController = c;
    }

    /**
     * Gibt den Kartenleser zurück
     *
     * @return Den Kartenleser
     */
    public CardReader getCardReader() {
        return this.cardReader;
    }

    /**
     * Gib die Anbindung an die Datenbank zurück.
     *
     * @return Die Anbindung an die Datenbank.
     */
    public DataManager getDataRetriever() {
        return this.dataManager;
    }

    /**
     * Gibt den Ausführungsmanager zurück.
     *
     * @return Den Ausführungsmanager.
     */
    public ExecutionManager getExecutionManager() {
        return this.executionManager;
    }

    public IDeviceRegistrationService getDeviceRegistrationService() {
        return this.deviceRegistrationService;
    }

    /**
     * Gibt die Utilities-Instanz zurück.
     *
     * @return Die Utilities-Instanz.
     */
    public Utilities getUtilities() {
        return this.utilities;
    }

    /**
     * Gibt das Hauptfenster zurück
     *
     * @return Das Hauptfenster
     */
    public Stage getPrimaryStage() {
        return this.primaryStage;
    }

    /**
     * Registriert einen neuen Interessenten am Schließen-Event des Haupfensters
     *
     * @param l Der Interessent am Schließen-Event des Hauptfensters
     */
    public void listenToCloseRequest(ICloseRequestListener l) {
        this.closeRequestListeners.add(l);
    }

    /**
     * Registriert einen neuen Interessenten an Schließen-Anfragen des
     * Haupfensters
     *
     * @param l Der Interessent an Schließen-Anfragen des Haupfensters
     */
    public void listenToCloseEvent(ICloseListener l) {
        this.closeListeners.add(l);
    }

    /**
     * Wird aufgerufen, sobald ein Hauptfenster verfügbar ist
     *
     * @param primaryStage Das Hauptfenster
     */
    protected void onPrimaryStageStart(Stage primaryStage) {
        this.primaryStage = primaryStage;
        if (this.cardReader == null) {
            this.cardReader = new CardReader(primaryStage);
        }
    }

    /**
     * Eine Anfrage zum Schließen des Hauptfensters behandeln
     *
     * @param e Das Window-Event
     */
    protected void onCloseRequest(WindowEvent e) {
        this.logger.debug("Processing close request");
        for (final ICloseRequestListener l : this.closeRequestListeners) {
            l.onCloseRequest(e);
            if (e.isConsumed()) {
                // Breche Ausführung ab, wenn das Event abgefangen wurde und das
                // Schließen so verhindert wird
                return;
            }
        }

        // Close-Request wurde nicht abgebrochen. Informiere über das Beenden
        // der Anwendung.
        this.onClose(false);
    }

    /**
     * Wird aufgerufen, sobald die Anwendung beendet werden soll. Informiert
     * alle Manager über das Ende der Anwendung.
     */
    public void onClose(boolean restart) {
        this.logger.info("Application is terminating now.");
        for (final ICloseListener l : this.closeListeners) {
            l.onClose(restart);
        }
    }

    /**
     * Startet alle Manager neu.
     */
    public void restart() {
        this.onClose(true);
        this.mainFormController.initialize(null, null);
    }

    /**
     * Gibt den Ort zurück, an dem dieser Client stationiert ist.
     */
    public Location getLocation() throws SQLException, NoDataFoundException {
        thisLocation.update();
        return thisLocation;
    }

    /**
     * Gibt alle Geräte zurück, die von diesem Client verwaltet werden sollen.
     */
    public List<Device> getManagedDevices() throws SQLException, NoDataFoundException {
        return this.dataManager.getDevicesToDisplay(this.getLocation());
    }

    /**
     * Die Zeit des Starts der Anwendung.
     *
     * @return
     */
    public LocalDateTime getStartupTime() {
        return startupTime;
    }
}
