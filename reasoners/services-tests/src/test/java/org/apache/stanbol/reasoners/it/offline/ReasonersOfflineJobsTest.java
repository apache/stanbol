package org.apache.stanbol.reasoners.it.offline;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.Executor;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.reasoners.it.offline.ReasonersOfflineTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests background jobs
 *
 */
public class ReasonersOfflineJobsTest extends ReasonersOfflineTest {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @BeforeClass
    public static void prepare() throws URISyntaxException {
        ReasonersOfflineTest.prepare();
    }

    @Before
    public void setupMultipart() {
        super.setupMultipart();
    }
    
    @Test
    public void testSubsequentJobs() throws Exception{
        log.info("testSubsequentJobs()");
        // We send a file to a job then we ping it, we do this for all services and tasks
        
        for(String s : allServices() ){
            for(String t : TASKS){
                StringBuilder sb = new StringBuilder(REASONERS_PATH);
                sb.append(s).append(t).append("/job");
                Request request = buildMultipartRequest(sb.toString(),multiPart);
                executeAndPingSingleJob(request);
            }
        }
    }
    
    @Test
    public void testSubsequentJobs2() throws Exception{
        log.info("testSubsequentJobs2()");
        
        // We start all jobs and the we ping all
        List<String> locations = new ArrayList<String>();
        for(String s : allServices() ){
            for(String t : TASKS){
                StringBuilder sb = new StringBuilder(REASONERS_PATH);
                sb.append(s).append(t).append("/job");
                Request request = buildMultipartRequest(sb.toString(), multiPart);
                String location = createJob(request);
                log.info("Started job {}", location);
                locations.add(location);
            }
        }
        
        // We ping all in sequence
        for(String l : locations){
            pingSingleJob(l);
        }
    }
}
