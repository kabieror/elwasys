package org.kabieror.elwasys.webportal.components;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;
import org.kabieror.elwasys.webportal.WaschportalUI;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

/**
 * Das Hauptmenü
 * 
 * @author Oliver Kabierschke
 *
 */
public class MainMenu extends CssLayout {

    /**
     *
     */
    private static final long serialVersionUID = 7537113808360695587L;
    private final Navigator navigator;
    private final LinkedHashMap<View, Button> menuItems = new LinkedHashMap<View, Button>();
    private final CssLayout menuItemsLayout = new CssLayout();
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Konstruktor
     * 
     * @param navigator
     *            Navigator
     */
    public MainMenu(WaschportalUI ui, Navigator navigator) {
        this.navigator = navigator;

        final HorizontalLayout top = new HorizontalLayout();
        top.setWidth("100%");
        top.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        top.addStyleName("valo-menu-title");
        this.addComponent(top);

        // Button zum Zeigen des Menüs
        final Button showMenu = new Button("Menu", e -> {
            if (this.getStyleName().contains("valo-menu-visible")) {
                this.removeStyleName("valo-menu-visible");
            } else {
                this.addStyleName("valo-menu-visible");
            }
        });
        showMenu.addStyleName(ValoTheme.BUTTON_PRIMARY);
        showMenu.addStyleName(ValoTheme.BUTTON_SMALL);
        showMenu.addStyleName("valo-menu-toggle");
        showMenu.setIcon(FontAwesome.LIST);
        this.addComponent(showMenu);

        final Label title = new Label("<h3>Waschportal " + WashportalManager.VERSION + "</h3>",
                ContentMode.HTML);
        title.setSizeUndefined();
        top.addComponent(title);
        top.setExpandRatio(title, 1);

        // Aktueller Benutzer
        final MenuBar settings = new MenuBar();
        settings.addStyleName("user-menu");
        final MenuItem settingsItem = settings.addItem(
                WashportalManager.instance.getSessionManager().getCurrentUser().getName(),
                new ThemeResource("img/usericon.png"), null);
        settingsItem.addItem("Einstellungen", i -> {
            try {
                this.getUI().addWindow(new UserSettingsWindow(
                        WashportalManager.instance.getSessionManager().getCurrentUser()));
            } catch (Exception e1) {
                this.logger.error("Could not modify the user.", e1);
                WashportalManager.instance.showError("Interner Fehler", e1.getLocalizedMessage());
            }
        });
        settingsItem.addItem("Passwort ändern", i -> {
            try {
                this.getUI().addWindow(new ChangePasswordWindow(
                        WashportalManager.instance.getSessionManager().getCurrentUser()));
            } catch (Exception e1) {
                this.logger.error("Could not change the password of a user.", e1);
                WashportalManager.instance.showError("Interner Fehler", e1.getLocalizedMessage());
            }
        });
        settingsItem.addItem("Logout", i -> {
            WashportalManager.instance.getSessionManager().logout();
            ui.loadSessionContent();
        });
        this.addComponent(settings);

        this.menuItemsLayout.setPrimaryStyleName("valo-menuitems");
        this.addComponent(this.menuItemsLayout);

        // Listener zum Markieren des auswählten Menüeintrags erstellen
        navigator.addViewChangeListener(new ViewChangeListener() {

            /**
             * 
             */
            private static final long serialVersionUID = -5227224681158046407L;

            @Override
            public boolean beforeViewChange(ViewChangeEvent event) {
                return true;
            }

            @Override
            public void afterViewChange(ViewChangeEvent event) {
                if (event.getOldView() != null
                        && MainMenu.this.menuItems.containsKey(event.getOldView())) {
                    MainMenu.this.menuItems.get(event.getOldView()).removeStyleName("selected");
                }
                if (event.getNewView() != null
                        && MainMenu.this.menuItems.containsKey(event.getNewView())) {
                    MainMenu.this.menuItems.get(event.getNewView()).addStyleName("selected");
                }
                MainMenu.this.removeStyleName("valo-menu-visible");
            }
        });
    }

    /**
     * Fügt dem Menü einen Punkt hinzu
     * 
     * @param caption
     *            Die Beschriftung des Menüpunktes
     * @param destination
     *            Das Navigations-Ziel
     * @param icon
     *            Das Icon, das neben dem Punkt angezeigt wird
     */
    public void addMenuItem(String caption, String destination, Resource icon, View view) {
        final Button b = new Button(caption, e -> this.navigator.navigateTo(destination));
        b.setHtmlContentAllowed(true);
        b.setPrimaryStyleName("valo-menu-item");
        b.setIcon(icon);
        this.menuItemsLayout.addComponent(b);

        this.menuItems.put(view, b);
    }
}
