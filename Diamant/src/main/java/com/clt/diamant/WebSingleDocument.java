package com.clt.diamant;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.clt.dialogos.plugin.*;
import com.clt.script.DefaultEnvironment;
import com.clt.script.exp.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.clt.dialog.client.ConnectionChooser;
import com.clt.dialog.client.Connector;
import com.clt.dialog.client.ServerDevice;
import com.clt.diamant.graph.Graph;
import com.clt.diamant.graph.GraphOwner;
import com.clt.diamant.graph.Node;
import com.clt.diamant.graph.Procedure;
import com.clt.diamant.graph.nodes.NodeExecutionException;
import com.clt.diamant.graph.nodes.ProcNode;
import com.clt.diamant.graph.search.NodeSearchFilter;
import com.clt.diamant.graph.search.SearchResult;
import com.clt.diamant.suspend.DialogSuspendedException;
import com.clt.event.ProgressListener;
import com.clt.gui.ProgressDialog;
import com.clt.properties.Property;
import com.clt.script.Environment;
import com.clt.script.exp.values.StringValue;
import com.clt.util.DefaultLongAction;
import com.clt.util.LongAction;
import com.clt.util.TemplateBundle;
import com.clt.util.UserCanceledException;
import com.clt.xml.AbstractHandler;
import com.clt.xml.Base64;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import java.io.InputStream;
import java.util.stream.Collectors;

//Basically a Copy of SingleDocument just another PluginManager... that basically ignores everything the pluginManager should do....
public class WebSingleDocument extends Document implements GraphOwner {

    private final class DeviceXMLHandler extends AbstractHandler {

        private final Graph graph;
        private final IdMap uid_map;
        private final XMLReader r;

        private DeviceXMLHandler(String elementName, Graph graph, IdMap uid_map, XMLReader r) {

            super(elementName);
            this.graph = graph;
            this.uid_map = uid_map;
            this.r = r;
        }

