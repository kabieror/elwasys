package org.kabieror.elwasys.webportal.components;

import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class LogViewerWindow extends Window {

    public LogViewerWindow(List<String> logContents) {
        this.setCaption("Log");
        this.setClosable(true);
        this.setModal(true);
        this.setWidth("70em");
        this.setHeight("40em");
        this.setResizable(true);

        final VerticalLayout content = new VerticalLayout();
        content.setSizeFull();

        final TextArea tfLog = new TextArea();

        tfLog.setSizeFull();
        tfLog.setValue(StringUtils.join(logContents, "\n"));
        tfLog.setReadOnly(true);
        tfLog.addStyleName("log-textfield");

        content.addComponent(tfLog);

        this.setContent(content);
    }
}
