package org.kabieror.elwasys.webportal.views;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Table.Align;
import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.common.NoDataFoundException;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.components.ConfirmWindow;
import org.kabieror.elwasys.webportal.components.DeviceWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Seite Geräte
 *
 * @author Oliver Kabierschke
 *
 */
public class DevicesView extends VerticalLayout implements View {

    /**
     * Der Name des Views, der in der Adresszeile angezeigt wird
     */
    public static final String VIEW_NAME = "devices";
    /**
     *
     */
    private static final long serialVersionUID = -3004697899657078108L;
    private static final String ICON_PROPERTY = "icon";
    private static final String INDEX_PROPERTY = "ID";
    private static final String POSITION_PROPERTY = "Position";
    private static final String NAME_PROPERTY = "Name";
    private static final String LOCATION_PROPERTY = "Standort";
    private static final String BUTTONS_PROPERTY = "buttons";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Table devicesTable;
    private final IndexedContainer devicesContainer;

    public DevicesView() {
        this.setMargin(true);
        this.setSpacing(true);
        this.setSizeFull();

        // 1. Menüleiste erstellen
        final HorizontalLayout topLayout = new HorizontalLayout();
        this.addComponent(topLayout);
        topLayout.setWidth("100%");
        final Label title = new Label("Geräte");
        topLayout.addComponent(title);
        title.addStyleName("h1");

        final MenuBar menuBar = new MenuBar();
        topLayout.addComponent(menuBar);
        final MenuItem menuAdd = menuBar.addItem("Neu", i -> this.newDevice());
        menuAdd.setIcon(FontAwesome.PLUS);

        topLayout.setComponentAlignment(menuBar, Alignment.BOTTOM_RIGHT);
        topLayout.setExpandRatio(menuBar, 2);
        topLayout.setExpandRatio(title, 1);

        this.devicesTable = new Table();
        this.addComponent(this.devicesTable);

        this.devicesTable.setSizeFull();
        this.devicesTable.setMultiSelect(false);
        this.devicesTable.setSelectable(false);

        this.devicesContainer = new IndexedContainer();
        this.devicesContainer.addContainerProperty(ICON_PROPERTY, Label.class, null);
        this.devicesContainer.addContainerProperty(INDEX_PROPERTY, Integer.class, null);
        this.devicesContainer.addContainerProperty(POSITION_PROPERTY, Integer.class, null);
        this.devicesContainer.addContainerProperty(NAME_PROPERTY, String.class, null);
        this.devicesContainer.addContainerProperty(LOCATION_PROPERTY, String.class, null);

        this.devicesTable.setContainerDataSource(this.devicesContainer);
        this.devicesTable.setColumnWidth(ICON_PROPERTY, 40);
        this.devicesTable.setColumnHeader(ICON_PROPERTY, "");
        this.devicesTable.setColumnAlignment(ICON_PROPERTY, Align.CENTER);
        this.devicesTable.setColumnWidth(INDEX_PROPERTY, 40);
        this.devicesTable.setColumnWidth(POSITION_PROPERTY, 40);

        this.devicesTable.addContainerProperty(BUTTONS_PROPERTY, CssLayout.class, null);
        this.devicesTable.setColumnHeader(BUTTONS_PROPERTY, "");
        this.devicesTable.setColumnWidth(BUTTONS_PROPERTY, 100);
        this.devicesTable.setColumnAlignment(BUTTONS_PROPERTY, Align.CENTER);

        this.devicesTable.addGeneratedColumn(BUTTONS_PROPERTY,
                (Table source, Object itemId, Object columnId) -> {
                    Device device;
                    try {
                        device = WashportalManager.instance.getDataManager()
                                .getDevice((int) itemId);
                    } catch (final SQLException e2) {
                        this.logger.error("Could not load device to generate buttons for.", e2);
                        WashportalManager.instance.showDatabaseError(e2);
                        return null;
                    }
                    final CssLayout group = new CssLayout();
                    group.addStyleName("v-component-group");

                    final Button btnEdit = new Button("");
                    group.addComponent(btnEdit);
                    btnEdit.setIcon(FontAwesome.PENCIL_SQUARE_O);
                    btnEdit.setDescription("Bearbeiten");
                    btnEdit.addStyleName("small");
                    btnEdit.addClickListener(e -> {
                        try {
                            final DeviceWindow win = new DeviceWindow(device);
                            win.addDeviceUpdatedEventListener(d -> {
                                this.updateDevice(d);
                            });
                            this.getUI().addWindow(win);
                        } catch (final Exception e1) {
                            this.logger.error("Could not load device to edit.", e1);
                            WashportalManager.instance.showError("Interner Fehler",
                                    "Ein fehler ist aufgetreten. " + e1.getLocalizedMessage());
                        }
                    });

                    final Button btnDelete = new Button("");
                    group.addComponent(btnDelete);
                    btnDelete.setIcon(FontAwesome.TRASH_O);
                    btnDelete.setDescription("Löschen");
                    btnDelete.addStyleName("small danger");
                    btnDelete.addClickListener(e -> {
                        try {
                            final ConfirmWindow win = new ConfirmWindow("Gerät löschen",
                                    "Möchten Sie dieses Gerät wirklich löschen?<br><b>"
                                            + device.getName() + "</b>",
                                    () -> {
                                try {
                                    device.delete();
                                } catch (final SQLException e1) {
                                    this.logger.error("Could not delete device.", e1);
                                    WashportalManager.instance.showDatabaseError(e1);
                                    return;
                                } catch (final Exception e1) {
                                    this.logger.error("Could not delete device.", e1);
                                    WashportalManager.instance.showError(e1);
                                    return;
                                }
                                this.updateDevice(device);
                            });
                            this.getUI().addWindow(win);
                        } catch (final Exception e1) {
                            this.logger.error("Could not load program to delete.", e1);
                            WashportalManager.instance.showError(e1);
                        }
                    });

                    return group;
                });

        this.setExpandRatio(this.devicesTable, 1);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        this.loadData();
    }

