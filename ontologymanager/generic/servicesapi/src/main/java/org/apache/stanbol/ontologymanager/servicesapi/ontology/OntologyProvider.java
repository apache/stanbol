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
package org.apache.stanbol.ontologymanager.servicesapi.ontology;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.stanbol.ontologymanager.ontonet.api.OntologyNetworkConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.collector.ImportManagementPolicy;
import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * A system responsible for maintaining stored ontologies.<br>
 * <br>
 * TODO see if full CRUD operation support is necessary.
 * 
 * @author alexdma
 * 
 * @param <S>
 *            the storage system actually used by this provider.
 */
public interface OntologyProvider<S> {

    /**
     * The status of a stored ontology entry in the {@link OntologyProvider}.
     * 
     * @author alexdma
     * 
     */
    public enum Status {

        /**
         * A non-null ontology object can be obtained by querying the ontology provider for this entry.
         */
        MATCH,

        /**
         * No such entry is registered as either a primary key or an alias in the ontology provider.
         */
        NO_MATCH,

        /**
         * The entry is registered and assigned a graph name, but the corresponding graph cannot be found.
         */
        ORPHAN,

        /**
         * The entry is registered but not assigned a graph.
         */
        UNCHARTED

    }

    /**
     * The key used to configure the prefix to be used for addressing ontologies stored by this provider.
     */
    public String GRAPH_PREFIX = "org.apache.stanbol.ontologymanager.ontonet.graphPrefix";

    /**
     * The key used to configure the import management policy.
     */
    public String IMPORT_POLICY = "org.apache.stanbol.ontologymanager.ontonet.importPolicy";

    /**
     * The key used to configure the identifier of the meta-level graph
     */
    public String META_GRAPH_ID = "org.apache.stanbol.ontologymanager.ontonet.metaGraphId";

    /**
     * The key used to configure the default import resolution policy for this provider.
     */
    public String RESOLVE_IMPORTS = "org.apache.stanbol.ontologymanager.ontonet.resolveImports";

    /**
     * The key used to configure the default import failure policy for this provider.
     */
    public String MISSING_IMPORTS_FAIL = "org.apache.stanbol.ontologymanager.ontonet.failOnMissingImports";

    boolean addAlias(OWLOntologyID primaryKey, OWLOntologyID alias);

    OWLOntologyID createBlankOntologyEntry(OWLOntologyID publicKey);

    /**
     * Gets the policy adopted by this provider whenever an import statement is found in an ontology <i>that
     * has already been loaded</i> (e.g. when exporting it). It does <b>not</b> influence how the system
     * should <i>resolve</i> imports of newly found ontologies.
     * 
     * @return the import management policy.
     */
    ImportManagementPolicy getImportManagementPolicy();

    /**
     * Gets a string that can be used to directly access the ontology whose logical identifier is
     * <tt>ontologyIRI</tt>.
     * 
     * @deprecated public keys are now of type {@link OWLOntologyID} and should match ontologyIri, or
     *             derivative versioned IDs, whenever possible.
     * @see #listVersions(IRI)
     * @see #listAliases(OWLOntologyID)
     * @param ontologyIri
     *            the logical identifier of the ontology.
     * @return the public key (note that it might be different from the graph name).
     */
    String getKey(IRI ontologyIri);

    /**
     * Gets a string that can be used to directly access the ontology whose logical identifier is
     * <tt>ontologyId</tt>.
     * 
     * @deprecated public keys are now of type {@link OWLOntologyID} and should match ontologyId. To obtain
     *             alternate public keys (aliases) or versioned public keys, see
     *             {@link #listAliases(OWLOntologyID)} and {@link #listVersions(IRI)} respectively.
     * @see #listVersions(IRI)
     * @see #listAliases(OWLOntologyID)
     * 
     * @param ontologyId
     *            the logical identifier of the ontology.
     * @return the public key (note that it might be different from the graph name).
     */
    String getKey(OWLOntologyID ontologyId);

    /**
     * Returns the graph that stores all the information on stored ontologies. Whether the returned triple
     * collection is a {@link ImmutableGraph} or a {@link Graph} depends on the provider's policy on allowing external
     * modifications to the meta-level graph or not.
     * 
     * @param returnType
     * @return
     */
    <O extends Graph> O getMetaGraph(Class<O> returnType);

