package org.apache.stanbol.entityhub.query.clerezza;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class SparqlQueryUtilsTest {

    @Test
    public void testCreateFullTextQueryString() {
        List<String> keywords = Arrays.asList("test", "keyword");
        assertEquals("\"test\" OR \"keyword\"", SparqlQueryUtils.createFullTextQueryString(keywords));

        keywords = Arrays.asList("test keyword");
        assertEquals("(\"test\" AND \"keyword\")", SparqlQueryUtils.createFullTextQueryString(keywords));

        keywords = Arrays.asList("'test' \"keyword\"");
        assertEquals("(\"'test'\" AND \"\\\"keyword\\\"\")",
            SparqlQueryUtils.createFullTextQueryString(keywords));
    }

}
