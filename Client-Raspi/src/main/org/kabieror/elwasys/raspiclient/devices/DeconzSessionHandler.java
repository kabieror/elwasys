package org.kabieror.elwasys.raspiclient.devices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class DeconzSessionHandler extends TextWebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.logger.info("Connection to deCONZ started");
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        this.logger.warn("Connection to deCONZ closed");
        super.afterConnectionClosed(session, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        this.logger.info("Received: " + message.toString());
    }
}
