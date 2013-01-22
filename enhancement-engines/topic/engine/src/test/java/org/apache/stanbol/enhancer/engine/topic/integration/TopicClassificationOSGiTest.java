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
package org.apache.stanbol.enhancer.engine.topic.integration;

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.apache.stanbol.enhancer.engine.topic.TopicClassificationEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;

// Disabled integration test because SCR configuration factory init is crashing
//@RunWith(JUnit4TestRunner.class)
//@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class TopicClassificationOSGiTest {

    @Inject
    BundleContext context;

    // inject http service to ensure that jetty init thread is finished before tearing down otherwise the test
    // harness will crash
    @Inject
    HttpService httpService;

    @Before
    public void registerSolrCore() throws Exception {
        // TODO
    }

    @org.ops4j.pax.exam.junit.Configuration()
    public Option[] config() {
        return options(
            systemProperty("org.osgi.service.http.port").value("8181"),
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),
            mavenBundle("commons-codec", "commons-codec").versionAsInProject(),
            mavenBundle("org.apache.httpcomponents", "httpcore-osgi").versionAsInProject(),
            mavenBundle("commons-io", "commons-io").versionAsInProject(),
            // for some reason: versionAsInProject does not work for the following:
            mavenBundle("org.apache.clerezza.ext", "com.ibm.icu").version("0.5-incubating-SNAPSHOT"),
            mavenBundle("org.wymiwyg", "wymiwyg-commons-core").versionAsInProject(),
            mavenBundle("org.apache.commons", "commons-compress").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.http.jetty").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.scr").versionAsInProject(),
            mavenBundle("org.apache.stanbol", "org.apache.stanbol.commons.stanboltools.datafileprovider")
                    .versionAsInProject(),
            mavenBundle("org.apache.stanbol", "org.apache.stanbol.commons.solr.core").versionAsInProject(),
            mavenBundle("org.apache.stanbol", "org.apache.stanbol.commons.solr.managed").versionAsInProject(),
            mavenBundle("org.apache.clerezza", "utils").versionAsInProject(),
            mavenBundle("org.apache.clerezza", "rdf.core").versionAsInProject(),

            mavenBundle("org.apache.stanbol", "org.apache.stanbol.enhancer.servicesapi").versionAsInProject(),

            // TODO: instead of deploying a previous version of the bundle built by maven, find a way to wrap
            // the engine class as a bundle directly in this test runtime.
            mavenBundle("org.apache.stanbol", "org.apache.stanbol.enhancer.engine.topic")
                    .versionAsInProject(), junitBundles(), felix(), equinox());
        // Note: the equinox tests can only be run if the test container is switched to the slower non-native,
        // implementation
    }

    // Disabled integration test because SCR configuration factory init is crashing
    // @Test
    public void testTopicClassification() throws Exception {
        System.out.println("Running test on bundle: " + context.getBundle());
        ServiceReference reference = context.getServiceReference(ConfigurationAdmin.class.getName());

        ConfigurationAdmin configAdmin = (ConfigurationAdmin) context.getService(reference);
        Configuration config = configAdmin.createFactoryConfiguration(TopicClassificationEngine.class
                .getName());
        Dictionary<String,String> parameters = new Hashtable<String,String>();
        parameters.put(EnhancementEngine.PROPERTY_NAME, "testclassifier");
        // TODO: put the coreId of the solr server registered in @Before
        config.update(parameters);

        // TODO: use a service track to wait for the registration of the service
        ServiceReference topicEngineReference = context.getServiceReference(TopicClassificationEngine.class
                .getName());
        TestCase.assertNotNull(topicEngineReference);
        TopicClassificationEngine engine = (TopicClassificationEngine) context
                .getService(topicEngineReference);
        TestCase.assertNotNull(engine);
        // TODO: test classification here
    }
}
