package com.clt.webServer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.clt.webServer.Servlets.DialogLoadServlet;
import com.clt.webServer.WebSockets.AudioWebSocketSender;
import com.clt.webServer.WebSockets.WebSocketAudioReceiver;

public class TestAudioServer {
    public static void main(String[] args) throws Exception {
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

        JettyWebSocketServletContainerInitializer.configure(servletContext, (context, container) -> {
            container.addMapping("/audio-stream", AudioWebSocketSender.class);
            container.addMapping("/audio-receive", WebSocketAudioReceiver.class);
        });

        HandlerList handlers = new HandlerList();
        handlers.addHandler(staticContext);
        handlers.addHandler(servletContext);

        server.setHandler(handlers);

        server.start();
        System.out.println("Server running at http://localhost:" + port);
        System.out.println("WebSocket endpoints:");
        System.out.println("     ws://localhost:" + port + "/audio-stream");
        System.out.println("     ws://localhost:" + port + "/audio-receive");
        server.join();
    }
}