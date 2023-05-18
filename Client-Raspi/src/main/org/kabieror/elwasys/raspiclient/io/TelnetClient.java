package org.kabieror.elwasys.raspiclient.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Diese Klasse verbindet sich mit einem Telnet-Server und kann mit ihm
 * kommunizieren.
 *
 * @author Oliver Kabierschke
 */
public class TelnetClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String serverName;
    private final int port;
    /**
     * When the server sends an answer, it is stored in this variable.
     */
    private final LinkedBlockingQueue<String> response = new LinkedBlockingQueue<>();
    private final Runnable dataReceiverRunnable;
    private Socket connection;
    private DataOutputStream out;
    private BufferedReader in;
    /**
     * This thread is being interrupted when a new answer has been received from
     * the server.
     */
    private Thread newAnswerBlockerThread;
    private Thread dataReceiverThread;

    /**
     * Constructor
     *
     * @param serverName The name or ip address of the server to connect to.
     * @param port       The port to use for the connection.
     */
    public TelnetClient(String serverName, int port) {
        this.connection = new Socket();
        this.serverName = serverName;
        this.port = port;

        this.dataReceiverRunnable = () -> {
            while (!Thread.interrupted()) {
                try {
                    final String answer = this.in.readLine();
                    this.response.add(answer);
                    this.logger.trace("From " + this.serverName + ": '" + answer + "'");
                } catch (final IOException e) {
                    if (!Thread.interrupted()) {
                        this.logger.error("IOException while waiting for data.", e);
                    }
                    break;
                }
            }
        };
    }

    /**
     * Checks if the connection to the server is established.
     *
     * @return True, if the connection to the server is established.
     */
    public boolean isAlive() {
        return (this.connection != null && this.connection.isConnected()) ||
                (this.dataReceiverThread != null && this.dataReceiverThread.isAlive());
    }

    /**
     * Opens a new connection to the server.
     *
     * @param timeout The timeout for the connect operation.
     * @throws IOException If the connection cannot be opened.
     */
    public void openConnection(int timeout) throws IOException {
        if (this.connection != null && !this.connection.isClosed()) {
            try {
                this.connection.close();
            } catch (final IOException e) {
                // Ignore exception while closing.
            }
        }
        if (this.dataReceiverThread != null && this.dataReceiverThread.isAlive()) {
            this.dataReceiverThread.interrupt();
        }
        this.connection = new Socket();
        this.connection.connect(new InetSocketAddress(this.serverName, this.port), timeout);
        this.out = new DataOutputStream(this.connection.getOutputStream());
        this.in = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
        this.startDataReceiverThread();
    }

    /**
     * Schließt die Verbindung und bricht das Warten auf Antworten vom Server
     * ab.
     */
    public void shutdown() {
        this.logger.debug("Closing connection to " + this.serverName);
        if (this.dataReceiverThread != null && this.dataReceiverThread.isAlive()) {
            this.dataReceiverThread.interrupt();
        }
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (final IOException e) {
                this.logger.warn("Cannot close the connection.", e);
            }
        }
    }

    /**
     * Sends a command to the server.
     *
     * @param command The command to send.
     * @throws IOException If the command cannot be sent.
     */
    public void sendCommand(String command) throws IOException {
        this.logger.trace("To " + this.serverName + ": '" + command + "'");
        this.out.writeBytes(command + "\n\r");
    }

    /**
     * Blocks the currently running thread, until a response is received from
     * the server or a timeout occurs.
     *
     * @param timeout The milliseconds to wait for a response before returning null. -1: No timeout.
     * @return The response received from the server or null, if a timeout occurred.
     * @throws IOException          If the connection is broken.
     * @throws InterruptedException
     */
    public String waitForResponse(int timeout, TimeUnit unit) throws IOException, InterruptedException {
        if (!this.isAlive()) {
            throw new IOException("The connection is broken.");
        }
        return this.response.poll(timeout, unit);
    }

    /**
     * Returns the data that has been received since the last call of this
     * method or {@link waitForResponse()}.
     *
     * @return The lines that have been received since the last call of this method or {@link waitForResponse()}.
     */
    public String emptyResponseBuffer() {
        String res = "";
        while (!this.response.isEmpty()) {
            res += this.response.poll();
        }
        return res;
    }

    /**
     * Starts listening for incoming data from the server.
     */
    private void startDataReceiverThread() {
        this.dataReceiverThread = new Thread(this.dataReceiverRunnable);
        this.dataReceiverThread.setName("TelnetClient.DataReceiverThread");
        this.dataReceiverThread.start();
    }
}
