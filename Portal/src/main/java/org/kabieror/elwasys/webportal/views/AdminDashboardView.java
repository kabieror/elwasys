package org.kabieror.elwasys.webportal.views;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.*;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.common.Execution;
import org.kabieror.elwasys.common.Location;
import org.kabieror.elwasys.common.maintenance.*;
import org.kabieror.elwasys.webportal.MaintenanceConnectionManager;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.components.LocationWindow;
import org.kabieror.elwasys.webportal.components.LogViewerWindow;
import org.kabieror.elwasys.webportal.events.ILocationUpdatedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Vector;

/**
 * Seite Administrator-Dashboard
 *
 * @author Oliver Kabierschke
 */
public class AdminDashboardView extends VerticalLayout implements View {
    /**
     * Der Name des Views, der in der Adresszeile angezeigt wird
     */
    public static final String VIEW_NAME = "dashboard";
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final String USER_PROPERTY = "user";
    private static final String DATE_PROPERTY = "date";
    private static final String DURATION_PROPERTY = "duration";
    private static final String PRICE_PROPERTY = "price";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final VerticalLayout locationsContainer;

    public AdminDashboardView() {
        this.setStyleName("dashboard-view admin-dashboard");

        this.setMargin(true);
        this.setSpacing(true);

        final VerticalLayout body = new VerticalLayout();
        this.addComponent(body);
        body.setSpacing(true);

        final Label caption = new Label("Dashboard");
        caption.addStyleName("h1");
        body.addComponent(caption);

        this.locationsContainer = new VerticalLayout();
        body.addComponent(this.locationsContainer);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        this.loadData();
    }

    /**
     * Lädt die verfügbaren Standorte aus der Datenbank.
     *
     * @throws SQLException
     */
    private void loadData() {
        // Aufräumen
        this.locationsContainer.removeAllComponents();

        List<Location> locations;
        try {
            locations = WashportalManager.instance.getDataManager().getLocations();
        } catch (final SQLException e) {
            WashportalManager.instance.showDatabaseError(e);
            return;
        }

        for (final Location loc : locations) {
            this.locationsContainer.addComponent(new AdminDashboardLocationPanel(loc));
        }
    }

    /**
     * Diese Klasse zeigt die Daten eines Standorts an.
     *
     * @author Oliver Kabierschke
     */
    private class AdminDashboardLocationPanel extends VerticalLayout implements ILocationUpdatedEventListener {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private final Location location;

        public AdminDashboardLocationPanel(Location location) {
            this.location = location;
            this.addStyleName("location-pane");
            this.refresh();
        }

        private void refresh() {
            try {
                this.removeAllComponents();
                this.buildToolbar();
                this.buildStatusInfo();
                this.buildDeviceInfo();
            } catch (final SQLException e) {
                this.logger.error("Could not look up the devices at the location '" + this.location.getName() + "'.");
                WashportalManager.instance.showDatabaseError(e);
                return;
            }
        }

        /**
         * Erstellt die Toolbar
         */
        private void buildToolbar() {
            // Toolbar definition
            final HorizontalLayout header = new HorizontalLayout();
            header.addStyleName("location-pane-header");

            final Label captionLabel = new Label(this.location.getName());
            captionLabel.addStyleName(ValoTheme.LABEL_H4);
            captionLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);
            captionLabel.addStyleName(ValoTheme.LABEL_COLORED);
            header.addComponent(captionLabel);

            final MenuBar toolbar = new MenuBar();
            toolbar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
            toolbar.addStyleName(ValoTheme.MENUBAR_SMALL);
            toolbar.addStyleName("toolbar");

