package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Item;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.kabieror.elwasys.common.*;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.events.IUserGroupUpdatedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dieses Fenster erlaubt das Bearbeiten und Erstellen von Benutzergruppen
 *
 * @author Oliver Kabierschke
 */
public class UserGroupWindow extends Window {

    private static final long serialVersionUID = 6281036476918365426L;

    private static final String CAPTION_PROPERTY = "caption";
    private static final String VALUE_PROPERTY = "value";
    private static final String FIX_PROPERTY = "fix";
    private static final String FACTOR_PROPERTY = "factor";
    private static final String NONE_PROPERTY = "none";

    private final ObjectProperty<String> discountFixProperty = new ObjectProperty<String>("0€");
    private final ObjectProperty<String> discountFactorProperty = new ObjectProperty<String>("0%");

    /**
     * Der Modus des Formulars
     */
    private final Mode mode;
    /**
     * Listener, die nach Abschluss benachrichtigt werden
     */
    private final List<IUserGroupUpdatedEventListener> listeners = new Vector<>();
    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Die Benutzergruppe, die bearbeitet wird
     */
    private UserGroup groupToEdit;


    /**
     * Standort-Auswahl
     */
    private TwinColSelect selLocations;
    private IndexedContainer locationsContainer;
    /**
     * Geräte-Auswahl
     */
    private TwinColSelect selDevices;
    private IndexedContainer devicesContainer;
    /**
     * Programm-Auswahl
     */
    private TwinColSelect selPrograms;
    private IndexedContainer programsContainer;

    /**
     * Der Name
     */
    private TextField tfName;
    /**
     * Der Typ der Rabattierung (fix, faktor, keiner)
     */
    private OptionGroup ogDiscountType;
    /**
     * Der Rabatt, falls fixe Rabattierung
     */
    private TextField tfDiscountFix;
    /**
     * Der Rabatt, falls Faktor-Rabattierung
     */
    private TextField tfDiscountFactor;


    /**
     * Konstruktor
     */
    public UserGroupWindow(UserGroup groupToEdit) {
        this.mode = Mode.EDIT_GROUP;

        this.groupToEdit = groupToEdit;
        this.init();

        this.tfName.setValue(groupToEdit.getName());

        switch (groupToEdit.getDiscountType()) {
            case Factor:
                this.ogDiscountType.select(UserGroupWindow.FACTOR_PROPERTY);
                this.tfDiscountFactor.setValue(NumberFormat.getPercentInstance(Locale.GERMANY)
                        .format(this.groupToEdit.getDiscountValue()));
                break;
            case Fix:
                this.ogDiscountType.select(UserGroupWindow.FIX_PROPERTY);
                this.tfDiscountFix.setValue(FormatUtilities
                        .formatCurrency(this.groupToEdit.getDiscountValue()));
                break;
            default:
                this.ogDiscountType.select(UserGroupWindow.NONE_PROPERTY);
                break;
        }

        // Standorte laden
        HashSet<Integer> validLocations = new HashSet<>();
        try {
            for (Location l : this.groupToEdit.getValidLocations()) {
                validLocations.add(l.getId());
            }
        } catch (SQLException e) {
            this.logger.error("Could not load the available locations from the database.", e);
            WashportalManager.instance.showDatabaseError(e);
        }
        this.selLocations.setValue(validLocations);

        // Geräte laden
        HashSet<Integer> validDevices = new HashSet<>();
        try {
            for (Device d : this.groupToEdit.getValidDevices()) {
                validDevices.add(d.getId());
            }
        } catch (SQLException e) {
            this.logger.error("Could not load the available devices from the database.", e);
            WashportalManager.instance.showDatabaseError(e);
        }
        this.selDevices.setValue(validDevices);

        // Programme laden
        HashSet<Integer> validPrograms = new HashSet<>();
        try {
            for (Program p : this.groupToEdit.getValidPrograms()) {
                validPrograms.add(p.getId());
            }
        } catch (SQLException e) {
            this.logger.error("Could not load the available programs from the database.", e);
            WashportalManager.instance.showDatabaseError(e);
        }
        this.selPrograms.setValue(validPrograms);
    }

