package org.kabieror.elwasys.raspiclient.ui.medium.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.kabieror.elwasys.common.*;
import org.kabieror.elwasys.raspiclient.application.ActionContainer;
import org.kabieror.elwasys.raspiclient.application.ElwaManager;
import org.kabieror.elwasys.raspiclient.executions.FhemException;
import org.kabieror.elwasys.raspiclient.ui.ComponentControlInstance;
import org.kabieror.elwasys.raspiclient.ui.MainFormState;
import org.kabieror.elwasys.raspiclient.ui.medium.IViewController;
import org.kabieror.elwasys.raspiclient.ui.medium.MainFormController;
import org.kabieror.elwasys.raspiclient.ui.medium.state.ToolbarState;
import org.kabieror.elwasys.raspiclient.ui.scheduler.BacklightManager;
import org.kabieror.elwasys.raspiclient.ui.scheduler.InactivityFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Controller für die Geräteauswahl-Seite.
 */
public class DeviceViewController implements Initializable, IViewController, BacklightManager.LightOnEventListener {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MainFormController mainFormController;

    private Map<Device, ComponentControlInstance<DeviceListEntry>> devices = new HashMap<>();

    @FXML
    private Pane devicesPane;
    @FXML
    private HBox devicesContainer;

    private ToolbarState toolbarState = new ToolbarState(null, null, null, null);

    private InactivityFuture refreshFuture;

    /**
     * Initialisiert die Komponenten nachdem die Oberfläche dieser Komponente geladen ist
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    /**
     * Wird aufgerufen, sobald alle Manager geladen sind und die Benutzeroberfläche mit Daten befüllt werden soll.
     *
     * @param mfc Der Haupt-Controller.
     */
    public void onStart(MainFormController mfc) {
        this.mainFormController = mfc;

        // Lade Geräte
        List<Device> devices = null;
        try {
            devices = ElwaManager.instance.getManagedDevices();
        } catch (SQLException | NoDataFoundException e) {
            this.logger.error("Cannot get devices to display.", e);
            return;
        }
        for (Device d : devices) {
            ComponentControlInstance<DeviceListEntry> i = DeviceListEntry.createInstance();
            i.getController().setDevice(d);
            i.getController().setDeviceViewController(this);
            i.getController().onStart(this.mainFormController);
            devicesContainer.getChildren().add(i.getComponent());
            this.devices.put(d, i);
        }

        this.mainFormController.getBacklightManager().listenToLightOnEvent(this);

        // Bei Inaktivität alle 5 Minuten aktuellen Zustand aktualisieren
        this.refreshFuture = this.mainFormController.getInactivityScheduler()
                .scheduleJob(this::syncDevices, 5, TimeUnit.MINUTES, -1);
        this.refreshFuture.setName("DeviceViewController.SyncDevicesJob");
    }

    /**
     * Aktualisiere Geräte beim Licht-an-Ereignis
     */
    @Override
    public void onLightOn() {
        this.syncDevices();
    }

    /**
     * Synchronisiert die dargestellten Geräte mit der Datenbank
     */
    private void syncDevices() {
        if (this.mainFormController.getRegisteredUser() != null) {
            this.logger.debug("Updating devices suspended while user is logged in.");
            return;
        }
        if (this.mainFormController.getMainFormState() != MainFormState.SELECT_DEVICE) {
            // Nur aktualisieren, wenn die Geräteseite angezeigt wird
            this.logger.debug("Updating devices suspended while not in state " + MainFormState.SELECT_DEVICE.name() +
                    ". Current state: " + this.mainFormController.getMainFormState().name());
            return;
        }

        // Starte Messung der Datenbank-Zeit
        long startDb = System.currentTimeMillis();

        List<Device> devices;
        try {
            devices = ElwaManager.instance.getManagedDevices();
        } catch (SQLException e) {
            this.logger.error("Could not load devices to display.", e);
            this.mainFormController
                    .displayError("Datenbankfehler", e.getLocalizedMessage(), new ActionContainer(this::syncDevices),
                            true);
            return;
        } catch (NoDataFoundException e) {
            this.logger.error("Could not load devices to display.", e);
            this.mainFormController.displayError("Allgemeiner Fehler", e.getLocalizedMessage(),
                    new ActionContainer(this::syncDevices), true);
            return;
        }

        // Ende der Messung der Datenbank-Zeit
        long endDb = System.currentTimeMillis();

        Platform.runLater(() -> {
            // Starte Messung der GUI-Zeit
            long startGui = System.currentTimeMillis();

            // Suche nach neuen Geräten
            for (int i = 0; i < devices.size(); i++) {
                Device cDev = devices.get(i);
                if (!this.devices.containsKey(cDev)) {
                    // Neues Gerät gefunden
                    ComponentControlInstance<DeviceListEntry> inst = DeviceListEntry.createInstance();
                    inst.getController().setDevice(cDev);
                    inst.getController().setDeviceViewController(this);
                    inst.getController().onStart(this.mainFormController);
                    devicesContainer.getChildren().add(i, inst.getComponent());
                    this.devices.put(cDev, inst);
                } else if (this.devicesContainer.getChildren().get(i) != this.devices.get(cDev).getComponent()) {
                    // Gerät an der falschen Position
                    this.devicesContainer.getChildren().remove(this.devices.get(cDev).getComponent());
                    this.devicesContainer.getChildren().add(i, this.devices.get(cDev).getComponent());
                }
                if (cDev.getCurrentExecution() == null) {
                    this.devices.get(cDev).getController().refresh();
                }
            }

            // Suche nach gelöschten Geräten
            List<Device> currentDevs = new ArrayList<>(this.devices.keySet());
            for (Device d : currentDevs) {
                if (!devices.contains(d)) {
                    // Gelöschtes Gerät gefunden
                    if (d.getCurrentExecution() != null) {
                        // Lösche Gerät nicht bei laufender Ausführung
                        continue;
                    }
                    this.devices.get(d).getController().onTerminate();
                    devicesContainer.getChildren().remove(this.devices.get(d).getComponent());
                    this.devices.remove(d);
                }
            }

            // Ende der Messung der GUI-Zeit
            long endGui = System.currentTimeMillis();

            this.logger.trace(String
                    .format("Refreshed devices in %1dms (DB), (%2dms between), %3dms (GUI)", endDb - startDb,
                            startGui - endDb, endGui - startGui));
        });
    }

