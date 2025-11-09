package com.clt.webServer.Servlets;

import java.util.*;

import com.clt.webServer.ConfigReader;
import com.clt.webServer.ConnectionManager;
import com.google.gson.Gson;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet("/load")
public class DialogLoadServlet extends HttpServlet {

    private ConfigReader configReader;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String configPath = "/inf.json";
            configReader = new ConfigReader(getClass().getResourceAsStream(configPath));
            System.out.println("Loaded config: " + configReader.getDocuments());
        } catch (Exception e) {
            throw new ServletException("Could not load config file", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = req.getParameter("userId");
        String graphName = req.getParameter("graphName");

        if (userId == null || graphName == null) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Missing userId or graphName\"}");
            return;
        }

        try {
            String path = configReader.getDocumentPath(graphName);
            if (path == null)
                throw new IllegalArgumentException("No document found for graphName: " + graphName);

            ConnectionManager.getInstance().openConnection(userId, path);

            //ConnectionManager.getGraphManager(userId).printGraph();

            int inputPort = ConnectionManager.getGraphManager(userId).getInputPort();
            int outputPort = ConnectionManager.getGraphManager(userId).getOutputPort();

            System.out.println("Sending ports to Website: " + inputPort + "\n" + outputPort);
            System.out.flush();
            resp.setContentType("application/json");
            resp.getWriter().write(new Gson().toJson(
                Map.of("status", "ok", "userId", userId, "graphName", graphName, "inputPort", inputPort,
                "outputPort", outputPort)
            ));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }
}