    public UserGroupWindow() {
        this.mode = Mode.CREATE_GROUP;
        this.init();
    }

    /**
     * Komponenten initialisieren
     */
    @SuppressWarnings("unchecked")
    private void init() {
        String caption;
        String btnSaveCaption;
        if (this.mode.equals(Mode.EDIT_GROUP)) {
            caption = "Gruppe bearbeiten";
            btnSaveCaption = "Speichern";
        } else {
            caption = "Gruppe erstellen";
            btnSaveCaption = "Erstellen";
        }

        // Beschriftung des Fensters
        this.setCaption(caption);
        this.setWidth("50em");
        this.setResizable(false);
        this.setModal(true);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        final FormLayout form = new FormLayout();
        content.addComponent(form);
        form.setSizeFull();
        form.setMargin(false);

        // Textfeld: Name
        this.tfName = new TextField("Name");
        form.addComponent(this.tfName);
        this.tfName.setRequired(true);
        this.tfName.setRequiredError("Bitte Name eingeben.");
        this.tfName.setValidationVisible(false);
        this.tfName.setSizeFull();
        this.tfName.setNullRepresentation("");

        // Option-Group: Programmtyp
        this.ogDiscountType = new OptionGroup("Rabattierung");
        form.addComponent(this.ogDiscountType);
        this.ogDiscountType.setRequired(true);
        this.ogDiscountType.setRequiredError("Bitte Rabattierung auswählen");
        this.ogDiscountType.setValidationVisible(false);
        this.ogDiscountType.setSizeFull();
        this.ogDiscountType.addStyleName("horizontal");
        this.ogDiscountType.setItemCaptionPropertyId(CAPTION_PROPERTY);

        final IndexedContainer typeContainer = new IndexedContainer();
        this.ogDiscountType.setContainerDataSource(typeContainer);
        typeContainer.addContainerProperty(CAPTION_PROPERTY, String.class, null);

        // Textfeld: Rabattierung: Faktor
        this.tfDiscountFactor = new TextField("Rabatt (%)");
        this.tfDiscountFactor.setSizeFull();
        this.tfDiscountFactor.setRequired(true);
        this.tfDiscountFactor.setRequiredError("Bitte Rabatt eingeben.");
        this.tfDiscountFactor.setValidationVisible(false);
        this.tfDiscountFactor.setPropertyDataSource(this.discountFactorProperty);
        this.tfDiscountFactor.setNullRepresentation("");
        this.tfDiscountFactor.addValidator(new RegexpValidator("^\\d+([,.]\\d+)?\\s?%?$",
                "Bitte einen gültigen Prozentsatz eingeben, z.B. '21,5%'."));

        // Textfeld: Rabattierung: Faktor
        this.tfDiscountFix = new TextField("Rabatt (€)");
        this.tfDiscountFix.setSizeFull();
        this.tfDiscountFix.setRequired(true);
        this.tfDiscountFix.setRequiredError("Bitte Rabatt eingeben.");
        this.tfDiscountFix.setValidationVisible(false);
        this.tfDiscountFix.setPropertyDataSource(this.discountFixProperty);
        this.tfDiscountFix.setNullRepresentation("");
        this.tfDiscountFix.addValidator(
                new RegexpValidator("^\\d+([,.]\\d+)?\\s?€?$", "Bitte einen gültigen Betrag eingeben, z.B. '0,50€'."));

        final Item iNone = typeContainer.addItem(UserGroupWindow.NONE_PROPERTY);
        iNone.getItemProperty(CAPTION_PROPERTY).setValue("Keiner");
        final Item iStatic = typeContainer.addItem(UserGroupWindow.FIX_PROPERTY);
        iStatic.getItemProperty(CAPTION_PROPERTY).setValue("Fix");
        final Item iDynamic = typeContainer.addItem(UserGroupWindow.FACTOR_PROPERTY);
        iDynamic.getItemProperty(CAPTION_PROPERTY).setValue("Faktor");
        this.ogDiscountType.addValueChangeListener(e -> {
            final String val = (String) e.getProperty().getValue();
            if (val.equals(UserGroupWindow.FIX_PROPERTY)) {
                form.removeComponent(this.tfDiscountFactor);
                form.addComponent(this.tfDiscountFix, 2);
            } else if (val.equals(UserGroupWindow.FACTOR_PROPERTY)) {
                form.removeComponent(this.tfDiscountFix);
                form.addComponent(this.tfDiscountFactor, 2);
            } else if (val.equals(UserGroupWindow.NONE_PROPERTY)) {
                form.removeComponent(this.tfDiscountFix);
                form.removeComponent(this.tfDiscountFactor);
            } else {
                WashportalManager.instance.showError("Fehler",
                        "Beim setzen des Typs ist ein Fehler aufgetreten. Der übermittelte Typ ist nicht bekannt.");
            }
        });
        this.ogDiscountType.setValue(NONE_PROPERTY);


        // ==== Berechtigungen ====
        // Standorte
        try {
            this.locationsContainer = new IndexedContainer();
            this.locationsContainer.addContainerProperty(CAPTION_PROPERTY, String.class, "");
            this.locationsContainer.addContainerProperty(VALUE_PROPERTY, Location.class, "");
            for (final Location l : WashportalManager.instance.getDataManager().getLocations()) {
                final Item i = this.locationsContainer.addItem(l.getId());
                i.getItemProperty(CAPTION_PROPERTY).setValue(l.getName());
                i.getItemProperty(VALUE_PROPERTY).setValue(l);
            }
        } catch (final UnsupportedOperationException e2) {
            this.logger.error("Error while loading the available locations.", e2);
            WashportalManager.instance
                    .showError("Interner Fehler", "Die verfügbaren Standorte konnten nicht geladen werden.");
        } catch (final SQLException e2) {
            this.logger.error("Konnte die verfügbaren Standorte nicht aus der Datenbank laden.", e2);
            WashportalManager.instance.showDatabaseError(e2);
        }

        final Label locationsCaptionLabel =
                new Label("<h3 style='margin-top:20px;margin-bottom:0'>Standorte</h3>", ContentMode.HTML);
        content.addComponent(locationsCaptionLabel);
        this.selLocations = new TwinColSelect();
        content.addComponent(this.selLocations);
        this.selLocations.setLeftColumnCaption("Gesperrt");
        this.selLocations.setRightColumnCaption("Freigegeben");
        this.selLocations.setSizeFull();
        this.selLocations.setNewItemsAllowed(false);
        this.selLocations.setRows(5);
        this.selLocations.setContainerDataSource(this.locationsContainer);
        this.selLocations.setItemCaptionPropertyId(CAPTION_PROPERTY);

        // Geräte
        try {
            this.devicesContainer = new IndexedContainer();
            this.devicesContainer.addContainerProperty(CAPTION_PROPERTY, String.class, "");
            this.devicesContainer.addContainerProperty(VALUE_PROPERTY, Device.class, "");
            for (final Device d : WashportalManager.instance.getDataManager().getDevices()) {
                final Item i = this.devicesContainer.addItem(d.getId());
                i.getItemProperty(CAPTION_PROPERTY)
                        .setValue(String.format("%s (%s)", d.getName(), d.getLocation().getName()));
                i.getItemProperty(VALUE_PROPERTY).setValue(d);
            }
        } catch (final UnsupportedOperationException e2) {
            this.logger.error("Error while loading the available devices.", e2);
            WashportalManager.instance
                    .showError("Interner Fehler", "Die verfügbaren Geräte konnten nicht geladen werden.");
        } catch (final SQLException e2) {
            this.logger.error("Konnte die verfügbaren Geräte nicht aus der Datenbank laden.", e2);
            WashportalManager.instance.showDatabaseError(e2);
        }

        final Label devicesCaptionLabel =
                new Label("<h3 style='margin-top:20px;margin-bottom:0'>Geräte</h3>", ContentMode.HTML);
        content.addComponent(devicesCaptionLabel);
        this.selDevices = new TwinColSelect();
        content.addComponent(this.selDevices);
        this.selDevices.setLeftColumnCaption("Gesperrt");
        this.selDevices.setRightColumnCaption("Freigegeben");
        this.selDevices.setSizeFull();
        this.selDevices.setNewItemsAllowed(false);
        this.selDevices.setRows(5);
        this.selDevices.setContainerDataSource(this.devicesContainer);
        this.selDevices.setItemCaptionPropertyId(CAPTION_PROPERTY);

        // Programme
        try {
            this.programsContainer = new IndexedContainer();
            this.programsContainer.addContainerProperty(CAPTION_PROPERTY, String.class, "");
            this.programsContainer.addContainerProperty(VALUE_PROPERTY, Program.class, "");
            for (final Program p : WashportalManager.instance.getDataManager().getPrograms()) {
                final Item i = this.programsContainer.addItem(p.getId());
                i.getItemProperty(CAPTION_PROPERTY).setValue(p.getName());
                i.getItemProperty(VALUE_PROPERTY).setValue(p);
            }
        } catch (final UnsupportedOperationException e2) {
            this.logger.error("Error while loading the available programs.", e2);
            WashportalManager.instance
                    .showError("Interner Fehler", "Die verfügbaren Programme konnten nicht geladen werden.");
        } catch (final SQLException e2) {
            this.logger.error("Konnte die verfügbaren Programme nicht aus der Datenbank laden.", e2);
            WashportalManager.instance.showDatabaseError(e2);
        }

        final Label programsCaptionLabel =
                new Label("<h3 style='margin-top:20px;margin-bottom:0'>Programme</h3>", ContentMode.HTML);
        content.addComponent(programsCaptionLabel);
        this.selPrograms = new TwinColSelect();
        content.addComponent(this.selPrograms);
        this.selPrograms.setLeftColumnCaption("Gesperrt");
        this.selPrograms.setRightColumnCaption("Freigegeben");
        this.selPrograms.setSizeFull();
        this.selPrograms.setNewItemsAllowed(false);
        this.selPrograms.setRows(5);
        this.selPrograms.setContainerDataSource(this.programsContainer);
        this.selPrograms.setItemCaptionPropertyId(CAPTION_PROPERTY);


        // Formularfuß mit Buttons
        final HorizontalLayout footer = new HorizontalLayout();
        content.addComponent(footer);
        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName("v-window-bottom-toolbar");

        final Label footerText = new Label("");
        footer.addComponent(footerText);
        footer.setExpandRatio(footerText, 1);

        final Button btnCancel = new Button("Abbrechen");
        footer.addComponent(btnCancel);
        btnCancel.addClickListener(e -> {
            this.setVisible(false);
            this.getUI().removeWindow(this);
        });

        final Button btnSave = new Button(btnSaveCaption);
        footer.addComponent(btnSave);
        btnSave.addClickListener(e -> {
            try {
                this.save();
            } catch (final SQLException e1) {
                this.logger.error("Unable to store user group into database.", e1);
                WashportalManager.instance.showDatabaseError(e1);
            } catch (final Exception e1) {
                this.logger.error("Unable to save user group.", e1);
                WashportalManager.instance.showError(e1);
            }
        });
        btnSave.addStyleName("primary");
        btnSave.setClickShortcut(KeyCode.ENTER);
    }