    @Deprecated
    OWLOntologyID getOntologyId(String storageKey);

    @Deprecated
    OntologyNetworkConfiguration getOntologyNetworkConfiguration();

    /**
     * XXX This method is temporary until {@link Multiplexer} becomes an OSGi component.
     * 
     * @return
     */
    Multiplexer getOntologyNetworkDescriptor();

    /**
     * Gets the key of the ontology with the supplied ontology ID. Note that both ontoloeyIRI and versionIRI
     * (if present) must match, otherwise it will return null.
     * 
     * @deprecated public keys are now of type {@link OWLOntologyID} and should match ontologyId. To obtain
     *             alternate public keys (aliases) or versioned public keys, see
     *             {@link #listAliases(OWLOntologyID)} and {@link #listVersions(IRI)} respectively.
     * @see #listVersions(IRI)
     * @see #listAliases(OWLOntologyID)
     * 
     * @param ontologyId
     * @return
     */
    String getPublicKey(OWLOntologyID ontologyId);

    /**
     * Gets the set of all the strings that can be used to access the ontologies stored by this provider.
     * 
     * @deprecated Please use {@link #listPrimaryKeys()} instead.
     * 
     * @return the ontology key set.
     */
    Set<OWLOntologyID> getPublicKeys();

    Status getStatus(OWLOntologyID publicKey);

    /**
     * Returns the storage system used by this ontology provider (e.g. a {@link TcProvider} or an
     * {@link OWLOntologyManager}).
     * 
     * @return the ontology store.
     */
    S getStore();

    /**
     * Same as {@link OntologyProvider#getStoredOntology(String, Class)}, but instead of the internal key it
     * uses an IRI that <i>publicly</i> identifies or references an ontology. This can be, ordered by
     * preference most relevant first:
     * <ol>
     * <li>The version IRI
     * <li>The ontology IRI
     * <li>The physical IRI, if different from the above
     * </ol>
     * 
     * @deprecated
     * @param reference
     *            the IRI that references the ontology.
     * @param returnType
     *            the desired type that the method should return, if supported, otherwise an
     *            {@link UnsupportedOperationException} is thrown. Can be null, in which case a default return
     *            type is chosen.
     * @return the stored ontology in the desired format, or null if no such ontology is managed by the
     *         provider.
     */
    <O> O getStoredOntology(IRI reference, Class<O> returnType);

    /**
     * Same as {@link OntologyProvider#getStoredOntology(String, Class, boolean)}, but instead of the internal
     * key it uses an IRI that <i>publicly</i> identifies or references an ontology. This can be, ordered by
     * preference most relevant first:
     * 
     * @deprecated
     * @param reference
     *            the IRI that references the ontology.
     * @param returnType
     *            The expected type for the returned ontology object. If null, the provider will arbitrarily
     *            select a supported return type. If the supplied type is not supported (i.e. not assignable
     *            to any type contained in the result of {@link #getSupportedReturnTypes()}) an
     *            {@link UnsupportedOperationException} should be thrown.
     * @param forceMerge
     *            if true, the ontology will be merged with all its imports, thus overriding the import
     *            management policy set for this provider.
     * @return the stored ontology in the desired format, or null if no such ontology is managed by the
     *         provider.
     */
    <O> O getStoredOntology(IRI reference, Class<O> returnType, boolean merge);

    <O> O getStoredOntology(OWLOntologyID reference, Class<O> returnType);

    <O> O getStoredOntology(OWLOntologyID reference, Class<O> returnType, boolean merge);

    /**
     * Returns a stored ontology that is internally identified by the provided key.
     * 
     * @deprecated
     * @param key
     *            the key used to identify the ontology in this provider. They can or cannot coincide with the
     *            logical and/or physical IRI of the ontology.
     * @param returnType
     *            The expected type for the returned ontology object. If null, the provider will arbitrarily
     *            select a supported return type. If the supplied type is not supported (i.e. not assignable
     *            to any type contained in the result of {@link #getSupportedReturnTypes()}) an
     *            {@link UnsupportedOperationException} should be thrown.
     * @return the stored ontology in the desired format, or null if no such ontology is managed by the
     *         provider.
     */
    <O> O getStoredOntology(String key, Class<O> returnType);

