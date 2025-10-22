package com.clt.webServer.WebSockets;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import com.clt.webServer.ConnectionManager;
import com.clt.webServer.DocumentManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketAudioReceiver {

    // Maps userId to the input plugin instance
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final Map<Session, String> sessionToUser = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        String query = session.getUpgradeRequest().getQueryString();
        String userId = null;
        if (query != null && query.contains("userId=")) {
            userId = query.split("userId=")[1];
        }
        if (userId != null) {
            sessionToUser.put(session, userId);
            sessions.put(userId, session);
            System.out.println("Audio input WebSocket connected for " + userId);
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, byte[] payload, int offset, int length) {
        String userId = sessionToUser.get(session);
        if (userId == null) 
            return;

        DocumentManager gM = ConnectionManager.getInstance().getGraphManager(userId);

        byte[] audio = new byte[length];
        System.arraycopy(payload, offset, audio, 0, length);

        if (gM != null) {
            gM.forwardAudio(audio);
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String userId = sessionToUser.remove(session);
        if (userId != null) {
            System.out.println("Closed WebSocket audio input for " + userId);
        }
    }
}

