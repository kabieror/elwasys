package org.kabieror.elwasys.webportal;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.*;
import org.kabieror.elwasys.webportal.components.PasswordForgotWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Öffentliches Layout mit Login-Bildschirm
 *
 * @author Oliver Kabierschke
 *
 */
public class PublicLayout extends VerticalLayout {

    /**
     *
     */
    private static final long serialVersionUID = -8341674810128009429L;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public PublicLayout(WaschportalUI ui) {
        this.setSizeFull();
        this.setStyleName("login-container");

        final Panel loginPanel = new Panel();
        this.addComponent(loginPanel);
        loginPanel.setSizeUndefined();
        loginPanel.setStyleName("login-panel");
        this.setComponentAlignment(loginPanel, Alignment.MIDDLE_CENTER);

        final VerticalLayout loginContent = new VerticalLayout();
        loginPanel.setContent(loginContent);
        loginContent.setStyleName("login-panel-content");
        loginContent.setSpacing(true);

        final CssLayout top = new CssLayout();
        loginContent.addComponent(top);
        top.setStyleName("login-labels");

        final Label titleLogin = new Label("Login");
        top.addComponent(titleLogin);
        titleLogin.setSizeUndefined();
        titleLogin.addStyleName("h4 colored");

        final Label titleCaption = new Label("Waschportal");
        top.addComponent(titleCaption);
        titleCaption.setSizeUndefined();
        titleCaption.addStyleName("h3 float-right");

        final HorizontalLayout form = new HorizontalLayout();
        loginContent.addComponent(form);
        form.setSpacing(true);

        final TextField tfUser = new TextField("Benutzername");
        form.addComponent(tfUser);
        tfUser.setIcon(FontAwesome.USER);
        tfUser.setStyleName("inline-icon");

        final PasswordField tfPassword = new PasswordField("Passwort");
        form.addComponent(tfPassword);
        tfPassword.setIcon(FontAwesome.LOCK);
        tfPassword.setStyleName("inline-icon");

        final CssLayout form2 = new CssLayout();
        loginContent.addComponent(form2);
        form2.setStyleName("login-labels");

        final Button btnLogin = new Button("Login");
        form2.addComponent(btnLogin);
        btnLogin.addClickListener(e -> {
            try {
                if (!WashportalManager.instance.getSessionManager().login(tfUser.getValue(),
                        tfPassword.getValue())) {
                    // Login fehlgeschlagen
                    final Notification msg = new Notification("Login fehlgeschlagen",
                            "Bitte prüfen Sie die Anmeldedaten und versuchen Sie es erneut.");
                    msg.setDelayMsec(3000);
                    msg.setPosition(Position.TOP_CENTER);
                    msg.setStyleName("error bar");
                    msg.show(Page.getCurrent());
                } else {
                    // Login erfolgreich
                    ui.loadSessionContent();
                }
            } catch (final SQLException e1) {
                this.logger.error("Failed to log user in.", e1);
                final Notification msg = new Notification("Datenbankfehler", e1.getMessage());
                msg.setDelayMsec(-1);
                msg.setPosition(Position.TOP_CENTER);
                msg.setStyleName("error bar");
                msg.show(Page.getCurrent());
            }
        });
        btnLogin.setClickShortcut(KeyCode.ENTER);
        btnLogin.addStyleName("primary float-right");

        final Button btnPwForgot = new Button("Passwort vergessen?");
        form2.addComponent(btnPwForgot);
        btnPwForgot.addStyleName("link float-right tiny");
        btnPwForgot.addClickListener(e -> {
            try {
                UI.getCurrent().addWindow(new PasswordForgotWindow());
            } catch (final SQLException e1) {
                this.logger.error("Could not show password forgot window.", e1);
                WashportalManager.instance.showDatabaseError(e1);
            } catch (final Exception e1) {
                this.logger.error("Could not show password forgot window.", e1);
                WashportalManager.instance.showError(e1);
            }
        });

        tfUser.focus();
    }

}
