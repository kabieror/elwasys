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
import org.kabieror.elwasys.common.Program;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.components.ConfirmWindow;
import org.kabieror.elwasys.webportal.components.ProgramWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Seite: Programme
 *
 * @author Oliver Kabierschke
 *
 */
public class ProgramsView extends VerticalLayout implements View {
    /**
     * Der Name des Views, der in der Adresszeile angezeigt wird
     */
    public static final String VIEW_NAME = "programs";
    /**
     *
     */
    private static final long serialVersionUID = -2187129231010562676L;
    private static final String ICON_PROPERTY = "icon";
    private static final String INDEX_PROPERTY = "ID";
    private static final String NAME_PROPERTY = "Name";
    private static final String TYPE_PROPERTY = "Typ";
    private static final String PRICE_PROPERTY = "Preis";
    private static final String BUTTONS_PROPERTY = "buttons";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Table programsTable;
    private final IndexedContainer programsContainer;

    public ProgramsView() {
        this.setMargin(true);
        this.setSpacing(true);
        this.setSizeFull();

        // 1. Menüleiste erstellen
        final HorizontalLayout topLayout = new HorizontalLayout();
        this.addComponent(topLayout);
        topLayout.setWidth("100%");
        final Label title = new Label("Programme");
        topLayout.addComponent(title);
        title.addStyleName("h1");

        final MenuBar menuBar = new MenuBar();
        final MenuItem menuAdd = menuBar.addItem("Neu", i -> this.newProgram());
        menuAdd.setIcon(FontAwesome.PLUS);

        topLayout.addComponent(menuBar);
        topLayout.setComponentAlignment(menuBar, Alignment.BOTTOM_RIGHT);
        topLayout.setExpandRatio(menuBar, 2);
        topLayout.setExpandRatio(title, 1);

        this.programsTable = new Table();
        this.addComponent(this.programsTable);
        this.programsTable.setSizeFull();
        this.programsTable.setMultiSelect(false);
        this.programsTable.setSelectable(false);

        this.programsContainer = new IndexedContainer();
        this.programsContainer.addContainerProperty(ICON_PROPERTY, Label.class, null);
        this.programsContainer.addContainerProperty(INDEX_PROPERTY, Integer.class, null);
        this.programsContainer.addContainerProperty(NAME_PROPERTY, String.class, null);
        this.programsContainer.addContainerProperty(TYPE_PROPERTY, String.class, null);
        this.programsContainer.addContainerProperty(PRICE_PROPERTY, String.class, null);

        this.programsTable.setContainerDataSource(this.programsContainer);
        this.programsTable.setColumnWidth(ICON_PROPERTY, 40);
        this.programsTable.setColumnHeader(ICON_PROPERTY, "");
        this.programsTable.setColumnAlignment(ICON_PROPERTY, Align.CENTER);
        this.programsTable.setColumnWidth(INDEX_PROPERTY, 40);

        this.programsTable.addContainerProperty(BUTTONS_PROPERTY, CssLayout.class, null);
        this.programsTable.addGeneratedColumn(BUTTONS_PROPERTY,
                (Table source, Object itemId, Object columnId) -> {
                    Program program;
                    try {
                        program = WashportalManager.instance.getDataManager()
                                .getProgramById((int) itemId);
                    } catch (final SQLException e2) {
                        this.logger.error("Could not load program to generate buttons for.", e2);
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
                            final ProgramWindow win = new ProgramWindow(program);
                            win.addProgramUpdatedEventListener(p -> {
                                this.updateProgram(p);
                            });
                            this.getUI().addWindow(win);
                        } catch (final Exception e1) {
                            this.logger.error("Could not load program to edit.", e1);
                            WashportalManager.instance.showError(e1);
                        }
                    });

                    final Button btnDelete = new Button("");
                    group.addComponent(btnDelete);
                    btnDelete.setIcon(FontAwesome.TRASH_O);
                    btnDelete.setDescription("Löschen");
                    btnDelete.addStyleName("small danger");
                    btnDelete.addClickListener(e -> {
                        try {
                            // Lade Geräte, die dieses Programm
                            // verwenden
                            final List<Device> devices =
                                    WashportalManager.instance.getDataManager().getDevices(program);
                            if (devices.isEmpty()) {
                                final ConfirmWindow win = new ConfirmWindow("Programm löschen",
                                        "Möchten Sie dieses Programm wirklich löschen?<br><b>"
                                                + program.getName() + "</b>",
                                        () -> {
                                    try {
                                        program.delete();
                                    } catch (final SQLException e1) {
                                        this.logger.error("Could not delete program.", e1);
                                        WashportalManager.instance.showDatabaseError(e1);
                                        return;
                                    } catch (final Exception e1) {
                                        this.logger.error("Could not delete program.", e1);
                                        WashportalManager.instance.showError(e1);
                                        return;
                                    }
                                    this.updateProgram(program);
                                });
                                this.getUI().addWindow(win);
                            } else {
                                WashportalManager.instance.showError(
                                        "Programm kann kann nicht gelöscht werden.",
                                        "Das Programm <b>" + program.getName()
                                                + "</b> ist noch auf " + devices.size()
                                                + " Gerät(en) verfügbar.");
                            }
                        } catch (final SQLException e1) {
                            this.logger.error("Could not load program to delete.", e1);
                            WashportalManager.instance.showDatabaseError(e1);
                        } catch (final Exception e1) {
                            this.logger.error("Could not load program to delete.", e1);
                            WashportalManager.instance.showError(e1);
                        }
                    });

                    return group;
                });
        this.programsTable.setColumnHeader(BUTTONS_PROPERTY, "");
        this.programsTable.setColumnWidth(BUTTONS_PROPERTY, 100);
        this.programsTable.setColumnAlignment(BUTTONS_PROPERTY, Align.CENTER);

        this.setExpandRatio(this.programsTable, 1);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        this.loadData();
    }

    private void loadData() {
        List<Program> programs;

        try {
            programs = WashportalManager.instance.getDataManager().getPrograms();
        } catch (final SQLException e) {
            this.logger.error("Error while catching the available programs from the database", e);
            WashportalManager.instance.showDatabaseError(e);
            return;
        }

        for (final Program p : programs) {
            this.updateProgram(p);
        }
    }

    /**
     * Aktualisiert ein Programm in der Tabelle
     *
     * @param p
     *            Das zu aktualisierende Programm
     */
    @SuppressWarnings("unchecked")
    private void updateProgram(Program p) {
        if (p == null) {
            return;
        }

        try {
            p.update();
        } catch (final SQLException e) {
            this.logger.error("Could not update program to display");
            return;
        } catch (final NoDataFoundException e) {
            // Programm wurde gelöscht
            if (this.programsContainer.containsId(p.getId())) {
                this.programsContainer.removeItem(p.getId());
            }
            return;
        }

        Item i;
        if (this.programsContainer.containsId(p.getId())) {
            i = this.programsContainer.getItem(p.getId());
        } else {
            i = this.programsContainer.addItem(p.getId());
        }

        String unit = "?";
        if (p.getTimeUnit() != null) {
            switch (p.getTimeUnit()) {
            case HOURS:
                unit = "h";
                break;
            case MINUTES:
                unit = "min";
                break;
            case SECONDS:
                unit = "s";
                break;
            default:
                unit = "?";
                break;
            }
        }
        String type;
        String price;
        switch (p.getType()) {
        case DYNAMIC:
            type = "Dynamisch";
            price = p.getFlagfall().toString() + " € + " + p.getRate() + " € / " + unit;
            break;
        case FIXED:
            type = "Statisch";
            price = p.getFlagfall().toString() + " €";
            break;
        default:
            type = "Unbekannt";
            price = "?";
            break;
        }

        final Label lblIcon = new Label(FontAwesome.GEAR.getHtml(), ContentMode.HTML);
        lblIcon.addStyleName("icon-program-normal");

        i.getItemProperty(ICON_PROPERTY).setValue(lblIcon);
        i.getItemProperty(INDEX_PROPERTY).setValue(p.getId());
        i.getItemProperty(NAME_PROPERTY).setValue(p.getName());
        i.getItemProperty(TYPE_PROPERTY).setValue(type);
        i.getItemProperty(PRICE_PROPERTY).setValue(price);
    }

    /**
     * Ã–ffnet ein Fenster zum Erstellen eines neuen Programms
     */
    private void newProgram() {
        final ProgramWindow win = new ProgramWindow();
        win.addProgramUpdatedEventListener(p -> {
            this.updateProgram(p);
        });
        this.getUI().addWindow(win);
    }
}
