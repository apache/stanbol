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
package org.apache.stanbol.commons.owl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

public final class OWLOntologyManagerFactory {

    /**
     * Restrict instantiation
     */
    private OWLOntologyManagerFactory() {}

    /**
     * 
     * @param localDirs
     *            . If null or empty, no offline support will be added
     * @return
     */
    public static OWLOntologyManager createOWLOntologyManager(IRI[] locations) {
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        for (OWLOntologyIRIMapper mapper : getMappers(locations)) {
            mgr.addIRIMapper(mapper);
        }
        return mgr;
    }

    public static List<OWLOntologyIRIMapper> getMappers(IRI[] dirs) {
        List<OWLOntologyIRIMapper> mappers = new ArrayList<OWLOntologyIRIMapper>();
        if (dirs != null) {
            for (IRI path : dirs) {
                File dir = null;
                try {
                    dir = new File(path.toURI());
                } catch (Exception e) {
                    // Keep dir null
                }
                if (dir != null) {
                    if (dir.isDirectory()) mappers.add(new AutoIRIMapper(dir, true));
                    // We might want to construct other IRI mappers for regular files in the future...
                }
            }
        }
        return mappers;
    }

}
