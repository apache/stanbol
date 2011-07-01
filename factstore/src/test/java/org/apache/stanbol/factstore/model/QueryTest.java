package org.apache.stanbol.factstore.model;

import static org.junit.Assert.*;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdIRI;
import org.apache.stanbol.commons.jsonld.JsonLdResource;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class QueryTest {

    @Test
    public void testToQueryFromJsonLd() throws Exception {
        JsonLd jldq = new JsonLd();
        jldq.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        
        JsonLdResource subject = new JsonLdResource();
        JSONArray select = new JSONArray();
        select.put("person");
        subject.putProperty("select", select);
        subject.putProperty("from", "iks:employeeOf");

        JSONObject orga = new JSONObject();
        orga.put("organization", new JsonLdIRI("http://upb.de"));

        JSONObject eq = new JSONObject();
        eq.put("=", orga);
        
        JSONArray where = new JSONArray();
        where.put(eq);
        
        subject.putProperty("where", where);
        
        jldq.put(subject);
        
        Query query = Query.toQueryFromJsonLd(jldq);
        assertNotNull(query);
        
        assertNotNull(query.getFromSchemaURN());
        assertEquals("http://iks-project.eu/ont/employeeOf", query.getFromSchemaURN());
        
        assertNotNull(query.getRoles());
        assertEquals(1, query.getRoles().size());
        assertEquals("person", query.getRoles().iterator().next());
        
        assertNotNull(query.getWhereClauses());
        assertEquals(1, query.getWhereClauses().size());
        
        WhereClause wc = query.getWhereClauses().iterator().next();
        assertNotNull(wc);
        assertEquals(CompareOperator.EQ,wc.getCompareOperator());
        assertEquals("organization", wc.getComparedRole());
        assertEquals("http://upb.de", wc.getSearchedValue());
    }

    @Test
    public void testToQueryFromJsonLd2() throws Exception {
        JsonLd jldq = new JsonLd();
        jldq.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        jldq.addNamespacePrefix("http://upd.de/persons/", "upb");
        
        JsonLdResource subject = new JsonLdResource();
        JSONArray select = new JSONArray();
        select.put("person");
        subject.putProperty("select", select);
        subject.putProperty("from", "iks:employeeOf");

        JSONObject orga = new JSONObject();
        orga.put("person", new JsonLdIRI("upb:fchrist"));

        JSONObject eq = new JSONObject();
        eq.put("=", orga);
        
        JSONArray where = new JSONArray();
        where.put(eq);
        
        subject.putProperty("where", where);
        
        jldq.put(subject);
        
        Query query = Query.toQueryFromJsonLd(jldq);
        assertNotNull(query);
        
        assertNotNull(query.getFromSchemaURN());
        assertEquals("http://iks-project.eu/ont/employeeOf", query.getFromSchemaURN());
        
        assertNotNull(query.getRoles());
        assertEquals(1, query.getRoles().size());
        assertEquals("person", query.getRoles().iterator().next());
        
        assertNotNull(query.getWhereClauses());
        assertEquals(1, query.getWhereClauses().size());
        
        WhereClause wc = query.getWhereClauses().iterator().next();
        assertNotNull(wc);
        assertEquals(CompareOperator.EQ,wc.getCompareOperator());
        assertEquals("person", wc.getComparedRole());
        assertEquals("http://upd.de/persons/fchrist", wc.getSearchedValue());
    }
}
