package com.github.dialogos.plugin.remote.web.Input;

import com.clt.dialogos.plugin.AudioPlugin;
import com.clt.dialogos.plugin.PluginSettings;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;

public class WebSocketAudioInputPlugin implements AudioPlugin{

    private WebSocketInputStream inputStream;

    @Deprecated
    private AudioReceiver receiver;
    @Deprecated
    public interface AudioReceiver{
        void onAudioReceived(byte[] data);
    }

    @Deprecated
    public void establishConnection(AudioReceiver callback){
        this.receiver = callback;
        this.inputStream = new WebSocketInputStream();
        this.inputStream.startRecording();
        System.out.println("WebSocketAudioInputPlugin: Receiver established.");
        System.out.flush();
    }

    @Override
    public boolean isAudioInputPlugin(){
        return true;
    }

    @Override
    public boolean isAudioOutputPlugin(){
        return false;
    }

    @Override
    public String getId(){ 
        return "dialogos.plugin.webSocketAudioInput"; 
}

    @Override
    public String getName(){ 
        return "WebSocket Audio Input"; 
}

    @Override
    public String getVersion(){ 
        return "1.0"; 
}

    @Override
    public Icon getIcon(){ 
        return null; 
    }

    @Override
    public void initialize(){
        inputStream = new WebSocketInputStream();

    }

    @Override
    public PluginSettings createDefaultSettings(){
        return new WebSocketInputSettings();
    }

    @Override
    public boolean isRecording(){
        return inputStream != null && inputStream.isRecording();
    }

    @Override
    public InputStream setupAndGetAudioInput() throws IOException{
        if(inputStream == null)
            inputStream = new WebSocketInputStream();
        return inputStream;
    }

    @Override
    public void startRecording(){
        if(inputStream != null) 
            inputStream.startRecording();
    }

    @Override
    public void stopRecording(){
        if(inputStream != null) 
            inputStream.stopRecording();
    }

    public void receiveAudio(byte[] data){
        System.out.println("Received Audio");
        if(inputStream != null){
            inputStream.addAudioData(data);
        }
        /*@Deprecated
        if(receiver != null){
            receiver.onAudioReceived(data);
        }*/
    }
}
