package com.github.dialogos.plugin.remote.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HubRegistry {
    private static final Map<Integer, WebSocketHub> hubsByPort = new ConcurrentHashMap<>();

    public static void registerHub(int port, WebSocketHub hub) {
        hubsByPort.put(port, hub);
    }

    public static void unregisterHub(int port) {
        hubsByPort.remove(port);
    }

    public static WebSocketHub getHub(int port) {
        return hubsByPort.get(port);
    }
}