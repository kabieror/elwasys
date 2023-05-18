package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Item;
import com.vaadin.data.util.converter.StringToBigDecimalConverter;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import org.kabieror.elwasys.common.Execution;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Dieses Fenster zeigt die nicht abgerechneten Ausführungsaufträge eines
 * Benutzers an
 *
 * @author Oliver Kabierschke
 *
 */
public class ExpiredExecutionsWindow extends Window {

    /**
     *
     */
    private static final long serialVersionUID = -8912999752818636836L;
    private static final String START_DATE_PROPERTY = "startDate";
    private static final String DEVICE_PROPERTY = "device";
    private static final String PROGRAM_PROPERTY = "program";
    private static final String AMOUNT_PROPERTY = "amount";
    private static final String BUTTONS_PROPERTY = "buttons";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<Execution> executions;

    private final Table table;

    private final Runnable updateAction;

    /**
     * Konstruktor
     *
     * @param u
     *            Der Benutzer, von dem die noch abzurechnenden
     *            Ausführungsaufträge angezeigt werden sollen
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public ExpiredExecutionsWindow(User u, Runnable updateAction) throws SQLException {
        this.updateAction = updateAction;

        final List<Execution> executions =
                WashportalManager.instance.getDataManager().getNotFinishedExecutions(u);

        // Entferne noch laufende Ausführungsaufträge
        final Iterator<Execution> iter = executions.iterator();
        while (iter.hasNext()) {
            final Execution e = iter.next();
            if (!e.isExpired()) {
                iter.remove();
            }
        }

        this.executions = executions;

        this.setCaption("Verfallene Ausführungsaufträge von " + u.getName());
        this.setModal(true);
        this.setWidth("60em");
        this.setResizable(false);
        this.setClosable(true);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        final Label explanation = new Label(
                "Diese Ausführungsaufträge wurden gestartet, jedoch nie beendet, möglicherweise durch " +
                        "einen Fehler im elwaClient. Der höchstmögliche Betrag von laufenden Ausführung wird "
                        + "beim Berechnen des Guthabens eines Benutzers zwar bereits berücksichtigt, jedoch "
                        + "existiert für nicht korrekt beendete Ausführungen kein Eintrag im Guthaben-Konto "
                        + "eines Benutzers.");
        content.addComponent(explanation);
        explanation.addStyleName("small");

        final HorizontalLayout topLayout = new HorizontalLayout();
        content.addComponent(topLayout);
        topLayout.setWidth("100%");
        final Label title = new Label("");
        topLayout.addComponent(title);

        final MenuBar menuBar = new MenuBar();
        menuBar.addItem("Alle abrechnen", i -> {
            try {
                this.finishAll();
            } catch (final SQLException e1) {
                this.logger.error("Could not finish all executions.");
                WashportalManager.instance.showDatabaseError(e1);
            }
        });

        topLayout.addComponent(menuBar);
        topLayout.setComponentAlignment(menuBar, Alignment.BOTTOM_RIGHT);
        topLayout.setExpandRatio(menuBar, 2);
        topLayout.setExpandRatio(title, 1);

        this.table = new Table();
        content.addComponent(this.table);
        this.table.setSizeFull();

        this.table.addContainerProperty(START_DATE_PROPERTY, LocalDateTime.class, null);
        this.table.addContainerProperty(DEVICE_PROPERTY, String.class, null);
        this.table.addContainerProperty(PROGRAM_PROPERTY, String.class, null);
        this.table.addContainerProperty(AMOUNT_PROPERTY, BigDecimal.class, null);
        this.table.addContainerProperty(BUTTONS_PROPERTY, CssLayout.class, null);

        this.table.setColumnHeader(START_DATE_PROPERTY, "Startdatum");
        this.table.setColumnHeader(DEVICE_PROPERTY, "Gerät");
        this.table.setColumnHeader(PROGRAM_PROPERTY, "Programm");
        this.table.setColumnHeader(AMOUNT_PROPERTY, "Fälliger Betrag");
        this.table.setColumnHeader(BUTTONS_PROPERTY, "");

        this.table.setConverter(START_DATE_PROPERTY, new StringToLocalDateTimeConverter());
        this.table.setConverter(AMOUNT_PROPERTY, new StringToBigDecimalConverter() {
            private static final long serialVersionUID = 1671726149722094077L;

            @Override
            protected NumberFormat getFormat(Locale locale) {
                return NumberFormat.getCurrencyInstance(locale);
            }
        });

        for (final Execution e : executions) {
            final Item i = this.table.addItem(e.getId());
            i.getItemProperty(START_DATE_PROPERTY).setValue(e.getStartDate());
            i.getItemProperty(DEVICE_PROPERTY).setValue(
                    e.getDevice().getName() + " (" + e.getDevice().getLocation().getName() + ")");
            i.getItemProperty(PROGRAM_PROPERTY).setValue(e.getProgram().getName());
            i.getItemProperty(AMOUNT_PROPERTY).setValue(e.getPrice());

            final CssLayout group = new CssLayout();
            group.addStyleName("v-component-group");

            final Button btnFinish = new Button("Abrechnen");
            group.addComponent(btnFinish);
            btnFinish.addStyleName("small");
            btnFinish.addClickListener(e1 -> {
                try {
                    this.finish(e);
                } catch (final SQLException e2) {
                    this.logger.error("Could not finish execution.", e2);
                    WashportalManager.instance.showDatabaseError(e2);
                }
            });

            final Button btnDelete = new Button("");
            group.addComponent(btnDelete);
            btnDelete.setIcon(FontAwesome.TRASH_O);
            btnDelete.addStyleName("small danger");
            btnDelete.addClickListener(e1 -> {
                try {
                    e.delete();
                    this.table.removeItem(e.getId());
                    this.updateAction.run();
                } catch (final SQLException e2) {
                    this.logger.error("Could not delete execution.", e2);
                    WashportalManager.instance.showDatabaseError(e2);
                } catch (final Exception e2) {
                    this.logger.error("Could not delete execution.", e2);
                    WashportalManager.instance.showError(e2);
                }
            });

            i.getItemProperty(BUTTONS_PROPERTY).setValue(group);
        }

    }

    private void finish(Execution e) throws SQLException {
        try {
            e.getUser().payExecution(e);
        } catch (final SQLException e1) {
            this.logger.error("User could not pay execution.", e1);
        }
        e.finish();
        this.table.removeItem(e.getId());
        this.updateAction.run();
    }

    /**
     * Rechnet alle offenen Ausführungen ab
     *
     * @throws SQLException
     */
    private void finishAll() throws SQLException {
        for (final Execution e : this.executions) {
            this.finish(e);
        }
    }
}
