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

    //TODOSamir close the server...
    public void closeGraph(){
        if (this.document != null) {
            System.out.println("Release Devices");
            this.document.closeDevices();

            System.out.flush();
        }
    }

    public void startGraph(){
        WozInterface executer = new Executer(null, false);
        System.out.println("DocumentManager: Starting Graph");
        try{
            this.document.run(null, executer);
        } catch(Exception e){
            e.printStackTrace();
        }
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
