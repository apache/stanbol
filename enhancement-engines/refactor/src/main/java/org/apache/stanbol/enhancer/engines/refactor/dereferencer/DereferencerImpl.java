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
package org.apache.stanbol.enhancer.engines.refactor.dereferencer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author andrea.nuzzolese
 * 
 */
@Component(immediate = true)
@Service(Dereferencer.class)
public class DereferencerImpl implements Dereferencer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public InputStream resolve(String location) throws FileNotFoundException {
        InputStream inputStream = null;
        try {
            URI uri = new URI(location);
            if (uri.isAbsolute()) {
                log.info("The Refactor is fecthing on-line the graph associated to the resource " + location);
                URL url = new URL(location);

                URLConnection connection = url.openConnection();
                connection.addRequestProperty("Accept", "application/rdf+xml");
                inputStream = connection.getInputStream();
            } else {
                log.info("The Refactor is fecthing on your local machine the graph associated to the resource "
                         + location);
                inputStream = new FileInputStream(location);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new FileNotFoundException();
        } catch (IOException e) {
            throw new FileNotFoundException();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new FileNotFoundException();
        }

        return inputStream;

    }

    public boolean isAbsoluteLocation(String location) {
        URI uri;

        try {
            uri = new URI(location);
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return false;

    }

    public String getLocalName(String location) throws FileNotFoundException {
        String localName = null;
        try {
            URI uri = new URI(location);
            if (uri.isAbsolute()) {
                localName = location;
            } else {
                System.out.println("URL : not absolute " + location);
                File file = new File(location);
                localName = file.getName();
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new FileNotFoundException();
        }

        return localName;

    }

}