    /**
     * Returns a stored ontology that is internally identified by the provided key.
     * 
     * @deprecated
     * @param key
     *            the key used to identify the ontology in this provider. They can or cannot coincide with the
     *            logical and/or physical IRI of the ontology.
     * @param returnType
     *            The expected type for the returned ontology object. If null, the provider will arbitrarily
     *            select a supported return type. If the supplied type is not supported (i.e. not assignable
     *            to any type contained in the result of {@link #getSupportedReturnTypes()}) an
     *            {@link UnsupportedOperationException} should be thrown.
     * @param forceMerge
     *            if true, the ontology will be merged with all its imports, thus overriding the import
     *            management policy set for this provider.
     * @return the stored ontology in the desired format, or null if no such ontology is managed by the
     *         provider.
     */
    <O> O getStoredOntology(String key, Class<O> returnType, boolean forceMerge);

    /**
     * Returns an array containing the most specific types for ontology objects that this provider can manage
     * and return on a call to {@link #getStoredOntology(String, Class)}.
     * 
     * @return the supported ontology return types.
     */
    Class<?>[] getSupportedReturnTypes();

    /**
     * A convenience method for checking the availability of an ontology given its (physical or logical) IRI.
     * It is typically more efficient than calling {@link #getStoredOntology(IRI, Class)} and null-checking
     * the result.
     * 
     * @deprecated
     * @param ontologyIri
     * @return
     */
    boolean hasOntology(IRI ontologyIri);

    /**
     * Checks if an ontology with the specified OWL ontology ID is in the ontology provider's store.<br>
     * <br>
     * Implementations are typically faster than calling {@link #getStoredOntology(IRI, Class)} and checking
     * if the returned value is not null.
     * 
     * @deprecated the notion of "having an ontology" has become ambiguous. Please use
     *             {@link #getStatus(OWLOntologyID)} and verify its value.
     * 
     * @param publicKey
     *            the ontology id. If there is both an ontology IRI and a version IRI, both must match the
     *            ontology provider's records in order to return true. Otherwise, it will return true iff
     *            <i>any</i> match with the ontology IIR is found, no matter its version IRI.
     * @return true iff an ontology with the supplied id is in the provider's store.
     */
    boolean hasOntology(OWLOntologyID publicKey);

    /**
     * Returns all the alternate public keys of the ontology identified by the supplied public key. These
     * could include, for example, the physical URLs they were, or can be, retrieved from. The supplied public
     * key is not included in the returned set, and it needs not be the primary key. It can be an alias
     * itself, in whih case the primary key will be included in the results.
     * 
     * @param publicKey
     *            the public key of the ontology. It could be an alias itself.
     * @return the matching versions of stored ontologies.
     */
    Set<OWLOntologyID> listAliases(OWLOntologyID publicKey);

    Set<OWLOntologyID> listAllRegisteredEntries();

    Set<OWLOntologyID> listOrphans();

    /**
     * Returns the public keys of all the ontologies stored by this provider. Only primary keys are returned:
     * aliases are not included.
     * 
     * @return all the ontology public keys.
     */
    Set<OWLOntologyID> listPrimaryKeys();

    /**
     * Returns all the public keys of the ontologies whose ontology IRI matches the one supplied,a nd which
     * differ by version IRI. Only primary keys are returned: aliases are not included.
     * 
     * @param ontologyIri
     *            the ontology IRI to match
     * @return the matching versions of stored ontologies.
     */
    Set<OWLOntologyID> listVersions(IRI ontologyIri);

