package com.github.dialogos.plugin.remote.web;

import com.clt.dialogos.plugin.AudioPlugin;
import com.clt.dialogos.plugin.PluginSettings;

import javax.sound.sampled.AudioInputStream;
import javax.swing.*;
import java.io.IOException;

public class WebSocketAudioOutputPlugin implements AudioPlugin {
    private String userId;
    private AudioCallback callback;
    private Settings settings;
    private WebSocketStreamer currentStreamer = null;

    public interface AudioCallback {
        void onAudioChunk(String userId, byte[] data);
    }

    public WebSocketAudioOutputPlugin(){
        this.callback = null;
        this.userId = "Unknown";
    }

    public WebSocketAudioOutputPlugin(String userId, AudioCallback  callback) {
        this.userId = userId;
        this.callback = callback;
    }

    public void establishConnection(String userId, AudioCallback callback){
        this.userId = userId;
        this.callback = callback;
    }
    @Override
    public boolean isAudioInputPlugin() { return false; }

    @Override
    public boolean isAudioOutputPlugin() { return true; }

    @Override
    public void playAudio(AudioInputStream audioInputStream) {
        try {
            this.currentStreamer = new WebSocketStreamer(this.userId, audioInputStream, this.callback);
            this.currentStreamer.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void stopAudio() {
        if(this.currentStreamer != null)
            this.currentStreamer.stopStreaming();
    }

    @Override
    public void joinAudioOutputThread() throws InterruptedException {}

    @Override
    public String getId() { 
        return "dialogos.plugin.webSocketAudioOutput"; 
    }

    @Override
    public String getName() { 
        return "WebSocket Audio Output"; 
    }

    @Override
    public Icon getIcon() { 
        return null; 
    }

    @Override
    public String getVersion() { 
        return "1"; 
    }

    @Override
    public void initialize() {}

    @Override
    public PluginSettings createDefaultSettings() { 
        this.settings = new Settings();
        return this.settings; 
    }
}
