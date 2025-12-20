package com.github.dialogos.plugin.remote.web.Output;

import org.eclipse.jetty.websocket.api.Session;

import com.github.dialogos.plugin.remote.web.Manager;
import com.github.dialogos.plugin.remote.web.WebSocketHub;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;

/**
    Hybrid WebSocket Audio Output Plugin.
    Sends audio to an attached WebSocket Session or to clients
        connected to a local WebSocket server (configured port) 
*/
public class WebSocketAudioOutputPlugin implements com.clt.dialogos.plugin.AudioPlugin {
    private WebSocketOutputSettings settings;
    private String userId;
    private WebSocketHub hub;
    private WebSocketStreamer currentStreamer;

    public interface ServerControl {
        void startServer();
        void stopServer();
    }

    public interface AudioCallback {
        void onAudioChunk(String userId, byte[] data);
    }

    @Override
    public boolean isAudioInputPlugin() {
        return false;
    }

    @Override
    public boolean isAudioOutputPlugin() {
        return true;
    }

    @Override
    public String getId() {
        return "dialogos.plugin.webSocketAudioOutput";
    }

    @Override
    public String getName() {
        return "WebSocket Audio Output";
    }

    @Override
    public String getVersion() {
        return "1.2";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public void initialize() {}

    @Override
    public WebSocketOutputSettings createDefaultSettings() {
        this.settings = new WebSocketOutputSettings(
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

    public WebSocketOutputSettings getSettings() {
        return this.settings;
    }

    public void setPort(int port){
        System.out.println("WebSocketAudioOutputPlugin " + this + ": Setting Port to " + port);
        System.out.flush();
        this.settings.port.setValue(port);
    }

    public void setUserId(String userId){
        this.userId = userId;
    }

    public int getPort(){
        return this.settings.port.getValue();
    }

    /* 
        Called in DocumentManager to attach the Hub to the User immediatly
    */
    public void attachHub(String userId) {
        int port = (settings != null) ? settings.getPort().getValue() : 8080;
        this.userId = userId;
        this.hub = Manager.getOrCreateHub(userId, port);
        System.out.println("WebSocketAudioOutputPlugin " + this + ": Attached hub " + hub + " for userId " + this.userId);
        System.out.flush();
    }

    @Override
    public void playAudio(AudioInputStream audioInputStream) {
        System.out.println("WebSocketAudioOutputPlugin " + this + ": Starting playback thread...");
        System.out.flush();

        if (this.userId == null) {
            System.err.println("WebSocketAudioOutputPlugin: ERROR — userId is null! Cannot send audio.");
            System.out.flush();
            return;
        }

        if (this.hub == null) {
            System.out.println("WebSocketAudioOutputPlugin: Hub not found, attaching default.");
            System.out.flush();
            ensureHubStarted();
        }

        if (this.currentStreamer != null)
            this.currentStreamer.stopStreaming();

        this.currentStreamer = new WebSocketStreamer(this.userId, audioInputStream, 
        new AudioCallback(){
            @Override
            public void onAudioChunk(String userId, byte[] data) {
                WebSocketAudioOutputPlugin.this.sendAudio(userId, data);
            }    
        });
        this.currentStreamer.start();
    }

    public void sendAudio(String userId, byte[] audio) {
        if (this.hub == null) {
            ensureHubStarted();
        }

        Session s = hub.getOutputSession(userId);
        try{
            if(s.isOpen()){
                s.getRemote().sendBytes(ByteBuffer.wrap(audio));
            }
            else{
                System.out.println("WebSocketAudioOutputPlugin: The session for " + userId + " was not open");
            }
        } catch (IOException e) {
            System.err.println("WebSocketAudioOutputPlugin: sendAudio failed: " + e.getMessage());
        }
    }

    @Override
    public void stopAudio() {
        if (this.currentStreamer != null) {
            this.currentStreamer.stopStreaming();
            this.currentStreamer = null;
        }
    }

    @Override
    public void joinAudioOutputThread() throws InterruptedException {
        if (this.currentStreamer != null) 
            this.currentStreamer.join();
    }

    //In standalone GUI mode, this ensures the hub starts even if output plugin hasn’t run yet
    private void ensureHubStarted() {
        System.out.println("WebSocketAudioOutputPlugin: Ensuring default hub is running");
        System.out.flush();
        attachHub("default");
    }

    private static class WebSocketStreamer extends Thread {
        private final AudioInputStream audioStream;
        private final AudioCallback callback;
        private final String userId;
        private boolean running = true;

        public WebSocketStreamer(String userId, AudioInputStream stream, AudioCallback callback) {
            this.audioStream = stream;
            this.callback = callback;
            this.userId = userId;
            System.out.println("Incoming AudioInputStream format: " + audioStream.getFormat());

        }

        @Override
        public void run() {
            //I need to know where to send to...
            assert this.userId != null;

            try {

                //AudioFormat targetFormat = new AudioFormat(16000, 16, 1, true, false);
                //AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, this.audioStream);
                AudioFormat format = new AudioFormat(51000, 16, 1, true, false);



                byte[] buffer = new byte[32768];
                int bytesRead;
                int frameCount = 0; //bytesRead

                int frameSize = format.getFrameSize();
                int sampleRate = (int) format.getSampleRate();

                AudioFormat format1 = audioStream.getFormat();

                System.out.println("Starting audio streaming. Format: " + format1);


                while (this.running && (bytesRead = audioStream.read(buffer)) != -1) {
                    frameCount++;
                    if (frameCount % 100 == 0) {
                        System.out.println("Frame " + frameCount + " | Bytes read: " + bytesRead);
                    }

                    //byte[] chunk = new byte[bytesRead]; //copy as new chunk
                    byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                    //System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                    this.callback.onAudioChunk(this.userId, chunk);
                    AudioFormat fmt = audioStream.getFormat();
                    System.out.println(
                            "Sending chunk: bytes=" + bytesRead +
                                    ", frames=" + (bytesRead / fmt.getFrameSize()) +
                                    ", sampleRate=" + fmt.getSampleRate()
                    );





                    int ms = (int) (((double) bytesRead / frameSize) * 1000.0 / sampleRate);


                    Thread.sleep(ms);


                }


            } catch (Exception e) {
                System.out.println("WebSocketStreamer: Exception while streaming audio for user " + this.userId);
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.out.flush();
            }
        }

        public void stopStreaming() {
            this.running = false;
            interrupt();
        }
    }
}


