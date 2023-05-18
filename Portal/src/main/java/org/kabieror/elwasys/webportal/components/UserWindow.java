package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.common.UserGroup;
import org.kabieror.elwasys.common.Utilities;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.events.IUserUpdatedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

/**
 * Dieses Fenster erlaubt das Bearbeiten und Erstellen von Benutzern
 *
 * @author Oliver Kabierschke
 *
 */
public class UserWindow extends Window {

    /**
     *
     */
    private static final long serialVersionUID = 8374941531742336299L;

    private static final String CAPTION_PROPERTY = "caption";
    private static final String VALUE_PROPERTY = "value";

    /**
     * Der Modus des Formulars
     */
    private final Mode mode;
    /**
     * Listener, die nach Abschluss benachrichtigt werden
     */
    private final List<IUserUpdatedEventListener> listeners =
            new Vector<IUserUpdatedEventListener>();
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private User userToEdit;
    private TextField tfName;
    private TextField tfUsername;
    private TextField tfEmail;
    private TextArea tfCardIds;
    private ComboBox cbUserGroup;
    private IndexedContainer groupsContainer;
    private CheckBox cbBlocked;
    private CheckBox cbSendPassword;

    /**
     * Konstruktor
     */
    public UserWindow(User userToEdit) {
        this.mode = Mode.EDIT_USER;
        this.userToEdit = userToEdit;
        this.init();
        this.tfName.setValue(userToEdit.getName());
        this.tfUsername.setValue(userToEdit.getUsername());
        this.tfEmail.setValue(userToEdit.getEmail());
        this.tfCardIds.setValue(StringUtils.join(userToEdit.getCardIds(), "\n"));
        this.cbUserGroup.setValue(userToEdit.getGroup().getId());
        this.cbBlocked.setValue(userToEdit.isBlocked());
    }

    public UserWindow() {
        this.mode = Mode.CREATE_USER;
        this.init();
    }

