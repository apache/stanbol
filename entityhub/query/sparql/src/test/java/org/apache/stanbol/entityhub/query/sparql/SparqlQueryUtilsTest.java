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
package org.apache.stanbol.entityhub.query.sparql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery;
import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQueryFactory;
import org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.junit.Test;

public class SparqlQueryUtilsTest {

    @Test
    public void testCreateFullTextQueryString() {
        List<String> keywords = Arrays.asList("test", "keyword");
        assertEquals("\"test\" OR \"keyword\"", 
            SparqlQueryUtils.createFullTextQueryString(keywords));

        keywords = Arrays.asList("test keyword");
        assertEquals("(\"test\" AND \"keyword\")", 
            SparqlQueryUtils.createFullTextQueryString(keywords));

        keywords = Arrays.asList("'test' \"keyword\"");
        assertEquals("(\"\\'test\\'\" AND \"\\\"keyword\\\"\")",
            SparqlQueryUtils.createFullTextQueryString(keywords));
        
        keywords = Arrays.asList("1 Alpha ? Numeric Test .");
        assertEquals("(\"1\" AND \"Alpha\" AND \"Numeric\" AND \"Test\")",
            SparqlQueryUtils.createFullTextQueryString(keywords));
        
    }
    /**
     * Tests for grammar encoding (STANBOL-877)
     */
    @Test
    public void testGrammarEncodning(){
        assertEquals("test\\'s",SparqlQueryUtils.getGrammarEscapedValue("test's")); 
        assertEquals("test\\\"s",SparqlQueryUtils.getGrammarEscapedValue("test\"s")); 
        assertEquals("new\\\nline",SparqlQueryUtils.getGrammarEscapedValue("new\nline")); 
        
    }
    
    /**
     * Utility function for generating SparqlQuery
     * @param textWithDoubleQuote
     * @param patternType
     */
    private String generateSparqlQueryString(String textWithDoubleQuote, PatternType patternType) {
    	int limit = 10;
    	FieldQueryFactory qf = DefaultQueryFactory.getInstance();
    	FieldQuery query = qf.createFieldQuery();
      
    	String DEFAULT_AUTOCOMPLETE_SEARCH_FIELD = NamespaceEnum.rdfs+"label";
      
    	Collection<String> selectedFields = new ArrayList<String>();
    	selectedFields.add(DEFAULT_AUTOCOMPLETE_SEARCH_FIELD);
    	query.addSelectedFields(selectedFields);
    	query.setConstraint(DEFAULT_AUTOCOMPLETE_SEARCH_FIELD, 
      		new TextConstraint(textWithDoubleQuote, patternType, true, "en",null));
    	query.setLimit(10);
    	query.setOffset(0);
    	
    	final SparqlFieldQuery sparqlQuery = SparqlFieldQueryFactory.getSparqlFieldQuery(query);
    	
    	return SparqlQueryUtils.createSparqlSelectQuery(sparqlQuery, false,limit,SparqlEndpointTypeEnum.Standard);
	}
    
    @Test
    public void testDoubleQuotes(){
    	String testString = "double\"quote";
    	
    	String queryNone = generateSparqlQueryString(testString,PatternType.none);
    	//the quote have to be double escaped before checked with .contains
    	assertTrue(queryNone.contains(testString.replaceAll("\\\"", "\\\\\"")));
    	
    	String queryRegex = generateSparqlQueryString(testString,PatternType.regex);
    	assertTrue(queryRegex.contains(testString.replaceAll("\\\"", "\\\\\"")));
    }

	

}
