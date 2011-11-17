package org.apache.stanbol.reasoners.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReasonersTestBase extends StanbolTestBase{
    protected final String REASONERS_PATH = "/reasoners";
    protected final String[] SERVICES = {"/owl", "/owlmini", "/rdfs"};
    protected final String[] TASKS = {"/check", "/classify", "/enrich"};

    private final Logger log = LoggerFactory.getLogger(getClass());

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
    

    // Evoke the job service, returns the job Id
    protected String job(Request request) throws Exception{
        return executor.execute(request).assertStatus(200).getContent();
    }
    
    //
    protected void executeAndPingSingleJob(Request request) throws Exception{
        log.info("Executing: {}", request.getRequest().getURI());
        String jid = job(request);
        log.info("jid is {}", jid);
        // Get the result and ping the jId
        pingSingleJob(jid);
    }
    
    //
    protected void pingSingleJob(String jid) throws Exception{
        String url = REASONERS_PATH + "/jobs/ping/" + jid;
        log.info("Pinging {} ... ", url);
        boolean waiting = true;
        while(waiting){
            String content = executor.execute(builder.buildGetRequest(url)).assertStatus(200).getContent();
            if(content.equals("Job is still working")){
                log.info(" ... still working ...");
                Thread.sleep(500);
            }else{
                waiting = false;
                log.info(" ... done!");
            }
        }
    }
}
