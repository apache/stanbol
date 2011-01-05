package eu.iksproject.fise.ext.org.geonames;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.geonames.Style;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This test only correct values for score (set/getScore). An extension to the
 * web service client for geonames.org implemented to be able to write
 * fise:confidence values for fise:EntityAnnotations.
 * 
 * @author Rupert Westenthaler
 */
public class TestScore {

    private static final Logger log = LoggerFactory.getLogger(TestScore.class);

    @Test
    public void testSearch() throws Exception {
        ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
        searchCriteria.setName("Zealand");
        searchCriteria.setStyle(Style.FULL);
        searchCriteria.setMaxRows(5);
        try {
            ToponymSearchResult searchResult = WebService.search(searchCriteria);
            int i = 0;
            for (Toponym toponym : searchResult.getToponyms()) {
                i++;
                log.info("Result "+i+" "+ toponym.getGeoNameId()+" score= "+toponym.getScore());
                assertNotNull(toponym.getScore());
                assertTrue(toponym.getScore() >= Double.valueOf(0));
                assertTrue(toponym.getScore()<= Double.valueOf(100));
            }
        } catch(IOException e) {
            if (e instanceof UnknownHostException) {
                log.warn("Unable to test LocationEnhancemetEngine when offline! -> skipping this test",e.getCause());
            } else if (e instanceof SocketTimeoutException){
                log.warn("Seams like the geonames.org webservice is currently unavailable -> skipping this test",e.getCause());
            } else if (e.getMessage().contains("overloaded with requests")) {
                log.warn("Seams like the geonames.org webservice is currently unavailable -> skipping this test", e.getCause());
            } else {
                throw e;
            }
        }
    }

    @Test
    public void testHierarchy() throws Exception {
        ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
        searchCriteria.setName("New York");
        searchCriteria.setStyle(Style.FULL);
        searchCriteria.setMaxRows(1);
        try {
            ToponymSearchResult searchResult = WebService.search(searchCriteria);
            int testGeonamesId = searchResult.getToponyms().iterator().next().getGeoNameId();
            for (Toponym hierarchy : WebService.hierarchy(testGeonamesId, null,
                    Style.FULL)) {
                // this service does not provide an score, so test if 1.0 is
                // returned
                assertNotNull(hierarchy.getScore());
                assertEquals(hierarchy.getScore(), Double.valueOf(1.0));
            }
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                log.warn(
                        "Unable to test LocationEnhancemetEngine when offline! -> skipping this test",
                        e.getCause());
            } else if (e instanceof SocketTimeoutException) {
                log.warn(
                        "Seams like the geonames.org webservice is currently unavailable -> skipping this test",
                        e.getCause());
            } else if (e.getMessage().contains("overloaded with requests")) {
                log.warn(
                        "Seams like the geonames.org webservice is currently unavailable -> skipping this test",
                        e.getCause());
            } else {
                throw e;
            }
        }
    }

}
