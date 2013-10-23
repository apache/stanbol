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
package org.apache.stanbol.entityhub.it;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;

public class DbpediaDefaultdataConstants {
    
    private DbpediaDefaultdataConstants() { /* no instances */}
    
    public static final String DBPEDIA_SITE_ID = "dbpedia";
    public static final String DBPEDIA_SITE_PATH = "/entityhub/site/"+DBPEDIA_SITE_ID;

    
    public static final Set<String> DBPEDIA_DEFAULTDATA_REQUIRED_FIELDS;
    public static final Set<String> DBPEDIA_DEFAULTDATA_OPTIONAL_FIELDS;
    static {
        Set<String> required = new HashSet<String>();
        required.add(NamespaceEnum.rdfs+"label");
        //the new default data index supports surface forms
        required.add(NamespaceEnum.dbpediaOnt+"surfaceForm");
        required.add(NamespaceEnum.rdf+"type");
        required.add(NamespaceEnum.entityhub+"entityRank");
        DBPEDIA_DEFAULTDATA_REQUIRED_FIELDS = Collections.unmodifiableSet(required);

        Set<String> optional = new HashSet<String>();
        optional.add("http://purl.org/dc/terms/subject");
        optional.add("http://xmlns.com/foaf/0.1/depiction");
        optional.add("http://dbpedia.org/ontology/populationTotal");
        optional.add("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
        optional.add("http://www.w3.org/2003/01/geo/wgs84_pos#long");
        optional.add("http://www.w3.org/2003/01/geo/wgs84_pos#alt");
        optional.add("http://dbpedia.org/ontology/areaTotal");
        optional.add("http://dbpedia.org/ontology/birthDate");
        optional.add("http://dbpedia.org/ontology/deathDate");
        optional.add("http://xmlns.com/foaf/0.1/homepage");
        optional.add("http://www.w3.org/2000/01/rdf-schema#comment");
        DBPEDIA_DEFAULTDATA_OPTIONAL_FIELDS = Collections.unmodifiableSet(optional);
    }


}
