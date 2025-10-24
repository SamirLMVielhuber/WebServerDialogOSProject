package com.clt.webServer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.clt.diamant.SingleDocument;
import com.clt.diamant.graph.*;
import com.clt.diamant.suspend.DialogState;
import com.clt.diamant.DocumentLoader;
import com.clt.dialogos.plugin.AudioPlugin;
import com.clt.diamant.Document;

import com.clt.event.ProgressListener;
import com.github.dialogos.plugin.remote.web.Input.WebSocketAudioInputPlugin;
import com.github.dialogos.plugin.remote.web.Output.WebSocketAudioOutputPlugin;


public class DocumentManager{
    public enum Status{
        AKTIVE,
        PAUSED
    }
    private Status state;
    private SingleDocument document;
    private DialogState dS;
    private WebSocketAudioOutputPlugin audioPlugin;
    
    public DocumentManager(){
        this.dS = null;
    }

    public void loadGraph(String filePath){
        File f = new File(filePath);
        System.out.println("Loading File " + filePath);

        DocumentLoader loader = new DocumentLoader(null);
        ProgressListener progress = event -> System.out.println(event.getMessage());
        progress = null;
        Document d;
        try{
            d = loader.load(f, progress);
        }
        catch(IOException ex){
            throw new IllegalStateException("Could not load Document" + ex.getStackTrace() + "\n" + ex.getMessage());
        }
        if(d instanceof SingleDocument){
            this.document =(SingleDocument) d;
        } else{
            throw new IllegalStateException("Loaded document is not a SingleDocument!");
        }

        System.out.println("File loaded");
        System.out.flush();
        state = Status.AKTIVE;
    }
    /*public boolean pauseGraph(){
        Graph g = this.document.getOwnedGraph();
        Collection<? extends SearchResult> activeNodes = g.find(new NodeSearchFilter(){
            @Override
            public Collection<? extends SearchResult> match(Node n){
                List<SearchResult> results = new ArrayList<>();
                if(n.isActive()){ 
                    results.add(new NodeSearchResult(n, "Active node: " + n.getTitle(), SearchResult.Type.INFO));
                }
                return results;
                }
        });
        if(activeNodes.size() > 1){
            System.out.println("Multiple nodes where active is that allowed?");
            System.out.flush();
        }
        else if(activeNodes.size() == 1){
            System.out.println("Exactly one node was active should be fine");
            System.out.flush();
        }
        Node firstActiveNode = null;
        if(!activeResults.isEmpty()){
            SearchResult firstResult = activeResults.iterator().next();
            if(firstResult instanceof NodeSearchResult){
                firstActiveNode =((NodeSearchResult) firstResult).getNode();
                System.out.println("Suspending Node " + firstActiveNode.getName());            
                System.out.flush();
            }
        }
        try{
            g.suspend(firstActiveNode, null);
        }
        catch(DialogSuspendedException ex){
            this.dS = ex.getDialogState();
            this.state = Status.PAUSED;
            System.out.println("Successful try of suspending Node " + ex.getMessage());
            System.out.flush();
            return true;
        }
        catch(UnsupportedOperationException ex){
            System.out.println("Unspported try of suspending Node " + ex.getMessage());
            System.out.flush();
            return false;
        }
        return false;
    }

    //TODO maybe this wants to throw an exception later on
    public boolean resumeGraph(){
        if(this.state.compareTo(Status.AKTIVE)){
            System.out.println("Graph was never Inactive");
            return false;
        }
        
        if(this.dS != null){
            System.out.println("Resuming Dialog at saved State");        
            this.document.getOwnedGraph().resume(this.dS);
            this.resetDialogState();
            this.state = Status.AKTIVE;
            System.out.flush();
            return true;
        }
        else{
            System.out.println("DialogState was Null therefore can not Resume");
            System.out.flush();
        }
        return false;
    }
    private void resetDialogState(){
        System.out.println("Reset DialogState");
        System.out.flush();
        this.dS = null;
    }
    */

    public void setSenderAudioCallback(String userId, BiConsumer<String, byte[]> audioCallback){
        //maybe this maybe just get the ActiveAudioOutputPlugin and use establishConnection on it
        AudioPlugin output = this.document.getPluginManager().getActiveAudioOutputPlugin();
        System.out.println("Using AudioOutputPlugin\nid: " + output.getId() + "\nname: " + output.getName());
        if(output instanceof WebSocketAudioOutputPlugin){
            WebSocketAudioOutputPlugin.AudioCallback callback = new WebSocketAudioOutputPlugin.AudioCallback(){
                @Override
                public void onAudioChunk(String uid, byte[] data){
                    audioCallback.accept(uid, data);
                }
            };
            ((WebSocketAudioOutputPlugin) output).establishConnection(userId, callback);
        }
        else{
            throw new IllegalStateException("Currently there is no support for another Plugin than WebSocketAudioOutputPlugin");
        }
    }

    @Deprecated
    public void setRecieverAudioCallback(Consumer<byte[]> audioCallback){
        AudioPlugin input = this.document.getPluginManager().getActiveAudioInputPlugin();
        System.out.println("Using AudioOutputPlugin\nid: " + input.getId() + "\nname: " + input.getName());
        if(input instanceof WebSocketAudioInputPlugin){
            WebSocketAudioInputPlugin.AudioReceiver callback = new WebSocketAudioInputPlugin.AudioReceiver(){
                @Override
                public void onAudioReceived(byte[] data) {
                    audioCallback.accept(data);
                }
            };
            ((WebSocketAudioInputPlugin) input).establishConnection(callback);
        }
        else{
            throw new IllegalStateException("Currently there is no support for another Plugin than WebSocketAudioInputPlugin");
        }
        
    }

    public void forwardAudio(byte[] audio){
        AudioPlugin input = this.document.getPluginManager().getActiveAudioInputPlugin();
        if (input instanceof WebSocketAudioInputPlugin) {
            ((WebSocketAudioInputPlugin) input).receiveAudio(audio);
        }
    }

    public void closeGraph(){
        throw new UnsupportedOperationException("Closing of a Graph Currently not Implemented");
    }

    public void startGraph(){
        HeadlessWozInterface transition = new HeadlessWozInterface();
        System.out.println("Starting Graph");
        try{
            this.document.run(null, transition);
        } catch(Exception e){
            e.printStackTrace();
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
}
