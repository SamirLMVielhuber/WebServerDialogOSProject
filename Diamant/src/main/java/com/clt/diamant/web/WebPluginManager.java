package com.clt.diamant.web;

import javax.swing.JComponent;

import com.clt.dialogos.plugin.AudioPlugin;
import com.clt.dialogos.plugin.PluginManager;

//Basically should render the prev version of the PluginManager useless by just not using it...
public class WebPluginManager extends PluginManager {

    private AudioPlugin activeAudioInputPlugin;
    private AudioPlugin activeAudioOutputPlugin;

    public WebPluginManager() {
        super();
        activeAudioInputPlugin = null;
        activeAudioOutputPlugin = null;
    }

    @Override
    public AudioPlugin getActiveAudioInputPlugin() {
        return activeAudioInputPlugin;
    }

    @Override
    public AudioPlugin getActiveAudioOutputPlugin() {
        return activeAudioOutputPlugin;
    }

    @Override
    public void setActiveAudioInputPlugin(AudioPlugin plugin) {
        plugin.createDefaultSettings();
        plugin.initialize();
        this.activeAudioInputPlugin = plugin;
    }

    @Override
    public void setActiveAudioOutputPlugin(AudioPlugin plugin) {
        plugin.createDefaultSettings();
        plugin.initialize();
        this.activeAudioOutputPlugin = plugin;
    }

    @Override
    public JComponent createEditor() {
        return null;
    }
}