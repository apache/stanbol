package org.apache.stanbol.enhancer.engines.metaxa.core.html;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * HtmlParser.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 *
 */
public class HtmlParser {

    /**
     * This contains the logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HtmlParser.class);

    private HtmlCleaner htmlToXmlParser;
    private CleanerProperties parserProps;
    private DomSerializer domCreator;


    public HtmlParser() {
        this.htmlToXmlParser = new HtmlCleaner();
        this.parserProps = this.htmlToXmlParser.getProperties();
        this.parserProps.setRecognizeUnicodeChars(true);
        this.parserProps.setUseEmptyElementTags(true);
        // this.parserProps.setAdvancedXmlEscape(true);
        this.parserProps.setTranslateSpecialEntities(true);
        this.parserProps.setOmitComments(true);
        this.parserProps.setPruneTags("script,style,form,map,noscript");
        this.domCreator = new DomSerializer(this.parserProps);
        // TODO override otpions form config
    }


    public Document getDOM(String html) {
        Document doc = null;
        try {
            doc = domCreator.createDOM(this.htmlToXmlParser.clean(html));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }


    public Document getDOM(InputStream html, String charset) {
        Document doc = null;
        try {
            doc =
                domCreator.createDOM(this.htmlToXmlParser.clean(html, charset));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }
}
