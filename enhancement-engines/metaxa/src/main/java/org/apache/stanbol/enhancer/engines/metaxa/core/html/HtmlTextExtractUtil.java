/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.stanbol.enhancer.engines.metaxa.core.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerFactory;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.util.RDFTool;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.vocabulary.NCO;
import org.semanticdesktop.aperture.vocabulary.NIE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Utility class that provides core HTML text and metadata extraction independent of the configuration of Metaxa's main HTML extractor
 * 
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 * 
 */

public class HtmlTextExtractUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlTextExtractUtil.class);
    
    private static HtmlParser htmlParser = new HtmlParser();
    private static XsltExtractor htmlExtractor;
    
    public HtmlTextExtractUtil() throws InitializationException {
        if (HtmlTextExtractUtil.htmlExtractor == null) {
            TransformerFactory transFac = TransformerFactory.newInstance();
            transFac.setURIResolver(new BundleURIResolver());
            HtmlTextExtractUtil.htmlExtractor = new XsltExtractor("any", "xslt/htmlmetadata.xsl", transFac);
            HtmlTextExtractUtil.htmlExtractor.setSyntax(Syntax.RdfXml);
        }
    }
    
    public String getTitle(Model meta) {
        Statement stmt = RDFTool.findStatement(meta, Variable.ANY, NIE.title, Variable.ANY);
        if (stmt != null) {
            return stmt.getObject().toString();
        }
        return null;
    }
    
    public String getAuthor(Model meta) {
        Statement stmt = RDFTool.findStatement(meta, Variable.ANY, NCO.creator, Variable.ANY);
        if (stmt != null) {
            stmt = RDFTool.findStatement(meta, stmt.getSubject(), NCO.fullname, Variable.ANY);
            if (stmt != null) {
                return stmt.getObject().toString();
            }
        }
        return null;
    }
    
    public String getDescription(Model meta) {
        Statement stmt = RDFTool.findStatement(meta, Variable.ANY, NIE.description, Variable.ANY);
        if (stmt != null) {
            return stmt.getObject().toString();
        }
        return null;
    }
    
    public List<String> getKeywords(Model meta) {
        List<String> kws = new ArrayList<String>();
        ClosableIterator<Statement> it = meta.findStatements(Variable.ANY, NIE.keyword, Variable.ANY);
        while (it.hasNext()) {
            kws.add(it.next().getObject().toString());
        }
        it.close();
        return kws;
    }
    
    public String getText(Model meta) {
        Statement stmt = RDFTool.findStatement(meta, Variable.ANY, NIE.plainTextContent, Variable.ANY);
        if (stmt != null) {
            return stmt.getObject().toString();
        }
        return null;
    }
    
    public void extract(URI id, String charset, InputStream input, RDFContainer result) throws ExtractorException {
        String encoding = charset;
        if (charset == null) {
            try {
                encoding = CharsetRecognizer.detect(input, "html", null);
            } catch (IOException e) {
                LOG.error("Charset detection problem: " + e.getMessage());
                throw new ExtractorException("Charset detection problem: " + e.getMessage());
            }
        }
        Document doc = htmlParser.getDOM(input, encoding);
        htmlExtractor.extract(id.toString(), doc, null, result);
    }
    
}
