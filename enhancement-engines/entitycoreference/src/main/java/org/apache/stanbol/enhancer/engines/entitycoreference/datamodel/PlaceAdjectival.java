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
package org.apache.stanbol.enhancer.engines.entitycoreference.datamodel;

import org.apache.clerezza.commons.rdf.IRI;


/**
 * Represents a place adjectival inside a {@link Span}.
 * 
 * @author Cristian Petroaca
 * 
 */
public class PlaceAdjectival {
    /**
     * The start index in the {@link Span}.
     */
    private int startIdx;

    /**
     * The end index in the {@link Span}.
     */
    private int endIdx;

    /**
     * The {@link IRI} in the {@link SiteManager} or {@link Entityhub} that this place adjectival points
     * to.
     */
    private IRI placeUri;

    public PlaceAdjectival(int startIdx, int endIdx, IRI placeUri) {
        this.startIdx = startIdx;
        this.endIdx = endIdx;
        this.placeUri = placeUri;
    }

    public IRI getPlaceUri() {
        return placeUri;
    }

    public int getStart() {
        return this.startIdx;
    }

    public int getEnd() {
        return this.endIdx;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + startIdx;
        result = prime * result + endIdx;
        result = prime * result + placeUri.hashCode();

        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        PlaceAdjectival other = (PlaceAdjectival) obj;

        return this.startIdx == other.startIdx && this.endIdx == other.endIdx
               && this.placeUri.equals(other.placeUri);
    }
}
