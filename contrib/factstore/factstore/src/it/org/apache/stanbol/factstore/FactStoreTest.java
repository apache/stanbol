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
package org.apache.stanbol.factstore;

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactStoreTest extends StanbolTestBase {

    private static final Logger log = LoggerFactory.getLogger(FactStoreTest.class);

    @AfterClass
    public static void cleanDatabase() throws Exception {
        String workingDirName = System.getProperty("jar.executor.workingdirectory");
        if (workingDirName != null) {
            File workingDir = new File(workingDirName);
            File factstore = new File(workingDir, "factstore");
            log.info("Deleting integration test FactStore " + factstore.getAbsolutePath());
            FileUtils.deleteDirectory(factstore);
        }
    }

    @Test
    public void maximumSchemaURNLength() throws Exception {
        Request r = builder
                .buildOtherRequest(
                    new HttpPut(
                            builder
                                    .buildUrl("/factstore/facts/"
                                              + encodeURI("http://www.test.de/this/urn/is/a/bit/too/long/to/be/used/in/this/fact/store/implementation/with/derby"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r).assertStatus(400);
    }

    @Test
    public void createSimpleFactSchema() throws Exception {
        Request r = builder
                .buildOtherRequest(new HttpPut(builder.buildUrl("/factstore/facts/TestFactSchema")))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r).assertStatus(201);
    }

    @Test
    public void createURNFactSchema() throws Exception {
        Request r = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.iks-project.eu/ont/test"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r).assertStatus(201);
    }

    @Test
    public void getFactSchemaByURN() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.iks-project.eu/ont/test2"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r1).assertStatus(201);

        Request r2 = builder.buildOtherRequest(
            new HttpGet(builder.buildUrl("/factstore/facts/"
                                         + encodeURI("http://www.iks-project.eu/ont/test2")))).withHeader(
            "Content-Type", "application/json");

        String actual = executor.execute(r2).assertStatus(200).getContent();
        String expected = "{\"@context\":{\"@types\":{\"organization\":\"http://iks-project.eu/ont/organization\",\"person\":\"http://iks-project.eu/ont/person\"}}}";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void doubleCreateFactSchema() throws Exception {
        Request r = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.iks-project.eu/ont/double"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r).assertStatus(201);
        executor.execute(r).assertStatus(409);
    }

    @Test
    public void createSchemaMultiTypes() throws Exception {
        Request r = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.schema.org/attendees"))))
                .withContent(
                    "{\"@context\":{\"@types\":{\"organization\":\"http://iks-project.eu/ont/organization\",\"person\":[\"http://iks-project.eu/ont/person\",\"http://www.schema.org/Person\"]}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r).assertStatus(201);
    }

    @Test
    public void getSchemaMultiTypes() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.schema.org/Event.attendees"))))
                .withContent(
                    "{\"@context\":{\"@types\":{\"organization\":\"http://iks-project.eu/ont/organization\",\"person\":[\"http://iks-project.eu/ont/person\",\"http://www.schema.org/Person\"]}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r1).assertStatus(201);

        Request r2 = builder.buildOtherRequest(
            new HttpGet(builder.buildUrl("/factstore/facts/"
                                         + encodeURI("http://www.schema.org/Event.attendees")))).withHeader(
            "Accept", "application/json");

        String actual = executor.execute(r2).assertStatus(200).getContent();
        String expected = "{\"@context\":{\"@types\":{\"organization\":\"http://iks-project.eu/ont/organization\",\"person\":[\"http://iks-project.eu/ont/person\",\"http://www.schema.org/Person\"]}}}";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void postSingleFact() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://iks-project.eu/ont/employeeOf1"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r1).assertStatus(201);

        Request r2 = builder
                .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:employeeOf1\",\"person\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r2).assertStatus(200).assertHeader("Location", builder.buildUrl("/factstore/facts/http%3A%2F%2Fiks-project.eu%2Font%2FemployeeOf1/1"));
    }
    
    @Test
    public void postSingleFactNeg() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://iks-project.eu/ont/employeeOf1Neg"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r1).assertStatus(201);

        Request r2 = builder
                .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:employeeOf1Wrong\",\"person\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r2).assertStatus(500);
    }
    
    @Test
    public void postSingleFactNeg2() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://iks-project.eu/ont/employeeOf2Neg"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r1).assertStatus(201);

        Request r2 = builder
                .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:employeeOf2Neg\",\"people\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}}")
                .withHeader("Content-Type", "application/json");

        executor.execute(r2).assertStatus(500);
    }

    @Test
    public void postMultiFactsMultiTypes() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://iks-project.eu/ont/employeeOf2"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}")
                .withHeader("Content-Type", "application/json");
        executor.execute(r1).assertStatus(201);

        Request r2 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://iks-project.eu/ont/friendOf2"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"friend\":\"iks:person\"}}}")
                .withHeader("Content-Type", "application/json");
        executor.execute(r2).assertStatus(201);

        Request r3 = builder
                .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@subject\":[{\"@profile\":\"iks:employeeOf2\",\"person\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}},{\"@profile\":\"iks:friendOf2\",\"person\":{\"@iri\":\"upb:bnagel\"},\"friend\":{\"@iri\":\"upb:fchrist\"}}]}")
                .withHeader("Content-Type", "application/json");
        executor.execute(r3).assertStatus(200);
    }

    @Test
    public void postMultiFactsMultiTypesNeg() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://iks-project.eu/ont/employeeOf3Neg"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}")
                .withHeader("Content-Type", "application/json");
        executor.execute(r1).assertStatus(201);

        Request r2 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://iks-project.eu/ont/friendOf3Neg"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"friend\":\"iks:person\"}}}")
                .withHeader("Content-Type", "application/json");
        executor.execute(r2).assertStatus(201);

        Request r3 = builder
                .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
                .withContent(
                    "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@subject\":[{\"@profile\":\"iks:employeeOf3NegWrong\",\"person\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://uni-paderborn.de\"}},{\"@profile\":\"iks:friendOf3Neg\",\"person\":{\"@iri\":\"upb:bnagel\"},\"friend\":{\"@iri\":\"upb:fchrist\"}}]}")
                .withHeader("Content-Type", "application/json");
        executor.execute(r3).assertStatus(500);
    }
    
    @Test
    public void querySingleFact() throws Exception {
        Request r1 = builder
        .buildOtherRequest(
            new HttpPut(builder.buildUrl("/factstore/facts/"
                                         + encodeURI("http://iks-project.eu/ont/employeeOf"))))
        .withContent(
            "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}")
        .withHeader("Content-Type", "application/json");
        executor.execute(r1).assertStatus(201);

        Request r2 = builder
        .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
        .withContent(
            "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:employeeOf\",\"person\":{\"@iri\":\"upb:bnagel\"},\"organization\":{\"@iri\":\"http://upb.de\"}}")
        .withHeader("Content-Type", "application/json");
        executor.execute(r2).assertStatus(200);
        
        String queryString = "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\"},\"select\":[\"person\"],\"from\":\"iks:employeeOf\",\"where\":[{\"=\":{\"organization\":{\"@iri\":\"http://upb.de\"}}}]}";
        Request q = builder
        .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/query/")))
        .withContent(queryString)
        .withHeader("Content-Type", "application/json")
        .withHeader("Accept", "application/json");
                
        String expected = "{\"@subject\":\"R1\",\"person\":\"http://upb.de/persons/bnagel\"}";
        String actual = executor.execute(q).assertStatus(200).getContent();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void querySingleFactMultiResults() throws Exception {
        Request r1 = builder
        .buildOtherRequest(
            new HttpPut(builder.buildUrl("/factstore/facts/"
                                         + encodeURI("http://iks-project.eu/ont/emplOf"))))
        .withContent(
            "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}")
        .withHeader("Content-Type", "application/json");
        executor.execute(r1).assertStatus(201);

        Request r2 = builder
        .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
        .withContent(
            "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:emplOf\",\"person\":{\"@iri\":\"upb:jim\"},\"organization\":{\"@iri\":\"http://upb.de\"}}")
        .withHeader("Content-Type", "application/json");
        executor.execute(r2).assertStatus(200);
        
        Request r3 = builder
        .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
        .withContent(
            "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:emplOf\",\"person\":{\"@iri\":\"upb:john\"},\"organization\":{\"@iri\":\"http://upb.de\"}}")
        .withHeader("Content-Type", "application/json");
        executor.execute(r3).assertStatus(200);
        
        Request r4 = builder
        .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/facts/")))
        .withContent(
            "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"upb\":\"http://upb.de/persons/\"},\"@profile\":\"iks:emplOf\",\"person\":{\"@iri\":\"upb:james\"},\"organization\":{\"@iri\":\"http://upb.de\"}}")
        .withHeader("Content-Type", "application/json");
        executor.execute(r4).assertStatus(200);
        
        String queryString = "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\"},\"select\":[\"person\"],\"from\":\"iks:emplOf\",\"where\":[{\"=\":{\"organization\":{\"@iri\":\"http://upb.de\"}}}]}";
        Request q = builder
        .buildOtherRequest(new HttpPost(builder.buildUrl("/factstore/query/")))
        .withContent(queryString)
        .withHeader("Content-Type", "application/json")
        .withHeader("Accept", "application/json");
                
        String expected = "{\"@subject\":[{\"@subject\":\"R1\",\"person\":\"http://upb.de/persons/jim\"},{\"@subject\":\"R2\",\"person\":\"http://upb.de/persons/john\"},{\"@subject\":\"R3\",\"person\":\"http://upb.de/persons/james\"}]}";
        String actual = executor.execute(q).assertStatus(200).getContent();
        Assert.assertEquals(expected, actual);
    }
    
    private String encodeURI(String s) {
        StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (isUnsafe(ch)) {
                o.append('%');
                o.append(toHex(ch / 16));
                o.append(toHex(ch % 16));
            } else o.append(ch);
        }
        return o.toString();
    }

    private char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0) return true;
        return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
    }

    @SuppressWarnings("unused")
    private void toConsole(String actual) {
        System.out.println(actual);
        String s = actual;
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("\n", "\\n");
        System.out.println(s);
    }

}
