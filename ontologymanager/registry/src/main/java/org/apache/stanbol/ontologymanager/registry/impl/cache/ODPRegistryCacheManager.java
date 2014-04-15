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
/**
 * 
 */
package org.apache.stanbol.ontologymanager.registry.impl.cache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportEvent;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.slf4j.LoggerFactory;

/**
 * @author Enrico Daga
 * 
 */
public final class ODPRegistryCacheManager {

    /**
     * Restrict instantiation
     */
    private ODPRegistryCacheManager() {}

    /**
	  *
	  */
    private static final long serialVersionUID = 1L;

    /*
     * TODO bundle path or something
     */
    public static final String WORKSPACE_PATH = ""/*
                                                   * ResourcesPlugin.getWorkspace()
                                                   * .getRoot().getLocationURI(). toString()
                                                   */;

    public static final String TEMPORARY_DIR_NAME = ".xd";
    public static final String TEMPORARY_FILE_PREFIX = "uri";
    public static final String TEMPORARY_FILE_EXTENSION = ".res";
    public static final String TEMPORARY_URI_REGISTRY = ".registry";
    private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String URI_SEPARATOR = "/";

    private static Map<URI,File> uris = new HashMap<URI,File>();
    private static Map<URI,IRI> oiri = new HashMap<URI,IRI>();

    private static Set<URI> unresolvedURIs = new HashSet<URI>();

    private static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    public static OWLOntologyManager getManager() {
        return manager;
    }

    public static void addResource(OWLOntology ontology, URI virtualPhysicalURI) throws ODPRegistryCacheException {

        File file = newFile();
        try {
            cacheOntology(virtualPhysicalURI, file, ontology);
            // manager.saveOntology(ontology, new RDFXMLOntologyFormat(), file
            // .toURI());
        } catch (UnknownOWLOntologyException e) {
            throw new ODPRegistryCacheException(e);
        } catch (OWLOntologyStorageException e) {
            throw new ODPRegistryCacheException(e);
        }
        // uris.put(virtualPhysicalURI, file);
    }

    /**
     * uri is the physical uri
     * 
     * @param uri
     * @return
     * @throws ODPRegistryCacheException
     * @throws URIUnresolvableException
     */
    public static synchronized OWLOntology getOntology(URI uri) throws ODPRegistryCacheException,
                                                               URIUnresolvableException {
        if (getUnresolvedURIs().contains(uri)) throw new URIUnresolvableException();
        try {
            if (uris.containsKey(uri)) return retrieveLocalResource(uri);
            else return retrieveRemoteResource(uri);
        } catch (UnknownOWLOntologyException e) {
            throw new ODPRegistryCacheException(e);
        } catch (OWLOntologyCreationException e) {
            throw new ODPRegistryCacheException(e);
        } catch (OWLOntologyStorageException e) {
            throw new ODPRegistryCacheException(e);
        }
    }

    public static synchronized OWLOntologyDocumentSource getOntologyInputSource(URI uri) throws ODPRegistryCacheException,
                                                                                        URIUnresolvableException {
        if (getUnresolvedURIs().contains(uri)) throw new URIUnresolvableException();
        if (uris.containsKey(uri)) {
            File f = uris.get(uri);
            FileDocumentSource fds = new FileDocumentSource(f);
            return fds;
        } else {
            try {
                retrieveRemoteResource(uri);
                return getOntologyInputSource(uri);
            } catch (UnknownOWLOntologyException e) {
                throw new ODPRegistryCacheException(e);
            } catch (OWLOntologyCreationException e) {
                throw new ODPRegistryCacheException(e);
            } catch (OWLOntologyStorageException e) {
                throw new ODPRegistryCacheException(e);
            }

        }

    }

    public static URI getRegistryURI() {
        return URI.create(WORKSPACE_PATH + URI_SEPARATOR + TEMPORARY_DIR_NAME + URI_SEPARATOR
                          + TEMPORARY_URI_REGISTRY);
    }

    public static boolean registryContains(URI ontologyURI) {
        return uris.containsKey(ontologyURI);
    }

    public static URI getTemporaryFolder() {
        return URI.create(WORKSPACE_PATH + URI_SEPARATOR + TEMPORARY_DIR_NAME);
    }

    /**
     * @return the unresolvedURIs
     */
    public static Set<URI> getUnresolvedURIs() {
        return unresolvedURIs;
    }

    public static File newFile() {
        File file = new File(URI.create(getTemporaryFolder().toString() + URI_SEPARATOR
                                        + TEMPORARY_FILE_PREFIX + System.currentTimeMillis()
                                        + TEMPORARY_FILE_EXTENSION));
        return file;
    }

