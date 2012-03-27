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
package org.apache.stanbol.cmsadapter.core.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.helper.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.impl.Util;

/**
 * Provides utility classes that are used during the RDF bridging process
 * 
 * @author suat
 * 
 */
public class RDFBridgeHelper {
    private static final Logger log = LoggerFactory.getLogger(RDFBridgeHelper.class);

    public static final UriRef RDF_TYPE = new UriRef(NamespaceEnum.rdf + "type");

    private static Pattern pattern = Pattern.compile("\"(.*?)\"");

    /**
     * Extracts a list of {@link NonLiteral} which indicates URIs of resources representing the root objects
     * in the graph e.g the object that do not have {@code CMSAdapterVocabulary#CMS_OBJECT_PARENT_REF}
     * property. Returned URIs should also be included in the candidate URIs passed as a parameter.
     * 
     * @param candidates
     *            candidate URI list
     * @param graph
     *            {@link MGraph} in which root URIs will be searched
     * @return list of {@link NonLiteral}s
     */
    public static List<NonLiteral> getRootObjetsOfGraph(List<NonLiteral> candidates, MGraph graph) {
        List<NonLiteral> roots = getRootObjectsOfGraph(graph);
        List<NonLiteral> rootsToBeReturned = new ArrayList<NonLiteral>();
        for (NonLiteral root : roots) {
            if (candidates.contains(root)) {
                rootsToBeReturned.add(root);
            }
        }
        return rootsToBeReturned;
    }

    /**
     * Extracts a list of {@link NonLiteral} which indicates URIs of resources representing the root objects
     * in the graph e.g the object that do not have {link CMSAdapterVocabulary#CMS_OBJECT_PARENT_REF}
     * property. Returned URIs should have {@link CMSAdapterVocabulary#CMS_OBJECT_NAME} assertions which have
     * value equal with the <code>path</code> parameter passed. In other words, this method determines the
     * root objects under the <code>path</code> specified.
     * 
     * @param path
     *            content repository path
     * @param graph
     *            {@link MGraph} in which root URIs will be searched
     * @return list of {@link NonLiteral}s
     */
    public static List<NonLiteral> getRootObjectsOfGraph(String path, MGraph graph) {
        List<NonLiteral> roots = getRootObjectsOfGraph(graph);
        List<NonLiteral> rootsToBeReturned = new ArrayList<NonLiteral>();
        for (NonLiteral root : roots) {
            if (isUnderAbsolutePath(path, root, graph)) {
                rootsToBeReturned.add(root);
            }
        }
        return rootsToBeReturned;
    }

    /**
     * Extracts a list of {@link NonLiteral} which indicates URIs of resources representing the root objects
     * in the graph e.g the object that do not have {link CMSAdapterVocabulary#CMS_OBJECT_PARENT_REF}
     * property.
     * 
     * @param graph
     *            {@link MGraph} in which root URIs will be searched
     * @return list of {@link NonLiteral}s
     */
    public static List<NonLiteral> getRootObjectsOfGraph(MGraph graph) {
        List<NonLiteral> roots = new ArrayList<NonLiteral>();
        Iterator<Triple> it = graph.filter(null, RDF_TYPE, CMSAdapterVocabulary.CMS_OBJECT);
        while (it.hasNext()) {
            Triple t = it.next();
            if (isRoot(t, graph)) {
                roots.add(t.getSubject());
            }
        }
        return roots;
    }

