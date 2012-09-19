package org.apache.stanbol.enhancer.nlp.model.impl;

import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.osgi.framework.Constants;

@Component(immediate=true)
@Service(value=AnalysedTextFactory.class)
@Properties(value={
    @Property(name=Constants.SERVICE_RANKING,intValue=Integer.MIN_VALUE)
})
public class AnalysedTextFactoryImpl extends AnalysedTextFactory {

    @Override
    public AnalysedText createAnalysedText(ContentItem ci, Blob blob) throws IOException {
        ci.getLock().readLock().lock();
        try {
            AnalysedText existing = ci.getPart(AnalysedText.ANALYSED_TEXT_URI, AnalysedText.class);
            throw new IllegalStateException("The AnalysedText ContentPart already exists (impl: "
                +existing.getClass().getSimpleName()+"| blob: "+existing.getBlob().getMimeType()+")");
        }catch (NoSuchPartException e) {
            //this is the expected case
        }catch (ClassCastException e) {
            throw new IllegalStateException("A ContentPart with the URI '"
                + AnalysedText.ANALYSED_TEXT_URI+"' already exists but the parts "
                + "type is not compatible with "+AnalysedText.class.getSimpleName()+"!",
                e);
        } finally {
            ci.getLock().readLock().unlock();
        }
        //create the Analysed text
        AnalysedText at = createAnalysedText(blob);
        ci.getLock().writeLock().lock();
        try {
            //NOTE: there is a possibility that an other thread has added
            // the contentpart
            ci.addPart(AnalysedText.ANALYSED_TEXT_URI, at);
        } finally {
            ci.getLock().writeLock().unlock();
        }
        return at;
    }

    @Override
    public AnalysedText createAnalysedText(Blob blob) throws IOException {
        String text = ContentItemHelper.getText(blob);
        return new AnalysedTextImpl(blob,text);
    }
}
