package com.github.dialogos.plugin.remote.web.Input;

import com.clt.dialogos.plugin.PluginRuntime;
import com.clt.dialogos.plugin.PluginSettings;
import com.clt.diamant.IdMap;
import com.clt.properties.DefaultIntegerProperty;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;

import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;

public class WebSocketInputSettings extends PluginSettings {
    DefaultIntegerProperty port;

    public WebSocketInputSettings() {
        this.port = new DefaultIntegerProperty("Port" , "Port", null, 8080);
    }

    @Override
    public void writeAttributes(XMLWriter out, IdMap uidMap) {
        //TODO
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {
        //TODO
    }

    @Override
    public JComponent createEditor() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 3, 2, 3);

        port.addToPanel(p, gbc, false);

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

