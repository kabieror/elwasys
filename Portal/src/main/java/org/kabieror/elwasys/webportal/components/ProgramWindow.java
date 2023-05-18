package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.kabieror.elwasys.common.NoDataFoundException;
import org.kabieror.elwasys.common.Program;
import org.kabieror.elwasys.common.ProgramType;
import org.kabieror.elwasys.common.UserGroup;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.WashportalUtilities;
import org.kabieror.elwasys.webportal.events.IProgramUpdatedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Dieses Fenster erlaubt das Bearbeiten und Erstellen von Programmen
 *
 * @author Oliver Kabierschke
 */
public class ProgramWindow extends Window {

    /**
     *
     */
    private static final long serialVersionUID = 6281036476918365426L;
    private static final String CAPTION_PROPERTY = "caption";
    private static final String VALUE_PROPERTY = "value";
    private static final String STATIC_PROPERTY = "static";
    private static final String DYNAMIC_PROPERTY = "dynamic";
    private static final String HOURS_PROPERTY = "hours";
    private static final String MINUTES_PROPERTY = "minutes";
    private static final String SECONDS_PROPERTY = "seconds";
    /**
     * Der Modus des Formulars
     */
    private final Mode mode;
    /**
     * Listener, die nach Abschluss benachrichtigt werden
     */
    private final List<IProgramUpdatedEventListener> listeners = new Vector<IProgramUpdatedEventListener>();
    private final ObjectProperty<BigDecimal> priceProperty = new ObjectProperty<BigDecimal>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> flagfallProperty = new ObjectProperty<BigDecimal>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> rateProperty = new ObjectProperty<BigDecimal>(BigDecimal.ZERO);
    private final ObjectProperty<Integer> earliestAutoEndProperty = new ObjectProperty<Integer>(0);
    private final ObjectProperty<Integer> maxDurationProperty = new ObjectProperty<Integer>(0);
    private final ObjectProperty<Integer> freeDurationProperty = new ObjectProperty<Integer>(0);
    Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * Das Programm, das bearbeitet wird
     */
    private Program programToEdit;
    /**
     * Der Name des Programms
     */
    private TextField tfName;
    /**
     * Der Typ des Programms (statisch, dynamisch)
     */
    private OptionGroup ogType;
    /**
     * Der Preis des Programms, falls statisch
     */
    private TextField tfPrice;
    /**
     * Die Grundgebühr des Programms, falls dynamisch
     */
    private TextField tfFlagfall;
    /**
     * Die Zeitgebühr des Programms, falls dynamisch
     */
    private TextField tfRate;
    /**
     * Das Abrechnungsintervall des Programms, falls dynamisch
     */
    private ComboBox cbTimeUnit;
    /**
     * Gibt an, ob das Programm aufgrund von Leistungsmessung automatisch
     * beendet werden soll.
     */
    private CheckBox cbAutoEnd;
    /**
     * Die früheste Zeit nach Programmstart, bei der das Programm durch
     * Leistungsmessung automatisch beendet werden darf.
     */
    private TextField tfEarliestAutoEnd;
    private ComboBox cbEarliestAutoEndTimeUnit;

    /**
     * Benutzergruppen-Auswahl
     */
    private TwinColSelect selGroups;
    private IndexedContainer groupsContainer;

    /**
     * Die Höchstdauer des Programms
     */
    private TextField tfMaxDuration;
    /**
     * Die Zeiteinheit der Höchstdauer
     */
    private ComboBox cbMaxDurationTimeUnit;

    /**
     * Die Dauer, in der das Programm kostenlos ist
     */
    private TextField tfFreeDuration;
    /**
     * Die Zeiteinheit der kostenlosen Anfangszeit
     */
    private ComboBox cbFreeDurationTimeUnit;
    /**
     * Der Aktivierungzustand des Programms
     */
    private CheckBox cbEnabled;

