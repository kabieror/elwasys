package org.kabieror.elwasys.webportal;

import com.vaadin.server.Page;
import org.kabieror.elwasys.common.ConfigurationManager;
import org.kabieror.elwasys.common.Utilities;

/**
 *
 * @author Oliver Kabierschke
 *
 */
public class WashportalUtilities extends Utilities {

    public WashportalUtilities(ConfigurationManager config) {
        super(config);
    }

    /**
     * Gibt die HTML-Repräsentation des Symbols für erforderliche Textfelder
     * zurück.
     *
     * @return
     */
    public static String getRequiredAsterisk() {
        return "<span class=\"v-required-field-indicator\" aria-hidden=\"true\">*</span>";
    }

    /**
     * Erzeugt eine URL mit der ein Benutzer sein Passwort zurück setzen kann.
     *
     * @return Eine URL, mit der ein Benutzer sein Passwort zurück setzen kann.
     */
    public String getPasswordResetUrl(String key) {
        final String full = Page.getCurrent().getLocation().toString();
        final String base = full.substring(0, full.lastIndexOf("/") + 1);
        return base + "?rp=" + key;
    }
}
