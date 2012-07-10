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
package org.apache.stanbol.entityhub.servicesapi.site;

import java.io.IOException;
import java.io.InputStream;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Service used by {@link Site} to dereference {@link Representation}
 * for entity ids. Implementations of this interface are dependent on the
 * service provided by the referenced site.
 * @author Rupert Westenthaler
 *
 */
public interface EntityDereferencer {
    /**
     * The key used to define the baseUri of the service used for the
     * implementation of this interface.<br>
     * This constants actually uses the value of {@link SiteConfiguration#ACCESS_URI}
     */
    String ACCESS_URI = ReferencedSiteConfiguration.ACCESS_URI;

    /**
     * The base uri used to access this site
     * @return
     */
    String getAccessUri();
    /**
     * Whether the parsed entity ID can be dereferenced by this Dereferencer or
     * not.<br>
     * The implementation may not directly check if the parsed URI is present by
     * a query to the site, but only check some patterns of the parsed URI.
     * @param uri the URI to be checked
     * @return <code>true</code> of URIs of that kind can be typically dereferenced
     * by this service instance.
     */
    boolean canDereference(String uri);
    /**
     * Generic getter for the data of the parsed entity id
     * @param uri the entity to dereference
     * @param contentType the content type of the data
     * @return the data or <code>null</code> if not present or wrong data type
     * TODO: we should use exceptions instead of returning null!
     */
    InputStream dereference(String uri,String contentType) throws IOException;
    /**
     * Dereferences the Representation of the referred Entity
     * @param uri the uri of the referred entity
     * @return the representation of <code>null</code> if no Entity was found
     * for the parsed entity reference.
     */
    Representation dereference(String uri) throws IOException;

//    /**
//     * NOTE Moved to ReferencedSite
//     * @return
//     */
//    Dictionary<String, ?> getSiteConfiguration();

}
