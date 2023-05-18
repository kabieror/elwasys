package org.kabieror.elwasys.webportal.views;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.converter.StringToBigDecimalConverter;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Table.Align;
import org.kabieror.elwasys.common.FormatUtilities;
import org.kabieror.elwasys.common.NoDataFoundException;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.components.*;
import org.kabieror.elwasys.webportal.events.IUserUpdatedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Seite Benutzerverwaltung
 *
 * @author Oliver Kabierschke
 */
public class UsersView extends VerticalLayout implements View, IUserUpdatedEventListener {
    /**
     * Der Name des Views, der in der Adresszeile angezeigt wird
     */
    public static final String VIEW_NAME = "users";
    static final String ICON_PROPERTY = "icon";
    static final String INDEX_PROPERTY = "ID";
    static final String CAPTION_PROPERTY = "Name";
    static final String GROUP_PROPERTY = "Gruppe";
    static final String CARD_ID_PROPERTY = "Kartennummer";
    static final String CREDIT_PROPERTY = "Guthaben";
    static final String BUTTONS_PROPERTY = "buttons";
    /**
     *
     */
    private static final long serialVersionUID = 4574040337563844816L;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Table usersTable;
    private final Container usersContainer;

    public UsersView() {
        this.setMargin(true);
        this.setSpacing(true);
        this.setSizeFull();

        // 1. Menüleiste erstellen
        final HorizontalLayout topLayout = new HorizontalLayout();
        topLayout.setWidth("100%");
        final Label title = new Label("Benutzer");
        title.addStyleName("h1");
        topLayout.addComponent(title);

        final MenuBar menuBar = new MenuBar();
        final MenuItem menuAdd = menuBar.addItem("Neu", i -> this.newUser());
        menuAdd.setIcon(FontAwesome.PLUS);

        topLayout.addComponent(menuBar);
        topLayout.setComponentAlignment(menuBar, Alignment.BOTTOM_RIGHT);
        topLayout.setExpandRatio(menuBar, 2);
        topLayout.setExpandRatio(title, 1);

        this.addComponent(topLayout);

        // 2. Benutzertabelle erstellen
        this.usersTable = new Table();
        this.addComponent(this.usersTable);
        this.usersTable.setSizeFull();
        this.usersTable.setMultiSelect(false);
        this.usersTable.setSelectable(false);
        this.usersContainer = new IndexedContainer();

        this.usersContainer.addContainerProperty(ICON_PROPERTY, Button.class, null);
        this.usersContainer.addContainerProperty(INDEX_PROPERTY, Integer.class, null);
        this.usersContainer.addContainerProperty(CAPTION_PROPERTY, String.class, null);
        this.usersContainer.addContainerProperty(GROUP_PROPERTY, String.class, null);
        this.usersContainer.addContainerProperty(CARD_ID_PROPERTY, String.class, null);
        this.usersContainer.addContainerProperty(CREDIT_PROPERTY, BigDecimal.class, null);

        this.usersTable.setContainerDataSource(this.usersContainer);
        this.usersTable.setColumnWidth(ICON_PROPERTY, 40);
        this.usersTable.setColumnHeader(ICON_PROPERTY, "");
        this.usersTable.setColumnAlignment(ICON_PROPERTY, Align.CENTER);
        this.usersTable.setColumnWidth(INDEX_PROPERTY, 40);
        this.usersTable.setConverter(CREDIT_PROPERTY, new StringToBigDecimalConverter() {
            /**
             *
             */
            private static final long serialVersionUID = 1671726149722094077L;

            @Override
            protected NumberFormat getFormat(Locale locale) {
                return NumberFormat.getCurrencyInstance(locale);
            }
        });
        this.usersTable.setColumnAlignment(CREDIT_PROPERTY, Align.RIGHT);

        // 2.1. Buttons in Benutzer-Zeilen
        this.usersTable.addContainerProperty(BUTTONS_PROPERTY, CssLayout.class, null);
        this.usersTable.addGeneratedColumn(BUTTONS_PROPERTY, (Table source, Object itemId, Object columnId) -> {
            final CssLayout group = new CssLayout();
            group.addStyleName("v-component-group");

            final Button btnEdit = new Button("");
            group.addComponent(btnEdit);
            btnEdit.setIcon(FontAwesome.PENCIL_SQUARE_O);
            btnEdit.setDescription("Bearbeiten");
            btnEdit.addStyleName("small");
            btnEdit.addClickListener(e -> {
                try {
                    this.editUser(WashportalManager.instance.getDataManager().getUserById((Integer) itemId));
                } catch (final SQLException e1) {
                    this.logger.error("Could not load user to edit.", e1);
                    WashportalManager.instance.showDatabaseError(e1);
                } catch (final Exception e1) {
                    this.logger.error("Could not load user to edit.", e1);
                    WashportalManager.instance.showError(e1);
                }
            });

            final Button btnCredit = new Button("");
            group.addComponent(btnCredit);
            btnCredit.setIcon(FontAwesome.MONEY);
            btnCredit.setDescription("Guthaben aufwerten");
            btnCredit.addStyleName("small");
            btnCredit.addClickListener(e -> {
                try {
                    this.addCredit(WashportalManager.instance.getDataManager().getUserById((Integer) itemId));
                } catch (final SQLException e1) {
                    this.logger.error("Could not load user to add credit to.", e1);
                    WashportalManager.instance.showDatabaseError(e1);
                } catch (final Exception e1) {
                    this.logger.error("Could not load user to add credit to.", e1);
                    WashportalManager.instance.showError(e1);
                }
            });

            final Button btnCreditAccounting = new Button("");
            group.addComponent(btnCreditAccounting);
            btnCreditAccounting.setIcon(FontAwesome.BOOK);
            btnCreditAccounting.setDescription("Umsätze ansehen");
            btnCreditAccounting.addStyleName("small");
            btnCreditAccounting.addClickListener(e -> {
                try {
                    final CreditAccountingWindow window = new CreditAccountingWindow(
                            WashportalManager.instance.getDataManager().getUserById((Integer) itemId));
                    this.getUI().addWindow(window);
                    window.center();
                } catch (final SQLException e1) {
                    this.logger.error("Could not load user to show accounting window for.", e1);
                    WashportalManager.instance.showDatabaseError(e1);
                } catch (final Exception e1) {
                    this.logger.error("Could not load user to show accounting window for.", e1);
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
                    this.deleteUser(WashportalManager.instance.getDataManager().getUserById((Integer) itemId));
                } catch (final SQLException e1) {
                    this.logger.error("Could not load user to delete.", e1);
                    WashportalManager.instance.showDatabaseError(e1);
                } catch (final Exception e1) {
                    this.logger.error("Could not load user to delete.", e1);
                    WashportalManager.instance.showError(e1);
                }
            });

            return group;
        });
        this.usersTable.setColumnWidth(BUTTONS_PROPERTY, 190);
        this.usersTable.setColumnAlignment(BUTTONS_PROPERTY, Align.CENTER);
        this.usersTable.setColumnHeader(BUTTONS_PROPERTY, "");

        this.usersTable.setFooterVisible(true);

        this.setExpandRatio(this.usersTable, 1);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        this.loadData();
    }

