package org.kabieror.elwasys.webportal.components;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;

/**
 * Dieses Fenster ermöglicht es dem Benutzer, seine Auswahl zu bestätigen
 * 
 * @author Oliver Kabierschke
 *
 */
public class ConfirmWindow extends Window {
    /**
     * 
     */
    private static final long serialVersionUID = 2383494649907508187L;

    /**
     * Konstruktor
     * 
     * @param caption
     *            Der Titel des Fensters
     * @param question
     *            Die dem Benutzer zu stellende Frage
     * @param confirmAction
     *            Die Aktion, die nach Bestätigung der Frage ausgeführt werden
     *            soll
     */
    public ConfirmWindow(String caption, String question, Runnable confirmAction) {
        this.setCaption(caption);
        this.setWidth("18em");
        this.setResizable(false);
        this.setModal(true);
        this.setClosable(false);

        final VerticalLayout content = new VerticalLayout();
        this.setContent(content);
        content.setMargin(true);
        content.setSpacing(true);

        final Label lblQuestion = new Label(question, ContentMode.HTML);
        content.addComponent(lblQuestion);
        lblQuestion.addStyleName("align-center");

        final HorizontalLayout footer = new HorizontalLayout();
        content.addComponent(footer);
        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName("v-window-bottom-toolbar");

        final Button btnYes = new Button("Ja");
        footer.addComponent(btnYes);
        btnYes.setStyleName("primary");
        btnYes.addClickListener(e -> {
            confirmAction.run();
            this.setVisible(false);
            this.getUI().removeWindow(this);
        });
        btnYes.setClickShortcut(KeyCode.ENTER);

        final Label lblFooter = new Label("");
        footer.addComponent(lblFooter);
        footer.setExpandRatio(lblFooter, 1);

        final Button btnNo = new Button("Nein");
        footer.addComponent(btnNo);
        btnNo.addClickListener(e -> {
            this.setVisible(false);
            this.getUI().removeWindow(this);
        });
    }
}
