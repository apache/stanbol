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
package org.apache.stanbol.ontologymanager.registry.api;

import org.apache.stanbol.ontologymanager.registry.api.model.Library;

/**
 * Thrown whenever there is a request for the contents of an ontology library which have not been loaded yet
 * (e.g. due to lazy loading policies). Developers who catch this exception may, for example, decide to load
 * the library contents.
 * 
 * @author alexdma
 */
public class LibraryContentNotLoadedException extends RegistryContentException {

    /**
     * 
     */
    private static final long serialVersionUID = 4442769260608567120L;

    private Library library;

    /**
     * Creates a new instance of {@link LibraryContentNotLoadedException}.
     * 
     * @param library
     *            the ontology library that caused the exception.
     */
    public LibraryContentNotLoadedException(Library library) {
        super(library.toString());
        this.library = library;
    }

    /**
     * Returns the library whose content was requested that is not loaded yet.
     * 
     * @return the ontology library that caused the exception.
     */
    public Library getLibrary() {
        return library;
    }

}
