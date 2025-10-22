package com.clt.webServer;

import com.clt.diamant.WozInterface;
import com.clt.diamant.InputCenter;
import com.clt.diamant.SingleDocument;
import com.clt.diamant.Document;
import java.util.Collection;

import javax.swing.JLayeredPane;

import com.clt.diamant.graph.GraphExecutionListener;
import com.clt.diamant.graph.Node;
import com.clt.diamant.graph.nodes.OwnerNode;

import com.clt.diamant.Device;
import com.clt.diamant.DialogInput;

import com.clt.script.debug.Debugger;
import com.clt.script.exp.Pattern;
import com.clt.script.exp.Value;
import com.clt.script.cmd.Command;
import com.clt.script.exp.Expression;

public class HeadlessWozInterface implements WozInterface {

    private State currentState = State.NORMAL;

    @Override
    public boolean initInterface() {
        System.out.println("Initializing headless interface...");
        return true;
    }

    @Override
    public void disposeInterface(boolean error) {
        System.out.println("Disposing headless interface. Error: " + error);
    }

    @Override
    public void startDocument(Document d, String name, InputCenter input) {
        System.out.println("Starting document: " + name);
    }

    @Override
    public void endDocument(Document d) {
        System.out.println("Ending document: " + d.getTitle());
    }

    @Override
    public void output(Device d, Value value) {}

    @Override
    public void discardOldInput(Device d) {}

    @Override
    public DialogInput<?> getInput(Pattern[] alternatives, Device d,
                                   Collection<Device> allDevices,
                                   Collection<Device> waitDevices, long timeout, boolean forceTimeout) {
        return null;
    }

    @Override
    public void transition(Node source, Node destination, int index, String condition) {}

    @Override
    public void subgraph(OwnerNode owner, boolean enter) {}

    @Override
    public void error(String type, String message) {
        System.err.println("Graph error [" + type + "]: " + message);
    }

    @Override
    public String getName() {
        return "HeadlessWozInterface";
    }

    @Override
    public void setState(State state) {
        this.currentState = state;
        System.out.println("Graph state changed to: " + state);
    }

    @Override
    public void abort() {
        System.out.println("Graph execution aborted.");
    }

    @Override
    public boolean isDebugger() {
        return false;
    }

    @Override
    public boolean showSubdialogsDuringExecution() {
        return false;
    }

    @Override
    public void addGraphExecutionListener(GraphExecutionListener l) {}

    @Override
    public void removeGraphExecutionListener(GraphExecutionListener l) {}

    @Override
    public void setDelay(long delay) {}

    @Override
    public long getDelay() {
        return 0;
    }

    @Override
    public void preExecute(Node node) {}

    @Override
    public JLayeredPane getLayeredPane() {
        return null;
    }

    @Override
    public void log(String s){
        System.out.println(s);
        System.out.flush();
    }

    @Override
    public void preExecute(Command c){}

    @Override
    public void preEvaluate(Expression e){}
}

