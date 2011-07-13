package org.apache.stanbol.factstore.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdIRI;
import org.apache.stanbol.commons.jsonld.JsonLdResource;
import org.junit.Test;

public class QueryTest {

    @Test
    public void testToQueryFromJsonLd() throws Exception {
        JsonLd jldq = new JsonLd();
        jldq.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        
        JsonLdResource subject = new JsonLdResource();
        List<String> select = new ArrayList<String>();
        select.add("person");
        subject.putProperty("select", select);
        subject.putProperty("from", "iks:employeeOf");

        Map<String, Object> orga = new HashMap<String, Object>();
        orga.put("organization", new JsonLdIRI("http://upb.de"));

        Map<String, Object> eq = new HashMap<String, Object>();
        eq.put("=", orga);
        
        List<Object> where = new ArrayList<Object>();
        where.add(eq);
        
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
        List<String> select = new ArrayList<String>();
        select.add("person");
        subject.putProperty("select", select);
        subject.putProperty("from", "iks:employeeOf");

        Map<String, Object> orga = new HashMap<String, Object>();
        orga.put("person", new JsonLdIRI("upb:fchrist"));

        Map<String, Object> eq = new HashMap<String, Object>();
        eq.put("=", orga);
        
        List<Object> where = new ArrayList<Object>();
        where.add(eq);
        
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
