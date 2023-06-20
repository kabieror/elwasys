package org.kabieror.elwasys.raspiclient.devices.deconz;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.kabieror.elwasys.raspiclient.devices.deconz.model.DeconzEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class DeconzEventListener extends TextWebSocketHandler {
    private static final Integer INITIAL_RECONNECT_DELAY_SECONDS = 5;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<IDeconzPowerMeasurementEventListener> powerMeasurementEventListeners = new ArrayList<>();
    private final List<IDeconzDeviceStateEventListener> deviceStateEventListeners = new ArrayList<>();
    private Integer reconnectDelaySeconds = INITIAL_RECONNECT_DELAY_SECONDS;
    private final AtomicBoolean isReconnectRunning = new AtomicBoolean(false);
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final WebSocketClient client = new StandardWebSocketClient();
    private WebSocketSession webSocketSession;

    private final Gson gson = new Gson();
    private String host;
    private int port;

    public DeconzEventListener(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void listenToPowerMeasurementReceived(IDeconzPowerMeasurementEventListener listener) {
        this.powerMeasurementEventListeners.add(listener);
    }

    public void listenToDeviceStateEvent(IDeconzDeviceStateEventListener listener) {
        this.deviceStateEventListeners.add(listener);
    }

    public void start() {
        openConnection();
    }

    public void stop() {
        this.reconnectScheduler.shutdown();
        if (this.webSocketSession != null && this.webSocketSession.isOpen()) {
            try {
                this.webSocketSession.close();
            } catch (IOException e) {
                this.logger.warn("Failed to close web socket connection");
            }
        }
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
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        byte[] rawBytes = message.getPayload().getBytes(StandardCharsets.UTF_8);
        try {
            DeconzEvent event = gson.fromJson(new String(rawBytes), DeconzEvent.class);
            if (event.r().equals("sensors") && event.state() != null && event.state().power() != null) {
                this.powerMeasurementEventListeners.forEach(
                        l -> l.onPowerMeasurementReceived(event));
            } else if (event.r().equals("lights") && event.state() != null && event.state().on() != null) {
                this.deviceStateEventListeners.forEach(
                        l -> l.onDeviceStateChanged(event.uniqueid(), event.state().on()));
            }
        } catch (JsonSyntaxException e) {
            this.logger.error("Failed to read event data.", e);
        } catch (Exception e) {
            this.logger.error("Unexpected exception while trying to deserialize deCONZ event data.", e);
        }
    }

    private void scheduleReconnect() {
        if (reconnectScheduler.isShutdown()) {
            return;
        }
        if (isReconnectRunning.compareAndSet(false, true)) {
            this.logger.info("Scheduling reconnect in " + reconnectDelaySeconds + " seconds");
            reconnectScheduler.schedule(() -> openConnection(), reconnectDelaySeconds, TimeUnit.SECONDS);
            reconnectDelaySeconds = (int) Math.min(300, Math.round(reconnectDelaySeconds * 1.5));
        }
    }

    private void openConnection() {
        logger.info("Starting web socket connection to deCONZ");
        client.execute(this, String.format("ws://%s:%s", host, port))
                .whenComplete((result, ex) -> {
                    isReconnectRunning.set(false);
                    if (result != null) {
                        this.webSocketSession = result;
                        this.logger.info("Successfully connected");
                        this.reconnectDelaySeconds = INITIAL_RECONNECT_DELAY_SECONDS;
                    } else if (ex != null) {
                        this.logger.error("Failed to connect", ex);
                        scheduleReconnect();
                    }
                });
    }

}

