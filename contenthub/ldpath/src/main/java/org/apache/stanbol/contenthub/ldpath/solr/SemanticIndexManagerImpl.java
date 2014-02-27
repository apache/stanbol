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
package org.apache.stanbol.contenthub.ldpath.solr;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDProgramCollection;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.store.solr.manager.SolrCoreManager;
import org.apache.stanbol.enhancer.ldpath.EnhancerLDPath;
import org.apache.stanbol.enhancer.ldpath.backend.ContentItemBackend;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the LDProgramManager which managed the LDPath Programs submitted to the system. This
 * manager creates Solr core for every uploaded program and manages them along with their programs.
 * 
 * @author anil.pacaci
 * @author anil.sinaci
 * 
 */

@Component(immediate = false)
@Service
public class SemanticIndexManagerImpl implements SemanticIndexManager {

    private final Logger logger = LoggerFactory.getLogger(SemanticIndexManagerImpl.class);

    private final static String DEFAULT_ROOT_PATH = "datafiles/contenthub";
    private final static String DEFAULT_FOLDER_NAME = "ldpath";

    private File managedProgramsDir;

    private Map<String,String> nameProgramMap = new HashMap<String,String>();

    private LDPathUtils ldPathUtils;

    private Bundle bundle;

    @Reference
    private ManagedSolrServer managedSolrServer;

    @Reference
    private SiteManager referencedSiteManager;

    @Activate
    public void activator(ComponentContext cc) throws LDPathException, IOException {
        bundle = cc.getBundleContext().getBundle();
        ldPathUtils = new LDPathUtils(bundle, referencedSiteManager);

        String slingHome = cc.getBundleContext().getProperty("sling.home");
        if (!slingHome.endsWith(File.separator)) slingHome += File.separator;
        managedProgramsDir = new File(slingHome + DEFAULT_ROOT_PATH, DEFAULT_FOLDER_NAME);

        // if directory for programs does not exist, create it
        if (!managedProgramsDir.exists()) {
            if (managedProgramsDir.mkdirs()) {
                logger.info("Directory for programs created succesfully");
            } else {
                logger.error("Directory for programs COULD NOT be created");
                throw new LDPathException("Directory : " + managedProgramsDir.getAbsolutePath()
                                          + " cannot be created");
            }
        }

        // means that directory for programs created succesfully or it already exists
        // now need to get all programs to the memory
        File[] programs = managedProgramsDir.listFiles();
        for (File programFile : programs) {
            if (SolrCoreManager.getInstance(bundle.getBundleContext(), managedSolrServer).isManagedSolrCore(
                programFile.getName())) {
                // consider only if the corresponding solr core exists
                String program = FileUtils.readFileToString(programFile);
                nameProgramMap.put(programFile.getName(), program);
            } else {
                programFile.delete();
            }
        }
    }

    @Override
    public void submitProgram(String programName, String ldPathProgram) throws LDPathException {
        if (programName == null || programName.isEmpty()) {
            String msg = "Program name cannot be null or empty";
            logger.error(msg);
            throw new LDPathException(msg);
        }

        if (ldPathProgram == null || ldPathProgram.isEmpty()) {
            String msg = "LDPath program cannot be null or empty";
            logger.error(msg);
            throw new LDPathException(msg);
        }

        // Checks whether there is already a program with the same name
        if (nameProgramMap.containsKey(programName)) {
            String msg = String.format("There is already an LDProgram with name : %s", programName);
            logger.error(msg);
            throw new LDPathException(msg);
        }

        try {
            SolrCoreManager.getInstance(bundle.getBundleContext(), managedSolrServer).createSolrCore(
                programName, ldPathUtils.createSchemaArchive(programName, ldPathProgram));
        } catch (LDPathException e) {
            logger.error("Solr Core Service could NOT initialize Solr core with name : {}", programName);
            throw e;
        } catch (StoreException e) {
            throw new LDPathException(e.getMessage(), e);
        }

        String programFilePath = managedProgramsDir.getAbsolutePath() + File.separator + programName;
        try {
            FileUtils.writeStringToFile(new File(programFilePath), ldPathProgram);
        } catch (IOException e) {
            String msg = "Cannot write the LDPath program to file. The state is inconsistent now. "
                         + "The Solr core is created, but corresponding LDPath program does not exist.";
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }
        nameProgramMap.put(programName, ldPathProgram);

        logger.info("SolrCore for the {} created succesfully and corresponding LDPath program saved ",
            programName);
    }

    @Override
    public void submitProgram(String programName, Reader ldPathProgramReader) throws LDPathException {
        String ldPathProgram = null;
        try {
            ldPathProgram = IOUtils.toString(ldPathProgramReader);
        } catch (IOException e) {
            String msg = "Cannot convert to String from Reader (ldPathProgramReader) in submitProgram";
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }
        submitProgram(programName, ldPathProgram);
    }