    /**
     * Konstruktor
     */
    public ProgramWindow(Program programToEdit) {
        this.mode = Mode.EDIT_PROGRAM;

        this.programToEdit = programToEdit;
        this.init();

        this.tfName.setValue(programToEdit.getName());

        Long maxDuration = programToEdit.getMaxDuration().getSeconds();
        if (maxDuration % 3600 == 0) {
            maxDuration /= 3600;
            this.cbMaxDurationTimeUnit.select(HOURS_PROPERTY);
        } else if (maxDuration % 60 == 0) {
            maxDuration /= 60;
            this.cbMaxDurationTimeUnit.select(MINUTES_PROPERTY);
        } else {
            this.cbMaxDurationTimeUnit.select(SECONDS_PROPERTY);
        }
        this.maxDurationProperty.setValue(maxDuration.intValue());

        Long freeDuration = programToEdit.getFreeDuration().getSeconds();
        if (freeDuration % 3600 == 0) {
            freeDuration /= 3600;
            this.cbFreeDurationTimeUnit.select(HOURS_PROPERTY);
        } else if (freeDuration % 60 == 0) {
            freeDuration /= 60;
            this.cbFreeDurationTimeUnit.select(MINUTES_PROPERTY);
        } else {
            this.cbFreeDurationTimeUnit.select(SECONDS_PROPERTY);
        }
        this.freeDurationProperty.setValue(freeDuration.intValue());

        Long earliestAutoEnd = programToEdit.getEarliestAutoEnd().getSeconds();
        if (earliestAutoEnd % 3600 == 0) {
            earliestAutoEnd /= 3600;
            this.cbEarliestAutoEndTimeUnit.select(HOURS_PROPERTY);
        } else if (earliestAutoEnd % 60 == 0) {
            earliestAutoEnd /= 60;
            this.cbEarliestAutoEndTimeUnit.select(MINUTES_PROPERTY);
        } else {
            this.cbEarliestAutoEndTimeUnit.select(SECONDS_PROPERTY);
        }
        this.earliestAutoEndProperty.setValue(earliestAutoEnd.intValue());

        this.cbEnabled.setValue(programToEdit.isEnabled());

        this.cbAutoEnd.setValue(programToEdit.isAutoEnd());

        switch (programToEdit.getType()) {
            case DYNAMIC:
                this.ogType.select(ProgramWindow.DYNAMIC_PROPERTY);
                this.flagfallProperty.setValue(programToEdit.getFlagfall());
                this.rateProperty.setValue(programToEdit.getRate());
                switch (programToEdit.getTimeUnit()) {
                    case HOURS:
                        this.cbTimeUnit.select(HOURS_PROPERTY);
                        break;
                    case MINUTES:
                        this.cbTimeUnit.select(MINUTES_PROPERTY);
                        break;
                    case SECONDS:
                    default:
                        this.cbTimeUnit.select(SECONDS_PROPERTY);
                        break;
                }
                break;
            case FIXED:
            default:
                this.ogType.select(ProgramWindow.STATIC_PROPERTY);
                this.priceProperty.setValue(programToEdit.getFlagfall());
                break;
        }

        HashSet<Integer> selectedGroups = new HashSet<>();
        for (UserGroup g : programToEdit.getValidUserGroups()) {
            selectedGroups.add(g.getId());
        }
        this.selGroups.setValue(selectedGroups);
    }

    public ProgramWindow() {
        this.mode = Mode.CREATE_PROGRAM;
        this.init();
    }