    private static boolean isUnderAbsolutePath(String path, NonLiteral subject, MGraph graph) {
        String name = getResourceStringValue(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
        if (name.contentEquals("")) {
            return false;
        }
        String objectPath = getResourceStringValue(subject, CMSAdapterVocabulary.CMS_OBJECT_PATH, graph);
        int nameIndex = objectPath.lastIndexOf(name);
        if (nameIndex == -1) {
            return false;
        }
        String precedingPath = objectPath.substring(0, nameIndex);
        return precedingPath.contentEquals(path) || precedingPath.contentEquals(path + "/");
    }

    private static boolean isRoot(Triple cmsObjectTriple, MGraph graph) {
        NonLiteral subject = cmsObjectTriple.getSubject();
        if (graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, null).hasNext()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Gets {@link Resource} of {@code Triple} which is specified the <code>subject</code> and
     * <code>propName</code> parameters
     * 
     * @param subject
     * @param propName
     * @param graph
     * @return specified resource if it exists, otherwise it returns <code>null</code>
     */
    public static Resource getResource(NonLiteral subject, UriRef propName, MGraph graph) {
        Iterator<Triple> it = graph.filter(subject, propName, null);
        if (it.hasNext()) {
            return it.next().getObject();
        } else {
            log.warn("No triple for subject: {} and property: {}", subject, propName);
            return null;
        }
    }

    /**
     * Gets lexical form of the {@link Resource} of {@link Triple} which is specified the <code>subject</code>
     * and <code>propName</code> parameters if the target resource is an instance of {@link Literal}.
     * 
     * @param subject
     * @param predicate
     * @param graph
     * @return lexical value of specified resource it exists and an instance of {@link Literal}, otherwise it
     *         returns empty string
     */
    public static String getResourceStringValue(NonLiteral subject, UriRef predicate, MGraph graph) {
        Resource r = getResource(subject, predicate, graph);
        return getResourceStringValue(r);
    }

    /**
     * Gets lexical form of the specified {@link Resource} if it is an instance of {@link Literal}.
     * 
     * @param r
     * @return lexical value of specified resource it is not null and an instance of {@link Literal},
     *         otherwise it returns empty string
     */
    public static String getResourceStringValue(Resource r) {
        if (r != null) {
            if (r instanceof Literal) {
                return ((Literal) r).getLexicalForm();
            } else {
                log.warn("Resource value is not a Literal for subject: {} and property: {}", r);
                return "";
            }
        } else {
            return "";
        }
    }

    /**
     * Gets {@link UriRef} from the {@link Resource} of {@link Triple} which is specified by the
     * <code>subject</code> and <code>propName</code> parameters if the target resource is an instance of
     * {@link UriRef}.
     * 
     * @param subject
     *            subject of the target triple
     * @param predicate
     *            predicate of the target triple
     * @param graph
     *            graph which the target triple is in
     * @return {@link UriRef} of resource if it exists and is instance of {@link UriRef}, otherwise
     *         <code>null</code>
     */
    public static UriRef getResourceURIValue(NonLiteral subject, UriRef predicate, MGraph graph) {
        Resource r = getResource(subject, predicate, graph);
        return getResourceURIValue(r);
    }

    /**
     * Gets {@link UriRef} from the specified {@link Resource}.
     * 
     * @param r
     * @return {@link UriRef} of resource if is not <code>null</code> and instance of an {@link UriRef},
     *         otherwise <code>null</code>
     */
    public static UriRef getResourceURIValue(Resource r) {
        if (r != null) {
            if (r instanceof UriRef) {
                return new UriRef(removeEndCharacters(r.toString()));
            } else {
                log.warn("Resource value is not a UriRef for subject: {} and property: {}", r);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Extracts a short URI e.g <b>skos:Concept</b> from a specified {@link Resource}.
     * 
     * @param r
     * @return short URI if the resource is an instance of {@link Literal} or {@link UriRef}
     */
    public static String getShortURIFromResource(Resource r) {
        String shortURI = "";
        if (r instanceof Literal) {
            shortURI = getResourceStringValue(r);
        } else if (r instanceof UriRef) {
            UriRef uri = getResourceURIValue(r);
            shortURI = NamespaceEnum.getShortName(uri.getUnicodeString());
            if(uri.getUnicodeString().equals(shortURI)) {
                return "";
            }
        } else {
            log.warn("Unexpected resource type:{} of mixin type resource", r);
        }
        return shortURI;
    }

    /**
     * Remove first {@link Triple} specified with <code>subject</code> and <code>predicate</code> parameters
     * from the specified {@link MGraph}
     * 
     * @param subject
     *            {@link NonLiteral} subject of triple to be deleted
     * @param predicate
     *            {@link UriRef} predicate of triple to be deleted
     * @param mGraph
     *            {@link MGraph} from where the triple to be deleted
     */
    public static void removeExistingTriple(NonLiteral subject, UriRef predicate, MGraph mGraph) {
        Iterator<Triple> it = mGraph.filter(subject, predicate, null);
        if (it.hasNext()) {
            mGraph.remove(it.next());
        }
    }

    /**
     * Removes <b>&lt;</b> and <b>&gt;</b> characters from start and end of the string respectively
     * 
     * @param resource
     * @return
     */
    public static String removeEndCharacters(String resource) {
        return resource.replace("<", "").replace(">", "");
    }

    /**
     * Tries to separate the local name from the given {@link NonLiteral}
     * 
     * @param uri
     *            absolute URI from which local name will be extracted
     * @return extracted local name
     */
    public static String extractLocalNameFromURI(NonLiteral subject) {
        String uri = RDFBridgeHelper.removeEndCharacters(subject.toString());
        return uri.substring(Util.splitNamespace(uri));
    }

    /**
     * Add path annotations to the resources whose rdf:Type's is {@link CMSAdapterVocabulary#CMS_OBJECT}.
     * Paths of objects are constructed according to {@link CMSAdapterVocabulary#CMS_OBJECT_PARENT_REF}
     * annotations among the objects.
     * 
     * @param rootPath
     *            the path representing the location in the CMS. This will be added as a prefix in front of
     *            the path annotations
     * @param graph
     *            containing the target resource to be annotated
     */
    public static void addPathAnnotations(String rootPath, List<NonLiteral> candidates, MGraph graph) {
        // first detect root objects
        List<NonLiteral> roots = getRootObjetsOfGraph(candidates, graph);

        // assign paths to children recursively
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        for (NonLiteral root : roots) {
            assignChildrenPaths(rootPath, root, graph, literalFactory, true);
        }
    }

    private static void assignChildrenPaths(String cmsRootPath,
                                            NonLiteral root,
                                            MGraph graph,
                                            LiteralFactory literalFactory,
                                            boolean firstLevel) {
        String rootName = getResourceStringValue(root, CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
        String rootPath = cmsRootPath;
        if (firstLevel) {
            rootPath = formRootPath(cmsRootPath, rootName);
            graph.add(new TripleImpl(root, CMSAdapterVocabulary.CMS_OBJECT_PATH, literalFactory
                    .createTypedLiteral(rootPath)));
        }

        Iterator<Triple> it = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, root);
        while (it.hasNext()) {
            NonLiteral childSubject = it.next().getSubject();
            String childName = getResourceStringValue(childSubject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                graph);
            String childPath = formRootPath(rootPath, childName);
            graph.add(new TripleImpl(childSubject, CMSAdapterVocabulary.CMS_OBJECT_PATH, literalFactory
                    .createTypedLiteral(childPath)));
            assignChildrenPaths(childPath, childSubject, graph, literalFactory, false);
        }
    }

    private static String formRootPath(String targetRootPath, String objectName) {
        if (!targetRootPath.endsWith("/")) {
            targetRootPath += "/";
        }
        return targetRootPath + objectName;
    }

    /**
     * Adds the specified <code>localName</code> at the end of the <code>baseURI</code>
     * 
     * @param baseURI
     * @param localName
     * @return concatenated URI
     */
    public static String appendLocalName(String baseURI, String localName) {
        if (baseURI.endsWith("#") || baseURI.endsWith("/")) {
            return baseURI + localName;
        }
        return baseURI + "#" + localName;
    }

    /**
     * Returns if it is possible to get full URI of the specified short URI e.g <b>skos:Concept</b>
     * 
     * @param shortURI
     * @return <code>true</code> if it is possible to get full URI of the specified short URI
     */
    public static boolean isShortNameResolvable(String shortURI) {
        String fullName = NamespaceEnum.getFullName(shortURI);
        return !fullName.contentEquals(shortURI);
    }

    public static void createDefaultPropertiesForRDF(NonLiteral subject,
                                                     MGraph graph,
                                                     String path,
                                                     String name) {
        if (valueCheck(path)) {
            checkDefaultPropertyInitialization(subject, CMSAdapterVocabulary.CMS_OBJECT_PATH, path, graph);
        }

        if (valueCheck(name)) {
            checkDefaultPropertyInitialization(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME, name, graph);
        }
    }

    public static String parseStringValue(String typedString) {
        Matcher matcher = pattern.matcher(typedString);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return typedString;
        }
    }

    private static void checkDefaultPropertyInitialization(NonLiteral subject,
                                                           UriRef property,
                                                           String value,
                                                           MGraph graph) {
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        String oldValue = RDFBridgeHelper.getResourceStringValue(subject, property, graph);
        if (oldValue.contentEquals("")) {
            graph.add(new TripleImpl(subject, property, literalFactory.createTypedLiteral(value)));
        }
    }

    private static boolean valueCheck(String s) {
        return s != null && !s.trim().contentEquals("");
    }
}
