package com.github.dialogos.plugin.remote.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Manager {
    private static WebSocketHub globalHub;
    private static final Map<String, WebSocketHub> portHubs = new ConcurrentHashMap<>();
    private static boolean serverMode = false;
    //TODOSamir look into where to add the IP adress... and if IPAdress is null maybe take the IPAdress from the AudioInput/Output Settings????

    public static void setServerMode(boolean isServerMode) {
        serverMode = isServerMode;
    }

    /**
        Get or create a hub for the given port.
        If in GUI mode, just returns/creates a global singleton.
    */
    public static synchronized WebSocketHub getOrCreateHub(String userId, int port) {
        try {
            if (serverMode) {
                System.out.println("HubManager: Is Server mode");
                System.out.flush();
                if(portHubs.containsKey(Integer.toString(port))){
                    System.out.println("Port " + port + ", already existed. Adding User " + userId);
                    System.out.flush();
                    WebSocketHub hub = portHubs.get(Integer.toString(port));
                    return hub;
                }
                else{
                    WebSocketHub hub = new WebSocketHub(port);
                    hub.start();
                    portHubs.put(Integer.toString(port), hub);
                    System.out.println("Port " + port + ", did not exist. Adding User " + userId);
                    System.out.flush();
                    return hub;
                }
            } else {
                System.out.println("HubManager: Is Server not mode");
                System.out.flush();
                if (globalHub == null) {
                    globalHub = new WebSocketHub(port);
                    globalHub.start();
                    System.out.println("HubManager: Started global hub on port " + port);
                    System.out.flush();
                }
                return globalHub;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create hub", e);
        }
    }

    public static WebSocketHub getHub(String port) {
        return serverMode ? portHubs.get(port) : globalHub;
    }

    public static void removeHub(String port) {
        if (serverMode) {
            WebSocketHub hub = portHubs.remove(port);
            if (hub != null) {
                try { hub.stop(); } catch (Exception ignored) {}
                System.out.println("HubManager: Removed hub for Port " + port);
            }
        }
    }

    public static void stopAll() {
        if (globalHub != null) {
            try { globalHub.stop(); } catch (Exception ignored) {}
            globalHub = null;
        }
        portHubs.values().forEach(h -> {
            try { h.stop(); } catch (Exception ignored) {}
        });
        portHubs.clear();
    }
}
