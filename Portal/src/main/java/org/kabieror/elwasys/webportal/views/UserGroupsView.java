package org.kabieror.elwasys.webportal.views;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Table.Align;
import org.kabieror.elwasys.common.FormatUtilities;
import org.kabieror.elwasys.common.NoDataFoundException;
import org.kabieror.elwasys.common.UserGroup;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.components.ConfirmWindow;
import org.kabieror.elwasys.webportal.components.UserGroupWindow;
import org.kabieror.elwasys.webportal.events.IUserGroupUpdatedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Seite Benutzergruppenverwaltung
 *
 * @author Oliver Kabierschke
 */
public class UserGroupsView extends VerticalLayout implements View, IUserGroupUpdatedEventListener {
    /**
     * Der Name des Views, der in der Adresszeile angezeigt wird
     */
    public static final String VIEW_NAME = "groups";
    static final String ICON_PROPERTY = "icon";
    static final String INDEX_PROPERTY = "ID";
    static final String CAPTION_PROPERTY = "Name";
    static final String DISCOUNT_PROPERTY = "Rabatt";
    static final String BUTTONS_PROPERTY = "buttons";
    /**
     *
     */
    private static final long serialVersionUID = 4574040337563844816L;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Table groupsTable;
    private final Container groupsContainer;

    public UserGroupsView() {
        this.setMargin(true);
        this.setSpacing(true);
        this.setSizeFull();

        // 1. Menüleiste erstellen
        final HorizontalLayout topLayout = new HorizontalLayout();
        topLayout.setWidth("100%");
        final Label title = new Label("Benutzergruppen");
        title.addStyleName("h1");
        topLayout.addComponent(title);

        final MenuBar menuBar = new MenuBar();
        final MenuItem menuAdd = menuBar.addItem("Neu", i -> this.newUserGroup());
        menuAdd.setIcon(FontAwesome.PLUS);

        topLayout.addComponent(menuBar);
        topLayout.setComponentAlignment(menuBar, Alignment.BOTTOM_RIGHT);
        topLayout.setExpandRatio(menuBar, 2);
        topLayout.setExpandRatio(title, 1);

        this.addComponent(topLayout);

        // 2. Benutzertabelle erstellen
        this.groupsTable = new Table();
        this.addComponent(this.groupsTable);
        this.groupsTable.setSizeFull();
        this.groupsTable.setMultiSelect(false);
        this.groupsTable.setSelectable(false);
        this.groupsContainer = new IndexedContainer();

        this.groupsContainer.addContainerProperty(ICON_PROPERTY, Label.class, null);
        this.groupsContainer.addContainerProperty(INDEX_PROPERTY, Integer.class, null);
        this.groupsContainer.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        this.groupsContainer.addContainerProperty(DISCOUNT_PROPERTY, String.class, null);

        this.groupsTable.setContainerDataSource(this.groupsContainer);
        this.groupsTable.setColumnWidth(ICON_PROPERTY, 40);
        this.groupsTable.setColumnHeader(ICON_PROPERTY, "");
        this.groupsTable.setColumnAlignment(ICON_PROPERTY, Align.CENTER);
        this.groupsTable.setColumnWidth(INDEX_PROPERTY, 40);
        this.groupsTable.setColumnAlignment(DISCOUNT_PROPERTY, Align.LEFT);

        // 2.1. Buttons in Benutzer-Zeilen
        this.groupsTable.addContainerProperty(BUTTONS_PROPERTY, CssLayout.class, null);
        this.groupsTable.addGeneratedColumn(BUTTONS_PROPERTY, (Table source, Object itemId, Object columnId) -> {
            final CssLayout group = new CssLayout();
            group.addStyleName("v-component-group");

            final Button btnEdit = new Button("");
            group.addComponent(btnEdit);
            btnEdit.setIcon(FontAwesome.PENCIL_SQUARE_O);
            btnEdit.setDescription("Bearbeiten");
            btnEdit.addStyleName("small");
            btnEdit.addClickListener(e -> {
                try {
                    this.editUserGroup(WashportalManager.instance.getDataManager().getUserGroupById((Integer) itemId));
                } catch (final SQLException e1) {
                    this.logger.error("Could not load user to edit.", e1);
                    WashportalManager.instance.showDatabaseError(e1);
                } catch (final Exception e1) {
                    this.logger.error("Could not load user to edit.", e1);
                    WashportalManager.instance.showError(e1);
                }
            });

            final Button btnDelete = new Button("");
            group.addComponent(btnDelete);
            btnDelete.setIcon(FontAwesome.TRASH_O);
            btnDelete.setDescription("Löschen");
            btnDelete.addStyleName("small danger");
            btnDelete.addClickListener(e -> {
                try {
                    this.deleteUserGroup(
                            WashportalManager.instance.getDataManager().getUserGroupById((Integer) itemId));
                } catch (final SQLException e1) {
                    this.logger.error("Could not load user group to delete.", e1);
                    WashportalManager.instance.showDatabaseError(e1);
                } catch (final Exception e1) {
                    this.logger.error("Could not load user group to delete.", e1);
                    WashportalManager.instance.showError(e1);
                }
            });

            return group;
        });
        this.groupsTable.setColumnWidth(BUTTONS_PROPERTY, 190);
        this.groupsTable.setColumnAlignment(BUTTONS_PROPERTY, Align.CENTER);
        this.groupsTable.setColumnHeader(BUTTONS_PROPERTY, "");

        this.setExpandRatio(this.groupsTable, 1);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        this.loadData();
    }

