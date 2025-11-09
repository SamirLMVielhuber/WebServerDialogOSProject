package com.github.dialogos.plugin.remote.web;

public class GraphControlRegistry {
    private static GraphControlListener listener;

    public static void setListener(GraphControlListener listen) {
        listener = listen;
    }

    public static GraphControlListener getListener() {
        return listener;
    }
}