package com.github.dialogos.plugin.remote.web.Input;

import com.clt.dialogos.plugin.PluginRuntime;
import com.clt.dialogos.plugin.PluginSettings;
import com.clt.diamant.IdMap;
import com.clt.diamant.graph.Graph;
import com.clt.properties.DefaultIntegerProperty;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import com.github.dialogos.plugin.remote.web.Input.WebSocketAudioInputPlugin.ServerControl;

import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;

public class WebSocketInputSettings extends PluginSettings {
    private static final String INPUT_PORT = "INPUT_PORT";
    DefaultIntegerProperty port;
    private ServerControl serverControl;

    public WebSocketInputSettings(ServerControl control) {
        this.port = new DefaultIntegerProperty("Port" , "Port", null, 8080);
        this.serverControl = control;
    }

    @Override
    public void writeAttributes(XMLWriter out, IdMap uidMap) {
        //TODO if this is 0, What to do?
        if (this.port.getValue() != 0){
            Graph.printAtt(out, INPUT_PORT, this.port.getValue());
            System.out.println("Wrote Input Port to XML-File");
        }
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {
        //TODO if this can not be Read, What todo?
        if(name.equals(INPUT_PORT)){
            this.port.setValueFromString(value);
            System.out.println("Read Input Port from XML-File " + this.port.getValue());
        }
    }

    @Override
    public JComponent createEditor() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 3, 2, 3);

        port.addToPanel(p, gbc, false);

        gbc.gridy++;
        JButton startButton = new JButton("Start Server");
        JButton stopButton = new JButton("Stop Server");

        startButton.addActionListener(e -> {
            if (serverControl != null) 
                serverControl.startServer();
        });
        stopButton.addActionListener(e -> {
            if (serverControl != null) 
                serverControl.stopServer();
        });

        p.add(startButton, gbc);
        gbc.gridy++;
        p.add(stopButton, gbc);

        // make editor components stick to top of window
        JPanel superpanel = new JPanel(new BorderLayout());
        superpanel.add(p, BorderLayout.NORTH);

        return superpanel;
    }

    public DefaultIntegerProperty getPort(){
        return this.port;
    }

    @Override
    protected PluginRuntime createRuntime(Component parent) throws Exception {
        return null;
    }
}