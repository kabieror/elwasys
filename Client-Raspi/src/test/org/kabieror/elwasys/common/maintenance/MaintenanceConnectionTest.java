/**
 *
 */
package org.kabieror.elwasys.common.maintenance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * @author Oliver
 */
public class MaintenanceConnectionTest {
    /**
     * Diese Klasse dient als Implementierung eines einfachen Wartungs-Servers
     * zum Test.
     *
     * @throws InterruptedException
     */
    public static void main(String[] args) {
        final BufferedReader userReader = new BufferedReader(new InputStreamReader(System.in));
        MaintenanceServer server = null;
        try {
            server = new MaintenanceServer(3591, 50000);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while (!Thread.interrupted()) {
            String input;
            try {
                input = userReader.readLine();
            } catch (final IOException e) {
                e.printStackTrace();
                break;
            }

            if (input.equals("exit")) {
                server.shutdown();
                break;
            }

            if (input.equals("list")) {
                Set<String> connections = server.getClientConnections();
                System.out.println(String.format("Found %1d connections.", connections.size()));
                for (String c : connections) {
                    System.out
                            .println(String.format("- '%1s' (%2s)", c, server.getClientConnection(c).getHostAddress()));
                }
                continue;
            }

            // restart
            if (input.startsWith("status ")) {
                String location = input.replaceAll("^status ", "");
                try {
                    IClientConnection conn = server.getClientConnection(location);
                    if (conn != null) {
                        MaintenanceResponse response = conn.sendQuery(new GetStatusRequest());
                        if (response == null) {
                            System.err.println("No response.");
                            continue;
                        }
                        if (!(response instanceof GetStatusResponse)) {
                            System.err.println("Invalid response of type " + response.getClass().getName());
                            continue;
                        }
                        GetStatusResponse status = (GetStatusResponse) response;
                        System.out.println(String.format("Backlight: %1s", status.getBacklightStatus().name()));
                        System.out.println(String.format("Interface: %1s", status.getInterfaceStatus().name()));
                        System.out.println(String.format("Start up : %1s",
                                status.getStartupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                continue;
            }

            // restart
            if (input.startsWith("restart ")) {
                String location = input.replaceAll("^restart ", "");
                try {
                    IClientConnection conn = server.getClientConnection(location);
                    if (conn != null) {
                        conn.sendQuery(new RestartAppRequest());
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                continue;
            }

            // Error message. Command not found.
            System.out.println("Command invalid.\n" + "    list                   - List connected clients\n" +
                    "    restart <ID>           - Restart the client <ID>\n" +
                    "    exit                   - Close the connection");
        }
    }
}
