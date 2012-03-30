/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stanbol.webdav;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.webdav.resources.SimpleResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bradmcevoy.http.ApplicationConfig;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.MiltonServlet;

/**
 * The <code>AbstractMiltonDavServlet</code> is an abstract base
 * <code>MiltonServlet</code> used as the basis for the two Sling
 * implementations:
 * <dl>
 * <dt>{@link MiltonDavServlet}</dt>
 * <dd>A servlet registered directly with the OSGi Http Service to serve WebDAV
 * requests outside of Sling in its own URL space</dd>
 * <dt>{@link MiltonDavSlingServlet}</dt>
 * <dd>A servlet registered with Sling (using the whiteboard pattern) to provide
 * WebDAV services through the Sling request processing infrastructure</dd>
 * </dl>
 * <p>
 * This base class implementation only overwrites the
 * {@link #instantiate(String)} method to ensure using the bundle's class loader
 * and to not create a memory hole. The base class unfortunately uses the static
 * <code>Class.forName(String)</code> which is well-known for this problem.
 */
@Component(componentAbstract=true)
public abstract class AbstractMiltonDavServlet extends MiltonServlet {
	
	private Logger log = LoggerFactory.getLogger( AbstractMiltonDavServlet.class );
	
	@Reference
	private CollectionResource rootResource;

	
	@Override
    public void init( ServletConfig config ) throws ServletException {
        try {
            //this.config = config;
            init(new SimpleResourceFactory(rootResource), new SlingResponseHandler(), new ArrayList<String>());
         } catch( ServletException ex ) {
            log.error( "Exception starting milton servlet", ex );
            throw ex;
        } catch( Throwable ex ) {
            log.error( "Exception starting milton servlet", ex );
            throw new RuntimeException( ex );
        }
    }
	
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T instantiate(String className) throws ServletException {
    	throw new RuntimeException("this should not be invoked");
    }

}