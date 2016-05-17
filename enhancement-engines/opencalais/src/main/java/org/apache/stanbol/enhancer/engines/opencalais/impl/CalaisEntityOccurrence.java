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
package org.apache.stanbol.enhancer.engines.opencalais.impl;

import org.apache.clerezza.commons.rdf.RDFTerm;

/**
 * Stores the values extracted from the Calais entity data.
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */
public class CalaisEntityOccurrence {

    public RDFTerm id;
    public RDFTerm type;
    public String name;
    public Integer offset;
    public Integer length;
    public String exact;
    public String context;
    public Double relevance = -1.0;

    public CalaisEntityOccurrence() {
    }

    public String getTypeName() {
        if (type != null) {
            String tName = type.toString();
            int i = tName.lastIndexOf('/');
            if (i != -1) {
                return tName.substring(i + 1);
            }
        }
        return null;
    }

    public String toString() {
        return String.format("[id=%s, name=%s, exact=%s, type=%s, offset=%d, length=%d, context=\"%s\"]",
                id, name, exact, type, offset, length, context);
    }

}