    private static synchronized OWLOntology retrieveLocalResource(URI uri) throws OWLOntologyCreationException,
                                                                          ODPRegistryCacheException,
                                                                          URIUnresolvableException {
        File file = uris.get(uri);
        if (!file.exists()) {
            uris.remove(uri);
            return getOntology(uri);
        }
        manager.setSilentMissingImportsHandling(true);
        manager.addMissingImportListener(new MissingImportListener() {
            public void importMissing(MissingImportEvent arg0) {
                if (!getUnresolvedURIs().contains(arg0.getImportedOntologyURI())) getUnresolvedURIs().add(
                    arg0.getImportedOntologyURI().toURI());
            }
        });
        IRI oi = oiri.get(uri);
        OWLOntology ontology = null;
        ontology = manager.getOntology(oi);
        if (ontology == null) try {
            ontology = manager.loadOntologyFromOntologyDocument(IRI.create(file));
        } catch (OWLOntologyAlreadyExistsException e) {
            ontology = manager.getOntology(e.getOntologyID());
        }

        return ontology;
    }

    /**
     * Gets the remote ontology and saves it locally
     * 
     * @param uri
     * @return
     * @throws OWLOntologyCreationException
     * @throws UnknownOWLOntologyException
     * @throws OWLOntologyStorageException
     */
    private static synchronized OWLOntology retrieveRemoteResource(URI uri) throws OWLOntologyCreationException,
                                                                           UnknownOWLOntologyException,
                                                                           OWLOntologyStorageException {

        manager.setSilentMissingImportsHandling(true);
        manager.addMissingImportListener(new MissingImportListener() {
            public void importMissing(MissingImportEvent arg0) {
                if (!getUnresolvedURIs().contains(arg0.getImportedOntologyURI())) getUnresolvedURIs().add(
                    arg0.getImportedOntologyURI().toURI());
            }
        });
        manager.addOntologyLoaderListener(new OWLOntologyLoaderListener() {

            @Override
            public void startedLoadingOntology(LoadingStartedEvent event) {
                // Nothing to do
            }

            @Override
            public void finishedLoadingOntology(LoadingFinishedEvent event) {

                URI onturi = event.getDocumentIRI().toURI();

                if (event.getException() != null) {
                    getUnresolvedURIs().add(onturi);
                    LoggerFactory.getLogger(ODPRegistryCacheManager.class).warn(
                        "Failed to resolve ontology at " + onturi + " . Skipping.", event.getException());
                    return;
                }
                try {
                    if (!uris.containsKey(onturi)) {
                        cacheOntology(onturi, newFile(), manager.getOntology(event.getOntologyID()));
                    }
                } catch (UnknownOWLOntologyException e) {
                    LoggerFactory.getLogger(ODPRegistryCacheManager.class).warn(
                        "Failed to cache ontology at " + onturi + " . Skipping.", e);
                    getUnresolvedURIs().add(onturi);
                } catch (OWLOntologyStorageException e) {
                    LoggerFactory.getLogger(ODPRegistryCacheManager.class).warn(
                        "Failed to cache ontology at " + onturi + " . Skipping.", e);
                    getUnresolvedURIs().add(onturi);
                }
            }
        });

        OWLOntology ont;
        try {
            ont = manager.loadOntologyFromOntologyDocument(IRI.create(uri));
        } catch (OWLOntologyAlreadyExistsException e) {
            ont = manager.getOntology(e.getOntologyID().getOntologyIRI());
        }
        File file = newFile();
        cacheOntology(uri, file, ont);

        return ont;
    }

    private static synchronized void cacheOntology(URI physicalRemoteUri, File file, OWLOntology ont) throws UnknownOWLOntologyException,
                                                                                                     OWLOntologyStorageException {
        uris.put(physicalRemoteUri, file);
        oiri.put(physicalRemoteUri, ont.getOntologyID().getOntologyIRI());
        manager.setOntologyDocumentIRI(ont, IRI.create(file));
        manager.saveOntology(ont, new RDFXMLOntologyFormat(), IRI.create(file));
    }

    public static synchronized boolean save() {
        File registry = new File(ODPRegistryCacheManager.getRegistryURI());
        if (registry.exists()) registry.delete();
        try {
            registry.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(registry));

            for (URI u : uris.keySet()) {
                writer.write(u.toString() + "|" + uris.get(u).toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static synchronized boolean load() {
        File registry = new File(ODPRegistryCacheManager.getRegistryURI());
        if (!registry.exists()) return false;

        Map<URI,File> newUris = new HashMap<URI,File>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(registry));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf('|') < 0) continue;
                String[] splitted = line.split("\\|");
                newUris.put(URI.create(splitted[0]), new File(splitted[1]));
            }
            reader.close();
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger(ODPRegistryCacheManager.class).error(
                "Failed to load registry " + getRegistryURI() + " File not found.", e);
        } catch (IOException e) {
            LoggerFactory.getLogger(ODPRegistryCacheManager.class).error(
                "Failed to load registry " + getRegistryURI(), e);
        }
        uris.clear();
        uris = newUris;
        return true;
    }

    public static synchronized boolean clean() {
        // FileUriHelper fu = new FileUriHelper();
        try {
            // fu
            // .deleteDir(new File(ODPRegistryCacheManager
            // .getTemporaryFolder()));

            uris.clear();
            manager = null;
            manager = OWLManager.createOWLOntologyManager();
        } catch (Exception e) {
            LoggerFactory.getLogger(ODPRegistryCacheManager.class).error(
                "OWL cache manager cleanup failed. ", e);
            return false;
        }

        return true;
    }

}
