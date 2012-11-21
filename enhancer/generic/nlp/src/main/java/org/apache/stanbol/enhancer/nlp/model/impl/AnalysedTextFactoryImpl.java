package org.apache.stanbol.enhancer.nlp.model.impl;

import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.osgi.framework.Constants;

@Component(immediate=true)
@Service(value=AnalysedTextFactory.class)
@Properties(value={
    @Property(name=Constants.SERVICE_RANKING,intValue=Integer.MIN_VALUE)
})
public class AnalysedTextFactoryImpl extends AnalysedTextFactory {

    @Override
    public AnalysedText createAnalysedText(Blob blob) throws IOException {
        String text = ContentItemHelper.getText(blob);
        return new AnalysedTextImpl(blob,text);
    }
}
