package org.apache.stanbol.reasoners.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;

public class ReasonersTestBase extends StanbolTestBase{
    protected final String REASONERS_PATH = "/reasoners";
    protected final String[] SERVICES = {"/owl", "/owlmini", "/rdfs"};
    protected final String[] TASKS = {"/check", "/classify", "/enrich"};


    protected Request buildMultipartRequest(String path,MultipartEntity multiPart) {
        HttpPost httpPost = new HttpPost(builder.buildUrl(path));
        httpPost.setEntity(multiPart);
        /**
         * In case of Multipart requests, we must __NOT__ set the content-type header to multipart/form-data.
         * If we do it, we have a 400 response (bad request).
         */
        return this.builder.buildOtherRequest(httpPost);
    }

    protected List<String> allServices() {
        List<String> sl = new ArrayList<String>();
        sl.addAll(Arrays.asList(SERVICES));
        return sl;
    }
}
