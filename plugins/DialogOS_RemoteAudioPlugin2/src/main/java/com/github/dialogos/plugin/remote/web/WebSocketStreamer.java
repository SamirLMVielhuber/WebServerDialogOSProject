package com.github.dialogos.plugin.remote.web;

import com.clt.audio.AudioResampler;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;

public class WebSocketStreamer extends Thread {
    private final AudioInputStream audioStream;
    private final WebSocketAudioOutputPlugin.AudioCallback callback;
    private final String userId;
    private boolean running = true;

    public WebSocketStreamer(String userId, AudioInputStream stream, WebSocketAudioOutputPlugin.AudioCallback callback) {
        assert callback != null;
        
        this.audioStream = stream;
        this.callback = callback;
        this.userId = userId;
    }

    @Override
    public void run() {
        try {
            AudioFormat targetFormat = new AudioFormat(
                16000, 16, 1, true, false
            );
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);

            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = convertedStream.read(buffer)) != -1 && this.running) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                
                //Sends to whatever you gave it
                this.callback.onAudioChunk(this.userId, chunk);
                
                //This makes it work but is gernerally stupid idk search for a better approach
                Thread.sleep(50);
            }
            //Ensure maybe after this.running is set to false it still sends everything that was buffered inside the buffer function...
            byte[] chunk = new byte[bytesRead];
            System.arraycopy(buffer, 0, chunk, 0, bytesRead);
            
            this.callback.onAudioChunk(this.userId, chunk);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopStreaming() {
        this.running = false;
        interrupt();
    }
}
