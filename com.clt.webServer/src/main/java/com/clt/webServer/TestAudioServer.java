package com.clt.webServer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import com.clt.webServer.Servlets.DialogLoadServlet;
import com.clt.webServer.Servlets.InfJsonServlet;
import com.github.dialogos.plugin.remote.web.Manager;

public class TestAudioServer {

    public static void main(String[] args) throws Exception {
        boolean runLocalTest = false;

        // Check for "-local" argument
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-local")) {
                runLocalTest = true;
                break;
            }
        }

        if (runLocalTest) {
            runLocalWebSocketTest();
        } else {
            runWebServer();
        }
    }

    /**
     * Starts the normal DialogOS Web Server
     */
    private static void runWebServer() throws Exception {
        int port = 8080;
        Server server = new Server(port);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setResourceBase(TestAudioServer.class.getResource("/webapp").toExternalForm());
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        ContextHandler staticContext = new ContextHandler("/");
        staticContext.setHandler(resourceHandler);

        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath("/");
        servletContext.addServlet(new ServletHolder(new DialogLoadServlet()), "/load");
        servletContext.addServlet(new ServletHolder(new InfJsonServlet()), "/inf.json");


        HandlerList handlers = new HandlerList();
        handlers.addHandler(staticContext);
        handlers.addHandler(servletContext);

        server.setHandler(handlers);

        server.start();
        System.out.println("Server running at http://localhost:" + port);
        System.out.println("WebSocket endpoints will be created dynamically at:");
        System.out.println("     ws://localhost:{somePort}/audio-stream");
        System.out.println("     ws://localhost:{somePort}/audio-receive");
        Manager.setServerMode(true);

        server.join();
    }

    /**
     * Runs the local WebSocket test client
     */
    private static void runLocalWebSocketTest() throws Exception {
        String userId = "default";
        int port = 8080;

        WebSocketClient client = new WebSocketClient();
        client.start();

        URI receiveUri = new URI("ws://localhost:" + port + "/audio-receive?userId=" + userId);
        URI streamUri = new URI("ws://localhost:" + port + "/audio-stream?userId=" + userId);

        System.out.println("Connecting to /audio-receive...");
        Future<Session> receiveFuture = client.connect(new AudioReceiveSocket(), receiveUri);

        System.out.println("Connecting to /audio-stream...");
        Future<Session> streamFuture = client.connect(new AudioStreamSocket(), streamUri);

        Session receiveSession = receiveFuture.get();
        Session streamSession = streamFuture.get();

        // Send some fake audio data
        byte[] fakeAudio = new byte[1024];
        for (int i = 0; i < fakeAudio.length; i++)
            fakeAudio[i] = (byte) (Math.sin(i / 10.0) * 127);

        System.out.println("Sending fake audio data to /audio-receive...");
        receiveSession.getRemote().sendBytes(ByteBuffer.wrap(fakeAudio));

        Thread.sleep(3000);

        receiveSession.close();
        streamSession.close();
        client.stop();

        System.out.println("Local WebSocket test complete.");
    }

    @WebSocket
    public static class AudioReceiveSocket {
        @OnWebSocketConnect
        public void onConnect(Session session) {
            System.out.println("Connected to /audio-receive");
        }

        @OnWebSocketMessage
        public void onBinary(Session session, byte[] payload, int offset, int length) {
            System.out.println("Received binary message from server: " + length + " bytes");
        }

        @OnWebSocketClose
        public void onClose(Session session, int statusCode, String reason) {
            System.out.println("Closed /audio-receive: " + reason);
        }
    }

    @WebSocket
    public static class AudioStreamSocket {
        @OnWebSocketConnect
        public void onConnect(Session session) {
            System.out.println("Connected to /audio-stream");
        }

        @OnWebSocketMessage
        public void onBinary(Session session, byte[] payload, int offset, int length) {
            System.out.println("Received audio stream: " + length + " bytes");
        }

        @OnWebSocketMessage
        public void onText(Session session, String message) {
            System.out.println("Received text from /audio-stream: " + message);
        }

        @OnWebSocketClose
        public void onClose(Session session, int statusCode, String reason) {
            System.out.println("Closed /audio-stream: " + reason);
        }
    }
}