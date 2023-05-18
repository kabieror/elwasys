package org.kabieror.elwasys.webportal.views;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.converter.StringToBigDecimalConverter;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.*;
import com.vaadin.ui.Table.Align;
import org.kabieror.elwasys.common.CreditAccountingEntry;
import org.kabieror.elwasys.common.FormatUtilities;
import org.kabieror.elwasys.common.NoDataFoundException;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.kabieror.elwasys.webportal.components.StringToLocalDateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

/**
 * Seite Benutzer-Dashboard
 *
 * @author Oliver Kabierschke
 *
 */
public class UsersDashboardView extends VerticalLayout implements View {
    /**
     * Der Name des Views, der in der Adresszeile angezeigt wird
     */
    public static final String VIEW_NAME = "dashboard";
    static final String DATE_PROPERTY = "Datum";
    static final String TEXT_PROPERTY = "Text";
    static final String VALUE_PROPERTY = "Wert";
    /**
     *
     */
    private static final long serialVersionUID = 4574040337563844816L;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final User user;
    private final Label creditLabel;
    private final Table accountingTable;
    private final IndexedContainer accountingContainer;
    private final StringToBigDecimalConverter bigDecimalConverter =
            new StringToBigDecimalConverter() {
                /**
                 *
                 */
                private static final long serialVersionUID = 1671726149722094077L;

                @Override
                protected NumberFormat getFormat(Locale locale) {
                    return NumberFormat.getCurrencyInstance(locale);
                }
            };

    public UsersDashboardView(User user) {
        this.user = user;

        this.setStyleName("dashboard-view");

        this.setMargin(true);
        this.setSpacing(true);

        final VerticalLayout body = new VerticalLayout();
        this.addComponent(body);
        body.setSpacing(true);

        final CssLayout topPanels = new CssLayout();
        topPanels.addStyleName("sparks");
        body.addComponent(topPanels);

        final CssLayout creditPane = new CssLayout();
        creditPane.addStyleName("spark");
        final Label creditLabel =
                new Label(FormatUtilities
                        .formatCurrency(this.user.getCredit()));
        creditLabel.addStyleName("huge");
        creditPane.addComponent(creditLabel);

        final Label creditCaptionLabel = new Label("Guthaben");
        creditCaptionLabel.addStyleName("small light");
        creditPane.addComponent(creditCaptionLabel);
        topPanels.addComponent(creditPane);

        final CssLayout lastInpaymentPane = new CssLayout();
        lastInpaymentPane.addStyleName("spark");
        String lastInpayment = "-";
        try {
            final CreditAccountingEntry cae =
                    WashportalManager.instance.getDataManager().getLastInpayment(this.user);
            if (cae != null) {
                lastInpayment =
                        cae.getDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                .withLocale(VaadinSession.getCurrent().getLocale()));
            }
        } catch (final SQLException e) {
            this.logger.error("Could not get last inpayment from database.");
            WashportalManager.instance.showDatabaseError(e);
        }
        final Label lastInpaymentLabel = new Label(lastInpayment);
        lastInpaymentLabel.addStyleName("huge");
        lastInpaymentPane.addComponent(lastInpaymentLabel);

        final Label lastInpaymentCaptionLabel = new Label("Letzte Einzahlung");
        lastInpaymentCaptionLabel.addStyleName("small light");
        lastInpaymentPane.addComponent(lastInpaymentCaptionLabel);
        topPanels.addComponent(lastInpaymentPane);

        final Panel creditPanel = new Panel();
        // topPanels.addComponent(creditPanel);
        creditPanel.setCaption("Guthaben");
        final VerticalLayout creditContent = new VerticalLayout();
        creditPanel.setContent(creditContent);
        this.creditLabel = new Label();
        creditContent.addComponent(this.creditLabel);
        this.creditLabel.addStyleName("huge");
        this.creditLabel.addStyleName("dashboard-credit");

        final Label accountingHeader = new Label("Buchungen");
        accountingHeader.setStyleName("h2");
        body.addComponent(accountingHeader);

        this.accountingTable = new Table();
        body.addComponent(this.accountingTable);
        this.accountingTable.setMultiSelect(false);
        this.accountingTable.setSelectable(false);

        this.accountingContainer = new IndexedContainer();
        this.accountingContainer.addContainerProperty(DATE_PROPERTY, LocalDateTime.class, null);
        this.accountingContainer.addContainerProperty(VALUE_PROPERTY, BigDecimal.class, null);
        this.accountingContainer.addContainerProperty(TEXT_PROPERTY, String.class, null);

        this.accountingTable.setContainerDataSource(this.accountingContainer);
        this.accountingTable.setColumnWidth(DATE_PROPERTY, 150);
        this.accountingTable.setColumnAlignment(DATE_PROPERTY, Align.LEFT);
        this.accountingTable.setColumnHeader(DATE_PROPERTY, "Datum");
        this.accountingTable.setColumnWidth(VALUE_PROPERTY, 100);
        this.accountingTable.setColumnHeader(VALUE_PROPERTY, "Betrag");
        this.accountingTable.setColumnAlignment(VALUE_PROPERTY, Align.LEFT);
        this.accountingTable.setColumnHeader(TEXT_PROPERTY, "Buchungstext");
        this.accountingTable.setColumnAlignment(TEXT_PROPERTY, Align.LEFT);

        this.accountingTable.setConverter(DATE_PROPERTY, new StringToLocalDateTimeConverter());
        this.accountingTable.setConverter(VALUE_PROPERTY, new StringToBigDecimalConverter() {
            private static final long serialVersionUID = 1671726149722094077L;

            @Override
            protected NumberFormat getFormat(Locale locale) {
                return NumberFormat.getCurrencyInstance(locale);
            }
        });

    }

    @Override
    public void enter(ViewChangeEvent event) {
        this.loadData(1);
    }

    /**
     * Lädt die Benutzer aus der Datenbank
     *
     * @throws SQLException
     */
    private void loadData(int page) {
        try {
            this.user.update();
        } catch (final NoDataFoundException e2) {
            WashportalManager.instance.showError("Fehler",
                    "Das Benutzerkonto ist gelöscht worden.");
            WashportalManager.instance.getSessionManager().logout();
            return;
        } catch (final SQLException e2) {
            WashportalManager.instance.showDatabaseError(e2);
            this.logger.error("Could not update the user", e2);
            return;
        }

        this.creditLabel.setValue(this.bigDecimalConverter.convertToPresentation(
                this.user.getCredit(), String.class, VaadinSession.getCurrent().getLocale()));


        // Einträge in Konto-Tabelle
        List<CreditAccountingEntry> entries;
        try {
            entries = WashportalManager.instance.getDataManager().getAccountingEntries(this.user);
            this.accountingContainer.removeAllItems();
            for (final CreditAccountingEntry e : entries) {
                final Item i = this.accountingContainer.addItem(e.getId());
                i.getItemProperty(DATE_PROPERTY).setValue(e.getDate());
                i.getItemProperty(VALUE_PROPERTY).setValue(e.getAmount());
                i.getItemProperty(TEXT_PROPERTY).setValue(e.getDescription());
            }
        } catch (final SQLException e1) {
            this.logger.error("Could not load user accounting entries.", e1);
            WashportalManager.instance.showDatabaseError(e1);
        }
    }

}
