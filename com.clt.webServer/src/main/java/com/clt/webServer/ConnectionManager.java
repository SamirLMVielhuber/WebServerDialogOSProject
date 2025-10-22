package com.clt.webServer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.Map;


public class ConnectionManager {
    /**Connection Manager Singelton
        Keeps track of all connections via all Servlets to use them.
    */
    private final Map<String, DocumentManager> connections = new ConcurrentHashMap<>();

    private static final ConnectionManager INSTANCE = new ConnectionManager();

    private ConnectionManager() {}

    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void openConnection(String userId, String filePath, BiConsumer<String, byte[]> sendAudioCallback) throws Exception {
        System.out.println("Opening connection for userId " + userId +"\nWho wants Graph " + filePath);
        System.out.flush();
        //Remove old connection if exists
        if (connections.containsKey(userId)) {
            closeConnection(userId);
        }

        DocumentManager gM = new DocumentManager();
        gM.loadGraph(filePath);
        gM.setSenderAudioCallback(userId, sendAudioCallback);
        connections.put(userId, gM);
    }

    //TODO This currently would not delete the session just as a note here
    //  but idk if that would be nessecary
    public synchronized void closeConnection(String userId) {
        DocumentManager gM = connections.get(userId);
        System.out.println("Closing Graph for userId " + userId);
        System.out.flush();
        if (gM != null) {
            //gM.closeGraph();
            connections.remove(userId);
        }
    }

    public DocumentManager getGraphManager(String userId) {
        return connections.get(userId);
    }

    public Map<String, DocumentManager> getAllConnections() {
        return connections;
    }
}