    /**
     * Lädt die Benutzer aus der Datenbank
     */
    private void loadData() {
        this.usersContainer.removeAllItems();

        List<User> users;
        try {
            users = WashportalManager.instance.getDataManager().getUsers();
        } catch (final SQLException e) {
            this.logger.error("Error while catching the available users from the database", e);
            WashportalManager.instance.showDatabaseError(e);
            return;
        }
        for (final User u : users) {
            final Item i = this.usersContainer.addItem(u.getId());
            this.fillItemWithUserData(i, u);
        }
        this.usersTable.sort(new Object[]{GROUP_PROPERTY, CAPTION_PROPERTY}, new boolean[]{true, true});
        this.updateSumRow();
    }

    /**
     * Befüllt ein Item, mit Daten eines Benutzers
     *
     * @param i Das zu befüllende Item
     * @param u Der User, dessen Daten verwendet werden sollen
     */
    @SuppressWarnings("unchecked")
    private void fillItemWithUserData(Item i, User u) {
        final Button btn = new Button("");
        FontAwesome icon;

        try {
            u.update();
        } catch (NoDataFoundException | SQLException e2) {
            this.logger.error("Could not fill user table with data.", e2);
            return;
        }

        boolean userHasExpiredExecutions = false;
        try {
            userHasExpiredExecutions = u.hasExpiredExecutions();
        } catch (final SQLException e) {
            this.logger.error("Error while looking up if user has expired executions.", e);
        }

        if (u.isBlocked()) {
            icon = FontAwesome.MINUS_CIRCLE;
            btn.setDescription("Gesperrt");
            btn.setStyleName("icon-user-blocked");
            btn.setEnabled(false);
        } else if (userHasExpiredExecutions) {
            icon = FontAwesome.EXCLAMATION_TRIANGLE;
            btn.setDescription("Es gibt nicht abgerechnete Programmausführungen");
            btn.setStyleName("icon-user-expired-executions");
            btn.addClickListener(e -> {
                ExpiredExecutionsWindow window;
                try {
                    window = new ExpiredExecutionsWindow(u, () -> {
                        this.onUserUpdated(u);
                    });
                } catch (final SQLException e1) {
                    this.logger.error("Could not load the window to show expired executions.", e1);
                    WashportalManager.instance.showDatabaseError(e1);
                    return;
                } catch (final Exception e1) {
                    this.logger.error("Could not load the window to show expired executions.", e1);
                    WashportalManager.instance.showError(e1);
                    return;
                }
                this.getUI().addWindow(window);
                window.center();
            });
        } else {
            icon = FontAwesome.USER;
            btn.setStyleName("icon-user-normal");
            btn.setEnabled(false);
        }
        btn.setIcon(icon);
        btn.addStyleName("borderless small");

        i.getItemProperty(ICON_PROPERTY).setValue(btn);
        i.getItemProperty(INDEX_PROPERTY).setValue(u.getId());
        i.getItemProperty(CAPTION_PROPERTY).setValue(u.getName());
        i.getItemProperty(GROUP_PROPERTY).setValue(u.getGroup().getName());
        i.getItemProperty(CARD_ID_PROPERTY).setValue(u.getCardIds().length > 0 ?
                u.getCardIds().length == 1 ? u.getCardIds()[0] : u.getCardIds().length + " Karten" : "");
        i.getItemProperty(CREDIT_PROPERTY).setValue(u.getCredit());
    }

