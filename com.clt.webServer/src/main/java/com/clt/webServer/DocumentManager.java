package com.clt.webServer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.clt.diamant.WozInterface;
import com.clt.diamant.graph.*;
import com.clt.diamant.web.WebDocumentLoader;
import com.clt.diamant.web.WebSingleDocument;
import com.clt.diamant.Executer;
import com.clt.dialogos.plugin.AudioPlugin;
import com.clt.dialogos.plugin.PluginManager;
import com.clt.diamant.Document;

import com.clt.event.ProgressListener;
import com.clt.webServer.DocumentBase.DocumentPostProcessor;
import com.github.dialogos.plugin.remote.web.Input.WebSocketAudioInputPlugin;
import com.github.dialogos.plugin.remote.web.Output.WebSocketAudioOutputPlugin;

import edu.cmu.lti.dialogos.sphinx.client.Sphinx;

public class DocumentManager{
    public enum Status{
        AKTIVE,
        PAUSED
    }
    private WebSingleDocument document;
    private String userId;
    private Thread graphThread;
    private volatile boolean stopRequested = false;
    private WozInterface executer;
    
    public DocumentManager(){}

    public void loadGraph(String filePath, String userId){
        File f = new File(filePath);
        this.userId = userId;
        System.out.println("DocumentManager: Loading File " + filePath +"\nfor User: " + userId);

        WebDocumentLoader loader = new WebDocumentLoader(null);
        ProgressListener progress = event -> System.out.println(event.getMessage());
        progress = null;
        Document d;
        try{
            d = loader.load(f, progress);
        }
        catch(IOException ex){
            throw new IllegalStateException("Could not load Document" + ex.getStackTrace() + "\n" + ex.getMessage());
        }
        if(d instanceof WebSingleDocument){
            this.document =(WebSingleDocument) d;
        } else{
            throw new IllegalStateException("Loaded document is not a WebSingleDocument!");
        }

        System.out.println("DocumentManager: File loaded");
        System.out.flush();
        WebSocketAudioInputPlugin input = new WebSocketAudioInputPlugin();
        WebSocketAudioOutputPlugin output = new WebSocketAudioOutputPlugin();
        PluginManager pm = document.getPluginManager();
        pm.setActiveAudioInputPlugin(input);
        pm.setActiveAudioOutputPlugin(output);

        Sphinx recognizer = new Sphinx();
        DocumentPostProcessor.bindRecognizers(this.document, recognizer);
        WebSingleDocument.printAllVariables(this.document.getOwnedGraph());
    }

