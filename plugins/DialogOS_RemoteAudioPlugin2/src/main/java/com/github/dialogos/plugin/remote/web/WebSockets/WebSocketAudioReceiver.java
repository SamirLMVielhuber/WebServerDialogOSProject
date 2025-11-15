package com.github.dialogos.plugin.remote.web.WebSockets;

import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import com.github.dialogos.plugin.remote.web.HubRegistry;
import com.github.dialogos.plugin.remote.web.WebSocketHub;

@WebSocket
public class WebSocketAudioReceiver {
    private WebSocketHub hub;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        String userId = extractUserId(session);
        int localPort = ((java.net.InetSocketAddress) session.getLocalAddress()).getPort();
        this.hub = HubRegistry.getHub(localPort);

        if (this.hub == null) {
            System.err.println("WebSocketAudioReceiver: no hub for port " + localPort);
            System.out.flush();
            return;
        }
        
        this.hub.registerInputSession(userId, session);
        System.out.println("WebSocketAudioReceiver: /audio-receive connected for userId=" + userId + " on port: " + localPort);
        System.out.flush();
    }

    @OnWebSocketMessage
    public void onMessage(Session session, byte[] payload, int offset, int length) {
        String userId = extractUserId(session);
        if (this.hub == null){
            System.out.println("WebSocketAudioReceiver: Hub does not exist, can not receive Message");
            System.out.flush();
            return;
        }
        byte[] audio = new byte[length];
        System.arraycopy(payload, offset, audio, 0, length);

        if (this.hub.getAudioInputCallback(userId) != null)
            this.hub.getAudioInputCallback(userId).accept(audio);
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        String userId = extractUserId(session);
        if (this.hub != null && userId != null) {
            this.hub.unregisterInputSession(userId);
            System.out.println("WebSocketAudioReceiver: disconnected " + userId);
            System.out.flush();
        }
    }

    private String extractUserId(Session session) {
        Map<String, List<String>> params = session.getUpgradeRequest().getParameterMap();
        if (params.containsKey("userId")) 
            return params.get("userId").get(0);
        return null;
    }
}