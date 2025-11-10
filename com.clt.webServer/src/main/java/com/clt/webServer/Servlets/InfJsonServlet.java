package com.clt.webServer.Servlets;

import java.io.IOException;

import java.io.InputStream;
import java.io.FileInputStream;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/inf.json")
public class InfJsonServlet extends HttpServlet {
    private static final String PATH = ".\\src\\main\\resources\\inf.json"; 

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("Current working dir: " + System.getProperty("user.dir"));
        System.out.flush();
        resp.setContentType("application/json");

        try (InputStream is = new FileInputStream(PATH)) {
            is.transferTo(resp.getOutputStream());
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"Could not read inf.json\"}");
            e.printStackTrace();
        }
    }
}