    private void loadData() {
        List<Device> devices;
        try {
            devices = WashportalManager.instance.getDataManager().getDevices();
        } catch (final SQLException e) {
            this.logger.error("Could not load the devices to display.", e);
            WashportalManager.instance.showDatabaseError(e);
            return;
        }

        for (final Device d : devices) {
            this.updateDevice(d);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateDevice(Device d) {
        if (d == null) {
            return;
        }

        try {
            d.update();
        } catch (final SQLException e) {
            this.logger.error("Could not update the device to display.");
            return;
        } catch (final NoDataFoundException e) {
            // Gerät wurde gelöscht
            if (this.devicesContainer.containsId(d.getId())) {
                this.devicesContainer.removeItem(d.getId());
            }
            return;
        }

        Item i;
        if (this.devicesContainer.containsId(d.getId())) {
            i = this.devicesContainer.getItem(d.getId());
        } else {
            i = this.devicesContainer.addItem(d.getId());
        }

        final Label lblIcon = new Label(FontAwesome.CUBE.getHtml(), ContentMode.HTML);
        i.getItemProperty(ICON_PROPERTY).setValue(lblIcon);
        lblIcon.addStyleName("icon-device-normal");
        i.getItemProperty(INDEX_PROPERTY).setValue(d.getId());
        i.getItemProperty(POSITION_PROPERTY).setValue(d.getPosition());
        i.getItemProperty(NAME_PROPERTY).setValue(d.getName());
        i.getItemProperty(LOCATION_PROPERTY).setValue(d.getLocation().getName());
    }

    private void newDevice() {
        final DeviceWindow win = new DeviceWindow();
        win.addDeviceUpdatedEventListener((d) -> this.updateDevice(d));
        this.getUI().addWindow(win);
    }

}
