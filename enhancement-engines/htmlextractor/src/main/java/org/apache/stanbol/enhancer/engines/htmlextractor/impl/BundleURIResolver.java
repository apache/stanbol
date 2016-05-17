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
package org.apache.stanbol.enhancer.engines.htmlextractor.impl;

import java.net.URI;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BundleURIResolver.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */
public class BundleURIResolver implements URIResolver {

    public static Bundle BUNDLE;
    private static final Logger LOG = LoggerFactory.getLogger(BundleURIResolver.class);

    public Source resolve(String href, String base) throws TransformerException {
        //base contains the importing/including script, href the imported/included script
        //it is assumed here that href is a relative path
        //TODO href specifying an absolute URI
        if (base == null) {
            LOG.error("No base given for: " + href);
            return null;
        }
        String resource;
        try {
            LOG.debug("base: " + base + "\n" + "href: " + href);
            URL newUrl;
            if (base.startsWith("bundle:")) {
                URI baseURI = new URI(base);
                String path = baseURI.getPath();
                resource = path.substring(1, path.lastIndexOf('/') + 1) + href;
                newUrl = BUNDLE.getEntry(resource);
                LOG.debug("RDFTerm: " + resource);
                if (newUrl != null) {
                    return new StreamSource(newUrl.openStream(), newUrl.toString());
                } else {
                    return null;
                }
            }
            // for non-bundles assume that we have a normal URL as base
            resource = base.substring(0, base.lastIndexOf('/') + 1) + href;
            newUrl = new URL(resource);
            return new StreamSource(newUrl.openStream(), newUrl.toString());
        } catch (Exception ex) {
            throw new TransformerException("BundleURIResolver failed: " + ex.getMessage());
        }
    }

}
