package com.github.dialogos.plugin.remote.web.WebSockets;

import java.util.Map;
import java.util.List;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import com.github.dialogos.plugin.remote.web.GraphControlListener;
import com.github.dialogos.plugin.remote.web.GraphControlRegistry;
import com.github.dialogos.plugin.remote.web.HubRegistry;
import com.github.dialogos.plugin.remote.web.WebSocketHub;

@WebSocket
public class AudioWebSocketStreamer {

    private String userId;
    private WebSocketHub hub;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        userId = extractUserId(session);
        int localPort = ((java.net.InetSocketAddress) session.getLocalAddress()).getPort();
        hub = HubRegistry.getHub(localPort);

        if (hub == null) {
            System.err.println("AudioWebSocketSender: no hub for port " + localPort);
            System.out.flush();
            return;
        }

        if (userId == null) 
            userId = "unknown";
        
        hub.registerOutputSession(userId, session);
        System.out.println("AudioWebSocketSender: /audio-stream connected for userId=" + userId + " on port " + localPort);
        System.out.flush();
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        if (hub == null || userId == null){
            System.out.println("AudioWebSocketStreamer: hub or userId are null, \n    so no flow control possible");
            return;
        }

        GraphControlListener listener = GraphControlRegistry.getListener();
        if (listener == null) 
            return;
        String msg = message.trim().toUpperCase();

        System.out.println("Received GraphControl Command " + msg);
        System.out.flush();
        
        switch (msg) {
            case "START_GRAPH":
                listener.onStartRequested(userId);
                break;
            case "STOP_GRAPH":
                listener.onStopRequested(userId);
                //TODOSAMIR stop server for this port...
                break;
            case "PAUSE_GRAPH":
                listener.onPauseRequested(userId);
                break;
            default:
                break;
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        if (hub != null && userId != null) {
            hub.unregisterOutputSession(userId);
            System.out.println("AudioWebSocketSender: disconnected " + userId);
        }
    }

    private String extractUserId(Session session) {
        Map<String, List<String>> params = session.getUpgradeRequest().getParameterMap();
        if (params.containsKey("userId")) 
            return params.get("userId").get(0);
        return null;
    }
}