    /**
     * Komponenten initialisieren
     */
    @SuppressWarnings("unchecked")
    private void init() {
        String caption;
        String btnSaveCaption;
        if (this.mode.equals(Mode.EDIT_PROGRAM)) {
            caption = "Programm bearbeiten";
            btnSaveCaption = "Speichern";
        } else {
            caption = "Programm erstellen";
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

        this.cbEnabled = new CheckBox("Aktiviert");
        this.cbEnabled.setDescription("Gibt an, ob das Programm zur Verfügung steht.");
        form.addComponent(this.cbEnabled);

        // Option-Group: Programmtyp
        this.ogType = new OptionGroup("Typ");
        form.addComponent(this.ogType);
        this.ogType.setRequired(true);
        this.ogType.setRequiredError("Bitte Typ auswählen");
        this.ogType.setValidationVisible(false);
        this.ogType.setSizeFull();
        this.ogType.addStyleName("horizontal");
        this.ogType.setItemCaptionPropertyId(CAPTION_PROPERTY);

        final IndexedContainer typeContainer = new IndexedContainer();
        this.ogType.setContainerDataSource(typeContainer);
        typeContainer.addContainerProperty(CAPTION_PROPERTY, String.class, null);

        final Item iStatic = typeContainer.addItem(ProgramWindow.STATIC_PROPERTY);
        iStatic.getItemProperty(CAPTION_PROPERTY).setValue("Statisch");
        final Item iDynamic = typeContainer.addItem(ProgramWindow.DYNAMIC_PROPERTY);
        iDynamic.getItemProperty(CAPTION_PROPERTY).setValue("Dynamisch");
        this.ogType.addValueChangeListener(e -> {
            final String val = (String) e.getProperty().getValue();
            if (val.equals(ProgramWindow.STATIC_PROPERTY)) {
                form.removeComponent(this.tfFlagfall);
                form.removeComponent(this.tfRate);
                form.removeComponent(this.cbTimeUnit);

                form.addComponent(this.tfPrice, 2);
            } else if (val.equals(ProgramWindow.DYNAMIC_PROPERTY)) {
                form.removeComponent(this.tfPrice);

                form.addComponent(this.tfFlagfall, 2);
                form.addComponent(this.tfRate, 3);
                form.addComponent(this.cbTimeUnit, 4);
            } else {
                WashportalManager.instance.showError("Fehler",
                        "Beim setzen des Typs ist ein Fehler aufgetreten. Der übermittelte Typ ist nicht bekannt.");
            }
        });

        // Textfeld: Grundgebühr
        this.tfFlagfall = new TextField("Grundgebühr");
        this.tfFlagfall.setSizeFull();
        this.tfFlagfall.setRequired(true);
        this.tfFlagfall.setRequiredError("Bitte Grundgebühr eingeben.");
        this.tfFlagfall.setValidationVisible(false);
        this.tfFlagfall.setPropertyDataSource(this.flagfallProperty);
        this.tfFlagfall.setNullRepresentation("");

        // Textfeld: Zeitpreis
        this.tfRate = new TextField("Zeitpreis");
        this.tfRate.setSizeFull();
        this.tfRate.setRequired(true);
        this.tfRate.setRequiredError("Bitte Zeitpreis eingeben.");
        this.tfRate.setValidationVisible(false);
        this.tfRate.setPropertyDataSource(this.rateProperty);
        this.tfRate.setNullRepresentation("");

        // Combo-Box: Abrechnungsintervall
        this.cbTimeUnit = new ComboBox("Abr.-Intervall");
        this.cbTimeUnit
                .setDescription("Abrechnungs-Intervall. Nach jeder verstrichenen Einheit wird der Zeitpreis fällig.");
        this.cbTimeUnit.setRequired(true);
        this.cbTimeUnit.setRequiredError("Bitte Abrechnungsintervall auswählen.");
        this.cbTimeUnit.setValidationVisible(false);
        this.cbTimeUnit.setItemCaptionPropertyId(CAPTION_PROPERTY);
        final IndexedContainer timeUnitContainer = new IndexedContainer();
        timeUnitContainer.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        this.cbTimeUnit.setContainerDataSource(timeUnitContainer);
        final Item iHours = timeUnitContainer.addItem(HOURS_PROPERTY);
        final Item iMinutes = timeUnitContainer.addItem(MINUTES_PROPERTY);
        final Item iSeconds = timeUnitContainer.addItem(SECONDS_PROPERTY);
        iHours.getItemProperty(CAPTION_PROPERTY).setValue("Stunden");
        iMinutes.getItemProperty(CAPTION_PROPERTY).setValue("Minuten");
        iSeconds.getItemProperty(CAPTION_PROPERTY).setValue("Sekunden");

        // Textfeld: Preis
        this.tfPrice = new TextField("Preis");
        this.tfPrice.setSizeFull();
        this.tfPrice.setRequired(true);
        this.tfPrice.setRequiredError("Bitte Preis eingeben.");
        this.tfPrice.setValidationVisible(false);
        this.tfPrice.setPropertyDataSource(this.priceProperty);
        this.tfPrice.setNullRepresentation("");

        // Komponentengruppe: Maximaldauer
        final CssLayout group = new CssLayout();
        form.addComponent(group);
        group.setCaption("Maximaldauer" + WashportalUtilities.getRequiredAsterisk());
        group.setCaptionAsHtml(true);
        group.addStyleName("v-component-group");
        group.setSizeFull();

        this.tfMaxDuration = new TextField();
        group.addComponent(this.tfMaxDuration);
        this.tfMaxDuration.setWidth("65%");
        this.tfMaxDuration.setPropertyDataSource(this.maxDurationProperty);
        this.tfMaxDuration.addValidator(new IntegerRangeValidator("Der Wert muss größer als 0 sein.", 1, null));
        this.tfMaxDuration.setRequired(true);
        this.tfMaxDuration.setRequiredError("Bitte Zahl eingeben.");
        this.tfMaxDuration.setNullRepresentation("");
        this.tfMaxDuration.addStyleName("required-hidden");
        this.tfMaxDuration.setValidationVisible(false);

        this.cbMaxDurationTimeUnit = new ComboBox();
        group.addComponent(this.cbMaxDurationTimeUnit);
        this.cbMaxDurationTimeUnit.setWidth("35%");
        this.cbMaxDurationTimeUnit.setItemCaptionPropertyId(CAPTION_PROPERTY);
        final IndexedContainer mbTimeUnitContainer = new IndexedContainer();
        this.cbMaxDurationTimeUnit.setContainerDataSource(mbTimeUnitContainer);
        mbTimeUnitContainer.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        final Item iMbHours = mbTimeUnitContainer.addItem(HOURS_PROPERTY);
        final Item iMbMinutes = mbTimeUnitContainer.addItem(MINUTES_PROPERTY);
        final Item iMbSeconds = mbTimeUnitContainer.addItem(SECONDS_PROPERTY);
        iMbHours.getItemProperty(CAPTION_PROPERTY).setValue("h");
        iMbMinutes.getItemProperty(CAPTION_PROPERTY).setValue("min");
        iMbSeconds.getItemProperty(CAPTION_PROPERTY).setValue("s");
        this.cbMaxDurationTimeUnit.addValidator(new Validator() {

            /**
             *
             */
            private static final long serialVersionUID = -1917820994644609027L;

            @Override
            public void validate(Object value) throws InvalidValueException {
                if (value == null) {
                    throw new InvalidValueException("Bitte Zeiteinheit auswählen");
                }
            }
        });
        this.cbMaxDurationTimeUnit.setValidationVisible(false);


        // Komponentengruppe: Freie Zeit
        final CssLayout freeGroup = new CssLayout();
        form.addComponent(freeGroup);
        freeGroup.setCaption("Freie Zeit" + WashportalUtilities.getRequiredAsterisk());
        freeGroup.setCaptionAsHtml(true);
        freeGroup.setDescription("Die freie Anfangszeit, in der ein Programmabbruch keine Kosten verursacht.");
        freeGroup.addStyleName("v-component-group");
        freeGroup.setSizeFull();

        this.tfFreeDuration = new TextField();
        freeGroup.addComponent(this.tfFreeDuration);
        this.tfFreeDuration.setWidth("65%");
        this.tfFreeDuration.setPropertyDataSource(this.freeDurationProperty);
        this.tfFreeDuration.addValidator(new IntegerRangeValidator("Der Wert darf nicht negativ sein.", 0, null));
        this.tfFreeDuration.setRequired(true);
        this.tfFreeDuration.setRequiredError("Bitte Zahl eingeben.");
        this.tfFreeDuration.addStyleName("required-hidden");
        this.tfFreeDuration.setNullRepresentation("");
        this.tfFreeDuration.setValidationVisible(false);

        this.cbFreeDurationTimeUnit = new ComboBox();
        freeGroup.addComponent(this.cbFreeDurationTimeUnit);
        this.cbFreeDurationTimeUnit.setWidth("35%");
        this.cbFreeDurationTimeUnit.setItemCaptionPropertyId(CAPTION_PROPERTY);
        final IndexedContainer fdTimeUnitContainer = new IndexedContainer();
        this.cbFreeDurationTimeUnit.setContainerDataSource(fdTimeUnitContainer);
        fdTimeUnitContainer.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        final Item iFdHours = fdTimeUnitContainer.addItem(HOURS_PROPERTY);
        final Item iFdMinutes = fdTimeUnitContainer.addItem(MINUTES_PROPERTY);
        final Item iFdSeconds = fdTimeUnitContainer.addItem(SECONDS_PROPERTY);
        iFdHours.getItemProperty(CAPTION_PROPERTY).setValue("h");
        iFdMinutes.getItemProperty(CAPTION_PROPERTY).setValue("min");
        iFdSeconds.getItemProperty(CAPTION_PROPERTY).setValue("s");
        this.cbFreeDurationTimeUnit.addValidator(new Validator() {

            /**
             *
             */
            private static final long serialVersionUID = -1917820994644609027L;

            @Override
            public void validate(Object value) throws InvalidValueException {
                if (value == null) {
                    throw new InvalidValueException("Bitte Zeiteinheit auswählen");
                }
            }
        });
        this.cbFreeDurationTimeUnit.setValidationVisible(false);


        this.cbAutoEnd = new CheckBox("Auto-Ende");
        this.cbAutoEnd.setDescription("Programmausführung aufgrund von Leistungsmessung automatisch beenden.");
        form.addComponent(this.cbAutoEnd);

        // Komponentengruppe: Frühestes Programmende
        final CssLayout earliestAutoEndGroup = new CssLayout();
        form.addComponent(earliestAutoEndGroup);
        earliestAutoEndGroup.setCaption("Frühester Abbruch" + WashportalUtilities.getRequiredAsterisk());
        earliestAutoEndGroup.setDescription(
                "Zeit ab Beginn der Programmausführung, in der niedrige gemessene Leistung " +
                        "nicht zum automatischen Beenden des laufenden Programms führen soll.");
        earliestAutoEndGroup.setCaptionAsHtml(true);
        earliestAutoEndGroup.addStyleName("v-component-group");
        earliestAutoEndGroup.setSizeFull();

        this.tfEarliestAutoEnd = new TextField();
        earliestAutoEndGroup.addComponent(this.tfEarliestAutoEnd);
        this.tfEarliestAutoEnd.setWidth("65%");
        this.tfEarliestAutoEnd.setPropertyDataSource(this.earliestAutoEndProperty);
        this.tfEarliestAutoEnd.addValidator(new IntegerRangeValidator("Der Wert darf nicht negativ sein.", 0, null));
        this.tfEarliestAutoEnd.setRequired(true);
        this.tfEarliestAutoEnd.setRequiredError("Bitte Zahl eingeben.");
        this.tfEarliestAutoEnd.setNullRepresentation("");
        this.tfEarliestAutoEnd.addStyleName("required-hidden");
        this.tfEarliestAutoEnd.setValidationVisible(false);

        this.cbEarliestAutoEndTimeUnit = new ComboBox();
        earliestAutoEndGroup.addComponent(this.cbEarliestAutoEndTimeUnit);
        this.cbEarliestAutoEndTimeUnit.setWidth("35%");
        this.cbEarliestAutoEndTimeUnit.setItemCaptionPropertyId(CAPTION_PROPERTY);
        final IndexedContainer mbTimeUnitContainer1 = new IndexedContainer();
        this.cbEarliestAutoEndTimeUnit.setContainerDataSource(mbTimeUnitContainer1);
        mbTimeUnitContainer1.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        final Item iMbHours1 = mbTimeUnitContainer1.addItem(HOURS_PROPERTY);
        final Item iMbMinutes1 = mbTimeUnitContainer1.addItem(MINUTES_PROPERTY);
        final Item iMbSeconds1 = mbTimeUnitContainer1.addItem(SECONDS_PROPERTY);
        iMbHours1.getItemProperty(CAPTION_PROPERTY).setValue("h");
        iMbMinutes1.getItemProperty(CAPTION_PROPERTY).setValue("min");
        iMbSeconds1.getItemProperty(CAPTION_PROPERTY).setValue("s");
        this.cbEarliestAutoEndTimeUnit.addValidator(new Validator() {

            /**
             *
             */
            private static final long serialVersionUID = -1917820994644609027L;

            @Override
            public void validate(Object value) throws InvalidValueException {
                if (value == null) {
                    throw new InvalidValueException("Bitte Zeiteinheit auswählen");
                }
            }
        });
        this.cbEarliestAutoEndTimeUnit.setValidationVisible(false);

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
                new Label("<h3 style='margin-top:20px;margin-bottom:0'>Benutzergruppen</h3>", ContentMode.HTML);
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
                this.logger.error("Unable to store program into database.", e1);
                WashportalManager.instance.showDatabaseError(e1);
            } catch (final Exception e1) {
                this.logger.error("Unable to save program.", e1);
                WashportalManager.instance.showError(e1);
            }
        });
        btnSave.addStyleName("primary");
        btnSave.setClickShortcut(KeyCode.ENTER);
    }

    /**
     * Speichert das Formular in die Datenbank
     *
     * @throws SQLException
     * @throws NoDataFoundException
     */
    private void save() throws SQLException {
        // Felder validieren
        try {
            this.tfName.validate();
            this.ogType.validate();
            this.tfMaxDuration.validate();
            this.cbMaxDurationTimeUnit.validate();
            this.tfFreeDuration.validate();
            this.cbFreeDurationTimeUnit.validate();
            this.cbEarliestAutoEndTimeUnit.validate();
            if (this.ogType.getValue() != null) {
                if (this.ogType.getValue().equals(STATIC_PROPERTY)) {
                    this.tfPrice.validate();
                }
                if (this.ogType.getValue().equals(DYNAMIC_PROPERTY)) {
                    this.tfFlagfall.validate();
                    this.tfRate.validate();
                    this.cbTimeUnit.validate();
                }
            }
        } catch (final InvalidValueException e) {
            this.tfName.setValidationVisible(true);
            this.ogType.setValidationVisible(true);
            this.tfMaxDuration.setValidationVisible(true);
            this.cbMaxDurationTimeUnit.setValidationVisible(true);
            this.tfFreeDuration.setValidationVisible(true);
            this.cbFreeDurationTimeUnit.setValidationVisible(true);
            this.cbEarliestAutoEndTimeUnit.setValidationVisible(true);
            if (this.ogType.getValue() != null) {
                if (this.ogType.getValue().equals(STATIC_PROPERTY)) {
                    this.tfPrice.setValidationVisible(true);
                }
                if (this.ogType.getValue().equals(DYNAMIC_PROPERTY)) {
                    this.tfFlagfall.setValidationVisible(true);
                    this.tfRate.setValidationVisible(true);
                    this.cbTimeUnit.setValidationVisible(true);
                }
            }
            return;
        }

        ProgramType type;
        switch ((String) this.ogType.getValue()) {
            case DYNAMIC_PROPERTY:
                type = ProgramType.DYNAMIC;
                break;
            case STATIC_PROPERTY:
            default:
                type = ProgramType.FIXED;
                break;
        }

        ChronoUnit timeUnit = null;
        if (this.cbTimeUnit.getValue() != null) {
            switch ((String) this.cbTimeUnit.getValue()) {
                case HOURS_PROPERTY:
                    timeUnit = ChronoUnit.HOURS;
                    break;
                case MINUTES_PROPERTY:
                    timeUnit = ChronoUnit.MINUTES;
                    break;
                case SECONDS_PROPERTY:
                default:
                    timeUnit = ChronoUnit.SECONDS;
                    break;
            }
        }

        Duration maxDuration;
        switch ((String) this.cbMaxDurationTimeUnit.getValue()) {
            case HOURS_PROPERTY:
                maxDuration = Duration.ofHours(this.maxDurationProperty.getValue());
                break;
            case MINUTES_PROPERTY:
                maxDuration = Duration.ofMinutes(this.maxDurationProperty.getValue());
                break;
            case SECONDS_PROPERTY:
            default:
                maxDuration = Duration.ofSeconds(this.maxDurationProperty.getValue());
                break;
        }

        Duration freeDuration;
        switch ((String) this.cbFreeDurationTimeUnit.getValue()) {
            case HOURS_PROPERTY:
                freeDuration = Duration.ofHours(this.freeDurationProperty.getValue());
                break;
            case MINUTES_PROPERTY:
                freeDuration = Duration.ofMinutes(this.freeDurationProperty.getValue());
                break;
            case SECONDS_PROPERTY:
            default:
                freeDuration = Duration.ofSeconds(this.freeDurationProperty.getValue());
                break;
        }

        Duration earliestAutoEnd;
        switch ((String) this.cbEarliestAutoEndTimeUnit.getValue()) {
            case HOURS_PROPERTY:
                earliestAutoEnd = Duration.ofHours(this.earliestAutoEndProperty.getValue());
                break;
            case MINUTES_PROPERTY:
                earliestAutoEnd = Duration.ofMinutes(this.earliestAutoEndProperty.getValue());
                break;
            case SECONDS_PROPERTY:
            default:
                earliestAutoEnd = Duration.ofSeconds(this.earliestAutoEndProperty.getValue());
                break;
        }

        BigDecimal flagfall;
        switch (type) {
            case DYNAMIC:
                flagfall = this.flagfallProperty.getValue();
                break;
            case FIXED:
            default:
                flagfall = this.priceProperty.getValue();
                break;
        }

        // Load selected user groups
        final List<UserGroup> validUserGroups = ((Set<Integer>) (this.selGroups.getValue())).stream()
                .map(i -> (UserGroup) this.groupsContainer.getItem(i).getItemProperty(VALUE_PROPERTY).getValue())
                .collect(Collectors.toCollection(Vector::new));

        final Program program;
        switch (this.mode) {
            case CREATE_PROGRAM:
                program =
                        new Program(WashportalManager.instance.getDataManager(), this.tfName.getValue(), type, flagfall,
                                this.rateProperty.getValue(), timeUnit, maxDuration, freeDuration,
                                this.cbAutoEnd.getValue(), earliestAutoEnd, this.cbEnabled.getValue(), validUserGroups);
                break;
            case EDIT_PROGRAM:
                program = this.programToEdit;
                program.modify(this.tfName.getValue(), type, flagfall, this.rateProperty.getValue(), timeUnit,
                        maxDuration, freeDuration, this.cbAutoEnd.getValue(), earliestAutoEnd,
                        this.cbEnabled.getValue(), validUserGroups);
                break;
            default:
                this.logger.error("Unknown state. Cannot save user.");
                WashportalManager.instance.showError("Zustandsfehler", "Dieses Fenster hat einen ungültigen Zustand.");
                return;
        }

        for (final IProgramUpdatedEventListener l : this.listeners) {
            l.onProgramUpdated(program);
        }

        this.setVisible(false);
        this.getUI().removeWindow(this);
    }

    /**
     * Fügt einen Listener hinzu, der benachrichtigt werden möchte, sobald ein
     * Programm erstellt oder bearbeitet wurde.
     *
     * @param l Der Listener
     */
    public void addProgramUpdatedEventListener(IProgramUpdatedEventListener l) {
        this.listeners.add(l);
    }

    /**
     * Der Modus, in dem das Fenster geöffnet werden kann
     *
     * @author Oliver Kabierschke
     */
    public enum Mode {
        EDIT_PROGRAM, CREATE_PROGRAM,
    }
}
