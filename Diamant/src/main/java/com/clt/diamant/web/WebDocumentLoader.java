package com.clt.diamant.web;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.clt.diamant.Document;
import com.clt.diamant.DocumentLoader;
import com.clt.diamant.Resources;
import com.clt.event.ProgressEvent;
import com.clt.event.ProgressListener;
import com.clt.gui.ProgressDialog;
import com.clt.util.DefaultLongAction;
import com.clt.xml.AbstractHandler;
import com.clt.xml.XMLProgressListener;
import com.clt.xml.XMLReader;

/**
    This Accepts all Documents marked with Wizard so SingleDocument but loads them as WebSingleDocument
 */
public class WebDocumentLoader extends DocumentLoader {

    public WebDocumentLoader(Document d) {
        super(d);
    }

    @Override
    public Document load(final File f, ProgressListener progress) throws IOException {
        final XMLReader r = new XMLReader(Document.validateXML);

        try {
            String description = Resources.format("Loading", f.getName());
            WebLoadingAction loading = new WebLoadingAction(description, r, f);

            if (progress == null) {
                try {
                    new ProgressDialog(null).run(loading);
                } catch (java.awt.HeadlessException he) {
                    ProgressListener pl = e -> System.err.println(e.getMessage());
                    loading.addProgressListener(pl);
                    try {
                        loading.run();
                    } finally {
                        loading.removeProgressListener(pl);
                    }
                }
            } else {
                loading.addProgressListener(progress);
                progress.progressChanged(new ProgressEvent(this, loading.getDescription(), 0, 0, 0));
                try {
                    loading.run();
                } finally {
                    loading.removeProgressListener(progress);
                }
            }
        } catch (InvocationTargetException exn) {
            if (exn.getTargetException() instanceof IOException) {
                throw (IOException) exn.getTargetException();
            } else if (exn.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) exn.getTargetException();
            } else {
                throw new IOException(exn.getTargetException().toString());
            }
        } catch (IOException | RuntimeException exn) {
            throw exn;
        } catch (Exception exn) {
            throw new IOException(exn.toString());
        }

        return this.getDocument();
    }

    /**
        Only supports WebSingleDocument under <wizard>.
     */
    class WebLoadingAction extends DefaultLongAction {

        private XMLReader r;
        private File f;

        public WebLoadingAction(String description, XMLReader r, File f) {
            super(description);
            this.r = r;
            this.f = f;
        }

        @Override
        public void run(final ProgressListener l) throws IOException {
            if (l != null) {
                final ProgressEvent evt = new ProgressEvent(WebDocumentLoader.this, this.getDescription() + "...", 0, 400, 0);
                XMLProgressListener progress = new XMLProgressListener() {
                    public void percentComplete(float percent) {
                        evt.setCurrent((int) (evt.getEnd() * percent));
                        l.progressChanged(evt);
                    }
                };
                this.r.addProgressListener(progress);
            }

            this.r.parse(this.f, new AbstractHandler() {
                @Override
                public void start(String name, Attributes atts) throws SAXException {
                    if (name.equals("wizard")) {
                        if (WebDocumentLoader.this.getDocument() == null) {
                            WebDocumentLoader.this.setDocument(new WebSingleDocument());
                        } else if (!(WebDocumentLoader.this.getDocument() instanceof WebSingleDocument)) {
                            r.raiseException(Resources.getString("DocumentTypeChanged"));
                        }
                        WebDocumentLoader.this.getDocument().load(WebLoadingAction.this.f, WebLoadingAction.this.r);
                    } else {
                        // Reject all other types
                        r.raiseException(Resources.getString("UnknownDocumentType"));
                    }
                }
            });
        }
    }
}

