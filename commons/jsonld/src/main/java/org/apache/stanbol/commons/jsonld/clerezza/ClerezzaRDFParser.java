/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.stanbol.commons.jsonld.clerezza;

import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFParser;

/**
 * Converts a Clerezza {@link Graph} to the {@link RDFDataset} used
 * by the {@link JsonLdProcessor}
 * 
 * @author Rupert Westenthaler
 *
 */
public class ClerezzaRDFParser implements RDFParser {

    private static String RDF_LANG_STRING = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";
    
    private long count = 0;
    
    @Override
    public RDFDataset parse(Object input) throws JsonLdError {
        count = 0;
        Map<BlankNode,String> bNodeMap = new HashMap<BlankNode,String>(1024);
        final RDFDataset result = new RDFDataset();
        if(input instanceof Graph){
            for(Triple t : ((Graph)input)){
                handleStatement(result,t, bNodeMap);
            }
        }
        bNodeMap.clear(); //help gc
        return result;
    }

    private void handleStatement(RDFDataset result, Triple t, Map<BlankNode,String> bNodeMap) {
        final String subject = getResourceValue(t.getSubject(), bNodeMap);
        final String predicate = getResourceValue(t.getPredicate(), bNodeMap);
        final RDFTerm object = t.getObject();
        
        if (object instanceof Literal) {
            
            final String value = ((Literal)object).getLexicalForm();
            final String language;
            final String datatype;
            datatype = getResourceValue(((Literal)object).getDataType(), bNodeMap);
            Language l = ((Literal)object).getLanguage();
            if(l == null){
                language = null;
            } else {
                language = l.toString();
            }
            result.addTriple(subject, predicate, value, datatype, language);
            count++;
        } else {
            result.addTriple(subject, predicate, getResourceValue((BlankNodeOrIRI) object, bNodeMap));
            count++;
        }
        
    }
    
    /**
     * The count of processed triples (not thread save)
     * @return the count of triples processed by the last {@link #parse(Object)} call
     */
    public long getCount() {
        return count;
    }
    
    private String getResourceValue(BlankNodeOrIRI nl, Map<BlankNode, String> bNodeMap) {
        if (nl == null) {
            return null;
        } else if (nl instanceof IRI) {
            return ((IRI) nl).getUnicodeString();
        } else if (nl instanceof BlankNode) {
            String bNodeId = bNodeMap.get(nl);
            if (bNodeId == null) {
                bNodeId = Integer.toString(bNodeMap.size());
                bNodeMap.put((BlankNode) nl, bNodeId);
            }
            return new StringBuilder("_:b").append(bNodeId).toString();
        } else {
            throw new IllegalStateException("Unknwon BlankNodeOrIRI type " + nl.getClass().getName() + "!");
        }
    }
}
    
