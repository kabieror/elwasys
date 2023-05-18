package org.kabieror.elwasys.webportal;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import org.kabieror.elwasys.webportal.components.MainMenu;
import org.kabieror.elwasys.webportal.views.UsersDashboardView;
import org.kabieror.elwasys.webportal.views.UsersView;

import java.util.ArrayList;
import java.util.List;

/**
 * Das Layout, das Administratoren gezeigt wird
 * 
 * @author Oliver Kabierschke
 *
 */
public class UserLayout extends HorizontalLayout {

    /**
     * 
     */
    private static final long serialVersionUID = -5219053915716711109L;

    private final Navigator navigator;

    private final MainMenu mainMenu;
    private final CssLayout menuContainer = new CssLayout();

    private final CssLayout contentContainer = new CssLayout();

    private final List<String> availableViews = new ArrayList<String>();

    // View objects
    private final View dashboardView = new UsersDashboardView(
            WashportalManager.instance.getSessionManager().getCurrentUser());

    public UserLayout(WaschportalUI ui) {
        // Layout über die ganze Seite erstrecken
        this.setSizeFull();

        this.menuContainer.setPrimaryStyleName("valo-menu");

        this.contentContainer.setPrimaryStyleName("valo-content");
        this.contentContainer.addStyleName("v-scrollable");
        this.contentContainer.setSizeFull();

        this.addComponents(this.menuContainer, this.contentContainer);
        this.setExpandRatio(this.contentContainer, 1);

        // Verfügbare Sichten auflisten
        this.availableViews.add(UsersDashboardView.VIEW_NAME);

        // Navigator erzeugen
        this.navigator = new Navigator(ui, this.contentContainer);
        this.navigator.addView("", new UsersView());
        this.navigator.addView(UsersDashboardView.VIEW_NAME, this.dashboardView);

        // Hauptmenü erzeugen
        this.mainMenu = new MainMenu(ui, this.navigator);
        this.mainMenu.addMenuItem("Übersicht", UsersDashboardView.VIEW_NAME, FontAwesome.HOME,
                this.dashboardView);

        // Hauptmenü zum Layout hinzufügen
        this.mainMenu.addStyleName("valo-menu-part");
        this.menuContainer.addComponent(this.mainMenu);

        if (this.availableViews.contains(this.navigator.getState())) {
            this.navigator.navigateTo(this.navigator.getState());
        } else {
            this.navigator.navigateTo(UsersDashboardView.VIEW_NAME);
        }
    }
}
