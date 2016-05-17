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
package org.apache.stanbol.enhancer.nlp.morpho;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.IRI;
/**
 * Defines verb tenses as defined by the
 * <a href="">OLIA</a> Ontology.<p>
 * The hierarchy is represented by this enumeration.
 * The {@link Set} of parent concepts is accessible via
 * the {@link #getParent()} and {@link #getTenses()}.
 */
public enum Tense {
    NotAnchored("NotTemporallyAnchored"),
    Absolute("AbsoluteTense"),
    CloseFuture(Absolute),
    Future(Absolute),
    HodiernalFuture(Future),
    ImmediateFuture(Future),
    NearFuture(Future),
    PostHodiernalFuture(Future),
    RemoteFuture(Future),
    SimpleFuture(Future),
    Past(Absolute),
    HesternalPast(Past),
    HodiernalPast(Past),
    ImmediatePast(Past),
    RecentPast(Past),
    RemotePast(Past),
    SimplePast(Past),
    StillPast(Past),
    Imperfect(StillPast),
    Aorist(Past),
    Perfect(Absolute),
    PreHodiernalPast(Absolute),
    Present(Absolute),
    Transgressive(Present),
    AbsoluteRelative("AbsoluteRelativeTense"),
    FutureInFuture(AbsoluteRelative),
    FutureInPast(AbsoluteRelative),
    PastPerfect("PastPerfectTense",AbsoluteRelative),
    PastInFuture(AbsoluteRelative),
    PluperfectTense(AbsoluteRelative),
    Relative("RelativeTense"),
    FuturePerfect(Relative),
    RelativePast(Relative),
    RelativePresent(Relative),
    ;
    static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";
    IRI uri;
    Tense parent;
    
    Tense() {
        this(null,null);
    }
    Tense(Tense parent) {
        this(null,parent);
    }

    Tense(String name) {
        this(name,null);
    }
    Tense(String name,Tense parent) {
        uri = new IRI(OLIA_NAMESPACE + (name == null ? name() : name));
        this.parent = parent;
    }
    /**
     * Getter for the parent tense (e.g.
     * {@link Tense#Future} for {@link Tense#NearFuture})
     * @return the direct parent or <code>null</code> if none
     */
    public Tense getParent() {
        return parent;
    }
    
    /**
     * Returns the transitive closure over
     * the {@link #getParent() parent} tenses including
     * this instance (e.g.
     * [{@link Tense#Absolute}, {@link Tense#Future}, {@link Tense#NearFuture}] for
     * {@link Tense#NearFuture}).<p>
     * Implementation Note: Internally an {@link EnumSet} is used 
     * to represent the transitive closure. As the iteration order
     * of an {@link EnumSet} is based on the natural order (the
     * {@link Enum#ordinal()} values) AND the ordering of the
     * Tenses in this enumeration is from generic to specific the
     * ordering of the Tenses in the returned Set is guaranteed
     * to be from generic to specific.
     * @return the transitive closure over parent
     * tenses.
     */
    public Set<Tense> getTenses() {
        return transitiveClosureMap.get(this);
    }
    
    public IRI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri.getUnicodeString();
    }
    
    /**
     * This is needed because one can not create EnumSet instances before the
     * initialization of an Enum has finished.<p>
     * To keep using the much faster {@link EnumSet} a static member initialised
     * in an static {} block is used as a workaround. The {@link Tense#getTenses()}
     * method does use this static member instead of a member variable
     */
    private static final Map<Tense,Set<Tense>> transitiveClosureMap;
    
    static {
        transitiveClosureMap = new EnumMap<Tense,Set<Tense>>(Tense.class);
        for(Tense tense : Tense.values()){
            Set<Tense> parents = EnumSet.of(tense);
            Set<Tense> transParents = transitiveClosureMap.get(tense.getParent());
            if(transParents != null){
                parents.addAll(transParents);
            } else if(tense.getParent() != null){
                parents.add(tense.getParent());
            } // else no parent
            transitiveClosureMap.put(tense, parents);
        }
    }
}