    private void init() {
        String caption;
        String btnSaveCaption;
        if (this.mode.equals(Mode.EDIT_USER)) {
            caption = "Benutzer bearbeiten";
            btnSaveCaption = "Speichern";
        } else {
            caption = "Benutzer erstellen";
            btnSaveCaption = "Erstellen";
        }

        // Beschriftung des Fensters
        this.setCaption(caption);
        this.setWidth("35em");
        this.setResizable(false);
        this.setModal(true);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        final FormLayout form = new FormLayout();
        content.addComponent(form);
        form.setSizeFull();

        this.tfName = new TextField("Name");
        form.addComponent(this.tfName);
        this.tfName.setRequired(true);
        this.tfName.setRequiredError("Bitte Name eingeben.");
        this.tfName.setValidationVisible(false);
        this.tfName.setSizeFull();
        this.tfName.setMaxLength(50);

        this.tfUsername = new TextField("Username");
        form.addComponent(this.tfUsername);
        this.tfUsername.setRequired(true);
        this.tfUsername.setRequiredError("Bitte Benutzernamen eingeben.");
        this.tfUsername.setValidationVisible(false);
        this.tfUsername.setSizeFull();

        // Das Datenbankfeld hat die maximale Länge 50. Beim Löschen wird ein
        // 10-Zeichen langer Zusatz an den Benutzernamen gehängt.
        this.tfUsername.setMaxLength(40);

        this.tfEmail = new TextField("Email");
        form.addComponent(this.tfEmail);
        this.tfEmail.addValidator(new EmailValidator("Das ist keine gültige Email-Adresse"));
        this.tfEmail.setSizeFull();
        this.tfEmail.setRequiredError(
                "Für das Zusenden eines Passworts wird eine Email-Adresse benötigt.");
        this.tfEmail.setValidationVisible(false);
        this.tfEmail.setMaxLength(50);

        this.tfCardIds = new TextArea("Kartennummern");
        this.tfCardIds.setDescription(
                "Die Kartennnummern, die dem Benutzer zugeordnet sind. Eine Nummer pro Zeile.");
        form.addComponent(this.tfCardIds);
        this.tfCardIds.setWidth("100%");
        this.tfCardIds.addValidator(new Validator() {

            @Override
            public void validate(Object value) throws InvalidValueException {
                if (!(value instanceof String)) {
                    throw new InvalidValueException("Ungültiger Datentyp.");
                }
                final String str = (String) value;
                final String[] values = str.split("\n");
                for (final String v : values) {
                    if (v.isEmpty()) {
                        continue;
                    }
                    if (!v.matches("^\\d+$")) {
                        throw new InvalidValueException(
                                "Die Kartennummer '" + v + "' ist ungültig.");
                    }
                    User u;
                    try {
                        u = WashportalManager.instance.getDataManager().getUserByCardId(v);
                    } catch (final SQLException e) {
                        WashportalManager.instance.showDatabaseError(e);
                        throw new InvalidValueException(
                                "Konnte nicht auf doppelte Verwendung der Kartennummern prüfen.");
                    }
                    if (u != null && u != UserWindow.this.userToEdit) {
                        throw new InvalidValueException(
                                "Die Karte '" + v + "' ist bereits zu Benutzer " + u.getName()
                                        + " (" + u.getUsername() + ") zugeordnet.");
                    }
                }
            }
        });


        // Auswahl: Standort
        this.cbUserGroup = new ComboBox("Benutzergruppe");
        form.addComponent(this.cbUserGroup);
        this.cbUserGroup.setRequired(true);
        this.cbUserGroup.setRequiredError("Bitte Benutzergruppe auswählen");
        this.cbUserGroup.setValidationVisible(false);
        this.groupsContainer = new IndexedContainer();
        this.cbUserGroup.setContainerDataSource(this.groupsContainer);
        this.cbUserGroup.setItemCaptionPropertyId(CAPTION_PROPERTY);
        this.groupsContainer.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        this.groupsContainer.addContainerProperty(VALUE_PROPERTY, UserGroup.class, null);

        try {
            for (final UserGroup g : WashportalManager.instance.getDataManager().getUserGroups()) {
                final Item i = this.groupsContainer.addItem(g.getId());
                i.getItemProperty(CAPTION_PROPERTY).setValue(g.getName());
                i.getItemProperty(VALUE_PROPERTY).setValue(g);
            }
        } catch (final SQLException e2) {
            this.logger.error("Could not load the available user groups");
            WashportalManager.instance.showDatabaseError(e2);
        }


        this.cbBlocked = new CheckBox();
        final HorizontalLayout blockedWrapper = new HorizontalLayout();
        blockedWrapper.setCaption("Gesperrt");
        blockedWrapper.addComponent(this.cbBlocked);
        form.addComponent(blockedWrapper);

        this.cbSendPassword = new CheckBox();
        this.cbSendPassword.setCaption("Sende dem Benutzer per Email ein neues Passwort");
        this.cbSendPassword.addStyleName("small-label");
        this.cbSendPassword.setValue(this.mode == Mode.CREATE_USER);
        final HorizontalLayout sendPwWrapper = new HorizontalLayout();
        sendPwWrapper.setCaption("Neues Passwort");
        sendPwWrapper.addComponent(this.cbSendPassword);
        form.addComponent(sendPwWrapper);

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
                this.logger.error("Unable to store user into database.", e1);
                WashportalManager.instance.showDatabaseError(e1);
            } catch (final Exception e1) {
                this.logger.error("Unable to save user", e1);
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
     */
    private void save() throws SQLException {
        this.tfEmail.setRequired(this.cbSendPassword.getValue());
        // Felder validieren
        try {
            this.tfName.validate();
            this.tfUsername.validate();
            this.tfCardIds.validate();
            this.tfEmail.validate();
            this.cbUserGroup.validate();
        } catch (final InvalidValueException e) {
            this.tfName.setValidationVisible(true);
            this.tfUsername.setValidationVisible(true);
            this.tfCardIds.setValidationVisible(true);
            this.tfEmail.setValidationVisible(true);
            this.cbUserGroup.setValidationVisible(true);
            return;
        }

        final String[] cardIds = this.tfCardIds.getValue().split("\n+");

        final UserGroup userGroup =
                (UserGroup) this.groupsContainer.getContainerProperty(this.cbUserGroup.getValue(), VALUE_PROPERTY)
                        .getValue();

        User user;
        switch (this.mode) {
        case CREATE_USER:
            user = new User(WashportalManager.instance.getDataManager(), this.tfName.getValue(),
                    this.tfUsername.getValue(), this.tfEmail.getValue(), cardIds, this.cbBlocked.getValue(), false,
                    !this.tfEmail.getValue().isEmpty(), userGroup);
            break;
        case EDIT_USER:
            user = this.userToEdit;
            this.userToEdit.modify(this.tfName.getValue(), this.tfUsername.getValue(),
                    this.tfEmail.getValue(), cardIds, this.cbBlocked.getValue(), this.userToEdit.isAdmin(),
                    !this.tfEmail.getValue().isEmpty(), userGroup, true);
            break;
        default:
            this.logger.error("Unknown state. Cannot save user.");
            WashportalManager.instance.showError("Zustandsfehler",
                    "Dieses Fenster hat einen ungültigen Zustand.");
            return;
        }
        for (final IUserUpdatedEventListener l : this.listeners) {
            l.onUserUpdated(user);
        }

        // Send new password
        if (this.cbSendPassword.getValue()) {
            try {
                final String newPw = Utilities.generatePassword();
                user.changePassword(newPw);
                String message = "Hallo " + user.getName() + ",\n\n";
                message += "hier ist dein neues Passwort für das Waschportal: " + newPw + "\n";
                message += "Zusammen mit deinem Benutzernamen '" + user.getUsername()
                        + "' kannst du dich jetzt unter http://waschportal.hilaren.de einloggen "
                        + "und dort dein Guthaben und abgebuchte Waschvorgänge ansehen.\n\n";
                message += "--\nWaschportal";
                WashportalManager.instance.getUtilities().sendEmail("Waschportal - Neues Passwort",
                        message, user);
                WashportalManager.instance.showSuccessMessage("Erfolg", "Passwort wurde versandt");
            } catch (final NoSuchAlgorithmException e) {
                this.logger.error("Could not change the password of the user.", e);
                WashportalManager.instance.showError("Interner Fehler",
                        "Konnte das Passwort nicht ändern. " + e.getLocalizedMessage());
            } catch (final EmailException e) {
                this.logger.error("Could not send the email to the user.", e);
                WashportalManager.instance.showError("Interner Fehler",
                        "Konnte keine Email senden. " + e.getLocalizedMessage());
            }
        }

        this.setVisible(false);
        this.getUI().removeWindow(this);
    }

    /**
     * Fügt einen Listener hinzu, der benachrichtigt werden möchte, sobald ein
     * Benutzer erstellt oder bearbeitet wurde.
     *
     * @param l
     *            Der Listener
     */
    public void addUserUpdatedEventListener(IUserUpdatedEventListener l) {
        this.listeners.add(l);
    }

    /**
     * Der Modus, in dem das Fenster geöffnet werden kann
     *
     * @author Oliver Kabierschke
     */
    public enum Mode {
        EDIT_USER, CREATE_USER,
    }
}
