package org.kabieror.elwasys.raspiclient.devices;

import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

public class DeconzSessionHandler extends TextWebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private List<IDeconzSessionClosedEventListener> closedListeners = new ArrayList<>();

    public void listenToConnectionClosed(IDeconzSessionClosedEventListener listener) {
        this.closedListeners.add(listener);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.logger.info("Connection to deCONZ started");
        super.afterConnectionEstablished(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        this.logger.warn("Transport Error: ", exception);
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        this.logger.warn("Connection to deCONZ closed");
        super.afterConnectionClosed(session, status);
        this.closedListeners.forEach(l -> l.onDeconzSessionClosed());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        this.logger.info("Received: " + message.toString());
    }
}
