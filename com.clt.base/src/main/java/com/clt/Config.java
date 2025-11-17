package com.clt;

import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Properties props = new Properties();

    static {
        try (InputStream is = Config.class.getResourceAsStream("/com/server.properties")) {
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load server.properties", e);
        }
    }

    public static String IP() {
        System.out.println("Config: Getting server IP: " + props.getProperty("server.ip"));
        System.out.flush();
        return props.getProperty("server.ip");
    }

    public static String PATH() {
        System.out.println("Config: Getting keystore path: " + props.getProperty("server.keystore.path"));
        System.out.flush();
        return props.getProperty("server.keystore.path");
    }

    public static String PASS() {
        System.out.println("Config: Getting keystore password: " + props.getProperty("server.keystore.password"));
        System.out.flush();
        return props.getProperty("server.keystore.password");
    }
}
