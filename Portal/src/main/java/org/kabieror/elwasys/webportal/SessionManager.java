package org.kabieror.elwasys.webportal;

import com.vaadin.server.VaadinSession;
import org.kabieror.elwasys.common.User;

import java.sql.SQLException;

public class SessionManager {

    public final static String ATTRIBUTE_AUTHORIZED = "authorized";

    public final static String ATTRIBUTE_USER = "user";

    /**
     * Registriert die aktuelle Session als Admin-Session
     *
     * @throws SQLException
     */
    public boolean login(String username, String password) throws SQLException {
        if (username == null || username.isEmpty() || password == null) {
            return false;
        }
        for (final User u : WashportalManager.instance.getDataManager().getUsers()) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                if (u.checkPassword(password)) {
                    u.updateLastLogin();
                    if (u.isAdmin()) {
                        VaadinSession.getCurrent().setAttribute(ATTRIBUTE_AUTHORIZED,
                                AuthorizedType.ADMINISTRATOR);
                    } else {
                        VaadinSession.getCurrent().setAttribute(ATTRIBUTE_AUTHORIZED,
                                AuthorizedType.USER);
                    }
                    VaadinSession.getCurrent().setAttribute(ATTRIBUTE_USER, u);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public void logout() {
        VaadinSession.getCurrent().setAttribute(ATTRIBUTE_AUTHORIZED, AuthorizedType.PUBLIC);
        VaadinSession.getCurrent().setAttribute(ATTRIBUTE_USER, null);
    }

    public AuthorizedType getAuthorizedType() {
        final Object at = VaadinSession.getCurrent().getAttribute(ATTRIBUTE_AUTHORIZED);
        if (at == null || !(at instanceof AuthorizedType)) {
            return AuthorizedType.PUBLIC;
        } else {
            return (AuthorizedType) at;
        }
    }

    /**
     *
     * @return
     */
    public User getCurrentUser() {
        final User user = (User) VaadinSession.getCurrent().getAttribute(ATTRIBUTE_USER);
        if (user != null) {
            return user;
        } else {
            return User.getAnonymous();
        }
    }

    public enum AuthorizedType {
        PUBLIC, USER, ADMINISTRATOR,
    }
}
