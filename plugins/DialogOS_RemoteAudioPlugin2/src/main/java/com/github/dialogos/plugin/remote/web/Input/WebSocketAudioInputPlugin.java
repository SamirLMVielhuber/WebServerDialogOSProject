package com.github.dialogos.plugin.remote.web.Input;

import com.github.dialogos.plugin.remote.web.Manager;
import com.github.dialogos.plugin.remote.web.WebSocketHub;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
    Hybrid WebSocket Audio Input Plugin
    Works with either an injected Jetty WebSocket Session (multi-user mode)
        or starts its own listener on the configured port (standalone GUI mode).
*/
public class WebSocketAudioInputPlugin implements com.clt.dialogos.plugin.AudioPlugin {
    private WebSocketInputSettings settings;
    private WebSocketInputStream inputStream;
    private String userId;
    private WebSocketHub hub;

    public interface ServerControl {
        void startServer();
        void stopServer();
    }

    @Override
    public boolean isAudioInputPlugin() {
        return true;
    }

    @Override
    public boolean isAudioOutputPlugin() {
        return false;
    }

    @Override
    public String getId() {
        return "dialogos.plugin.webSocketAudioInput";
    }

    @Override
    public String getName() {
        return "WebSocket Audio Input";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public void initialize() {
        System.out.println("Creating new WebSocketInputStream");
        System.out.flush();
        this.inputStream = new WebSocketInputStream();
    }

    @Override
    public WebSocketInputSettings createDefaultSettings() {
        this.settings = new WebSocketInputSettings(
            new ServerControl() {
                @Override
                public void startServer() {
                    ensureHubStarted();
                    try {
                        hub.start();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        System.out.println("WebSocketInputSettings: Error starting Server: " + e.getMessage());
                        e.printStackTrace();
                        System.out.flush();
                    }
                }

                @Override
                public void stopServer() {
                    if(hub != null){
                        try {
                            hub.stop();
                            hub = null;
                            System.out.println("Settings stopped hub");
                        } catch (Exception e) {
                            System.out.println("Settings could not stop hub");
                            System.out.println(e.getMessage());
                        }
                    }
                    System.out.flush();
                }
            });
        return this.settings;
    }

    public WebSocketInputSettings getSettings() {
        return this.settings;
    }

    @Override
    public boolean isRecording() {
        return this.inputStream != null && this.inputStream.isRecording();
    }

    public void setPort(int port){
        System.out.println("WebSocketAudioInputPlugin: Setting Port to " + port);
        System.out.flush();
        this.settings.port.setValue(port);
    }
    public void setUserId(String userId){
        this.userId = userId;
    }
    public int getPort(){
        return this.settings.port.getValue();
    }

    @Override
    public InputStream setupAndGetAudioInput() throws IOException {
        if (this.inputStream == null)
            this.inputStream = new WebSocketInputStream();
        System.out.println("Sets and Gets AudioInput from " + this.userId +"\nthat is " + this.inputStream);
        System.out.flush();
        return this.inputStream;
    }

    @Override
    public void startRecording() {
        if (this.inputStream != null)
            this.inputStream.startRecording();
    }

    @Override
    public void stopRecording() {
        if (this.inputStream != null)
            this.inputStream.stopRecording();
    }

    /**
        This is called by WebSocketAudioReceiver on incoming binary data
    */
    public void receiveAudio(byte[] data) {
        //System.out.println("WebSocketAudioInputPlugin: Received Audio for User: " + this.userId);
        //System.out.flush();
        if (this.inputStream != null){
            this.inputStream.addAudioData(data);
            //System.out.println("     Added Audio to " + this.inputStream);
            //System.out.flush();
        }
    }

    public void attachHub(String userId) {
        int port = settings != null ? settings.getPort().getValue() : 9000;
        hub = Manager.getOrCreateHub(userId, port);
        hub.setAudioInputCallback(this::receiveAudio);
        System.out.println("WebSocketAudioInputPlugin: Attached Hub " + hub + " for: " + userId);
        System.out.flush();
    }

    //In standalone GUI mode, this ensures the hub starts even if output plugin hasn’t run yet
    public void ensureHubStarted() {
        attachHub("default");
    }

    private class WebSocketInputStream extends InputStream{
        private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
        private byte[] currentBuffer = null;
        private int currentBufferPosition = 0;
        private volatile boolean recording = false;

        public void startRecording(){
            this.recording = true;
        }
        public boolean isRecording(){
            return this.recording;
        }
        public void stopRecording(){
            this.recording = false;
            synchronized(audioQueue){
                audioQueue.offer(new byte[0]);
            }
        }
        public void addAudioData(byte[] audioData){
            //System.out.println(this + ": Put Audio into Queue");
            //System.out.flush();
            if(this.recording && audioData != null && audioData.length > 0)
                this.audioQueue.offer(audioData);
        }

        @Override
        public int read() throws IOException{
            //System.out.println("WebSocketInputStream: Reads from Buffer");
            //System.out.flush();
            if(this.currentBuffer == null || this.currentBufferPosition >= currentBuffer.length){
                try{
                    this.currentBuffer = audioQueue.take();
                    this.currentBufferPosition = 0;
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
                if(currentBuffer.length == 0)
                    return -1;
            }
            return currentBuffer[currentBufferPosition++] & 0xFF;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException{
            //System.out.println(this + ": Reads " + len + " Bytes from buffer");
            //System.out.flush();
            int bytesRead = 0;
            while(bytesRead < len){
                if(this.currentBuffer == null || this.currentBufferPosition >= this.currentBuffer.length){
                    try{
                        this.currentBuffer = audioQueue.take();
                        this.currentBufferPosition = 0;
                    }catch(InterruptedException e){
                        Thread.currentThread().interrupt();
                        throw new IOException(e);
                    }
                    if(currentBuffer.length == 0) 
                        return bytesRead == 0 ? -1 : bytesRead;
                }
                int bytesToCopy = Math.min(len - bytesRead, currentBuffer.length - currentBufferPosition);
                System.arraycopy(currentBuffer, currentBufferPosition, b, off + bytesRead, bytesToCopy);
                currentBufferPosition += bytesToCopy;
                bytesRead += bytesToCopy;
            }
            return bytesRead;
        }
    }
}
