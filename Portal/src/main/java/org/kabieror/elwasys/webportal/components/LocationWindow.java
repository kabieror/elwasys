package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.ShortcutAction;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.kabieror.elwasys.common.Location;
import org.kabieror.elwasys.common.UserGroup;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.events.ILocationUpdatedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Fenster zum Bearbeiten eines Standorts
 *
 * @author Oliver Kabierschke
 */
public class LocationWindow extends Window {

    private static final String CAPTION_PROPERTY = "caption";
    private static final String VALUE_PROPERTY = "value";
    private final Location location;
    private final ILocationUpdatedEventListener listener;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * Der Name des Programms
     */
    private TextField tfName;

    /**
     * Benutzergruppen-Auswahl
     */
    private TwinColSelect selGroups;
    private IndexedContainer groupsContainer;


    public LocationWindow(Location location, ILocationUpdatedEventListener listener) {
        this.location = location;
        this.listener = listener;
        this.setCaption("Standort bearbeiten");
        this.setWidth("40em");
        this.setResizable(false);
        this.setModal(true);
        this.setClosable(false);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        tfName = new TextField();
        tfName.setValue(location.getName());
        tfName.setCaption("Name:");
        tfName.setWidth("100%");
        content.addComponent(tfName);

        // ==== Benutzergruppen ====
        // Verfügbare Gruppen laden
        try {
            this.groupsContainer = new IndexedContainer();
            this.groupsContainer.addContainerProperty(CAPTION_PROPERTY, String.class, "");
            this.groupsContainer.addContainerProperty(VALUE_PROPERTY, UserGroup.class, "");
            for (final UserGroup g : WashportalManager.instance.getDataManager().getUserGroups()) {
                final Item i = this.groupsContainer.addItem(g.getId());
                i.getItemProperty(CAPTION_PROPERTY).setValue(g.getName());
                i.getItemProperty(VALUE_PROPERTY).setValue(g);
            }
        } catch (final UnsupportedOperationException e2) {
            this.logger.error("Error while loading the available user groups.", e2);
            WashportalManager.instance
                    .showError("Interner Fehler", "Die verfügbaren Benutzergruppen konnten nicht geladen werden.");
        } catch (final SQLException e2) {
            this.logger.error("Konnte die verfügbaren Benutzergruppen nicht aus der Datenbank laden.", e2);
            WashportalManager.instance.showDatabaseError(e2);
        }

        // Komponente erstellen
        final Label userGroupsCaptionLabel =
                new Label("<h3 style='margin-top:0;margin-bottom:0'>Benutzergruppen</h3>", ContentMode.HTML);
        content.addComponent(userGroupsCaptionLabel);
        this.selGroups = new TwinColSelect();
        content.addComponent(this.selGroups);
        this.selGroups.setLeftColumnCaption("Gesperrt");
        this.selGroups.setRightColumnCaption("Freigegeben");
        this.selGroups.setSizeFull();
        this.selGroups.setNewItemsAllowed(false);
        this.selGroups.setRows(5);
        this.selGroups.setContainerDataSource(this.groupsContainer);
        this.selGroups.setItemCaptionPropertyId(CAPTION_PROPERTY);

        // Bereits freigegebene Benutzergruppen eintragen
        HashSet<Integer> selectedGroups = new HashSet<>();
        for (UserGroup g : location.getValidUserGroups()) {
            selectedGroups.add(g.getId());
        }
        this.selGroups.setValue(selectedGroups);

        // ==== Footer ====
        final HorizontalLayout footer = new HorizontalLayout();
        content.addComponent(footer);
        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName("v-window-bottom-toolbar");

        final Button btnYes = new Button("OK");
        footer.addComponent(btnYes);
        btnYes.setStyleName("primary");
        btnYes.addClickListener(e -> {
            this.save();
        });
        btnYes.setClickShortcut(ShortcutAction.KeyCode.ENTER);

        final Label lblFooter = new Label("");
        footer.addComponent(lblFooter);
        footer.setExpandRatio(lblFooter, 1);

        final Button btnNo = new Button("Abbrechen");
        footer.addComponent(btnNo);
        btnNo.addClickListener(e -> {
            this.setVisible(false);
            this.getUI().removeWindow(this);
        });
    }

    /**
     * Speichert den Standort in der Datenbank.
     */
    private void save() {
        // Load selected user groups
        final List<UserGroup> validUserGroups = ((Set<Integer>) (this.selGroups.getValue())).stream()
                .map(i -> (UserGroup) this.groupsContainer.getItem(i).getItemProperty(VALUE_PROPERTY).getValue())
                .collect(Collectors.toCollection(Vector::new));

        try {
            location.modify(tfName.getValue(), validUserGroups);
            this.setVisible(false);
            this.getUI().removeWindow(this);
            listener.onLocationUpdated(location);
        } catch (final SQLException e1) {
            WashportalManager.instance.showDatabaseError(e1);
        }
    }
}