            {
                // Log button
                final MenuItem logItem = toolbar.addItem("", FontAwesome.BOOK, (e) -> {
                    IClientConnection connection =
                            WashportalManager.instance.getMaintenanceConnectionManager().getClient(this.location);
                    if (connection != null) {
                        MaintenanceResponse res;
                        try {
                            res = connection.sendQuery(new GetLogRequest());
                            if (!(res instanceof GetLogResponse)) {
                                this.logger.warn("Expected GetLogResponse but got " + res.getClass().getName());
                                WashportalManager.instance
                                        .showError("Kommunikationsfehler", "Unerwartete Antwort vom Server.");
                            } else {
                                this.getUI().addWindow(new LogViewerWindow(((GetLogResponse) res).getLogContent()));
                            }
                        } catch (final IOException e1) {
                            this.logger.error("Kommunikationsfehler", e1);
                            WashportalManager.instance.showError(e1);
                        }
                    } else {
                        WashportalManager.instance.showError("Fehler", "Keine Verbindung zum Client");
                    }
                });
                logItem.setDescription("Log-Datei anzeigen");
            }

            {
                // Power menu
                final MenuItem powerItem = toolbar.addItem("", FontAwesome.POWER_OFF, null);
                final MenuItem restartItem = powerItem.addItem("Anwendung neu starten", (i) -> {
                    try {
                        final IClientConnection connection =
                                WashportalManager.instance.getMaintenanceConnectionManager().getClient(this.location);
                        if (connection != null) {
                            connection.sendCommand(new RestartAppRequest());
                            WashportalManager.instance
                                    .showSuccessMessage("Neustart", "Der Neustart wurde in Auftrag gegeben.");
                        } else {
                            WashportalManager.instance.showError("Fehler", "Keine Verbindung zum Standort.");
                        }
                    } catch (final Exception e) {
                        this.logger.warn("Could not restart the server.", e);
                        WashportalManager.instance.showError(e);
                    }
                });
                restartItem.setDescription("Starte den Client neu.");
            }

            {
                // Edit menu
                final MenuItem editItem = toolbar.addItem("", FontAwesome.GEAR, null);
                editItem.addItem("Bearbeiten", FontAwesome.PENCIL, (i) -> {
                    final Window win = new LocationWindow(this.location, this);
                    this.getUI().addWindow(win);
                });
            }

            header.addComponent(toolbar);
            this.addComponent(header);
        }

        /**
         * Erstellt die Status-Informationen zum aktuellen Gerät
         */
        private void buildStatusInfo() {
            final HorizontalLayout container = new HorizontalLayout();
            container.setSpacing(true);

            Label connectionLabel = new Label("OK");

            MaintenanceConnectionManager connManager = WashportalManager.instance.getMaintenanceConnectionManager();
            IClientConnection client;
            if (connManager == null) {
                this.logger.error("No maintenance connection manager is available.");
                client = null;
            } else {
                client = connManager.getClient(this.location);
            }

            if (client == null) {
                connectionLabel = new Label("Fehler");
                connectionLabel.setDescription("Client ist nicht zum Server verbunden.");
            }

            connectionLabel.addStyleName(client != null ? "connection-success" : "connection-error");
            container.addComponent(this.buildKeyValue("Verbindung", connectionLabel));

            final Label ipLabel = new Label(client != null ? client.getHostAddress() : " - ");
            container.addComponent(this.buildKeyValue("IP-Adresse", ipLabel));

            this.addComponent(container);
        }

        private Component buildKeyValue(String key, Label value) {
            final HorizontalLayout container = new HorizontalLayout();
            container.addStyleName("key-value");
            final Label keyLabel = new Label(key);
            keyLabel.addStyleName(ValoTheme.LABEL_COLORED);
            keyLabel.addStyleName(ValoTheme.LABEL_SMALL);
            keyLabel.addStyleName("key");

            value.addStyleName("value");
            container.addComponent(keyLabel);
            container.addComponent(value);
            return container;
        }

