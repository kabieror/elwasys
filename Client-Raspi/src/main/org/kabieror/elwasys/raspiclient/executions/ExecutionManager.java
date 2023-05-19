package org.kabieror.elwasys.raspiclient.executions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.pushover.client.*;
import org.apache.commons.mail.EmailException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kabieror.elwasys.common.Device;
import org.kabieror.elwasys.common.Execution;
import org.kabieror.elwasys.common.NoDataFoundException;
import org.kabieror.elwasys.raspiclient.application.ElwaManager;
import org.kabieror.elwasys.raspiclient.application.ICloseListener;
import org.kabieror.elwasys.raspiclient.configuration.WashguardConfiguration;
import org.kabieror.elwasys.raspiclient.devices.DevicePowerState;
import org.kabieror.elwasys.raspiclient.devices.IDevicePowerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Dieser Manager verwaltet laufende Ausführungsaufträge.
 *
 * @author Oliver Kabierschke
 */
public class ExecutionManager implements ICloseListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * Listener
     */
    private final List<IExecutionFinishedListener> finishListeners;
    private final List<IExecutionErrorListener> errorListeners;
    private final List<IExecutionStartedListener> startListeners;

    /**
     * Service, der die laufenden Aufträge ausführt
     */
    private final ScheduledExecutorService executorService;

    /**
     * Alle geplanten Operationen, die zum Ende einer Programmausführung
     * ausgeführt werden
     */
    private final Map<Execution, ExecutionFinisher> executionFinishers = new HashMap<>();

    /**
     * Alle geplanten Beendigungen von Ausführungen aufgrund von geringer
     * Leistung.
     */
    private final Map<Execution, ScheduledFuture> plannedStops = new HashMap<>();

     /**
     * Erstellt eine Instanz des Ausführungsmanager
     */
    public ExecutionManager() {
        this.startListeners = new Vector<>();
        this.finishListeners = new Vector<>();
        this.errorListeners = new Vector<>();
        this.executorService = Executors.newScheduledThreadPool(4);

        ElwaManager.instance.listenToCloseEvent(this);

        this.executorService.scheduleAtFixedRate(() -> {
            // Plane Sicherung vor externer Aktivierung der Stromzufuhr von Geräten
            try {
                for (Device d : ElwaManager.instance.getManagedDevices()) {
                    this.logger.trace(String.format("[%1s] Checking power state", d.getName()));
                    synchronized (d) {
                        if (d.getCurrentExecution() == null) {
                            // Stelle sicher, dass die Stromzufuhr des Geräts aus ist.
                            DevicePowerState state;
                            try {
                                state = ElwaManager.instance.getDevicePowerManager().getState(d);
                            } catch (InterruptedException | FhemException | IOException e1) {
                                this.logger.error(String.format("[%1s] Could not check power state.", d.getName()), e1);
                                return;
                            }
                            if (state == DevicePowerState.ON) {
                                // Schalte Gerät aus.
                                try {
                                    this.logger.warn(String
                                            .format("[%1s] Device has been powered on but there is no execution running. " +
                                                    "Switching it" + " off now" + ".", d.getName()));
                                    ElwaManager.instance.getDevicePowerManager()
                                            .setDevicePowerState(d, DevicePowerState.OFF);
                                } catch (IOException | InterruptedException | FhemException e1) {
                                    this.logger.error(String.format("[%1s] Could not power off device.", d.getName()), e1);
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                this.logger.warn("Could not get managed devices.", e);
            } catch (NoDataFoundException e) {
                this.logger.warn("No devices found");
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    /**
     * Startet die Ausführung eines Programms
     *
     * @param e Die zu startende Ausführung
     * @throws SQLException
     * @throws IOException
     */
    public void startExecution(Execution e) throws SQLException, IOException, InterruptedException, FhemException {
        synchronized (e.getDevice()) {
            this.logger.info("[" + e.getDevice().getName() + "] Starting execution " + e.getId());

            final ExecutionFinisher r = new ExecutionFinisher(e);

            this.executionFinishers.put(e, r);

            // Startzeit setzen
            try {
                e.start();
            } catch (final SQLException ex) {
                this.executionFinishers.remove(e);
                throw ex;
            }
            this.logger.debug("[" + e.getDevice().getName() + "] Database updated");

            // Strom freigeben
            try {
                ElwaManager.instance.getDevicePowerManager()
                        .setDevicePowerState(e.getDevice(), DevicePowerState.ON);
            } catch (final IOException | InterruptedException | FhemException ex) {
                this.executionFinishers.remove(e);
                e.reset();
                throw ex;
            }
            this.logger.debug("[" + e.getDevice().getName() + "] Power enabled");

            // Gerät benachrichten
            e.getDevice().onExecutionStarted(e);

            r.setScheduledFuture(this.executorService.schedule(r, e.getRemainingTime().getSeconds(), TimeUnit.SECONDS));
            this.logger.debug("[" + e.getDevice().getName() + "] Finisher scheduled to run in " +
                    e.getRemainingTime().getSeconds() + "s");

            // Plane automatischen Stop, falls keine elektrische Leistung vom Gerät
            // abgenommen wird.
            this.onPowerMeasurementAvailable(e, 0);
        }

        // Benachrichtige Listener
        for (IExecutionStartedListener listener : this.startListeners) {
            listener.onExecutionStarted(e);
        }
    }

    /**
     * Bricht eine Programmausführung ab
     *
     * @param e Die abzubrechende Programmausführung
     */
    public void abortExecution(Execution e) {
        final ExecutionFinisher finisher = this.executionFinishers.get(e);
        if (finisher == null) {
            throw new InvalidParameterException("The execution to abort is not running");
        }
        finisher.abort();
    }

    private void autoEndExecution(Execution e) {
        final ExecutionFinisher finisher = this.executionFinishers.get(e);
        if (finisher == null) {
            throw new InvalidParameterException("The execution to abort is not running");
        }
        finisher.run();
    }

    /**
     * Versucht das erneute Abbrechen nach einem Fehler in der Beeindigung einer
     * Programmausführung.
     *
     * @param e Die Abzuschließende Programmausführung.
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     */
    public void retryFinishExecution(Execution e)
            throws SQLException, IOException, InterruptedException, FhemException {
        final ExecutionFinisher finisher = this.executionFinishers.get(e);
        if (finisher == null) {
            throw new InvalidParameterException("The execution to finish is already finished");
        }
        finisher.retry();
    }

    /**
     * Gibt eine Liste aller laufenden Ausführungen zurück.
     *
     * @return Eine Liste aller laufenden Ausführungen.
     */
    public List<Execution> getRunningExecutions() {
        return this.executionFinishers.keySet().stream().filter(Execution::isRunning)
                .collect(Collectors.toCollection(Vector::new));
    }

    /**
     * Gibt die Ausführung zurück, die derzeit auf dem gegebenen Gerät
     * ausgeführt wird, oder null, wenn das Gerät frei ist.
     *
     * @param device Das Gerät, dessen laufende Ausführung gesucht ist.
     * @return Die laufende Ausführung, oder null, wenn das Gerät frei ist.
     */
    public Execution getRunningExecution(Device device) {
        for (final Execution e : this.executionFinishers.keySet()) {
            if (e.getDevice().equals(device)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Wird aufgerufen, sobald ein neuer Messwert für die aktuelle Leistung
     * eines Geräts verfügbar ist.
     *
     * @param execution Die Ausführung, zu der ein neuer Messwert verfügbar ist.
     * @param power     Die aktuelle Leistung des Geräts in Watt.
     */
    public void onPowerMeasurementAvailable(Execution execution, double power) {
        this.logger.debug("[" + execution.getDevice().getName() + "] Power: " + power + "W");
        if (execution.getProgram().isAutoEnd()) {
            if (power < execution.getDevice().getAutoEndPowerThreashold()) {
                if (!this.plannedStops.containsKey(execution) || this.plannedStops.get(execution).isDone()) {
                    final long delay = execution.getEarliestAutoEnd().getSeconds();
                    this.logger
                            .debug("[" + execution.getDevice().getName() + "] Planned auto-end of program in " + delay +
                                    "s");
                    this.plannedStops.put(execution, this.executorService.schedule(() -> {
                        this.logger.info("[" + execution.getDevice().getName() +
                                "] Power measurement detected end of program");
                        this.plannedStops.remove(execution);
                        this.autoEndExecution(execution);
                    }, delay, TimeUnit.SECONDS));
                }
            } else {
                if (this.plannedStops.containsKey(execution)) {
                    this.logger.debug("[" + execution.getDevice().getName() + "] Aborted planned auto-end of program");
                    this.plannedStops.get(execution).cancel(false);
                    this.plannedStops.remove(execution);
                }
            }
        }
    }

    /**
     * Registriert einen Listener zum Ereignis der Fertigstellung einer Ausführung
     *
     * @param l Der Listener, der bei einer fertiggestellten Programmausführung benachrichtigt werden soll
     */
    public void listenToExecutionFinishedEvent(IExecutionFinishedListener l) {
        if (!this.finishListeners.contains(l)) {
            this.finishListeners.add(l);
        }
    }

    /**
     * Entfernt einen Listener vom Ereignis der Fertigstellung einer Ausführung
     *
     * @param l Der Listener, der bei einer fertiggestellten Programmausführung benachrichtigt werden sollte
     */
    public void stopListenToExecutionFinishedEvent(IExecutionFinishedListener l) {
        if (this.finishListeners.contains(l)) {
            this.finishListeners.remove(l);
        }
    }

    /**
     * Registriert einen Listener zum Ereignis des Startes einer Ausführung
     *
     * @param l Der Listener, der bei einer gestarteten Programmausführung benachrichtigt werden soll
     */
    public void listenToExecutionStartedEvent(IExecutionStartedListener l) {
        if (!this.startListeners.contains(l)) {
            this.startListeners.add(l);
        }
    }

    /**
     * Entfernt einen Listener vom Ereignis des Startes einer Ausführung
     *
     * @param l Der Listener, der bei einer gestarteten Programmausführung benachrichtigt werden sollte
     */
    public void stopListenToExecutionStartedEvent(IExecutionStartedListener l) {
        if (this.startListeners.contains(l)) {
            this.startListeners.add(l);
        }
    }

    /**
     * Registriert einen Listener zum Ereignis eines Fehlers bei einer Programmausführung
     *
     * @param l Der Listener, der bei einer fehlgeschlagenen Programmausführung benachrichtigt werden soll
     */
    public void listenToExecutionErrorEvent(IExecutionErrorListener l) {
        if (!this.errorListeners.contains(l)) {
            this.errorListeners.add(l);
        }
    }

    /**
     * Entfernt einen Listener vom Ereignis eines Fehlers bei einer Programmausführung
     *
     * @param l Der Listener, der bei einer fehlgeschlagenen Programmausführung benachrichtigt werden sollte
     */
    public void stopListenToExecutionErrorEvent(IExecutionErrorListener l) {
        if (this.errorListeners.contains(l)) {
            this.errorListeners.add(l);
        }
    }

    @Override
    public void onClose(boolean restart) {
        this.logger.debug("Shutting down execution manager");
        this.executorService.shutdownNow();
    }

    /**
     * Diese Klasse führt die bei der Beendigung einer Programmausführung notwendigen Operationen aus
     *
     * @author Oliver Kabierschke
     */
    private class ExecutionFinisher implements Runnable {
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private final Integer lock = 0;

        private final Execution e;

        private ScheduledFuture<?> future;

        private Boolean executed = false;

        private boolean aborted = false;

        ExecutionFinisher(Execution e) {
            this.e = e;
        }

        @Override
        public void run() {
            synchronized (this.lock) {
                synchronized (e.getDevice()) {
                    if (this.executed) {
                        return;
                    }
                    try {
                        this.executeAction();
                        this.executed = true;
                    } catch (final Exception e) {
                        this.logger.error("Execution finisher failed.", e);
                        for (final IExecutionErrorListener l : ExecutionManager.this.errorListeners) {
                            l.onExecutionFailed(this.e, e);
                        }
                    }
                }
            }
        }

        void abort() {
            this.aborted = true;
            this.run();
        }

        /**
         * Versucht das erneute Ausführen der Fertigstellung einer
         * Programmausführung
         */
        void retry() throws SQLException, IOException, InterruptedException, FhemException {
            this.executeAction();
        }

        private void executeAction() throws SQLException, IOException, InterruptedException, FhemException {
            this.logger.info("[" + this.e.getDevice().getName() + "] Stopping execution " + this.e.getId());
            this.logger.info("[" + this.e.getDevice().getName() + "] User: " + this.e.getUser().getName());
            this.logger.info("[" + this.e.getDevice().getName() + "] Total time: " + this.e.getElapsedTimeString());

            // Breche geplante Ausführung ab, falls nicht von dieser
            // gestartet
            if (this.future != null) {
                this.future.cancel(false);
            }

            // Breche geplante automatische Stops ab
            if (ExecutionManager.this.plannedStops.containsKey(this.e)) {
                ExecutionManager.this.plannedStops.get(this.e).cancel(false);
            }

            // Schalte den Strom der Maschine aus
            try {
                ElwaManager.instance.getDevicePowerManager()
                        .setDevicePowerState(this.e.getDevice(), DevicePowerState.OFF);
            } catch (final IOException | InterruptedException | FhemException e1) {
                this.logger.error("[" + this.e.getDevice().getName() + "] Could not power off the device.", e1);
                throw e1;
            }

            // Informiere Ausführung über dessen Ende
            try {
                this.e.stop();
            } catch (final SQLException e1) {
                this.logger.error("[" + this.e.getDevice().getName() + "] Could not stop the execution.", e1);
                throw e1;
            }

            // Informiere Gerät über Ende der Ausführung
            this.e.getDevice().onExecutionEnded();

            // Veranlasse Benutzer zum Zahlen
            try {
                this.e.getUser().payExecution(this.e);
            } catch (final SQLException e1) {
                this.logger.error("[" + this.e.getDevice().getName() + "] User " + this.e.getUser().getName() +
                        " could not pay the execution.", e1);
                throw e1;
            }

            // Ausführung aus der Liste entfernen
            if (ExecutionManager.this.executionFinishers.containsKey(this.e)) {
                ExecutionManager.this.executionFinishers.remove(this.e);
            }

            if (ExecutionManager.this.plannedStops.containsKey(this.e)) {
                ExecutionManager.this.plannedStops.remove(this.e);
            }

            // Informiere alle Listener über das Ende der Programmausfürung
            for (final IExecutionFinishedListener l : ExecutionManager.this.finishListeners) {
                l.onExecutionFinished(this.e);
            }

            // Bereite Benachrichtigungs-Email vor
            String notificationTitle;
            String notificationMessageShort;
            String notificationMessageLong;
            if (this.aborted) {
                notificationTitle = "Waschvorgang abgebrochen!";
                notificationMessageShort =
                        "Der Waschvorgang auf " + this.e.getDevice().getName() + " wurde abgebrochen.";
                notificationMessageLong = "Hallo " + this.e.getUser().getName() + ",\n\n dein Waschvorgang auf " +
                        this.e.getDevice().getName() + " wurde gerade abgebrochen.\n" + "Uhrzeit: " +
                        LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) +
                        "\n\n--\nelwasys";
            } else {
                notificationTitle = this.e.getDevice().getName() + " ist fertig!";
                notificationMessageShort =
                        this.e.getDevice().getName() + " ist fertig. Bitte entferne die Wäsche umgehend.";
                notificationMessageLong =
                        "Hallo " + this.e.getUser().getName() + ",\n\n" + this.e.getDevice().getName() +
                                " ist gerade fertig.\n" + "Uhrzeit: " +
                                LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) +
                                "\n" + "Bitte entferne die Wäsche umgehend.\n\n--\nelwasys";
            }
            notificationTitle = new String(notificationTitle.getBytes(), Charset.defaultCharset());
            notificationMessageLong = new String(notificationMessageLong.getBytes(), Charset.defaultCharset());
            notificationMessageShort = new String(notificationMessageShort.getBytes(), Charset.defaultCharset());

            // Sende Benachrichtigungs-Email
            if (this.e.getUser().getEmailNotification()) {
                try {
                    ElwaManager.instance.getUtilities()
                            .sendEmail(notificationTitle, notificationMessageLong, this.e.getUser());
                    this.logger.debug("Sent notification to " + this.e.getUser().getEmail());
                } catch (final EmailException e1) {
                    this.logger.error("Could not send the notification mail.", e1);
                }
            } else {
                this.logger.debug("User is not to be notified.");
            }

            // Sende Pushover-Benachrichtigung
            if (this.e.getUser().getPushoverUserKey() != null && !this.e.getUser().getPushoverUserKey().isEmpty()) {
                try {
                    final PushoverRestClient client = new PushoverRestClient();
                    final Status result = client.pushMessage(PushoverMessage
                            .builderWithApiToken(ElwaManager.instance.getConfigurationManager().getPushoverApiToken())
                            .setUserId(this.e.getUser().getPushoverUserKey()).setMessage(notificationMessageShort)
                            .setPriority(MessagePriority.HIGH).setTitle(notificationTitle)
                            .setUrl("http://waschportal.hilaren.de").setTitleForURL("Waschportal").build());
                    this.logger.debug("Sent push notification. Status: " + result.getStatus());
                } catch (final PushoverException e1) {
                    this.logger.error("Could not send push notification.", e1);
                }
            }

            // Sende elwaApp-Pushbenachrichtigung
            if (this.e.getUser().isPushEnabled()
                    && this.e.getUser().getPushIonicId() != null
                    && !this.e.getUser().getPushIonicId().isEmpty()) {
                try {
                    HttpResponse<JsonNode> jsonResponse = Unirest.post("https://api.ionic.io/push/notifications")
                            .header("PROFILE_TAG", "dev")
                            .header("Authorization", "Bearer " + ElwaManager.instance.getConfigurationManager().getIonicApiToken())
                            .header("Content-Type", "application/json")
                            .body(new JSONObject()
                                    .put("user_ids", new JSONArray().put(this.e.getUser().getPushIonicId()))
                                    .put("profile", "dev")
                                    .put("notification", new JSONObject()
                                            .put("title", notificationTitle)
                                            .put("message", notificationMessageShort))
                            )
                            .asJson();
                    if (jsonResponse.getStatus() > 299) {
                        this.logger.error("Could not send ionic notification. Status: " + jsonResponse.getStatus() + " "
                                + jsonResponse.getStatusText() + "\n" + jsonResponse.getBody().toString());
                    } else {
                        this.logger.debug("Sent ionic notification. "
                                + jsonResponse.getStatus() + " " + jsonResponse.getStatusText());
                    }
                } catch (UnirestException e) {
                    this.logger.error("Could not send ionic notification.", e);
                }
            }

        }

        void setScheduledFuture(ScheduledFuture<?> future) {
            this.future = future;
        }
    }

}
