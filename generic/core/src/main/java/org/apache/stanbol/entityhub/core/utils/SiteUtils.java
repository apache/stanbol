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
package org.apache.stanbol.entityhub.core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

//import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.site.License;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
/**
 * Utilities used for the Implementation of ReferenceSite and ManagedSite
 * @author Rupert Westenthaler
 *
 */
public final class SiteUtils {

    private SiteUtils(){}
    /**
     * Initialises the {@link Entity#getMetadata()} with the metadata of the
     * site and entity specific metadata.
     * @param entity the entity
     * @param siteMetadata the site metadata
     * @param entitySpecific metadata
     */
    public static void initEntityMetadata(Entity entity, Map<String,Object> siteMetadata, Map<String,Object> entityMetadata) {
        Representation metadata = entity.getMetadata();
        if(siteMetadata != null){
            for(Entry<String,Object> entry : siteMetadata.entrySet()){
                metadata.add(entry.getKey(), entry.getValue());
            }
        }
        if(entityMetadata != null){
            for(Entry<String,Object> entry : entityMetadata.entrySet()){
                metadata.add(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * Sites need to provide Metadata about managed Entities (e.g. license,
     * attribution, ...) those information are provided by the configuration
     * of the site and need to be included with each requested entity.<p>
     * This method implements the extracting of those information from the
     * configuration.
     * @param siteConfiguration the configuration
     * @param vf The {@link ValueFactory} used to create values of the returned
     * Map.
     * @return the metadata for the parsed configuration
     */
    public static Map<String,Object> extractSiteMetadata(SiteConfiguration siteConfiguration, ValueFactory vf) {
        Map<String,Object> siteMetadata = new HashMap<String,Object>();
        if(siteConfiguration.getAttribution() != null){
            siteMetadata.put(NamespaceEnum.cc.getNamespace()+"attributionName", 
                vf.createText(siteConfiguration.getAttribution()));
        }
        if(siteConfiguration.getAttributionUrl() != null){
            siteMetadata.put(NamespaceEnum.cc.getNamespace()+"attributionURL", 
                vf.createReference(siteConfiguration.getAttributionUrl()));
        }
        //add the licenses
        if(siteConfiguration.getLicenses() != null){
            for(License license : siteConfiguration.getLicenses()){
                if(license.getUrl() != null){
                    siteMetadata.put(NamespaceEnum.cc.getNamespace()+"license", 
                        vf.createReference(license.getUrl()));
                } else if(license.getText() != null){
                    siteMetadata.put(NamespaceEnum.cc.getNamespace()+"license", 
                        vf.createText(license.getText()));
                }
                //if defined add the name to dc:license
                if(license.getName() != null){
                    siteMetadata.put(NamespaceEnum.dcTerms.getNamespace()+"license", 
                        vf.createText(license.getName()));
                }
                //link to the license via cc:license
            }
        }
        return siteMetadata;
    }
}