    @Override
    public void onTerminate() {
        for (ComponentControlInstance<DeviceListEntry> cci : this.devices.values()) {
            cci.getController().onTerminate();
            devicesContainer.getChildren().remove(cci.getComponent());
        }
        this.mainFormController.getBacklightManager().stopListeningToLightOnEvent(this);
        if (this.refreshFuture != null && !this.refreshFuture.isDone()) {
            this.refreshFuture.cancel();
        }
        this.devices.clear();
    }

    @Override
    public void onActivate() {
        devicesPane.setVisible(true);
        for (ComponentControlInstance<DeviceListEntry> cci : this.devices.values()) {
            cci.getController().onActivate();
        }
    }

    @Override
    public void onDeactivate() {
        devicesPane.setVisible(false);
        for (ComponentControlInstance<DeviceListEntry> cci : this.devices.values()) {
            cci.getController().onDeactivate();
        }
    }

    @Override
    public void onReturnFromError() {
        for (ComponentControlInstance<DeviceListEntry> cci : this.devices.values()) {
            cci.getController().onReturnFromError();
        }
    }

    @Override
    public ToolbarState getToolbarState() {
        return this.toolbarState;
    }

    /**
     * Öffnet die Tür eines Gerätes.
     *
     * @param device Das Gerät, dessen Tür geöffnet werden soll.
     */
    void onOpenDoor(Device device) {
        ActionContainer ac = new ActionContainer();
        ac.setAction(() -> {
            Thread t = new Thread(() -> {
                try {
                    ElwaManager.instance.getExecutionManager().startExecution(
                            Execution.getOfflineExecution(device, Program.getDoorOpenProgram(), User.getAnonymous()));
                } catch (SQLException e) {
                    this.logger.error("Could not switch on device.", e);
                    this.mainFormController.displayError("Datenbankfehler",
                            "Das Gerät konnte aufgrund eines Datenbankfehlers nicht geschaltet werden.\n" +
                                    exceptionToString(e), ac, true);
                } catch (IOException e) {
                    this.logger.error("Could not switch on device.", e);
                    this.mainFormController.displayError("Kommunikationsfehler",
                            "Das Gerät konnte aufgrund eines Kommunikationsfehlers nicht geschaltet werden.\n" +
                                    exceptionToString(e), ac, true);
                } catch (InterruptedException e) {
                    this.logger.error("Could not switch on device.", e);
                    this.mainFormController.displayError("Unterbrechung",
                            "Das Gerät konnte aufgrund einer Unterbrechung nicht geschaltet werden.\n" +
                                    exceptionToString(e), ac, true);
                } catch (FhemException e) {
                    this.logger.error("Could not switch on device.", e);
                    this.mainFormController.displayError("Kommunikationsfehler",
                            "Das Gerät ist nicht erreichbar.\n" + exceptionToString(e), ac, true);
                } finally {
                    this.mainFormController.endWait();
                }
            });
            t.setName("openDoorThread");
            t.start();
            this.mainFormController.beginWait();
        });
        ac.getAction().run();
    }

    /**
     * Leitet die Buchung eines Gerätes ein.
     *
     * @param device Das zu buchende Gerät.
     */
    void onDeviceSelected(Device device) {
        this.mainFormController.onDeviceSelected(device);
    }

    private static String exceptionToString(Exception e) {
        if (e.getLocalizedMessage() != null) {
            return e.getLocalizedMessage();
        } else {
            return e.getClass().getName();
        }
    }
}
