package com.ds.coursework2;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerConnection {
    private static int[] serverPorts = new int[] { 11436, 11437, 11438 };

    public static int getServerPort() {
        boolean serverStarted = false;
        Server server = null;
        for (int i = 0; i < serverPorts.length; i++) {
            int port = serverPorts[i];
            try {
                server = ServerBuilder.forPort(port).build();
                server.start();
                serverStarted = true;
            } catch (Exception ex) {
                System.err.println("Cannot connect to the port number " + port);
            }

            if (serverStarted) {
                server.shutdownNow();
                return port;
            }
        }
        return -1;
    }
}
