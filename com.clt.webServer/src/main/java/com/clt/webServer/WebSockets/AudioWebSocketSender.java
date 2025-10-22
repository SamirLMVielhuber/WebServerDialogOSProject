package com.clt.webServer.WebSockets;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import com.clt.webServer.ConnectionManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class AudioWebSocketSender {
    
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Session, String> sessionToUser = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        String query = session.getUpgradeRequest().getQueryString();
        String userId = "Unknown";
        if (query != null && query.startsWith("userId=")) {
            userId = query.split("=")[1];
        }
        sessions.put(userId, session);
        sessionToUser.put(session, userId);
        System.out.println("WebSocket connected for userId: " + userId);
        System.out.flush();
        System.out.println("He has the following Graph: ");
        ConnectionManager.getInstance().getGraphManager(userId).printGraph();
        System.out.flush();
        ConnectionManager.getInstance().getGraphManager(userId).startGraph();
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String userId = sessionToUser.remove(session);
        if (userId != null) sessions.remove(userId);
        System.out.println("WebSocket closed: " + reason + " for userId: " + userId);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, byte[] buffer, int offset, int length) {
        if (!sessionToUser.containsKey(session)) {
            try {
                String userId = new String(buffer, offset, length, "UTF-8");
                sessions.put(userId, session);
                sessionToUser.put(session, userId);
                System.out.println("User registered for audio WebSocket: " + userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        System.out.println("Received audio data from userId: " + sessionToUser.get(session));
    }

    public static void sendToUser(String userId, ByteBuffer audioData) {
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendBytes(audioData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
