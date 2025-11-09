package com.clt.webServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
    New Version of the connection Manager actually Manages what port is already in use
    Is Singlet just like the ConnectionManager was
*/
public class PortManager {
    private static final PortManager INSTANCE = new PortManager();
    private static final int BASE_PORT = 9000;
    private static final int MAX_PORT = 10000;

    private final Map<Integer, String> usedPorts = new ConcurrentHashMap<>();

    private PortManager() {}

    public static PortManager getInstance() {
        return INSTANCE;
    }

    public synchronized int assignPort(String idandSuffix, int requestedPort) {
        //Looks up if the Port is already in use and searches for a better one
        if (!usedPorts.containsKey(requestedPort) && isPortAvailable(requestedPort)) {
            usedPorts.put(requestedPort, idandSuffix);
            return requestedPort;
        }

        for (int port = BASE_PORT; port < MAX_PORT; port++) {
            if (!usedPorts.containsKey(port) && isPortAvailable(port)) {
                usedPorts.put(port, idandSuffix);
                return port;
            }
        }

        throw new RuntimeException("No available ports found!");
    }

    public synchronized void releasePorts(String userId) {
        //TODOSamir this is unsafe i guess i mean should be fine for now but releasing a port if the UserId is inside another UserId idk about that one....
        usedPorts.entrySet().removeIf(e -> e.getValue().startsWith(userId));
        System.out.println("Released Ports for " + userId);
        System.out.flush();
    }

    public int findAvailablePort() {
        for (int port = 1024; port < 65535; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1;
    }
    
    public boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
