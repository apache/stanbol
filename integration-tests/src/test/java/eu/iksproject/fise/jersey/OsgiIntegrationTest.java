package eu.iksproject.fise.jersey;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.*;


@RunWith(JUnit4TestRunner.class)
public class OsgiIntegrationTest extends Assert {

    private static final String JERSEY_VERSION = "1.2-SNAPSHOT";
    private static final String IKS_VERSION = "0.9-SNAPSHOT";
    private static final String PAX_WEB_VERSION = "0.7.1";

    @Inject
    BundleContext bundleContext;

    @Configuration
    public static Option[] configure() {
        return options(
                logProfile(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),

                // Explicit deps. TODO: there is probably a way to generate this from POM.

                repositories("http://repo1.maven.org/maven2"
                        , "http://repository.apache.org/content/groups/snapshots-group"
                        , "http://repository.ops4j.org/maven2"
                        , "http://svn.apache.org/repos/asf/servicemix/m2-repo"
                        , "http://repository.springsource.com/maven/bundles/release"
                        , "http://repository.springsource.com/maven/bundles/external"
                        , "http://download.java.net/maven/2")

                , mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.4.0")
                , mavenBundle("org.osgi", "org.osgi.compendium", "4.2.0")

                , mavenBundle("eu.iksproject", "eu.iksproject.fise.servicesapi", IKS_VERSION)
                , mavenBundle("eu.iksproject", "eu.iksproject.fise.jobmanager", IKS_VERSION)
                , mavenBundle("eu.iksproject", "eu.iksproject.fise.standalone", IKS_VERSION)
                , mavenBundle("eu.iksproject", "eu.iksproject.fise.jersey", IKS_VERSION)

                , mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.4")
                , mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.4")

                , mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.2.4")
                , mavenBundle("org.apache.felix", "org.apache.felix.http.bundle", "2.0.4")

                , mavenBundle("javax.ws.rs", "jsr311-api", "1.1.1")
                , mavenBundle("com.sun.jersey", "jersey-core", JERSEY_VERSION)
                , mavenBundle("com.sun.jersey", "jersey-server", JERSEY_VERSION)
                , mavenBundle("com.sun.jersey", "jersey-client", JERSEY_VERSION)
                , mavenBundle("com.sun.jersey.test.osgi.http-service-tests", "http-service-test-bundle", JERSEY_VERSION)

                , mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jetty-bundle", "6.1.14_2")
                , mavenBundle("org.ops4j.pax.web", "pax-web-api", PAX_WEB_VERSION)
                , mavenBundle("org.ops4j.pax.web", "pax-web-spi", PAX_WEB_VERSION)
                , mavenBundle("org.ops4j.pax.web", "pax-web-runtime", PAX_WEB_VERSION)
                , mavenBundle("org.ops4j.pax.web", "pax-web-jetty", PAX_WEB_VERSION)

                , mavenBundle("commons-io", "commons-io")
                , mavenBundle("org.apache.clerezza", "org.apache.clerezza.rdf.core", "0.12-incubating-SNAPSHOT")
        );
    }

    /**
     * You will get a list of bundles installed by default
     * plus your testcase, wrapped into a bundle called pax-exam-probe
     */
    @Test
    public void testInstalledBundles() {
        for (Bundle b : bundleContext.getBundles()) {
            System.out.println("Bundle " + b.getBundleId() + " : " + b.getSymbolicName()
                    + " state: " + b.getState());
            assertEquals(32, b.getState());
        }
    }

    @Test
    public void testHttpService() {
        ServiceReference ref =
                bundleContext.getServiceReference(HttpService.class.getName());
        assertNotNull(ref);
        HttpService httpService = (HttpService) bundleContext.getService(ref);
        assertNotNull(httpService);
    }

    //@Test
    public void testStandaloneService() {
        // TODO: doesn't work, service reference is not found.
        ServiceReference ref =
                bundleContext.getServiceReference(JettyServer.class.getName());
        JettyServer svcObj = (JettyServer) bundleContext.getService(ref);
        // Functional tests should go there...
    }

}
