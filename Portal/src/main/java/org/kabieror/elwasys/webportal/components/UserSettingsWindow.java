package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.*;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Dieses Fenster zeigt die Umsätze eines Benutzers an
 *
 * @author Oliver Kabierschke
 *
 */
public class UserSettingsWindow extends Window {

    /**
     *
     */
    private static final long serialVersionUID = -4365804859608421600L;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TextField tfEmail;

    private final CheckBox cbEmailNotification;

    private final TextField tfPushoverKey;

    private final User user;

    @SuppressWarnings("unchecked")
    public UserSettingsWindow(User user) throws SQLException {
        this.user = user;

        this.setCaption("Benutzer ändern - " + user.getName());
        this.setWidth("35em");
        this.setResizable(false);
        this.setClosable(true);
        this.setModal(true);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        final FormLayout form = new FormLayout();

        this.tfEmail = new TextField();
        this.tfEmail.setCaption("Email");
        this.tfEmail.setWidth("100%");
        this.tfEmail.setRequiredError("Für Benachrichtigungen wird eine Email-Adresse benötigt.");
        this.tfEmail.addValidator(new EmailValidator("Dies ist keine gültige Email-Adresse."));
        this.tfEmail.setValidationVisible(false);
        this.tfEmail.setInputPrompt("Email-Adresse hier eintragen");
        if (this.user.getEmail() != null) {
            this.tfEmail.setValue(this.user.getEmail());
        }
        form.addComponent(this.tfEmail);

        final HorizontalLayout emailNotifContainer = new HorizontalLayout();
        emailNotifContainer.setCaption("Email-Benachrichtigung");
        this.cbEmailNotification = new CheckBox();
        this.cbEmailNotification.setDescription("Sende Benachrichtigungen über abgeschlossene "
                + "Waschvorgänge an meine Email-Adresse.");
        this.cbEmailNotification.setValue(this.user.getEmailNotification());
        emailNotifContainer.addComponent(this.cbEmailNotification);
        form.addComponent(emailNotifContainer);

        this.tfPushoverKey = new TextField();
        this.tfPushoverKey.setCaption("Pushover-Key");
        this.tfPushoverKey.setWidth("100%");
        this.tfPushoverKey.addValidator(new RegexpValidator("[a-zA-Z0-9]+",
                "Der Schlüssel muss aus Zahlen und Buchstaben bestehen."));
        this.tfPushoverKey.setValidationVisible(false);
        this.tfPushoverKey.setInputPrompt("Schlüssel hier eintragen");
        if (this.user.getPushoverUserKey() != null) {
            this.tfPushoverKey.setValue(this.user.getPushoverUserKey());
        }
        this.tfPushoverKey.setDescription(
                "Trage deinen User-Key von <a href='http://pushover.net' target='blank'>Pushover.net</a> "
                        + "hier ein, um dich per Push-Benachrichtigung über beendete Waschvorgänge "
                        + "benachrichtigen zu lassen. Lade dir hierzu zusätzlich die Pushover-App "
                        + "auf dein Smartphone herunter.");
        form.addComponent(this.tfPushoverKey);

        content.addComponent(form);

        // Formularfuß mit Buttons
        final HorizontalLayout footer = new HorizontalLayout();
        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName("v-window-bottom-toolbar");

        final Label footerText = new Label("");
        footer.addComponent(footerText);
        footer.setExpandRatio(footerText, 1);

        final Button btnCancel = new Button("Abbrechen");
        btnCancel.addClickListener(e -> {
            this.setVisible(false);
            this.getUI().removeWindow(this);
        });
        footer.addComponent(btnCancel);

        final Button btnSave = new Button("OK");
        btnSave.addClickListener(e -> {
            try {
                this.save();
            } catch (final Exception e1) {
                this.logger.error("Error during setting user settings.", e1);
                WashportalManager.instance.showError("Interner Fehler", e1.getLocalizedMessage());
            }
        });
        btnSave.addStyleName("primary");
        btnSave.setClickShortcut(KeyCode.ENTER);
        footer.addComponent(btnSave);

        content.addComponent(footer);
    }

    private void save() {
        this.tfEmail.setRequired(this.cbEmailNotification.getValue());
        // Felder validieren
        try {
            this.tfEmail.validate();
            this.tfPushoverKey.validate();
        } catch (final InvalidValueException e) {
            this.tfEmail.setValidationVisible(true);
            this.tfPushoverKey.setValidationVisible(true);
            return;
        }

        try {
            this.user.modify(this.user.getName(), this.user.getUsername(), this.tfEmail.getValue(),
                    this.user.getCardIds(), this.user.isBlocked(), this.user.isAdmin(),
                    this.cbEmailNotification.getValue(), this.user.getGroup(), this.user.isPushEnabled());
            this.user.setPushoverUserKey(this.tfPushoverKey.getValue());
        } catch (final SQLException e) {
            this.logger.error("Could not modify the user.", e);
            WashportalManager.instance.showDatabaseError(e);
            return;
        }

        this.setVisible(false);
        this.getUI().removeWindow(this);
    }
}
