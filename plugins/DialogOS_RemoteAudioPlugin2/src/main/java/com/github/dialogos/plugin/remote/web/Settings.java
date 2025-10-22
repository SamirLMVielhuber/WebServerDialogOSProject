package com.github.dialogos.plugin.remote.web;

import com.clt.dialogos.plugin.PluginRuntime;
import com.clt.dialogos.plugin.PluginSettings;
import com.clt.diamant.IdMap;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;

public class Settings extends PluginSettings {

    public Settings() {
    }

    @Override
    public void writeAttributes(XMLWriter out, IdMap uidMap) {

    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) throws SAXException {

    }

    @Override
    public JComponent createEditor() {
        return null;
    }

    @Override
    protected PluginRuntime createRuntime(Component parent) throws Exception {
        return null;
    }
}
