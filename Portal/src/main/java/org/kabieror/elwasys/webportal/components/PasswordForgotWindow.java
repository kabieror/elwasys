package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.*;
import org.apache.commons.mail.EmailException;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Dieses Fenster zeigt die Umsätze eines Benutzers an
 * 
 * @author Oliver Kabierschke
 *
 */
public class PasswordForgotWindow extends Window {

    /**
     *
     */
    private static final long serialVersionUID = -4365804859608421600L;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private TextField tfEmail;

    @SuppressWarnings("unchecked")
    public PasswordForgotWindow() throws SQLException {
        this.setCaption("Passwort zurücksetzen");
        this.setWidth("22em");
        this.setResizable(false);
        this.setClosable(true);
        this.setModal(true);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        content.addComponent(new Label(
                "Bitte gib hier deine Email-Adresse ein. Du wirst einen Link erhalten, mit welchem du ein neues Passwort setzen kannst."));

        tfEmail = new TextField();
        content.addComponent(tfEmail);
        tfEmail.setCaption("Email");
        tfEmail.setWidth("100%");
        tfEmail.setRequired(true);
        tfEmail.setRequiredError("Bitte altes Passwort eingeben.");
        tfEmail.addValidator(new EmailValidator("Bitte gültige Email-Adresse eingeben."));
        tfEmail.setValidationVisible(false);


        // FormularfuÃŸ mit Buttons
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

        final Button btnSave = new Button("OK");
        footer.addComponent(btnSave);
        btnSave.addClickListener(e -> {
            try {
                this.execute();
            } catch (final Exception e1) {
                this.logger.error("Error during sending password reset email.", e1);
                WashportalManager.instance.showError("Interner Fehler", e1.getLocalizedMessage());
            }
        });
        btnSave.addStyleName("primary");
        btnSave.setClickShortcut(KeyCode.ENTER);
    }

    private void execute() {
        // Felder validieren
        try {
            this.tfEmail.validate();
        } catch (final InvalidValueException e) {
            this.tfEmail.setValidationVisible(true);
            return;
        }

        try {
            User user = WashportalManager.instance.getDataManager()
                    .getUserByEmail(tfEmail.getValue());
            if (user == null) {
                WashportalManager.instance.showError("Email unbekannt",
                        "Es konnte kein Benutzer mit der angegebenen Email-Adresse gefunden werden.");
                return;
            }
            String key = user.generatePasswordResetKey();
            String message = "Hallo " + user.getName() + ",\n\n";
            message += "bitte besuche die folgende Webseite zum Setzen eines neuen Passworts.\n";
            message += WashportalManager.instance.getUtilities().getPasswordResetUrl(key);
            message += "\n\n--\nWaschportal";
            WashportalManager.instance.getUtilities().sendEmail("Passwort zurücksetzen", message,
                    user);
        } catch (NoSuchAlgorithmException e) {
            this.logger.error("Could not change the password of a user.", e);
            WashportalManager.instance.showError("Interner Fehler", e.getLocalizedMessage());
            return;
        } catch (SQLException e) {
            this.logger.error("Could not change the password of a user.", e);
            WashportalManager.instance.showDatabaseError(e);
            return;
        } catch (EmailException e) {
            this.logger.error("Could not send the email.", e);
            WashportalManager.instance.showError("Interner Fehler",
                    "Konnte die Email nicht senden.");
            return;
        }

        this.setVisible(false);
        this.getUI().removeWindow(this);
        WashportalManager.instance.showSuccessMessage("Email versandt",
                "Die Email wurde versandt. Prüfe dein Postfach!");
    }
}
