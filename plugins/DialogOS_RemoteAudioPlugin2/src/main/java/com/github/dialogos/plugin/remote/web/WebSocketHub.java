package com.github.dialogos.plugin.remote.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

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

    private static final String PATH = "somePath"; 
    private static final String PW = "somePW";
    private static final String IP = "someIP";

    public void start() throws Exception {
        if (server != null && server.isRunning()) 
            return;
        System.out.println("Starting Server on Port: " + this.port);
        server = new Server();


        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(PATH);
        sslContextFactory.setKeyStorePassword(PW);
        sslContextFactory.setKeyManagerPassword(PW);

        ServerConnector connector = new ServerConnector(server, sslContextFactory);
        connector.setHost(IP);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
            container.addMapping("/audio-receive", WebSocketAudioReceiver.class);
            container.addMapping("/audio-stream", AudioWebSocketStreamer.class);
        });

        server.setHandler(context);
        server.start();

        //Register hub so annotated sockets can find it by port
        HubRegistry.registerHub(port, this);

        System.out.println("WebSocketHub: started on port " + port);
        System.out.flush();
    }

    public void stop() throws Exception {
        if (server != null) {
            HubRegistry.unregisterHub(port);
            server.stop();
            server = null;
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
    
    public void registerInputSession(String userId, Session session) {
        Session old = this.inputSessions.put(userId, session);
        if (old != null && old.isOpen()) {
            System.out.println("There was a previous open Inputsession associated with user " + userId + "\nThat session has been overwritten and closed.");
            System.out.flush();
            try { 
                old.close();
            } catch (Exception ex) {}
        }
    }

    public void unregisterInputSession(String userId) {
        Session old = this.inputSessions.remove(userId);
        if (old != null && old.isOpen()){
            System.out.println("Unregistering Inputsession for " + userId);
            try{
                old.close();
            } catch (Exception ex){}
        }
        this.callbacks.remove(userId);
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
    }

    public void unregisterOutputSession(String userId) {
        Session old = this.outputSessions.remove(userId);
        if (old != null && old.isOpen()){
            System.out.println("Unregistering Outputsession for " + userId);
            try{
                old.close();
            } catch (Exception ex){}
        }
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