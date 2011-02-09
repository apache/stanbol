package org.apache.stanbol.entityhub.yard.solr;

import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.test.yard.YardTest;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test uses the system property "basedir" to configure an embedded Solr
 * Server. This property is set by the mvn surefire plugin. When using this
 * Unit Test within a build environment that does not set this property one need
 * to set it manually to the base directory of this module.
 * @author Rupert Westenthaler
 *
 */
public class SolrYardTest extends YardTest {
    
    private static Yard yard;
    private static String solrServer;
    public static final String testYardId = "testYard";
    
    @BeforeClass
    public final static void initYard() throws YardException {
        //get the working directory
        String baseDir = System.getProperty("basedir");
        solrServer = baseDir+"/src/test/resources/solr/test";
        System.out.println("BaseDir: "+baseDir);
        SolrYardConfig config = new SolrYardConfig(testYardId,solrServer);
        config.setName("Solr Yard Test");
        config.setDescription("The Solr Yard instance used to execute the Unit Tests defined for the Yard Interface");
        yard = new SolrYard(config);
    }
    
    @Override
    protected Yard getYard() {
        return yard;
    }
    /*
     * Three unit tests that check that SolrYardConfig does throw
     * IllegalArgumentExceptions when parsing an illegal parameters.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullParams() {
            new SolrYardConfig(null, null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullUrl() {
            new SolrYardConfig(testYardId, null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullID() {
            new SolrYardConfig(null, solrServer);
    }
    
    /**
     * This Method removes all Representations create via {@link #create()} or
     * {@link #create(String, boolean)} from the tested {@link Yard}.
     * It also removes all Representations there ID was manually added to the
     * {@link #representationIds} list.
     */
    @AfterClass
    public final static void clearUpRepresentations() throws YardException {
        yard.remove(representationIds);
    }
    
}
