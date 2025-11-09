package com.github.dialogos.plugin.remote.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import com.github.dialogos.plugin.remote.web.WebSockets.AudioWebSocketStreamer;
import com.github.dialogos.plugin.remote.web.WebSockets.WebSocketAudioReceiver;

public class WebSocketHub {
    //must currently be unique for every document
    private final int port;
    private Server server;

    private final Map<String, Session> inputSessions = new ConcurrentHashMap<>();
    private final Map<String, Session> outputSessions = new ConcurrentHashMap<>();
    private volatile Consumer<byte[]> audioInputCallback;

    public WebSocketHub(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        if (server != null && server.isRunning()) 
            return;

        server = new Server(port);
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

    public void setAudioInputCallback(Consumer<byte[]> callback) {
        this.audioInputCallback = callback;
    }
    public Consumer<byte[]> getAudioInputCallback(){
        return this.audioInputCallback;
    }
    
    public void registerInputSession(String userId, Session session) {
        inputSessions.put(userId, session);
    }

    public void unregisterInputSession(String userId) {
        inputSessions.remove(userId);
    }

    public void registerOutputSession(String userId, Session session) {
        outputSessions.put(userId, session);
    }

    public void unregisterOutputSession(String userId) {
        outputSessions.remove(userId);
    }

    public Session getOutputSession(String userId) {
        return outputSessions.get(userId);
    }
    public Session getInputSession(String userId) {
        return inputSessions.get(userId);
    }

    public void broadcastToOutputs(byte[] audio) {
        for (Session s : outputSessions.values()) {
            try {
                if (s.isOpen()) 
                    s.getRemote().sendBytes(java.nio.ByteBuffer.wrap(audio));
            } catch (Exception e) { 
                e.printStackTrace(); 
            }
        }
    }
}