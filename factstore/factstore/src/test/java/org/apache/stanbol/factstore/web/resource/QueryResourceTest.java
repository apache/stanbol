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
package org.apache.stanbol.factstore.web.resource;

import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.testing.http.BundleContextMock;
import org.apache.stanbol.commons.testing.http.ServletContextMock;
import org.apache.stanbol.factstore.FactStoreMock;
import org.apache.stanbol.factstore.HttpHeadersMock;
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
        
        Response response = qr.query(queryString, new HttpHeadersMock());
        assertTrue(response.getStatus() == Status.OK.getStatusCode());
    }
}
