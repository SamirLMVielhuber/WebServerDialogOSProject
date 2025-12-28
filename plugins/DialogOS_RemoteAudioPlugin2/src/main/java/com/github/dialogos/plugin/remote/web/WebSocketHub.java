package com.github.dialogos.plugin.remote.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import com.clt.Config;
import com.github.dialogos.plugin.remote.web.WebSockets.AudioWebSocketStreamer;
import com.github.dialogos.plugin.remote.web.WebSockets.WebSocketAudioReceiver;

public class WebSocketHub {
    private final int port;
    private Server server;
    private final Map<String, Session> inputSessions = new ConcurrentHashMap<>();
    private final Map<String, Session> outputSessions = new ConcurrentHashMap<>();
    
    private final Map<String, Consumer<byte[]>> callbacks = new ConcurrentHashMap<>();

    public WebSocketHub(int port) {
        this.port = port;
    }

    private static final Resource KEYSTORE = Config.KEYSTORE();
    private static final String PW = Config.PASS();
    private static final String IP = Config.IP();

    public void start() throws Exception{
        this.start(null);
    }

    private static void runWebServer() throws Exception {
        int port = 8000;
        
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreResource(KEYSTORE);
        sslContextFactory.setKeyStorePassword(PW);
        sslContextFactory.setKeyManagerPassword(PW);

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server, sslContextFactory);
        connector.setHost(IP);
        connector.setPort(port);

        server.addConnector(connector);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setResourceBase(WebSocketHub.class.getResource("/webappTest").toExternalForm());
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        ContextHandler staticContext = new ContextHandler("/");
        staticContext.setHandler(resourceHandler);

        HandlerList handlers = new HandlerList();
        handlers.addHandler(staticContext);

        server.setHandler(handlers);

        server.start();
        System.out.println("Server running at https://" + IP + ":" + port);
        System.out.println("WebSocket endpoints will be created dynamically at:");
        System.out.println("     wss://" + IP + ":{somePort}/audio-stream");
        System.out.println("     wss://"+ IP +":{somePort}/audio-receive");
    }
    
    //Safe to call because it instantly returns if it should not be possible to call it
    public void start(String ip) throws Exception {
        if (this.server != null && this.server.isRunning()) 
            return;

        System.out.println("Starting Server on Port: " + this.port);
        this.server = new Server();

        if(ip == null || ip.isEmpty())
            ip = IP;

        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreResource(KEYSTORE);
        sslContextFactory.setKeyStorePassword(PW);
        sslContextFactory.setKeyManagerPassword(PW);

        ServerConnector connector = new ServerConnector(this.server, sslContextFactory);
        connector.setHost(ip);
        connector.setPort(port);
        this.server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
            container.addMapping("/audio-receive", WebSocketAudioReceiver.class);
            container.addMapping("/audio-stream", AudioWebSocketStreamer.class);
        });

        this.server.setHandler(context);
        this.server.start();

        //Register hub so annotated sockets can find it by port
        HubRegistry.registerHub(port, this);

        System.out.println("WebSocketHub: started on port " + port);
        System.out.flush();

        if(!Manager.isServerMode())
            this.runWebServer(); //For testing
    }

    public void stop() throws Exception {
        if (this.server != null) {
            HubRegistry.unregisterHub(port);
            this.server.stop();
            this.server = null;
            this.removeAllUsers(); //Just in case some user is left
            System.out.println("WebSocketHub: stopped on port " + port);
            System.out.flush();
        }
    }

    public void registerAudioInputCallback(String userId, Consumer<byte[]> callback) {
        System.out.println("Registering Audio Callback for User " + userId);
        this.callbacks.put(userId, callback);
    }
    
    public Consumer<byte[]> getAudioInputCallback(String userId){
        return this.callbacks.get(userId);
    }
    
    public boolean isEmpty(){
        return this.inputSessions.isEmpty() && this.outputSessions.isEmpty();
    }

    private void maybeScheduleUserShutdown(String userId) {
        if (!this.inputSessions.containsKey(userId) && !this.outputSessions.containsKey(userId))
            UserTimeoutScheduler.getInstance().scheduleStopForUser(userId, this.port);
    }

    public void registerInputSession(String userId, Session session) {
        Session old = this.inputSessions.put(userId, session);
        if (old != null && old.isOpen()) {
            System.out.println("There was a previous open Inputsession associated with user " + userId + ", on Port " + this.port + "\nThat session has been overwritten and closed.");
            System.out.flush();
            try { 
                old.close();
            } catch (Exception ex) {}
        }
        UserTimeoutScheduler.getInstance().cancelStopForUser(userId);
    }

    public void unregisterInputSession(String userId) {
        Session old = this.inputSessions.remove(userId);
        if (old != null && old.isOpen()){
            System.out.println("Unregistering Inputsession for " + userId);
            System.out.flush();
            try{
                old.close();
            } catch (Exception ex){}
        }
        //this.callbacks.remove(userId); This is now done inside the timeout Scheduler...
        maybeScheduleUserShutdown(userId);
    }

    public void removeUser(String userId) {
        System.out.println("WebSocketHub: Deleting User: " + userId + " completely");
        Session input = inputSessions.remove(userId);
        if (input != null) {
            try {
                input.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Session output = outputSessions.remove(userId);
        if (output != null) {
            try {
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        callbacks.remove(userId);
    }

    public void removeAllUsers(){
        System.out.println("WebSocketHub: Removing all Users");
        //Just in Case create a Set of all Users to make sure to properly delete everysingle one of them
        Set<String> allUsers = new HashSet<>();

        allUsers.addAll(inputSessions.keySet());
        allUsers.addAll(outputSessions.keySet());
        allUsers.addAll(callbacks.keySet());

        for (String userId : allUsers)
            removeUser(userId);
    }

    public void registerOutputSession(String userId, Session session) {
        Session old = this.outputSessions.put(userId, session);
        if (old != null && old.isOpen()) {
            System.out.println("There was a previous open Outputsession associated with user " + userId + "\nThat session has been overwritten and closed.");
            System.out.flush();
            try { 
                old.close();
            } catch (Exception ex) {}
        }

        UserTimeoutScheduler.getInstance().cancelStopForUser(userId);
    }

    public void unregisterOutputSession(String userId) {
        Session old = this.outputSessions.remove(userId);
        if (old != null && old.isOpen()){
            System.out.println("Unregistering Outputsession for " + userId);
            System.out.flush();
            try{
                old.close();
            } catch (Exception ex){}
        }
        maybeScheduleUserShutdown(userId);
    }

    public Session getOutputSession(String userId) {
        return this.outputSessions.get(userId);
    }
    public Session getInputSession(String userId) {
        return this.inputSessions.get(userId);
    }

    public void broadcastToOutputs(byte[] audio) {
        for (Session s : this.outputSessions.values()) {
            try {
                if (s.isOpen()) 
                    s.getRemote().sendBytes(java.nio.ByteBuffer.wrap(audio));
            } catch (Exception e) { 
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.out.flush();
            }
        }
    }
}