    /**
     * Lädt die Benutzer aus der Datenbank
     */
    private void loadData() {
        this.groupsContainer.removeAllItems();

        List<UserGroup> groups;
        try {
            groups = WashportalManager.instance.getDataManager().getUserGroups();
        } catch (final SQLException e) {
            this.logger.error("Error while catching the available user groups from the database", e);
            WashportalManager.instance.showDatabaseError(e);
            return;
        }
        for (final UserGroup g : groups) {
            final Item i = this.groupsContainer.addItem(g.getId());
            this.fillItemWithUserGroupData(i, g);
        }
    }

    /**
     * Befüllt ein Item, mit Daten einer Benutzergruppe
     *
     * @param i Das zu befüllende Item
     * @param g Die Benutzergruppe, dessen Daten verwendet werden sollen
     */
    @SuppressWarnings("unchecked")
    private void fillItemWithUserGroupData(Item i, UserGroup g) {
        try {
            g.update();
        } catch (NoDataFoundException | SQLException e2) {
            this.logger.error("Could not fill user groups table with data.", e2);
            return;
        }

        final Label lblIcon = new Label(FontAwesome.USERS.getHtml(), ContentMode.HTML);
        lblIcon.setStyleName("icon-group-normal");

        i.getItemProperty(ICON_PROPERTY).setValue(lblIcon);
        i.getItemProperty(INDEX_PROPERTY).setValue(g.getId());
        i.getItemProperty(CAPTION_PROPERTY).setValue(g.getName());
        String discount;
        switch (g.getDiscountType()) {
            case Fix:
                discount = FormatUtilities.formatCurrency(g.getDiscountValue());
                break;
            case Factor:
                discount = NumberFormat.getPercentInstance(Locale.GERMANY).format(g.getDiscountValue());
                break;
            default:
                discount = "-";
                break;
        }
        i.getItemProperty(DISCOUNT_PROPERTY).setValue(discount);
    }

    private void newUserGroup() {
        final UserGroupWindow window = new UserGroupWindow();
        window.addUserGroupUpdatedEventListener(this);
        this.getUI().addWindow(window);
    }

    private void editUserGroup(UserGroup g) {
        final UserGroupWindow window = new UserGroupWindow(g);
        window.addUserGroupUpdatedEventListener(this);
        this.getUI().addWindow(window);
    }

    private void deleteUserGroup(UserGroup g) {
        final ConfirmWindow confirmWindow = new ConfirmWindow("Benutzergruppe löschen", String.format(
                "Möchten Sie diese Benutzergruppe wirklich löschen?<br><b>%2s</b><br>Benutzern, denen die Gruppe " +
                        "derzeit zugewiesen ist, wird die Standardgruppe zugewiesen.", g.getName()), () -> {
            try {
                g.delete();
                this.groupsContainer.removeItem(g.getId());
            } catch (final SQLException e) {
                this.logger.error("Could not delete the user group " + g.getId() + ".", e);
                WashportalManager.instance.showDatabaseError(e);
            } catch (final Exception e) {
                this.logger.error("Could not delete the user group " + g.getId() + ".", e);
                WashportalManager.instance.showError(e);
            }
        });
        this.getUI().addWindow(confirmWindow);
    }

    /**
     * Wird aufgerufen, sobald ein Benutzer bearbeitet oder erstellt wurde.
     * Aktualisiert die Benutzertabelle.
     */
    @Override
    public void onUserGroupUpdated(UserGroup g) {
        Item i;
        if (this.groupsContainer.containsId(g.getId())) {
            i = this.groupsContainer.getItem(g.getId());
        } else {
            i = this.groupsContainer.addItem(g.getId());
        }
        this.fillItemWithUserGroupData(i, g);
    }
}
