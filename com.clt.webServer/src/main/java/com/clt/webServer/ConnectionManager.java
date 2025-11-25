package com.clt.webServer;

import java.util.concurrent.ConcurrentHashMap;

import com.github.dialogos.plugin.remote.web.GraphControlListener;
import com.github.dialogos.plugin.remote.web.GraphControlRegistry;

import java.util.Map;


public class ConnectionManager {
    /** 
        Singleton that keeps track of all active graph connections. 
    */
    private final Map<String, DocumentManager> connections = new ConcurrentHashMap<>();
    private static final ConnectionManager INSTANCE = new ConnectionManager();

    private ConnectionManager() {
        
        //In theory I should probably make it so that it is definitly laoded before those events can fire but whatever will be the case i guess...
        GraphControlRegistry.setListener(new GraphControlListener() {
            @Override
            public void onStartRequested(String userId) {
                DocumentManager doc = ConnectionManager.getGraphManager(userId);
                if (doc != null) {
                    System.out.println("GraphControlRegistry: Starting Graph " + doc.getGraphName());
                    doc.startGraph();
                }
                System.out.flush();
            }

            @Override
            public void onStopRequested(String userId) {
                DocumentManager doc = ConnectionManager.getGraphManager(userId);
                System.out.println("GraphControlRegistry: Called on Stop Request for " + doc.getGraphName() + " for User: " + userId);
                if (doc != null) {
                    System.out.println("GraphControlRegistry: Stopping Graph");
                    System.out.flush();
                    doc.closeGraph();
                    PortManager.getInstance().releasePorts(userId);
                }
                System.out.flush();
            }

            @Override
            public void onPauseRequested(String userId) {
                throw new IllegalStateException("ConnectionManager: Pausing of Graph currently not possible " + userId);
            }
        });

    }

    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    /**
        Opens a new connection for a user and assigns ports for that user's graph.
    */
    public synchronized void openConnection(String userId, String filePath) throws Exception {
        System.out.println("Opening connection for userId " + userId + "\nWho wants Graph " + filePath);
        System.out.flush();

        if (connections.containsKey(userId)) {
            closeConnection(userId);
        }

        DocumentManager gM = new DocumentManager();
        gM.loadGraph(filePath, userId);

        int requestedInputPort = gM.getInputPort();

        System.out.println("ConnectionManager Wanted Port by Graph\n    Requested Port: " + requestedInputPort);

        PortManagerNew portManager = PortManagerNew.getInstance();

        //Try to assign the input port normally
        int assignedInputPort = portManager.assignPort(userId, requestedInputPort);

        gM.setInputPort(assignedInputPort);
        gM.setOutputPort(assignedInputPort);

        //This is a leftover from the way before where it was possible to use different ports for input and output
        System.out.println("Assigned ports for userId " + userId +"\n    input: " + assignedInputPort + ", output: " + assignedInputPort);
        System.out.flush();

        connections.put(userId, gM);
        gM.setUserId(userId);
        gM.startServer();
    }


    /**
        Closes an existing graph connection, and releases its assigned ports.
    */
    public synchronized void closeConnection(String userId) {
        DocumentManager gM = connections.get(userId);
        System.out.println("Closing Graph for userId " + userId);
        System.out.flush();

        if (gM != null) {
            try {
                PortManager.getInstance().releasePorts(userId);
            } catch (Exception e) {
                System.err.println("Failed to release ports for userId " + userId + ": " + e.getMessage());
            }
            connections.remove(userId);
            gM.closeGraph();
        }
    }

    public static DocumentManager getGraphManager(String userId) {
        return INSTANCE.connections.get(userId);
    }

    public Map<String, DocumentManager> getAllConnections() {
        return connections;
    }
}

