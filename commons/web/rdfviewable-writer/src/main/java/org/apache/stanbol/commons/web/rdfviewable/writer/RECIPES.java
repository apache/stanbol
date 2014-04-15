/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.commons.web.rdfviewable.writer;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Used ontologicaal terms from recipes ontology
 * 
 * TODO: generate this with the maven plugin in separate projects
 */
public final class RECIPES {

    /**
     * Restrict instantiation
     */
    private RECIPES() {}

    public static final UriRef Recipe = new UriRef("http://vocab.netlabs.org/recipe#Recipe");
    
    public static final UriRef recipeDomain = new UriRef("http://vocab.netlabs.org/recipe#recipeDomain");
    
    public static final UriRef ingredient = new UriRef("http://vocab.netlabs.org/recipe#ingredient");
    
    public static final UriRef ingredientProperty = new UriRef("http://vocab.netlabs.org/recipe#ingredientProperty");
    
    public static final UriRef ingredientInverseProperty = new UriRef("http://vocab.netlabs.org/recipe#ingredientInverseProperty");
}
