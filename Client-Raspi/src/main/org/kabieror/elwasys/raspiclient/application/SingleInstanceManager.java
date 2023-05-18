package org.kabieror.elwasys.raspiclient.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This manager detects the concurrent execution of another instance of the software
 */
public class SingleInstanceManager extends Thread implements ICloseListener {

    /**
     * Singleton instance
     */
    public static SingleInstanceManager instance = new SingleInstanceManager();


    private Logger logger = LoggerFactory.getLogger(SingleInstanceManager.class);

    private ExecutorService executorService = Executors.newFixedThreadPool(1);
    private CountDownLatch startupLatch = new CountDownLatch(1);

    private Future serverFuture;
    private ServerSocket serverSocket;

    private SingleInstanceManager() {

    }

    public void start(Integer port) throws AlreadyRunningException {
        if (this.serverSocket != null && this.serverSocket.isBound()) {
            // Server is running already
            return;
        }

        this.logger.debug("Starting prevention of concurrent program executions");
        try {
            Socket socket = new Socket("localhost", port);
            this.logger.error("Another instance of the application is running on port " + port);
            throw new AlreadyRunningException();
        } catch (IOException e) {
            // Could not connect -> No other instance is running
        }

        ElwaManager.instance.listenToCloseEvent(this);

        // Start server
        this.serverFuture = this.executorService.submit(() -> {
            try {
                this.serverSocket = new ServerSocket(port, 1);

                // Notify about successful startup
                this.startupLatch.countDown();

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.close();
                }
            } catch (IOException e) {
                this.startupLatch.countDown();
            }

            this.logger.info("Single instance server terminated");
        });

        // Wait until server has started
        try {
            this.startupLatch.await();
            if (this.serverSocket == null || !this.serverSocket.isBound()) {
                this.logger.error("Could not start single instance server");
                throw new AlreadyRunningException();
            }
            this.logger.info("Started single instance server on port " + port);
        } catch (InterruptedException e) {
            throw new AlreadyRunningException();
        }
    }

    @Override
    public void onClose(boolean restart) {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            // OK...
        }
        this.executorService.shutdownNow();
    }
}