    /**
     * Aktualisiert die Summenzeile
     */
    private void updateSumRow() {
        Collection<?> ids = this.usersContainer.getItemIds();
        this.usersTable.setColumnFooter(CAPTION_PROPERTY, ids.size() + " Benutzer");
        BigDecimal sum = BigDecimal.ZERO;
        for (Object id : ids) {
           Item i = this.usersContainer.getItem(id);
           sum = sum.add((BigDecimal) i.getItemProperty(CREDIT_PROPERTY).getValue());
        }
        this.usersTable.setColumnFooter(CREDIT_PROPERTY,
                FormatUtilities.formatCurrency(sum));
    }


    private void newUser() {
        final UserWindow window = new UserWindow();
        window.addUserUpdatedEventListener(this);
        this.getUI().addWindow(window);
    }

    private void editUser(User u) {
        final UserWindow window = new UserWindow(u);
        window.addUserUpdatedEventListener(this);
        this.getUI().addWindow(window);
    }

    private void deleteUser(User u) {
        final ConfirmWindow confirmWindow = new ConfirmWindow("Benutzer löschen",
                "Möchten Sie diesen Benutzer wirklich löschen?<br><b>" + u.getName() + "</b>", () -> {
            try {
                u.setDeleted(true);
                this.usersContainer.removeItem(u.getId());
                this.updateSumRow();
            } catch (final SQLException e) {
                this.logger.error("Could not delete the user " + u.getId() + ".", e);
                WashportalManager.instance.showDatabaseError(e);
            } catch (final Exception e) {
                this.logger.error("Could not delete the user " + u.getId() + ".", e);
                WashportalManager.instance.showError(e);
            }
        });
        this.getUI().addWindow(confirmWindow);
    }

    @SuppressWarnings("unchecked")
    private void addCredit(User u) {
        final UserCreditWindow creditWindow = new UserCreditWindow(u, () -> {
            final Item i = this.usersContainer.getItem(u.getId());
            i.getItemProperty(CREDIT_PROPERTY).setValue(u.getCredit());
        });
        creditWindow.addCloseListener((args) -> {
            this.updateSumRow();
        });
        this.getUI().addWindow(creditWindow);
    }

    /**
     * Wird aufgerufen, sobald ein Benutzer bearbeitet oder erstellt wurde.
     * Aktualisiert die Benutzertabelle.
     */
    @Override
    public void onUserUpdated(User u) {
        Item i;
        if (this.usersContainer.containsId(u.getId())) {
            i = this.usersContainer.getItem(u.getId());
        } else {
            i = this.usersContainer.addItem(u.getId());
        }
        this.fillItemWithUserData(i, u);
        this.updateSumRow();
    }
}
