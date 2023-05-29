package org.kabieror.elwasys.raspiclient.devices;

import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.raspiclient.application.ElwaManager;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.executions.FhemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeconzDevicePowerManager implements IDevicePowerManager {

    private static final Integer INITIAL_RECONNECT_DELAY_SECONDS = 5;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private WebSocketClient client;
    private WebSocketSession webSocketSession;
    private final DeconzSessionHandler sessionHandler;
    private AtomicBoolean isReconnectTriggered = new AtomicBoolean(false);
    private final ScheduledExecutorService reconnectScheduler;
    private Integer reconnectDelaySecconds = INITIAL_RECONNECT_DELAY_SECONDS;
    private List<IDevicePowerMeasurementHandler> powerMeasurementListeners = new ArrayList<>();

    public DeconzDevicePowerManager(WashguardConfiguration configurationManager) {
        client = new StandardWebSocketClient();
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        sessionHandler = new DeconzSessionHandler();

        sessionHandler.listenToConnectionClosed(() -> scheduleReconnect());
        ElwaManager.instance.listenToCloseEvent(restart -> onClosing());
        openEventConnection();
    }

    private void onClosing() {
        reconnectScheduler.shutdown();
        if (this.webSocketSession != null && this.webSocketSession.isOpen()) {
            try {
                this.webSocketSession.close();
            } catch (IOException e) {
                this.logger.warn("Failed to close web socket connection");
            }
        }
    }

    private void scheduleReconnect() {
        if (reconnectScheduler.isShutdown()) {
            return;
        }
        if (isReconnectTriggered.compareAndSet(false, true)) {
            this.logger.info("Scheduling reconnect in " + reconnectDelaySecconds + " seconds");
            reconnectScheduler.schedule(() -> openEventConnection(), reconnectDelaySecconds, TimeUnit.SECONDS);
            reconnectDelaySecconds = (int) Math.min(300, Math.round(reconnectDelaySecconds * 1.5));
        }
    }

    private void openEventConnection() {
        logger.info("Starting web socket connection to deCONZ");
        client.execute(sessionHandler, "ws://192.168.0.15:8943")
                .whenComplete((result, ex) -> {
                    if (result != null) {
                        this.webSocketSession = result;
                        this.logger.info("Successfully connected");
                        this.reconnectDelaySecconds = INITIAL_RECONNECT_DELAY_SECONDS;
                    } else if (ex != null) {
                        this.logger.error("Failed to connect", ex);
                        scheduleReconnect();
                    }
                });
        isReconnectTriggered.set(false);
    }

    @Override
    public void setDevicePowerState(Device device, DevicePowerState newState)
            throws IOException, InterruptedException, FhemException {
    }

    @Override
    public DevicePowerState getState(Device device) throws InterruptedException, FhemException, IOException {
        return DevicePowerState.OFF;
    }

    @Override
    public void addPowerMeasurementListener(IDevicePowerMeasurementHandler handler) {
        this.powerMeasurementListeners.add(handler);
    }

}