    /**
     * Speichert das Formular in die Datenbank
     */
    private void save() throws SQLException {
        // Felder validieren
        try {
            this.tfName.validate();
            this.ogDiscountType.validate();
            if (this.ogDiscountType.getValue() != null) {
                if (this.ogDiscountType.getValue().equals(FIX_PROPERTY)) {
                    this.tfDiscountFix.validate();
                }
                if (this.ogDiscountType.getValue().equals(FACTOR_PROPERTY)) {
                    this.tfDiscountFactor.validate();
                }
            }
        } catch (final InvalidValueException e) {
            this.tfName.setValidationVisible(true);
            this.ogDiscountType.setValidationVisible(true);
            if (this.ogDiscountType.getValue() != null) {
                if (this.ogDiscountType.getValue().equals(FIX_PROPERTY)) {
                    this.tfDiscountFix.setValidationVisible(true);
                }
                if (this.ogDiscountType.getValue().equals(FACTOR_PROPERTY)) {
                    this.tfDiscountFactor.setValidationVisible(true);
                }
            }
            return;
        }

        DiscountType type;
        double discountValue;
        switch ((String) this.ogDiscountType.getValue()) {
            case FIX_PROPERTY:
                type = DiscountType.Fix;
                discountValue =
                        Double.parseDouble(this.discountFixProperty.getValue().replace("€", "").replace(",", "."));
                break;
            case FACTOR_PROPERTY:
                type = DiscountType.Factor;
                discountValue =
                        Double.parseDouble(this.discountFactorProperty.getValue().replace("%", "").replace(",", ".")) /
                                100;
                break;
            default:
                type = DiscountType.None;
                discountValue = 0d;
                break;
        }

        final UserGroup group;
        switch (this.mode) {
            case CREATE_GROUP:
                group = new UserGroup(WashportalManager.instance.getDataManager(), this.tfName.getValue(), type,
                        discountValue);
                break;
            case EDIT_GROUP:
                group = this.groupToEdit;
                group.modify(this.tfName.getValue(), type, discountValue);
                break;
            default:
                this.logger.error("Unknown state. Cannot save user group.");
                WashportalManager.instance.showError("Zustandsfehler", "Dieses Fenster hat einen ungültigen Zustand.");
                return;
        }

        group.setValidLocations(((Set<Integer>) (this.selLocations.getValue())).stream()
                .map(i -> (Location) this.locationsContainer.getItem(i).getItemProperty(VALUE_PROPERTY).getValue())
                .collect(Collectors.toCollection(Vector::new)));

        group.setValidDevices(((Set<Integer>) (this.selDevices.getValue())).stream()
                .map(i -> (Device) this.devicesContainer.getItem(i).getItemProperty(VALUE_PROPERTY).getValue())
                .collect(Collectors.toCollection(Vector::new)));

        group.setValidPrograms(((Set<Integer>) (this.selPrograms.getValue())).stream()
                .map(i -> (Program) this.programsContainer.getItem(i).getItemProperty(VALUE_PROPERTY).getValue())
                .collect(Collectors.toCollection(Vector::new)));

        for (final IUserGroupUpdatedEventListener l : this.listeners) {
            l.onUserGroupUpdated(group);
        }

        this.setVisible(false);
        this.getUI().removeWindow(this);
    }

    /**
     * Fügt einen Listener hinzu, der benachrichtigt werden möchte, sobald eine
     * Benutzergruppe erstellt oder bearbeitet wurde.
     *
     * @param l Der Listener
     */
    public void addUserGroupUpdatedEventListener(IUserGroupUpdatedEventListener l) {
        this.listeners.add(l);
    }

    /**
     * Der Modus, in dem das Fenster geöffnet werden kann
     *
     * @author Oliver Kabierschke
     */
    public enum Mode {
        EDIT_GROUP, CREATE_GROUP,
    }
}
