package edu.cmu.lti.dialogos.sphinx.plugin;

import com.clt.diamant.Device;
import com.clt.diamant.Resources;
import com.clt.diamant.graph.nodes.AbstractInputNode;
import com.clt.diamant.graph.nodes.NodeExecutionException;
import com.clt.speech.SpeechException;
import com.clt.speech.recognition.LanguageName;
import com.clt.speech.recognition.RecognitionExecutor;
import edu.cmu.lti.dialogos.sphinx.client.Sphinx;

import javax.sound.sampled.AudioFormat;
import java.util.List;

public class SphinxNode extends AbstractInputNode {

    private static Device sphinxDevice = new Device(Resources.getString("Sphinx"));
    private Sphinx recognizer = null;

    @Override
    public AudioFormat getAudioFormat() {
        return Sphinx.getAudioFormat();
    }

    private Sphinx getRecognizer() {
        if(this.recognizer != null)
            return this.recognizer;
        return Plugin.getRecognizer();
    }

    public void setRecognizer(Sphinx recognizer){
        System.out.println("SphinxNode" + this.hashCode() + ": Setting Recognizer to " + recognizer);
        System.out.flush();
        this.recognizer = recognizer;
    }

    @Override
    public RecognitionExecutor createRecognitionExecutor(com.clt.srgf.Grammar recGrammar) {
        try {
            this.getRecognizer().stopRecognition();
        } catch (SpeechException exn) {
            throw new NodeExecutionException(this, Resources.getString("RecognizerError") + ".", exn);
        }

        if (getSettings().getSilentMode()) {
            return new SilentRecognitionExecutor();
        } else {
            recGrammar.requestRobustness(Boolean.TRUE == getProperty(ENABLE_GARBAGE));
            Sphinx sphinx = getRecognizer();
            // Set audioInputPlugin here, so that the sphinx recognizer can use it for the speech recognition
            // also the node has information about the singledocument
            sphinx.setAudioInputPlugin(getGraph().getOwner().getPluginManager().getActiveAudioInputPlugin());
            System.out.println("SphinxNode"+ this.hashCode()+": Using Recogniser: " + sphinx);
            System.out.println("    With Plugin " + getGraph().getOwner().getPluginManager().getActiveAudioInputPlugin());
            System.out.flush();

            return new SphinxRecognitionExecutor(sphinx);
        }
    }

    @Override
    public Device getDevice() {
        return sphinxDevice;
    }

    @Override
    public List<LanguageName> getAvailableLanguages() {
        return getSettings() != null ? getSettings().getLanguages() : Plugin.getAvailableLanguages();
    }

    @Override
    public LanguageName getDefaultLanguage() {
        assert this.getGraph() != null : "must not query default language when plugin settings are unreachable";
        return getSettings().getDefaultLanguage();
    }

    /**
     * retrieve the settings from the dialog graph (which is where they are
     * stored -- not within Plugin!)
     */
    private Settings getSettings() {
        if (getGraph() != null && getGraph().getOwner() != null) {
            return ((Settings) getGraph().getOwner().getPluginSettings(Plugin.class));
        } else {
            return null;
        }
    }
}
