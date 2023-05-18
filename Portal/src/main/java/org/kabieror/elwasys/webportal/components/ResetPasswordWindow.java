package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Validator;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.*;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * In diesem Fenster kann ein Benutzer sein Passwort zurück setzen.
 *
 * @author Oliver Kabierschke
 *
 */
public class ResetPasswordWindow extends Window {

    /**
     *
     */
    private static final long serialVersionUID = -4365804859608421600L;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PasswordField tfNewPw1;
    private final PasswordField tfNewPw2;
    private final User user;

    @SuppressWarnings("unchecked")
    public ResetPasswordWindow(User user) throws SQLException {
        this.user = user;

        this.setCaption("Passwort ändern - " + user.getName());
        this.setWidth("22em");
        this.setResizable(false);
        this.setClosable(true);
        this.setModal(true);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        this.tfNewPw1 = new PasswordField();
        content.addComponent(this.tfNewPw1);
        this.tfNewPw1.setCaption("Neues Passwort");
        this.tfNewPw1.setWidth("100%");
        this.tfNewPw1.setRequired(true);
        this.tfNewPw1.setRequiredError("Bitte neues Passwort eingeben.");
        this.tfNewPw1.setValidationVisible(false);
        this.tfNewPw1.setMaxLength(50);
        this.tfNewPw1.focus();

        this.tfNewPw2 = new PasswordField();
        content.addComponent(this.tfNewPw2);
        this.tfNewPw2.setCaption("Wiederholung");
        this.tfNewPw2.setWidth("100%");
        this.tfNewPw2.setRequired(true);
        this.tfNewPw2.setRequiredError("Bitte neues Passwort wiederholen.");
        this.tfNewPw2.addValidator(new NewPasswordValidator());
        this.tfNewPw2.setValidationVisible(false);
        this.tfNewPw2.setMaxLength(50);


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
                this.save();
            } catch (final Exception e1) {
                this.logger.error("Error during changing the password.", e1);
                WashportalManager.instance.showError("Interner Fehler", e1.getLocalizedMessage());
            }
        });
        btnSave.addStyleName("primary");
        btnSave.setClickShortcut(KeyCode.ENTER);
    }

    private void save() {
        // Felder validieren
        try {
            this.tfNewPw1.validate();
            this.tfNewPw2.validate();
        } catch (final InvalidValueException e) {
            this.tfNewPw1.setValidationVisible(true);
            this.tfNewPw2.setValidationVisible(true);
            return;
        }

        try {
            this.user.changePassword(this.tfNewPw1.getValue());
        } catch (final NoSuchAlgorithmException e) {
            this.logger.error("Could not change the password of a user.", e);
            WashportalManager.instance.showError("Interner Fehler", e.getLocalizedMessage());
            return;
        } catch (final SQLException e) {
            this.logger.error("Could not change the password of a user.", e);
            WashportalManager.instance.showDatabaseError(e);
            return;
        }

        this.setVisible(false);
        this.getUI().removeWindow(this);

        WashportalManager.instance.showSuccessMessage("Erfolg",
                "Passwort wurde erfolgreich geändert.");
    }

    private class NewPasswordValidator implements Validator {

        @Override
        public void validate(Object value) throws InvalidValueException {
            if (!ResetPasswordWindow.this.tfNewPw1.getValue().equals(ResetPasswordWindow.this.tfNewPw2.getValue())) {
                throw new InvalidValueException("Die Passwörter stimmen nicht überein.");
            }
        }

    }
}
