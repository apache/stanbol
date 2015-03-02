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
package org.apache.stanbol.commons.freemarker.impl;

import freemarker.cache.TemplateLoader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Service provides an {@link TemplateLoader} that provides templates
 * relative to the {@link #TEMPLATES_PATH_IN_BUNDLES}.
 */
@Component(immediate=true)
@Service(TemplateLoader.class)
public class BundleTemplateLoader implements TemplateLoader{
	
	private static final String TEMPLATES_PATH_IN_BUNDLES = "templates/";

	private static final Logger log = LoggerFactory.getLogger(BundleTemplateLoader.class);
	
	private final Collection<Bundle> bundles = new HashSet<Bundle>();
	
	private BundleListener bundleListener = new BundleListener() {
		
		@Override
		public void bundleChanged(BundleEvent event) {
			if ((event.getType() == BundleEvent.STARTED) && containsTemplates(event.getBundle())) {
				bundles.add(event.getBundle());
			} else {
				bundles.remove(event.getBundle());
			}
		}
	};
	
		
	@Override
	public Reader getReader(Object templateSource, String encoding)
			throws IOException {
		URL templateUrl = (URL) templateSource;
		return new InputStreamReader(templateUrl.openStream(), encoding);
	}
	
	@Override
	public long getLastModified(Object templateSource) {
		// not known
		return -1;
	}
	
	@Override
	public Object findTemplateSource(String name) throws IOException {
		if (!name.endsWith(".ftl")) {
			name = name +".ftl";
		}
		final String path = TEMPLATES_PATH_IN_BUNDLES+name;
		for (Bundle bundle : bundles) {
			URL res = bundle.getResource(path);
			if (res != null) {
				return res;
			}
		}
		log.warn("Template "+name+" not known");
		return null;
	}
	
	@Override
	public void closeTemplateSource(Object templateSource) throws IOException {

		
	}
	
	@Activate
	protected void activate(final ComponentContext context) {
		final Bundle[] registeredBundles = context.getBundleContext().getBundles();
		for (int i = 0; i < registeredBundles.length; i++) {
			if ((registeredBundles[i].getState() == Bundle.ACTIVE) 
					&& containsTemplates(registeredBundles[i])) {
				bundles.add(registeredBundles[i]);
			}
		}	
		context.getBundleContext().addBundleListener(bundleListener);
	}

	@Deactivate
	protected void deactivate(final ComponentContext context) {
		context.getBundleContext().removeBundleListener(bundleListener);
	}
	
    private boolean containsTemplates(Bundle bundle) {
        try {
            return bundle.getResource(TEMPLATES_PATH_IN_BUNDLES) != null;
        } catch(NullPointerException e){
            //sometimes this call caused a
            //java.lang.NullPointerException
            //    at org.apache.felix.framework.BundleRevisionImpl.getResourceLocal(BundleRevisionImpl.java:495)
            //    at org.apache.felix.framework.BundleWiringImpl.findClassOrResourceByDelegation(BundleWiringImpl.java:1472)
            //    at org.apache.felix.framework.BundleWiringImpl.getResourceByDelegation(BundleWiringImpl.java:1400)
            //    at org.apache.felix.framework.Felix.getBundleResource(Felix.java:1600)
            //    at org.apache.felix.framework.BundleImpl.getResource(BundleImpl.java:639)
            //    at org.apache.stanbol.commons.freemarker.impl.BundleTemplateLoader.containsTemplates(BundleTemplateLoader.java:117)
            log.warn(" ... unable to check for Path "+TEMPLATES_PATH_IN_BUNDLES
                +" in Bundle "+ bundle, e);
            return false;
        }
    }
	
}
