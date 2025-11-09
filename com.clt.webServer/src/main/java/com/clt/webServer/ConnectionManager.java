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
                    new Thread(() -> {
                        try {
                            doc.startGraph();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, "GraphRunner-" + userId).start();
                }
                System.out.flush();
            }

            @Override
            public void onStopRequested(String userId) {
                DocumentManager doc = ConnectionManager.getGraphManager(userId);
                if (doc != null) {
                    System.out.println("GraphControlRegistry: Stopping Graph " + doc.getGraphName());
                    doc.closeGraph();
                    //TODOSAMIR Delete Port entries here...
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
        int requestedOutputPort = gM.getOutputPort();

        System.out.println("ConnectionManager Wanted Port by Graph\n    Input:" + requestedInputPort + "\n    Output:" + requestedOutputPort);

        PortManager portManager = PortManager.getInstance();

        //Try to assign the input port normally
        int assignedInputPort = portManager.assignPort(userId + "_input", requestedInputPort);

        int assignedOutputPort;

        //Try to reuse the same port if wanted
        if (assignedInputPort == requestedOutputPort && portManager.isPortAvailable(assignedInputPort)) {
            assignedOutputPort = assignedInputPort;
        } 
        else {
            //Try to assign the requested output port normally
            assignedOutputPort = portManager.assignPort(userId + "_output", requestedOutputPort);

            //If they differ and you want them unified, fallback to using the same one
            if (assignedInputPort != assignedOutputPort) {
                System.out.println("Different ports assigned (" + assignedInputPort + " / " + assignedOutputPort + "), trying same port fallback...");
                int samePort = portManager.assignPort(userId + "_shared", assignedInputPort);
                assignedInputPort = samePort;
                assignedOutputPort = samePort;
            }
        }

        gM.setInputPort(assignedInputPort);
        gM.setOutputPort(assignedOutputPort);

        System.out.println("Assigned ports for userId " + userId +
                "\ninput: " + assignedInputPort + ", output: " + assignedOutputPort);
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
            //TODO clean up Graph...
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

