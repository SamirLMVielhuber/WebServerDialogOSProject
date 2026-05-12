package com.clt.webServer.DocumentBase;

import com.clt.diamant.web.WebSingleDocument;

import edu.cmu.lti.dialogos.sphinx.client.Sphinx;
import edu.cmu.lti.dialogos.sphinx.plugin.SphinxNode;

public class DocumentPostProcessor {
    //Idk if this would make sense to be parallelizable ... parallelisable? parallizable... no parallelizable right?
    //Force all Sphinx Nodes in one WebSingleDocument to use the same Recognizer this is a fix to a bigger architectural problem
    public static void bindRecognizers(WebSingleDocument doc, Sphinx recognizer) {
        doc.getOwnedGraph().getNodes().stream()
            .filter(SphinxNode.class::isInstance)
            .map(SphinxNode.class::cast)
            .forEach(n -> n.setRecognizer(recognizer));
    }
}
