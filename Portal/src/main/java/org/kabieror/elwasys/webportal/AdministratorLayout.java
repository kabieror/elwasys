package org.kabieror.elwasys.webportal;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import org.kabieror.elwasys.webportal.components.MainMenu;
import org.kabieror.elwasys.webportal.views.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Das Layout, das Administratoren gezeigt wird
 *
 * @author Oliver Kabierschke
 *
 */
public class AdministratorLayout extends HorizontalLayout {

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
    private final View dashboardView = new AdminDashboardView();
    private final View usersView = new UsersView();
    private final View userGroupsView = new UserGroupsView();
    private final View programsView = new ProgramsView();
    private final View devicesView = new DevicesView();

    public AdministratorLayout(WaschportalUI ui) {
        // Layout über die ganze Seite erstrecken
        this.setSizeFull();

        this.menuContainer.setPrimaryStyleName("valo-menu");

        this.contentContainer.setPrimaryStyleName("valo-content");
        this.contentContainer.addStyleName("v-scrollable");
        this.contentContainer.setSizeFull();

        this.addComponents(this.menuContainer, this.contentContainer);
        this.setExpandRatio(this.contentContainer, 1);

        // Verfügbare Sichten auflisten
        this.availableViews.add(AdminDashboardView.VIEW_NAME);
        this.availableViews.add(UsersView.VIEW_NAME);
        this.availableViews.add(UserGroupsView.VIEW_NAME);
        this.availableViews.add(ProgramsView.VIEW_NAME);
        this.availableViews.add(DevicesView.VIEW_NAME);

        // Navigator erzeugen
        this.navigator = new Navigator(ui, this.contentContainer);
        this.navigator.addView("", new UsersView());
        this.navigator.addView(AdminDashboardView.VIEW_NAME, this.dashboardView);
        this.navigator.addView(UsersView.VIEW_NAME, this.usersView);
        this.navigator.addView(UserGroupsView.VIEW_NAME, this.userGroupsView);
        this.navigator.addView(ProgramsView.VIEW_NAME, this.programsView);
        this.navigator.addView(DevicesView.VIEW_NAME, this.devicesView);

        // Hauptmenü erzeugen
        this.mainMenu = new MainMenu(ui, this.navigator);
        this.mainMenu.addMenuItem("Dashboard", AdminDashboardView.VIEW_NAME, FontAwesome.DASHBOARD,
                this.dashboardView);
        this.mainMenu.addMenuItem("Benutzer", UsersView.VIEW_NAME, FontAwesome.USER,
                this.usersView);
        this.mainMenu.addMenuItem("Benutzergruppen", UserGroupsView.VIEW_NAME, FontAwesome.USERS, this.userGroupsView);
        this.mainMenu.addMenuItem("Programme", ProgramsView.VIEW_NAME, FontAwesome.COGS,
                this.programsView);
        this.mainMenu.addMenuItem("Geräte", DevicesView.VIEW_NAME, FontAwesome.CUBES,
                this.devicesView);

        // Hauptmenü zum Layout hinzufügen
        this.mainMenu.addStyleName("valo-menu-part");
        this.menuContainer.addComponent(this.mainMenu);

        if (this.availableViews.contains(this.navigator.getState())) {
            this.navigator.navigateTo(this.navigator.getState());
        } else {
            this.navigator.navigateTo(AdminDashboardView.VIEW_NAME);
        }
    }
}