    /**
     * Retrieves an ontology by reading its content from a data stream and stores it using the storage system
     * attached to this provider. A key that can be used to identify the ontology in this provider is returned
     * if successful.
     * 
     * @param data
     *            the ontology content.
     * @param formatIdentifier
     *            the MIME type of the expected serialization format of this ontology. If null, all supported
     *            formats will be tried until all parsers fail or one succeeds. Whether the supplied format
     *            will be the only one to be attempted, or simply the preferred one, is left to arbitration
     *            and is implementation-dependent.
     * @param force
     *            if true, all mappings provided by the offline configuration will be ignored (both for the
     *            root ontology and its recursive imports) and the provider will forcibly try to resolve the
     *            location IRI. If some remote import is found, the import policy is aggressive and Stanbol is
     *            set on offline mode, this method will fail.
     * @return a key that can be used to retrieve the stored ontology afterwards, or null if loading/storage
     *         failed. If it was possible to set it as such, it will be the same as <tt>preferredKey</tt>.
     * @throws IOException
     *             if all attempts to load the ontology failed.
     * @throws UnsupportedFormatException
     *             if no parsers are able to parse the supplied format (or the actual file format).
     */
    OWLOntologyID loadInStore(InputStream data,
                              String formatIdentifier,
                              boolean force,
                              Origin<?>... references) throws IOException, UnsupportedFormatException;

    /**
     * Retrieves an ontology physically located at <code>location</code> (unless mapped otherwise by the
     * offline configuration) and stores it using the storage system attached to this provider. A key that can
     * be used to identify the ontology in this provider is returned if successful.
     * 
     * @param location
     *            the physical IRI where the ontology is located.
     * @param formatIdentifier
     *            the MIME type of the expected serialization format of this ontology. If null, all supported
     *            formats will be tried until all parsers fail or one succeeds. Whether the supplied format
     *            will be the only one to be attempted, or simply the preferred one, is left to arbitration
     *            and is implementation-dependent.
     * @param force
     *            if true, all mappings provided by the offline configuration will be ignored (both for the
     *            root ontology and its recursive imports) and the provider will forcibly try to resolve the
     *            location IRI. If the IRI is not local and Stanbol is set on offline mode, this method will
     *            fail.
     * @return a key that can be used to retrieve the stored ontology afterwards, or null if loading/storage
     *         failed.
     * @throws IOException
     *             if all attempts to load the ontology failed.
     * @throws UnsupportedFormatException
     *             if no parsers are able to parse the supplied format (or the actual file format).
     */
    OWLOntologyID loadInStore(IRI location, String formatIdentifier, boolean force, Origin<?>... references) throws IOException;

    /**
     * Stores an ontology that has already been loaded into an object. If the object is of a non-native yet
     * supported type, the ontology provider will try to perform a conversion prior to storing it.
     * 
     * @param ontology
     *            the ontology to be stored.
     * @param force
     *            if true, all mappings provided by the offline configuration will be ignored (both for the
     *            root ontology and its recursive imports) and the provider will forcibly try to resolve the
     *            location IRI. If some remote import is found, the import policy is aggressive and Stanbol is
     *            set on offline mode, this method will fail.
     * @return
     */
    OWLOntologyID loadInStore(Object ontology, boolean force, Origin<?>... references);

    /**
     * Removes the ontology identified by the supplied public key.
     * 
     * @param publicKey
     *            the public key for accessing the ontology.
     * @return true iff an ontology with that public key existed and was removed.
     */
    boolean removeOntology(OWLOntologyID publicKey) throws OntologyHandleException;

    /**
     * Sets the policy adopted by this provider whenever an import statement is found in an ontology <i>that
     * has already been loaded</i> (e.g. when exporting it). It does <b>not</b> influence how the system
     * should <i>resolve</i> imports of newly found ontologies.
     * 
     * @param policy
     *            the import management policy.
     */
    void setImportManagementPolicy(ImportManagementPolicy policy);

    /**
     * Tells this ontology provider that a stored ontology whose public key is <tt>publicKey</tt> can be (or
     * was) retrieved by dereferencing <tt>locator</tt>. If <tt>publicKey</tt>does not exist in the provider,
     * or if <tt>locator</tt> is already bound to an incompatible key, an {@link IllegalArgumentException}
     * will be thrown.
     * 
     * @deprecated this is now done by setting aliases. See {@link #addAlias(OWLOntologyID, OWLOntologyID)}.
     * 
     * @param locator
     *            a physical location for this ontology.
     * @param publicKey
     *            the public key of the stored ontology.
     */
    void setLocatorMapping(IRI locator, OWLOntologyID publicKey);

    /**
     * @deprecated this is now done by setting aliases. See {@link #addAlias(OWLOntologyID, OWLOntologyID)}.
     * 
     * @param locator
     * @param key
     */
    void setLocatorMapping(IRI locator, String key);

}
