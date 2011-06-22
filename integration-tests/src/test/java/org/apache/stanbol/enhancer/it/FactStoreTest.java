package org.apache.stanbol.enhancer.it;

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactStoreTest extends StanbolTestBase {

    private static final Logger log = LoggerFactory.getLogger(FactStoreTest.class);

    @BeforeClass
    public static void cleanDatabase() throws Exception {
        String workingDirName = System.getProperty("jar.executor.workingdirectory");
        if (workingDirName != null) {
            File workingDir = new File(workingDirName);
            File factstore = new File(workingDir, "factstore");
            log.info("Preparing FactStore and deleting " + factstore.getAbsolutePath());
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
                    "{\"@context\":{\"iks\":\"http:\\/\\/iks-project.eu\\/ont\\/\",\"#types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Accept", "application/json");

        executor.execute(r).assertStatus(400);
    }

    @Test
    public void createSimpleFactSchema() throws Exception {
        Request r = builder
                .buildOtherRequest(new HttpPut(builder.buildUrl("/factstore/facts/TestFactSchema")))
                .withContent(
                    "{\"@context\":{\"iks\":\"http:\\/\\/iks-project.eu\\/ont\\/\",\"#types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Accept", "application/json");

        executor.execute(r).assertStatus(201);
    }

    @Test
    public void createURNFactSchema() throws Exception {
        Request r = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.iks-project.eu/ont/test"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http:\\/\\/iks-project.eu\\/ont\\/\",\"#types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Accept", "application/json");

        executor.execute(r).assertStatus(201);
    }

    @Test
    public void getFactSchemaByURN() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.iks-project.eu/ont/test2"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http:\\/\\/iks-project.eu\\/ont\\/\",\"#types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Accept", "application/json");

        executor.execute(r1).assertStatus(201);

        Request r2 = builder.buildOtherRequest(
            new HttpGet(builder.buildUrl("/factstore/facts/"
                                         + encodeURI("http://www.iks-project.eu/ont/test2")))).withHeader(
            "Accept", "application/json");

        String actual = executor.execute(r2).assertStatus(200).getContent();
        String expected = "{\"@context\":{\"#types\":{\"organization\":\"http:\\/\\/iks-project.eu\\/ont\\/organization\",\"person\":\"http:\\/\\/iks-project.eu\\/ont\\/person\"}}}";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void doubleCreateFactSchema() throws Exception {
        Request r = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.iks-project.eu/ont/double"))))
                .withContent(
                    "{\"@context\":{\"iks\":\"http:\\/\\/iks-project.eu\\/ont\\/\",\"#types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}")
                .withHeader("Accept", "application/json");

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
                    "{\"@context\":{\"#types\":{\"organization\":\"http:\\/\\/iks-project.eu\\/ont\\/organization\",\"person\":[\"http:\\/\\/iks-project.eu\\/ont\\/person\",\"http:\\/\\/www.schema.org\\/Person\"]}}}")
                .withHeader("Accept", "application/json");

        executor.execute(r).assertStatus(201);
    }

    @Test
    public void getSchemaMultiTypes() throws Exception {
        Request r1 = builder
                .buildOtherRequest(
                    new HttpPut(builder.buildUrl("/factstore/facts/"
                                                 + encodeURI("http://www.schema.org/Event.attendees"))))
                .withContent(
                    "{\"@context\":{\"#types\":{\"organization\":\"http:\\/\\/iks-project.eu\\/ont\\/organization\",\"person\":[\"http:\\/\\/iks-project.eu\\/ont\\/person\",\"http:\\/\\/www.schema.org\\/Person\"]}}}")
                .withHeader("Accept", "application/json");

        executor.execute(r1).assertStatus(201);

        Request r2 = builder.buildOtherRequest(
            new HttpGet(builder.buildUrl("/factstore/facts/"
                                         + encodeURI("http://www.schema.org/Event.attendees")))).withHeader(
            "Accept", "application/json");

        String actual = executor.execute(r2).assertStatus(200).getContent();
        String expected = "{\"@context\":{\"#types\":{\"organization\":\"http:\\/\\/iks-project.eu\\/ont\\/organization\",\"person\":[\"http:\\/\\/iks-project.eu\\/ont\\/person\",\"http:\\/\\/www.schema.org\\/Person\"]}}}";
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
