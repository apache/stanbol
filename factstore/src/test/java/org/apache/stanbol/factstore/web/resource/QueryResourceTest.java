package org.apache.stanbol.factstore.web.resource;

import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.testing.http.BundleContextMock;
import org.apache.stanbol.commons.testing.http.ServletContextMock;
import org.apache.stanbol.factstore.FactStoreMock;
import org.apache.stanbol.factstore.api.FactStore;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;


public class QueryResourceTest {
    
    private ServletContextMock servletContext;

    @Before
    public void initMocks() {
        this.servletContext = new ServletContextMock();
        BundleContextMock bc = (BundleContextMock) this.servletContext.getAttribute(BundleContext.class
                .getName());
        bc.putService(FactStore.class.getName(), new FactStoreMock());
    }
    
    @Test
    public void testValidQuery() {
        QueryResource qr = new QueryResource(this.servletContext);
        
        String queryString = "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\"},\"select\":[\"person\"],\"from\":\"iks:employeeOf\",\"where\":[{\"=\":{\"organization\":{\"@iri\":\"http://upb.de\"}}}]}";
        
        Response response = qr.query(queryString);
        assertTrue(response.getStatus() == Status.OK.getStatusCode());
    }
}
