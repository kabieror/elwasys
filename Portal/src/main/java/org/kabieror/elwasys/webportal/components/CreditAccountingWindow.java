package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Item;
import com.vaadin.data.util.converter.StringToBigDecimalConverter;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.Align;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.kabieror.elwasys.common.CreditAccountingEntry;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.webportal.WashportalManager;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Dieses Fenster zeigt die Umsätze eines Benutzers an
 * 
 * @author Oliver Kabierschke
 *
 */
public class CreditAccountingWindow extends Window {
    /**
     * 
     */
    private static final long serialVersionUID = -4365804859608421600L;

    @SuppressWarnings("unchecked")
    public CreditAccountingWindow(User user) throws SQLException {
        this.setCaption("Umsätze von Benutzer " + user.getName());
        this.setWidth("60em");
        this.setHeight("40em");
        this.setResizable(true);
        this.setClosable(true);
        this.setModal(true);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);
        content.setSizeFull();

        final Table table = new Table();
        content.addComponent(table);
        table.setSizeFull();
        table.setMultiSelect(false);
        table.setSelectable(false);

        table.addContainerProperty("date", LocalDateTime.class, null, "Datum", null, Align.LEFT);
        table.addContainerProperty("amount", BigDecimal.class, null, "Betrag", null, Align.LEFT);
        table.addContainerProperty("text", String.class, null, "Buchungstext", null, Align.LEFT);

        table.setColumnWidth("date", 150);
        table.setColumnWidth("amount", 100);

        table.setConverter("date", new StringToLocalDateTimeConverter());
        table.setConverter("amount", new StringToBigDecimalConverter() {
            private static final long serialVersionUID = 1671726149722094077L;

            @Override
            protected NumberFormat getFormat(Locale locale) {
                return NumberFormat.getCurrencyInstance(locale);
            }
        });

        final List<CreditAccountingEntry> entries = WashportalManager.instance.getDataManager()
                .getAccountingEntries(user);
        for (final CreditAccountingEntry e : entries) {
            final Item i = table.addItem(e.getId());
            i.getItemProperty("date").setValue(e.getDate());
            i.getItemProperty("amount").setValue(e.getAmount());
            i.getItemProperty("text").setValue(e.getDescription());
        }
    }
}