        /**
         * Erstellt Status-Informationen zu den am aktuellen Standort
         * befindlichen Geräten.
         */
        private void buildDeviceInfo() throws SQLException {
            final CssLayout container = new CssLayout();
            container.addStyleName("device-info");
            Responsive.makeResponsive(container);

            final List<Device> devices = WashportalManager.instance.getDataManager().getDevicesToDisplay(this.location);

            for (final Device device : devices) {
                if (device == null) {
                    continue;
                }
                final CssLayout devOuterCont = new CssLayout();
                devOuterCont.addStyleName("device-container");

                final CssLayout devCont = new CssLayout();
                devCont.addStyleName("device");

                // Title content
                final HorizontalLayout titleCont = new HorizontalLayout();
                titleCont.addStyleName("title");

                final Label lblName = new Label(device.getName());
                lblName.addStyleName("device-name");
                titleCont.addComponent(lblName);

                final HorizontalLayout deviceKeyValue = new HorizontalLayout();
                deviceKeyValue.addStyleName("key-value");

                final Execution runningExecution =
                        WashportalManager.instance.getDataManager().getRunningExecution(device);
                final Label statusLabel = new Label(runningExecution == null ? "Frei" : "Besetzt");
                statusLabel.addStyleName("value " + (runningExecution == null ? "device-free" : "device-occupied"));
                deviceKeyValue.addComponent(statusLabel);
                titleCont.addComponent(deviceKeyValue);

                devCont.addComponent(titleCont);


                // Last executions table
                final Table tblLastExecutions = new Table();
                tblLastExecutions.setWidth("99%");
                tblLastExecutions.setHeight("15em");


                final IndexedContainer lastExeCont = new IndexedContainer();
                lastExeCont.addContainerProperty(DATE_PROPERTY, String.class, "");
                lastExeCont.addContainerProperty(USER_PROPERTY, String.class, "?");
                lastExeCont.addContainerProperty(DURATION_PROPERTY, String.class, "");
                lastExeCont.addContainerProperty(PRICE_PROPERTY, String.class, "");
                tblLastExecutions.setContainerDataSource(lastExeCont);

                tblLastExecutions.setColumnHeader(DATE_PROPERTY, "Datum");
                tblLastExecutions.setColumnHeader(USER_PROPERTY, "Benutzer");
                tblLastExecutions.setColumnHeader(DURATION_PROPERTY, "Dauer");
                tblLastExecutions.setColumnHeader(PRICE_PROPERTY, "Preis");

                // Laufende Ausführung hervorheben.
                tblLastExecutions.setCellStyleGenerator(new Table.CellStyleGenerator() {

                    @Override
                    public String getStyle(Table source, Object itemId, Object propertyId) {
                        if (propertyId == null) {
                            // Aussehen der Zeile
                            final List<String> res = new Vector<>();
                            try {
                                final Execution execution =
                                        WashportalManager.instance.getDataManager().getExecution((int) itemId);
                                if (execution.isRunning()) {
                                    res.add("running-execution");
                                }
                                if (execution.isExpired()) {
                                    res.add("expired-execution");
                                }
                            } catch (final SQLException e) {
                                AdminDashboardView.this.logger.error("Could not load the execution", e);
                                WashportalManager.instance.showDatabaseError(e);
                                return null;
                            }
                            return StringUtils.join(res, " ");
                        } else {
                            // Aussehen einer Zelle
                            return null;
                        }
                    }
                });

                final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
                final NumberFormat currencyFormat =
                        NumberFormat.getCurrencyInstance(VaadinSession.getCurrent().getLocale());

                for (final Execution e : WashportalManager.instance.getDataManager().getExecutions(device)) {
                    final Item item = lastExeCont.addItem(e.getId());

                    if (e.getStartDate() != null) {
                        item.getItemProperty(DATE_PROPERTY).setValue(e.getStartDate().format(dateTimeFormat));
                    } else {
                        item.getItemProperty(DATE_PROPERTY).setValue("-");
                    }

                    if (e.getUser() != null) {
                        item.getItemProperty(USER_PROPERTY).setValue(e.getUser().getName());
                    } else {
                        item.getItemProperty(USER_PROPERTY).setValue("-");
                    }
                    item.getItemProperty(DURATION_PROPERTY).setValue(
                            DurationFormatUtils.formatDuration(e.getElapsedTime().getSeconds() * 1000, "HH:mm:ss") +
                                    "h");
                    item.getItemProperty(PRICE_PROPERTY).setValue(currencyFormat.format(e.getPrice()));
                }

                devCont.addComponent(tblLastExecutions);

                devOuterCont.addComponent(devCont);

                container.addComponent(devOuterCont);
            }

            this.addComponent(container);
        }

        @Override
        public void onLocationUpdated(Location l) {
            this.refresh();
        }
    }
}
