package org.apache.stanbol.factstore.web.resource;

import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.testing.http.BundleContextMock;
import org.apache.stanbol.commons.testing.http.ServletContextMock;
import org.apache.stanbol.factstore.FactStoreMock;
import org.apache.stanbol.factstore.api.FactStore;
import org.apache.stanbol.factstore.web.resource.FactsResource;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class FactsResourceTest {

    private ServletContextMock servletContext;

    @Before
    public void initMocks() {
        this.servletContext = new ServletContextMock();
        BundleContextMock bc = (BundleContextMock) this.servletContext.getAttribute(BundleContext.class
                .getName());
        bc.putService(FactStore.class.getName(), new FactStoreMock());
    }

    @Test
    public void testGet() {
        FactsResource fr = new FactsResource(this.servletContext);
        Response response = fr.get();
        assertTrue(response.getStatus() == Status.OK.getStatusCode());
    }

    @Test
    public void testPutFactSchemaNoSchemaURI() {
        FactsResource fr = new FactsResource(this.servletContext);

        Response response = fr.putFactSchema("", null);
        assertTrue(response.getStatus() == Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testPutFactSchemaNoJSON() {
        FactsResource fr = new FactsResource(this.servletContext);

        Response response = fr.putFactSchema("no JSON-LD string", "test2");
        assertTrue(response.getStatus() == Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testPutFactSchemaNoJSONSchema() {
        FactsResource fr = new FactsResource(this.servletContext);

        Response response = fr.putFactSchema("{}", "test2");
        assertTrue(response.getStatus() == Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testTooLongURN() {
        FactsResource fr = new FactsResource(this.servletContext);

        Response response = fr
                .putFactSchema(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"#types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}",
                    "http://www.test.de/this/urn/is/a/bit/too/long/to/be/used/in/this/fact/store/implementation/with/derby");
        assertTrue(response.getStatus() == Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testPutFactSchemaValidInput() {
        FactsResource fr = new FactsResource(this.servletContext);

        Response response = fr
                .putFactSchema(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"#types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}",
                    "test2");
        assertTrue(response.getStatus() == Status.CREATED.getStatusCode());
    }

    @Test
    public void testPostSingleFact() {
        FactsResource fr = new FactsResource(this.servletContext);

        Response response = fr
                .postFacts("{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:employeeOf\",\"person\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}}");

        assertTrue(response.getStatus() == Status.OK.getStatusCode());
    }

    @Test
    public void testPostMultiFacts() {
        FactsResource fr = new FactsResource(this.servletContext);

        Response response = fr
                .postFacts("{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:employeeOf\",\"@\":[{\"person\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}},{\"person\":{\"@iri\":\"upb:fchrist\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}}]}");

        assertTrue(response.getStatus() == Status.OK.getStatusCode());
    }

    @Test
    public void testPostMultiFactsDifferentTypes() {
        FactsResource fr = new FactsResource(this.servletContext);

        Response response = fr
                .postFacts("{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@\":[{\"@profile\":\"iks:employeeOf\",\"person\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}},{\"@profile\":\"iks:friendOf\",\"person\":{\"@iri\":\"upb:bnagel\"},\"friend\":{\"@iri\":\"upb:fchrist\"}}]}");

        assertTrue(response.getStatus() == Status.OK.getStatusCode());
    }
}
