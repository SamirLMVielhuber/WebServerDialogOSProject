package com.github.dialogos.plugin.remote.web;

public interface AudioInputReceiver {
    void onAudioReceived(String userId, byte[] audioData);
}

