package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.*;
import org.kabieror.elwasys.common.NotEnoughCreditException;
import org.kabieror.elwasys.common.User;
import org.kabieror.elwasys.webportal.WashportalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Dieses Fenster ermöglicht es, das Guthaben eines Benutzers zu verändern
 *
 * @author Oliver Kabierschke
 *
 */
public class UserCreditWindow extends Window {

    /**
     *
     */
    private static final long serialVersionUID = -3316674124303342590L;
    private static final String INPAYMENT_TEXT = "Einzahlung vom Waschportal von "
            + WashportalManager.instance.getSessionManager().getCurrentUser().getName();
    private static final String PAYOUT_TEXT = "Auszahlung vom Waschportal von "
            + WashportalManager.instance.getSessionManager().getCurrentUser().getName();
    private static final String INPAYMENT_OPTION = "Einzahlung";
    private static final String PAYOUT_OPTION = "Auszahlung";
    /**
     * Der Benutzer, dessen Guthaben verändert werden soll
     */
    private final User user;
    private final OptionGroup actionOptions;
    private final ObjectProperty<BigDecimal> amountProperty =
            new ObjectProperty<BigDecimal>(BigDecimal.ZERO, BigDecimal.class);
    private final TextField tfAmount;
    private final TextField tfText;
    /**
     * Die Aktion, die nach der Änderung ausgeführt werden soll
     */
    private final Runnable finishAction;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Konstruktor
     *
     * @param user
     *            Der Benutzer, dessen Guthaben verändert werden soll
     */
    public UserCreditWindow(User user, Runnable finishAction) {
        this.user = user;
        this.finishAction = finishAction;

        this.setCaption("Guthaben von " + user.getName());
        this.setWidth("25em");
        this.setResizable(false);
        this.setModal(true);
        this.setClosable(true);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        final FormLayout form = new FormLayout();
        content.addComponent(form);
        form.setSizeFull();

        this.actionOptions = new OptionGroup();
        form.addComponent(this.actionOptions);
        this.actionOptions.addStyleName("horizontal");
        this.actionOptions.addItem(INPAYMENT_OPTION);
        this.actionOptions.addItem(PAYOUT_OPTION);
        this.actionOptions.select(INPAYMENT_OPTION);

        this.tfAmount = new TextField("Betrag");
        form.addComponent(this.tfAmount);
        this.tfAmount.setSizeFull();
        this.tfAmount.setPropertyDataSource(this.amountProperty);
        this.tfAmount.focus();
        this.tfAmount.selectAll();

        this.tfText = new TextField("Buchungstext");
        form.addComponent(this.tfText);
        this.tfText.setSizeFull();
        this.tfText.setValue(INPAYMENT_TEXT);

        final HorizontalLayout footer = new HorizontalLayout();
        content.addComponent(footer);
        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName("v-window-bottom-toolbar");

        final Label lblFooter = new Label("");
        footer.addComponent(lblFooter);
        footer.setExpandRatio(lblFooter, 1);

        final Button btnCancel = new Button("Abbrechen");
        footer.addComponent(btnCancel);
        btnCancel.addClickListener(e -> {
            this.setVisible(false);
            this.getUI().removeWindow(this);
        });

        final Button btnExecute = new Button("Buchen");
        footer.addComponent(btnExecute);
        btnExecute.addStyleName("primary");
        btnExecute.addClickListener(e -> {
            if (this.execute()) {
                this.setVisible(false);
                this.getUI().removeWindow(this);
            }
        });
        btnExecute.setClickShortcut(KeyCode.ENTER);


        this.actionOptions.addValueChangeListener(e -> {
            if (e.getProperty().getValue().equals(INPAYMENT_OPTION)) {
                // Wert auf null setzen, falls noch nicht manuell geändert
                if (this.amountProperty.getValue().equals(this.user.getCredit())) {
                    this.amountProperty.setValue(BigDecimal.ZERO);
                }

                // Buchungstext setzen, falls noch nicht manuell geändert
                if (this.tfText.getValue().equals(PAYOUT_TEXT)) {
                    this.tfText.setValue(INPAYMENT_TEXT);
                }
            }
            if (e.getProperty().getValue().equals(PAYOUT_OPTION)) {
                // Wert auf das gesamte Guthaben des Benutzers setzen, falls
                // noch nicht manuell geändert
                if (this.amountProperty.getValue().equals(BigDecimal.ZERO)
                        || this.amountProperty.getValue().compareTo(this.user.getCredit()) > 0) {
                    this.amountProperty.setValue(this.user.getCredit());
                }

                // Buchungstext setzen, falls noch nicht manuell geändert
                if (this.tfText.getValue().equals(INPAYMENT_TEXT)) {
                    this.tfText.setValue(PAYOUT_TEXT);
                }
            }
        });
    }

    /**
     * Führt die Veränderung des Guthabens aus
     *
     * @return True, wenn die Aktion ausgeführt wurde, false, falls es
     *         Validierungsfehler gibt.
     */
    private boolean execute() {
        try {
            this.validate();
        } catch (final InvalidValueException e) {
            this.tfAmount.setValidationVisible(true);
            return false;
        }
        try {
            if (this.actionOptions.getValue().equals(PAYOUT_OPTION)) {
                this.user.payout(this.amountProperty.getValue(), this.tfText.getValue());
            } else {
                this.user.inpayment(this.amountProperty.getValue(), this.tfText.getValue());
            }
        } catch (final SQLException e) {
            this.logger.error("Could not create an entry in the accounting table.", e);
            WashportalManager.instance.showDatabaseError(e);
            return false;
        } catch (final NotEnoughCreditException e) {
            WashportalManager.instance.showError("Fehler",
                    "Das Guthaben des Benutzers reicht nicht aus für diese Operation.");
            return false;
        }
        this.finishAction.run();
        return true;
    }

    private void validate() throws InvalidValueException {
        this.tfAmount.validate();
    }
}