    @Override
    public String getProgramByName(String programName) {
        if (programName == null || programName.isEmpty()) {
            logger.error("Should be given a valid program name");
            throw new IllegalArgumentException("Program name cannot be null or empty");
        }

        if (!nameProgramMap.containsKey(programName)) {
            logger.warn("No program named {}", programName);
            return null;
        }

        return nameProgramMap.get(programName);
    }

    @Override
    public Program<Object> getParsedProgramByName(String programName) {
        SiteManagerBackend backend = new SiteManagerBackend(referencedSiteManager);
        String ldPathProgram = getProgramByName(programName);
        ValueFactory vf = InMemoryValueFactory.getInstance();
        EntityhubLDPath ldPath = new EntityhubLDPath(backend, vf);
        Program<Object> program = null;
        try {
            program = ldPath.parseProgram(LDPathUtils.constructReader(ldPathProgram));
        } catch (LDPathParseException e) {
            String msg = "Should never happen!!!!! Cannot parse the already stored LDPath program.";
            logger.error(msg, e);
        } catch (LDPathException e) {
            String msg = "Should never happen!!!!! Cannot parse the already stored LDPath program.";
            logger.error(msg, e);
        }
        return program;
    }

    @Override
    public void deleteProgram(String programName) {
        if (isManagedProgram(programName)) {
            SolrCoreManager.getInstance(bundle.getBundleContext(), managedSolrServer).deleteSolrCore(
                programName);
            nameProgramMap.remove(programName);
            String programFilePath = managedProgramsDir.getAbsolutePath() + File.separator + programName;
            File programFile = new File(programFilePath);
            programFile.delete();
        }
    }

    @Override
    public boolean isManagedProgram(String programName) {
        if (nameProgramMap.containsKey(programName)) {
            return true;
        }
        return false;
    }

    @Override
    public LDProgramCollection retrieveAllPrograms() {
        return new LDProgramCollection(nameProgramMap);
    }

    @Override
    public Map<String,Collection<?>> executeProgram(String programName, Set<String> contexts, ContentItem ci) throws LDPathException {
        Map<String,Collection<?>> results = new HashMap<String,Collection<?>>();
        SiteManagerBackend backend = new SiteManagerBackend(referencedSiteManager);
        String ldPathProgram = getProgramByName(programName);
        ValueFactory vf = InMemoryValueFactory.getInstance();
        EntityhubLDPath ldPath = new EntityhubLDPath(backend, vf);
        Program<Object> program = null;
        try {
            program = ldPath.parseProgram(LDPathUtils.constructReader(ldPathProgram));
        } catch (LDPathParseException e) {
            logger.error("Should never happen!!!!!", e);
            return Collections.emptyMap();
        }

        Representation representation;
        for (String context : contexts) {
            representation = ldPath.execute(vf.createReference(context), program);
            Iterator<String> fieldNames = representation.getFieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                Iterator<Object> valueIterator = representation.get(fieldName);
                if (!valueIterator.hasNext()) continue;
                Set<Object> values = new HashSet<Object>();
                while (valueIterator.hasNext()) {
                    values.add(valueIterator.next());
                }
                if (results.containsKey(fieldName)) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> resultCollection = (Collection<Object>) results.get(fieldName);
                    Collection<Object> tmpCol = (Collection<Object>) values;
                    for (Object o : tmpCol) {
                        resultCollection.add(o);
                    }
                } else {
                    results.put(fieldName, values);
                }
            }
        }

        // execute the LDPath on the given ContentItem
        ContentItemBackend contentItemBackend = new ContentItemBackend(ci, true);
        LDPath<Resource> resourceLDPath = new LDPath<Resource>(contentItemBackend, EnhancerLDPath.getConfig());
        Program<Resource> resourceProgram;
        try {
            resourceProgram = resourceLDPath.parseProgram(new StringReader(ldPathProgram));
            Map<String,Collection<?>> ciBackendResults = resourceProgram.execute(contentItemBackend,
                ci.getUri());
            for (Entry<String,Collection<?>> result : ciBackendResults.entrySet()) {
                if (result.getValue() == null || result.getValue().isEmpty()) continue;
                if (results.containsKey(result.getKey())) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> resultsValue = (Collection<Object>) results.get(result.getKey());
                    resultsValue.addAll(result.getValue());
                } else {
                    results.put(result.getKey(), result.getValue());
                }

            }
        } catch (LDPathParseException e) {
            logger.error("Failed to create Program<Resource> from the LDPath program", e);
        }

        return results;
    }
}