    public void closeGraph(){
        System.out.println("DocumentManager: Closing Graph (cleanup)");
        System.out.flush();

        if (this.document != null) {
            try {
                System.out.println("DocumentManager: Release Devices");
                this.document.closeDevices();
            } catch (Exception ex) {
                System.out.println("Error closing devices: " + ex.getMessage());
                ex.printStackTrace();
            }
            System.out.flush();
        }

        if (this.executer != null) {
            try {
                System.out.println("DocumentManager: Requesting execution abort");
                this.executer.abort();             // sets abort flag and disposes input
            } catch (Exception ex) {
                System.out.println("Error calling executer.abort(): " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        if (this.graphThread != null && this.graphThread.isAlive()) {
            try {
                System.out.println("DocumentManager: Interrupting graph thread");
                this.graphThread.interrupt();
            } catch (Exception ex) {
                System.out.println("Error interrupting graph thread: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        if (this.graphThread != null) {
            final long JOIN_TIMEOUT_MS = 10000;
            try {
                long start = System.currentTimeMillis();
                System.out.println("DocumentManager: Waiting for graph thread to finish...");
                this.graphThread.join(JOIN_TIMEOUT_MS);
                long waited = System.currentTimeMillis() - start;
                System.out.println("DocumentManager: join returned; waited " + waited + " ms");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("DocumentManager: join interrupted: " + e.getMessage());
                e.printStackTrace();
            }

            if (this.graphThread.isAlive()) {
                System.out.println("DocumentManager: Thread still alive after join timeout. Will attempt interrupt again.");
                try {
                    this.graphThread.interrupt();
                } catch (Exception ex) {
                    System.out.println("Error re-interrupting graph thread: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                System.out.println("DocumentManager: Graph thread has terminated.");
            }
            System.out.flush();
        }

        if (this.executer != null && this.document != null) {
            try {
                System.out.println("DocumentManager: Finalizing executer endDocument");
                this.executer.endDocument(this.document);
            } catch (Exception ex) {
                System.out.println("Error in executer.endDocument: " + ex.getMessage());
                ex.printStackTrace();
            }
            System.out.flush();
        }

        this.executer = null;
        this.graphThread = null;

        System.out.println("DocumentManager: closeGraph complete");
        System.out.flush();
    }

    public void startGraph(){
        this.graphThread = new Thread(() -> {
            try {
                this.executer = new Executer(null, false);
                System.out.println("DocumentManager: Starting Graph");
                System.out.flush();
                this.document.run(null, executer);
            } catch (Exception e) {
                System.out.println("DocumentManager: Exception occured while running document: " + e.getMessage());
                e.printStackTrace();
            }
        }, "GraphRunner-" + this.userId);
        this.graphThread.start();
    }

    public int getInputPort() { 
        AudioPlugin input = this.document.getPluginManager().getActiveAudioInputPlugin();
        if (input instanceof WebSocketAudioInputPlugin) {
            return ((WebSocketAudioInputPlugin) input).getPort();
        }
        return -1;
    }

    public int getOutputPort() { 
        AudioPlugin input = this.document.getPluginManager().getActiveAudioOutputPlugin();
        if (input instanceof WebSocketAudioOutputPlugin) {
            return ((WebSocketAudioOutputPlugin) input).getPort();
        }
        return -1;
    }

    public void setUserId(String userId){
        System.out.println("DocumentManager: Started Setting User " + userId);
        AudioPlugin input = this.document.getPluginManager().getActiveAudioInputPlugin();
        if (input instanceof WebSocketAudioInputPlugin) 
            ((WebSocketAudioInputPlugin) input).setUserId(userId);
        
        AudioPlugin output = this.document.getPluginManager().getActiveAudioOutputPlugin();
        if (output instanceof WebSocketAudioOutputPlugin) 
            ((WebSocketAudioOutputPlugin) output).setUserId(userId);
    }

    //This is from a time where it was possible to use a different port for input and output should be put on deprecated i guess and a new function that just sets both
    public void setInputPort(int port) { 
        AudioPlugin input = this.document.getPluginManager().getActiveAudioInputPlugin();
        if (input instanceof WebSocketAudioInputPlugin) {
            ((WebSocketAudioInputPlugin) input).setPort(port);
            System.out.println("DocumentManager: Input, Choosen Port: " + port);
        }
    }
    public void setOutputPort(int port) { 
        AudioPlugin output = this.document.getPluginManager().getActiveAudioOutputPlugin();
        if (output instanceof WebSocketAudioOutputPlugin) {
            ((WebSocketAudioOutputPlugin) output).setPort(port);
            System.out.println("DocumentManager: Ouput, Choosen Port: " + port);
        }
    }

    public void printGraph(){
        Graph g = this.document.getOwnedGraph();
        System.out.println("Graph nodes:");
        for(Node n : g.getNodes()){
            System.out.println("Node: " + n.getTitle() + "(id=" + n.getId() + ")");
            List<Edge> edges = n.getOutEdges();
            if(edges.isEmpty()){
                System.out.println("  No outgoing edges!");
            } else{
                for(Edge e : edges){
                    Node tgt = e.getTarget();
                    System.out.println("  -> " +(tgt != null ? tgt.getTitle() : "null") +
                                    "(id=" +(tgt != null ? tgt.getId() : "null") + ")");
                }
            }
            System.out.println("  Connected? " + n.isConnected());
        }
        System.out.flush();
    }

    public String getGraphName(){
        return this.document.getGraphName();
    }
    
    public void startServer(){
        System.out.println("DocumentManager: Starting Server for User: " + this.userId);
        System.out.flush();
        AudioPlugin output = this.document.getPluginManager().getActiveAudioOutputPlugin();
        if (output instanceof WebSocketAudioOutputPlugin) {
            ((WebSocketAudioOutputPlugin) output).attachHub(this.userId);
        }        
        AudioPlugin input = this.document.getPluginManager().getActiveAudioInputPlugin();
        if (input instanceof WebSocketAudioInputPlugin) {
            ((WebSocketAudioInputPlugin) input).attachHub(this.userId);
        }
    }
}