        public void start(String name, Attributes atts) throws SAXException {

            if (name.equals("att")) {
                // ignore
            } else if (name.equals("device")) {
                final String id = atts.getValue("id");

                final Device d = new Device(id, "");
                this.uid_map.devices.put(d);
                this.r.setHandler(new AbstractHandler("device") {

                    int iconWidth = 0;

                    protected void start(String name, Attributes atts)
                            throws SAXException {

                        if (name.equals("connector")) {
                            try {
                                String cid = atts.getValue("class");
                                if (cid == null) {
                                    cid = atts.getValue("name");
                                }
                                final Connector connector = (Connector) Class.forName(cid)
                                        .newInstance();
                                d.setConnector(connector);
                                DeviceXMLHandler.this.r.setHandler(new AbstractHandler(
                                        "connector") {

                                    String pname;

                                    private void setConnectorProperty(String name, String value) {

                                        if (name != null) {
                                            Property<?> properties[] = connector.getProperties();
                                            for (Property<?> p : properties) {
                                                if (name.equals(p.getID())) {
                                                    try {
                                                        p.setValueFromString(value);
                                                    } catch (ParseException e) {
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    protected void start(String name, Attributes atts) {

                                        if (name.equals("att")) {
                                            String pname = atts.getValue("name");
                                            String value = atts.getValue("value");
                                            if (value != null) {
                                                this.setConnectorProperty(pname, value);
                                            } else {
                                                this.pname = pname;
                                            }
                                        }
                                    }

                                    protected void end(String name) {

                                        if (name.equals("att") && (this.pname != null)) {
                                            this.setConnectorProperty(this.pname, this.getValue());
                                        }
                                    }
                                });
                            } catch (Exception exn) {
                                DeviceXMLHandler.this.r
                                        .raiseException("Illegal type of connector: "
                                                + atts.getValue("name"));
                            }
                        } else if (name.equals("icon")) {
                            try {
                                this.iconWidth = Integer.parseInt(atts.getValue("width"));
                            } catch (Exception ignore) {
                            }
                        }
                    }

                    protected void end(String name) throws SAXException {

                        if (name.equals("name")) {
                            d.setName(this.getValue());
                        } else if (name.equals("icon")) {
                            byte[] cdata = Base64.decode(this.getValue());
                            if (cdata.length % 4 != 0) {
                                DeviceXMLHandler.this.r.raiseException("Illegal icon data for device " + id);
                            }
                            int data[] = WebSingleDocument.createIntArray(cdata);
                            if (this.iconWidth == 0) {
                                this.iconWidth = (int) Math.sqrt(data.length);
                            }
                            d.setIconData(new Device.Icon(data, this.iconWidth));
                        } else if (name.equals("type")) {
                            /* deprecated */
                        } else if (name.equals("port")) {
                            /* deprecated */
 /*
               * try { d.setPort(Integer.parseInt(getValue())); } catch
               * (NumberFormatException exn) {
               * r.raiseAttributeValueException(name); }
                             */
                        }
                    }
                });

                WebSingleDocument.this.getDevices().add(d);
            } else if (name.equals("plugin")) {
                String id = atts.getValue("type");
                Map<String,Class> plugins = PluginLoader.getPlugins().stream().
                        collect(Collectors.toMap(Plugin::getId, Object::getClass));
                if (plugins.containsKey(id)) {
                    try {
                        PluginSettings settings = WebSingleDocument.this.getPluginSettings(plugins.get(id));
                        settings.read(this.r, this.uid_map);
                    } catch (Exception exn) {
                        this.r.raiseException(exn);
                    }
                } else {
                    this.r.raiseException(Resources.format("PluginXNotFound", id));
                }
            } else if (name.equals("grammar")) {
                final Grammar g = new Grammar("grammar");
                g.setId(atts.getValue("id"));
                this.uid_map.grammars.put(g);
                this.graph.getGrammars().add(g);
                this.r.setHandler(new AbstractHandler("grammar") {

                    protected void end(String name) {

                        if (name.equals("file")) {
                            g.setName(this.getValue());
                            try {
                                File grammarFile = new File(this.getValue());
                                StringBuilder s = new StringBuilder((int) grammarFile.length());
                                Reader in = new BufferedReader(new FileReader(grammarFile));
                                char c;
                                try {
                                    while ((c = (char) in.read()) != -1) {
                                        s.append(c);
                                    }
                                } finally {
                                    in.close();
                                }
                                g.setGrammar(s.toString());
                            } catch (Exception exn) {
                                // dabo: die Warnings muessen anders
                                // hochgereicht werden
                                /*
                 * OptionPane.warning(WebSingleDocument.this, new String[] {
                 * "Could not load grammar " + g.getName() + ".",
                 * exn.getLocalizedMessage() });
                                 */
                            }
                        } else if (name.equals("name")) {
                            g.setName(this.getValue());
                        } else if (name.equals("value")) {
                            g.setGrammar(this.getValue());
                        }
                    }
                });
            } else if (name.equals("graph")) {
                this.graph.read(this.r, null, this.uid_map);
            } else if (name.equals("pluginManager")) {
                // Read the saved plugin ids for audioIn and audioOut, compare them to other audio plugins
                // and set those that match as active audio-IO plugins
                this.r.setHandler(new AbstractHandler("pluginManager") {
                    @Override
                    protected void start(String name, Attributes atts) throws SAXException {
                        String id = atts.getValue("id");
                        for (AudioPlugin plugin: PluginLoader.getAudioPlugins()) {
                            if (plugin.getId().equals(id)) {
                                if (name.equals("audioIn")) {
                                    pluginManager.setActiveAudioInputPlugin(plugin);
                                } else if (name.equals("audioOut")) {
                                    pluginManager.setActiveAudioOutputPlugin(plugin);
                                }
                            }
                        }

                    }
                });
            } else {
                this.r.raiseUnexpectedElementException(name);
            }
        }

        public void end(String name) {

            if (name.equalsIgnoreCase(WebSingleDocument.this.getDocumentType())) {
                this.r.addCompletionRoutine(new Runnable() {

                    public void run() {

                        DeviceXMLHandler.this.graph.updateEdges();
                        WebSingleDocument.this.setGraph(DeviceXMLHandler.this.graph);
                    }
                });
            }
        }
    }

    private Graph graph = null;

    private Map<Class<? extends Plugin>, PluginSettings> pluginSettings;
    private Collection<Device> devices;
    private WebPluginManager pluginManager;

    private Map<String, TemplateBundle> localizationBundles;

    private DefaultEnvironment environment;

    /**
     * Constructs a new single document with one start node.
     */
    public WebSingleDocument() {
        System.out.println("WebSingeDocument is created");
        System.out.flush();
        this.devices = new ArrayList<Device>();
        this.pluginSettings = new HashMap<Class<? extends Plugin>, PluginSettings>();
        this.pluginManager = new WebPluginManager();
        this.localizationBundles = new HashMap<String, TemplateBundle>();

        environment = new DeviceAwareEnvironment(devices);
        environment.registerFunction("DialogOS", new ExecutableFunctionDescriptor("getModelName", Type.String, new Type[] {}) {
            @Override
            public Value eval(Value[] args) {
                return new StringValue(WebSingleDocument.this.getTitle());
            }
        });
        environment.registerFunction("DialogOS", new ExecutableFunctionDescriptor("getModelPath", Type.String, new Type[] {}) {
            @Override
            public Value eval(Value[] args) {
                File f = WebSingleDocument.this.getFile();
                if (f == null) {
                    throw new EvaluationException("Error in getModelPath(): Model "
                            + WebSingleDocument.this.getTitle() + " hasn't been saved yet.");
                } else {
                    if (f.getParent() == null) {
                        return new StringValue(System.getProperty("user.dir"));
                    } else {
                        return new StringValue(f.getParent());
                    }
                }
            }
        });
        for (Plugin plugin : PluginLoader.getPlugins()) {
            this.pluginSettings.put(plugin.getClass(), plugin.createDefaultSettings());
            for (ExecutableFunctionDescriptor efd : plugin.registerScriptFunctions()) {
                environment.registerFunction(plugin.getName(), efd);
            }
        }

        Graph g = new Graph(this);
        this.setGraph(g);
    }

    void setGraph(Graph graph) {
        if (this.graph != graph) {
            Graph oldGraph = this.graph;
            this.graph = graph;

            this.firePropertyChange("graph", oldGraph, graph);
        }
    }

    public String getDocumentType() {
        return "Web";
    }

    /**
     * Establish socket connections to client modules.
     *
     * @param timeout Connection timeout in milliseconds.
     */
    public boolean connectDevices(ConnectionChooser chooser, int timeout) {

        Collection<Device> devices = this.getDevices();
        if (devices.size() > 0) {
            boolean ok = ServerDevice.connect(devices.toArray(new Device[devices.size()]),
                    timeout,
                    Resources.getString("ConnectingDevices"),
                    chooser);

            if (!ok) {
                this.closeDevices();
            }

            return ok;
        } else {
            return true;
        }
    }

    public void closeDevices() {
        for (Device d : this.getDevices()) {
            try {
                d.close();
            } catch (Exception ignore) {
            }
        }
    }

    public ExecutionResult run(Component parent, final WozInterface transition) throws Exception {
        Object message = null;
        Node node = null;
        int type = ExecutionResult.INFORMATION;

        final ProgressDialog d;
        if (parent != null) {
            d = new ProgressDialog(parent, 0);
        } else {
            d = null;
        }

        LongAction init = new DefaultLongAction(Resources.getString("InitializingModel")) {
            @Override
            protected void run(ProgressListener l) throws Exception {
                for (Plugin plugin : PluginLoader.getPlugins()) {
                    Class<? extends Plugin> c = plugin.getClass();
                    PluginSettings plSettings = WebSingleDocument.this.getPluginSettings(c);
                    if (plSettings.isRelevantForNodes(graph.getNodes()))
                            plSettings.initializeRuntime(d, transition);
                }
            }
        };

        LongAction uninit = new DefaultLongAction(Resources.getString("UninitializingModel")) {
            @Override
            protected void run(ProgressListener l) throws Exception {
                for (Plugin plugin : PluginLoader.getPlugins()) {
                    Class<? extends Plugin> c = plugin.getClass();
                    WebSingleDocument.this.getPluginSettings(c).disposeRuntime(transition);
                }
            }
        };

        if (d != null) {
            d.run(init);
        } else {
            init.run();
        }

        try {
            if (transition.initInterface()) {
                this.execute(parent, transition);

                message = Resources.getString("ExecutionComplete");
                type = ExecutionResult.INFORMATION;
            } else {
                message = Resources.format("TransitionInitError", transition.getName());
                type = ExecutionResult.ERROR;
            }
        } catch (ExecutionStoppedException exn) {
            message = Resources.getString("ExecutionStopped");
            type = ExecutionResult.INFORMATION;
        } catch (DialogSuspendedException exn) {
            // pass DialogSuspendedExceptions through to caller
            throw exn;
        } catch (Throwable exn) {
            if (exn instanceof InvocationTargetException) {
                exn = ((InvocationTargetException) exn).getTargetException();
            }
            if (exn instanceof UserCanceledException) {
                message = Resources.getString("ExecutionStopped");
                type = ExecutionResult.INFORMATION;
            } else {
                message = new String[]{Resources.getString("ExecutionError"), exn.toString()};
                type = ExecutionResult.ERROR;
                if (exn instanceof NodeExecutionException) {
                    node = ((NodeExecutionException) exn).getNode();
                    if (((NodeExecutionException) exn).getException() instanceof ExecutionStoppedException) {
                        message = Resources.getString("ExecutionStopped");
                        type = ExecutionResult.INFORMATION;
                    } else {
                        exn.printStackTrace();
                    }
                } else {
                    exn.printStackTrace();
                }
            }
        } finally {
            if (d != null) {
                d.run(uninit);
            } else {
                uninit.run();
            }

            transition.disposeInterface(type == ExecutionResult.ERROR);
            this.closeDevices();
        }

        return new ExecutionResult(type, message, node);
    }

    public void execute(Component parent, final WozInterface transition) throws Exception {
        final InputCenter input = new InputCenter(this.getDevices());
        final ExecutionLogger logger = new ExecutionLogger(graph, getGraphName(), Preferences.getPrefs().loggingEnabled.getValue());

        try {
            transition.startDocument(WebSingleDocument.this, WebSingleDocument.this.getTitle(), input);

            try {
                transition.setState(WozInterface.State.INSTRUCT);
                transition.setState(WozInterface.State.RUN);

                for (Device device : this.getDevices()) {
                    try {
                        device.start();
                    } catch (Exception exn) {
                    }
                }

                try {
                    this.graph.execute(transition, input, logger);
                } catch (ExecutionStoppedException exn) {
                    transition.error("abort", "Execution stopped by user");
                    throw exn;
                } catch (NodeExecutionException exn) {
                    if (exn.getException() instanceof ExecutionStoppedException) {
                        transition.error("abort", "Execution stopped by user");
                    } else {
                        transition.error(exn.getClass().getName(), exn.getLocalizedMessage());
                    }
                    throw exn;
                } catch (DialogSuspendedException exn) {
                    // pass through
                    throw exn;
                } catch (Exception exn) {
                    // exn.printStackTrace();
                    transition.error(exn.getClass().getName(), exn.getLocalizedMessage());
                    if (exn instanceof RuntimeException) {
                        throw (RuntimeException) exn;
                    } else {
                        throw new RuntimeException(exn.getLocalizedMessage());
                    }
                }
            } finally {
                transition.setState(WozInterface.State.END);

                transition.endDocument(WebSingleDocument.this);
            }
        } finally {
            input.dispose();
            transition.setState(WozInterface.State.NORMAL);
            logger.saveDocAsXML();
        }
    }

    public void load(File f, XMLReader r) {
        this.load(f, r, new IdMap());
    }

    protected void load(final File f, final XMLReader r, final IdMap uid_map) {
        final Graph graph = new Graph(this);
        this.devices.clear();
        r.setHandler(new DeviceXMLHandler(this.getDocumentType().toLowerCase(), graph, uid_map, r));
        this.setFile(f);
    }

    /**
     * Loads a dialog model from an input stream. This is useful, for instance,
     * when the dialog model is packaged in the Jar, and should be read from a
     * resource and not a file (for headless use).
     * 
     * @param is
     * @return
     * @throws IOException 
     */
    public static WebSingleDocument loadFromStream(InputStream is) throws IOException {
        final XMLReader r = new XMLReader(Document.validateXML);
        WebSingleDocument ret = new WebSingleDocument();

        r.parse(is, new AbstractHandler() {
            @Override
            public void start(String name, Attributes atts) throws SAXException {
                if (name.equals("wizard")) {
                    ret.setSubordinateHandler(r);
                }
            }
        });

        return ret;
    }

    private void setSubordinateHandler(XMLReader r) {
        final Graph graph = new Graph(this);
        this.devices.clear();
        r.setHandler(new DeviceXMLHandler(this.getDocumentType().toLowerCase(), graph, new IdMap(), r));
    }

    @Override
    public void validate(Collection<SearchResult> errors, ProgressListener progress) {
        super.validate(errors, progress);
        this.graph.validate(errors, progress);
    }

    private void writeDocumentSettings(XMLWriter out, File file, IdMap uid_map) {

        for (Device d : this.devices) {
            out.openElement("device", new String[]{"id"},
                    new Object[]{uid_map.devices.put(d)});

            out.printElement("name", d.getName());
            // out.printElement("port", new Integer(d.getPort()));
            Connector connector = d.getConnector();
            if (connector != null) {
                out.openElement("connector", new String[]{"class"},
                        new Object[]{connector
                                    .getClass().getName()});
                Property<?>[] properties = connector.getProperties();
                for (int j = 0; j < properties.length; j++) {
                    String value = properties[j].getValueAsString();
                    if (value != null) {
                        Graph.printAtt(out, properties[j].getID(), value);
                    }
                }
                out.closeElement("connector");
            }

            Device.Icon iconData = d.getIconData();
            if (iconData != null) {
                out.printElement("icon", new String[]{"width"},
                        new String[]{String
                                    .valueOf(iconData.getWidth())}, Base64
                        .encodeBytes(WebSingleDocument.createByteArray(iconData.getData())));
            }

            out.closeElement("device");
        }

        // PluginManager settings saves plugin id of the used plugins in audioIn and audioOut elements
        try {
            out.openElement("pluginManager");
            if (pluginManager.getActiveAudioInputPlugin() != null) {
                out.printElement("audioIn", new String[]{"id"}, new String[]{pluginManager.getActiveAudioInputPlugin().getId()});
            }
            if (pluginManager.getActiveAudioOutputPlugin() != null) {
                out.printElement("audioOut", new String[]{"id"}, new String[]{pluginManager.getActiveAudioOutputPlugin().getId()});
            }
            out.closeElement("pluginManager");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        for (Plugin plugin : PluginLoader.getPlugins()) {
            if (getPluginSettings(plugin.getClass()).isRelevantForNodes(this.graph.getNodes())) {
                out.openElement("plugin", new String[]{"type"}, new String[]{plugin.getId()});
                this.getPluginSettings(plugin.getClass()).writeAttributes(out, uid_map);
                out.closeElement("plugin");
            }
        }
    }

    /**
     * Helper method to convert byte arrays to int arrays
     */
    private static int[] createIntArray(byte[] byteArray) {

        if (byteArray.length % 4 != 0) {
            throw new IllegalArgumentException();
        }
        int intArray[] = new int[byteArray.length / 4];
        for (int i = 0; i < intArray.length; i++) {
            int b1 = byteArray[4 * i];
            int b2 = byteArray[4 * i + 1];
            int b3 = byteArray[4 * i + 2];
            int b4 = byteArray[4 * i + 3];
            if (b1 < 0) {
                b1 += 256;
            }
            if (b2 < 0) {
                b2 += 256;
            }
            if (b3 < 0) {
                b3 += 256;
            }
            if (b4 < 0) {
                b4 += 256;
            }
            intArray[i] = (b1 << 24) | (b2 << 16) | (b3 << 8) | (b4 << 0);
        }
        return intArray;
    }

    /**
     * Helper method to convert int arrays to byte arrays
     */
    private static byte[] createByteArray(int[] intArray) {
        byte byteArray[] = new byte[intArray.length * 4];
        for (int i = 0; i < intArray.length; i++) {
            byteArray[4 * i] = (byte) ((intArray[i] >> 24) & 0x000000ff);
            byteArray[4 * i + 1] = (byte) ((intArray[i] >> 16) & 0x000000ff);
            byteArray[4 * i + 2] = (byte) ((intArray[i] >> 8) & 0x000000ff);
            byteArray[4 * i + 3] = (byte) ((intArray[i] >> 0) & 0x000000ff);
        }
        return byteArray;
    }

    @Override
    public void write(final XMLWriter out, final IdMap uid_map) {
        out.openElement(this.getDocumentType().toLowerCase());
        this.writeDocumentSettings(out, this.getFile(), uid_map);
        this.graph.save(out, uid_map);
        out.closeElement(this.getDocumentType().toLowerCase());
    }

    private void addGlobalProcs(Graph addToThisGraph, Graph procsFromThisGraph,
            Mapping map) {
        Map<ProcNode, Node> freeprocs = procsFromThisGraph
                .getFreeProcedures(new Hashtable<ProcNode, Node>());

        for (ProcNode p : freeprocs.keySet()) {
            if (map.getNode(p) == p) {
                ProcNode newProc = (ProcNode) p.clone(map);
                addToThisGraph.add(newProc);
                map.addNode(p, newProc);
                this.addGlobalProcs(addToThisGraph, p.getOwnedGraph(), map);
            }
        }
    }

    public void export(final Graph export_graph, File file)
            throws IOException {
        if (export_graph instanceof Procedure) {
            List<Slot> p = ((Procedure) export_graph).getParameters();
            if (p.size() > 0) {
                throw new IOException(Resources.getString("CannotExportProcedures"));
            }
        }

        Mapping map = new Mapping();
        final Graph g = export_graph.clone(map);
        this.addGlobalProcs(g, export_graph, map);

        // update references to global variables and procedures
        // It is important to do an update here in order to update local
        // variable references before determining free variables
        g.update(map);

        // get free variables from the cloned graph, because we must include
        // variables
        // used by the global procedures.
        Map<Slot, Node> freevars = g.getFreeVariables(new Hashtable<Slot, Node>());
        List<Slot> gvars = g.getVariables();
        if (freevars.size() > 0) {
            // new ListSelectionDialog(parent, "Free variables", "Cannot export
            // graph because of references to these free variables:",
            // Misc.hashKeys(freevars), 0, false);
            // return;
            for (Slot v : freevars.keySet()) {
                for (int i = 0; i < gvars.size(); i++) {
                    if (gvars.get(i).getName().equals(v.getName())) {
                        throw new IOException(
                                "Cannot export graph because it contains a references to the global variable \""
                                + v.getName()
                                + "\", and it already contains a local variable with the same name.");
                    }
                }
            }
            for (Slot v : freevars.keySet()) {
                Slot newVar = v.clone(map);
                gvars.add(newVar);
            }
        }

        // reupdate references to variables
        g.update(map);

        g.setOwner(new GraphOwner() {

            public Graph getSuperGraph() {
                return null;
            }

            public Graph getOwnedGraph() {
                return g;
            }

            public Collection<Device> getDevices() {
                return WebSingleDocument.this.getDevices();
            }

            public List<Grammar> getGrammars() {
                return WebSingleDocument.this.getGrammars();
            }

            public PluginSettings getPluginSettings(
                    Class<? extends Plugin> pluginClass) {
                return WebSingleDocument.this.getPluginSettings(pluginClass);
            }

            @Override
            public PluginManager getPluginManager() {
                return WebSingleDocument.this.getPluginManager();
            }

            public Environment getEnvironment(boolean local) {
                return WebSingleDocument.this.getEnvironment(local);
            }

            public void setDirty(boolean dirty) {
            }

            public void export(Graph g, File f)
                    throws IOException {
                WebSingleDocument.this.export(g, f);
            }

            public String getGraphName() {
                return "Dialogue";
            }

            public void setGraphName(String name) {
            }
        });

        XMLWriter out = new XMLWriter(file);
        this.writeHeader(out);

        out.openElement(this.getDocumentType().toLowerCase());

        IdMap uid_map = new IdMap(true);
        this.writeDocumentSettings(out, file, uid_map);

        g.save(out, uid_map);
        out.closeElement(this.getDocumentType().toLowerCase());
        out.close();
    }

    public Environment getEnvironment(boolean local) {
        return this.environment;
    }

    public Collection<Device> getDevices() {
        return this.devices;
    }

    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    public PluginSettings getPluginSettings(Class<? extends Plugin> pluginClass) {
        return this.pluginSettings.get(pluginClass);
    }

    public List<Grammar> getGrammars() {
        return this.getOwnedGraph().getGrammars();
    }

    public Graph getSuperGraph() {
        return null;
    }

    public Graph getOwnedGraph() {
        return this.graph;
    }

    public String getGraphName() {
        return this.getTitle();
    }

    public void setGraphName(String name) {
        this.setTitle(name);
    }

    public Collection<SearchResult> find(NodeSearchFilter filter) {
        return this.graph.find(filter);
    }
}