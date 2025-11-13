package com.github.dialogos.plugin.remote.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Manager {
    private static WebSocketHub globalHub;
    private static final Map<String, WebSocketHub> userHubs = new ConcurrentHashMap<>();
    private static boolean serverMode = false;
    //TODOSamir look into where to add the IP adress... and if IPAdress is null maybe take the IPAdress from the AudioInput/Output Settings????

    public static void setServerMode(boolean isServerMode) {
        serverMode = isServerMode;
    }

    /**
        Get or create a hub for the given userId.
        If in GUI mode, just returns/creates a global singleton.
    */
    public static synchronized WebSocketHub getOrCreateHub(String userId, int port) {
        try {
            if (serverMode) {
                System.out.println("HubManager: Is Server mode");
                System.out.flush();
                return userHubs.computeIfAbsent(userId, id -> {
                    try {
                        WebSocketHub hub = new WebSocketHub(port);
                        System.out.flush();
                        hub.start();
                        System.out.println("HubManager: Created hub for user " + id + " on port " + port);
                        return hub;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
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

    public static WebSocketHub getHub(String userId) {
        return serverMode ? userHubs.get(userId) : globalHub;
    }

    public static void removeHub(String userId) {
        if (serverMode) {
            WebSocketHub hub = userHubs.remove(userId);
            if (hub != null) {
                try { hub.stop(); } catch (Exception ignored) {}
                System.out.println("HubManager: Removed hub for user " + userId);
            }
        }
    }

    public static void stopAll() {
        if (globalHub != null) {
            try { globalHub.stop(); } catch (Exception ignored) {}
            globalHub = null;
        }
        userHubs.values().forEach(h -> {
            try { h.stop(); } catch (Exception ignored) {}
        });
        userHubs.clear();
    }
}
