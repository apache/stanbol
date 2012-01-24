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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.manager.changes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates an OWLOntology object where to store rules and recipes.
 * 
 * @author elvio
 * @author andrea.nuzzolese
 * 
 */
@Component(immediate = true, metatype = true)
@Service(RuleStore.class)
public class RuleStoreImpl implements RuleStore {

    public static final String _RULE_NAMESPACE_DEFAULT = "http://incubator.apache.org/stanbol/ontology/rmi.owl#";

    public static final String _RULE_ONTOLOGY_DEFAULT = "";

    /**
     * Rewrites the rule ontology location and overwrites the given configuration.
     * 
     * @param configuration
     * @param filepath
     * @return
     */
    private static Dictionary<String,Object> _reconfigureLocation(Dictionary<String,Object> configuration,
                                                                  String filepath) {

        String location;
        if (filepath.isEmpty()) {

            // Obsolete code
            Properties configProps = System.getProperties();
            String userdir = configProps.getProperty("user.dir");

            String respath = "RuleConf" + System.getProperty("file.separator"); // "src/main/resources/";
            String filepath2 = "rmi_config.owl"; // "RuleOntology/rmi_config.owl";
            // userdir = userdir.substring(0, userdir.lastIndexOf("kres.") + 5) + "rules/";

            userdir += System.getProperty("file.separator");

            location = userdir + respath + filepath2;
            // The location string variable must be a valid uri
            location = new File(location).toURI().toString();
        } else location = filepath;

        // We do this instead of setting the global, non-static var
        configuration.put(RuleStore.RULE_ONTOLOGY, location);

        return configuration;
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private OWLOntology owlmodel;

    @Property(name = RuleStore.RULE_NAMESPACE, value = _RULE_NAMESPACE_DEFAULT)
    private String ruleNS;

    @Property(name = RuleStore.RULE_ONTOLOGY, value = _RULE_ONTOLOGY_DEFAULT)
    private String ruleOntologyLocation;

    /**
     * This construct returns RuleStoreImpl object with inside an ontology where to store the rules.
     * 
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the RuleStoreImpl instances do need to be configured! YOU
     * NEED TO USE {@link #RuleStoreImpl(ONManager, Dictionary)} or its overloads, to parse the configuration
     * and then initialise the rule store if running outside a OSGI environment.
     */
    public RuleStoreImpl() {}

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param configuration
     */
    public RuleStoreImpl(Dictionary<String,Object> configuration) {
        // This recursive constructor call will also invoke activate()
        this(configuration, (OWLOntology) null);
    }

    /**
     * This construct returns an ontology where to store the rules.
     * 
     * @param owl
     *            {OWLOntology object contains rules and recipe}
     */
    public RuleStoreImpl(Dictionary<String,Object> configuration, OWLOntology owl) {
        this();

        try {
            this.owlmodel = owl;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            this.owlmodel = null;
        }

        try {
            // activator has a branch for existing owlfile
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }

    }

    /**
     * This construct returns an ontology where to store the rules.
     * 
     * @param filepath
     *            {Ontology file path previously stored.}
     */
    public RuleStoreImpl(Dictionary<String,Object> configuration, String filepath) {
        this();
        try {
            activate(_reconfigureLocation(configuration, filepath));
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + RuleStoreImpl.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Should be called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {
    	
    	
        if (this.owlmodel != null) {
            ruleOntologyLocation = owlmodel.getOWLOntologyManager().getOntologyDocumentIRI(owlmodel)
                    .toString();
            if (!owlmodel.isAnonymous()) ruleNS = owlmodel.getOntologyID().getOntologyIRI() + "#";
        } else {
            ruleOntologyLocation = (String) configuration.get(RuleStore.RULE_ONTOLOGY);
            if (ruleOntologyLocation == null) ruleOntologyLocation = _RULE_ONTOLOGY_DEFAULT;
            ruleNS = (String) configuration.get(RuleStore.RULE_NAMESPACE);
            if (ruleNS == null) ruleNS = _RULE_NAMESPACE_DEFAULT;

            // Setup local IRI mappings
            OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
            // FIXME : awful, remove as soon as we can
            URL u = getClass().getResource("/rmi.owl");
            OWLOntologyIRIMapper mapper1, mapper2;
            try {
                mapper1 = new SimpleIRIMapper(
                        IRI.create("http://ontologydesignpatterns.org/ont/iks/kres/rmi.owl"), IRI.create(u));
                mapper2 = new SimpleIRIMapper(
                        IRI.create("http://ontologydesignpatterns.org/ont/iks/kres/rmi_config.owl"),
                        IRI.create(getClass().getResource("/META-INF/conf/rmi_config.owl")));
                // mapper = new AutoIRIMapper(new File(u.toURI()), true);
                mgr.addIRIMapper(mapper1);
                mgr.addIRIMapper(mapper2);
            } catch (URISyntaxException e) {
                log.error("Failed to add default IRI mapping for resource directory.", e);
            }

            // Rule ontology location not set
            if (ruleOntologyLocation == null || ruleOntologyLocation.equals("")) {
                String sep = System.getProperty("file.separator");
                String filedir = System.getProperty("user.dir") + sep + "RuleConf" + sep + "rmi_config.owl";

                // default RuleConf dir exists
                if ((new File(filedir)).exists()) {
                    this.ruleOntologyLocation = filedir;
                    try {
                        owlmodel = // OWLManager.createOWLOntologyManager()
                        mgr.loadOntologyFromOntologyDocument(new File(ruleOntologyLocation));
                    } catch (OWLOntologyCreationException e) {
                        log.error("Cannot create the ontology " + filedir.toString(), e);
                    } catch (Exception e) {
                        log.error("1 Rule Store: no rule ontology available.", e);
                    }
                }
                // default RuleConf dir does not exist
                else {
                    IRI inputontology = IRI
                            .create("http://ontologydesignpatterns.org/ont/iks/kres/rmi_config.owl");
                    try {
                        owlmodel = // OWLManager.createOWLOntologyManager()
                        mgr.loadOntology/* FromOntologyDocument */(inputontology);
                    } catch (OWLOntologyCreationException e) {
                        log.error("Cannot create the ontology " + inputontology.toString(), e);
                    } catch (Exception e) {
                        log.error("Rule Store: no rule ontology available.", e);
                    }
                }

                // Can we skip this step of saving the ontology?
                if (owlmodel != null) {

                    File dirs = new File("." + sep + "RuleConf");
                    if (!dirs.exists()) dirs.mkdir();
                    ruleOntologyLocation = "." + sep + "RuleConf" + sep + "rmi_config.owl";

                    FileOutputStream fos;
                    try {
                        fos = new FileOutputStream(ruleOntologyLocation);
                        mgr/* OWLManager.createOWLOntologyManager() */.saveOntology(owlmodel, owlmodel
                                .getOWLOntologyManager().getOntologyFormat(owlmodel), fos);
                    } catch (FileNotFoundException e) {
                        log.error("Cannot save the RMI configuration ontology", e);
                    } catch (OWLOntologyStorageException e) {
                        log.error("Cannot save the RMI configuration ontology", e);
                    }
                }
            }
            // Rule ontology location is set. Prefer absolute IRIs to files.
            else {

                IRI pathIri = IRI.create(ruleOntologyLocation);
                if (!pathIri.isAbsolute()) pathIri = IRI.create(new File(ruleOntologyLocation));

                try {
                    owlmodel = mgr
                    /* OWLManager.createOWLOntologyManager() */.loadOntology(pathIri);
                } catch (OWLOntologyCreationException e) {
                    log.error("Cannot load the RMI configuration ontology", e);
                } catch (Exception e) {
                    log.error("Rule Store: no rule ontology available.", e);
                }
            }
        }
        
        log.info("Rule Ontology Location set to be " + ruleOntologyLocation, this);
    }

    /*
     * Moved form AddRecipe class. The AddRecipe should not be used anymore.
     */
    @Override
    public boolean addRecipe(IRI recipeIRI, String recipeDescription) {
        boolean ok = false;
        log.debug("Adding recipe " + recipeIRI + " [" + recipeDescription + "]", this);
        OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        String owlIDrmi = "http://incubator.apache.org/stanbol/ontology/rmi.owl#";

        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipeIRI);
        OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasDescription"));
        OWLDataPropertyAssertionAxiom dataPropAssertion;

        if ((recipeIRI != null)) {
            if (!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))) {

                // Add the recipe istance
                OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
                owlmanager.addAxiom(owlmodel, classAssertion);

                // Add description
                if (recipeDescription != null && !recipeDescription.isEmpty()) {
                    // Add the rule description
                    dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind,
                        recipeDescription);
                    owlmanager.addAxiom(owlmodel, dataPropAssertion);
                }
                ok = true;
            } else {
                log.error("The recipe with name " + recipeIRI + " already exists. Please check the name.");
                
            }

        } else {
            log.error("The recipe with name and the set of rules cannot be empity or null.");
        }

        if (ok) {
            setStore(owlmodel);
            
            saveOntology();
        }

        return ok;
    }

    /**
     * 
     * @param recipe
     *            the recipe
     * @param RuleRule
     *            the rule in Rule syntax
     * 
     * @return the recipe we the new rule.
     */
    @Override
    public Recipe addRuleToRecipe(Recipe recipe, String stanbolSyntaxRule) {
        log.debug("Adding rule to recipe " + recipe);

        /**
         * Get the OWLDataFactory.
         */
        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        /**
         * Add the rule to the recipe in the rule ontology managed by the RuleStore. First we define the
         * object property hasRule and then we add the literal that contains the rule in Rule Syntax to the
         * recipe individual.
         */
        String ruleNS = "http://incubator.apache.org/stanbol/ontology/rmi.owl#";
        OWLObjectProperty hasRule = factory.getOWLObjectProperty(IRI.create(ruleNS + "hasRule"));
        OWLDataProperty hasBodyAndHead = factory.getOWLDataProperty(IRI.create(ruleNS + "hasBodyAndHead"));

        /**
         * The IRI of the recipe is fetched from the recipe object itself. From that IRI is obtained the
         * recipe owl individual.
         */
        IRI recipeIRI = recipe.getRecipeID();
        OWLNamedIndividual reipeIndividual = factory.getOWLNamedIndividual(recipeIRI);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        /**
         * Finally also the in-memory representation of the Recipe passed as input is modified.
         */
        KB kb = RuleParserImpl.parse(stanbolSyntaxRule);
        RuleList ruleList = kb.getkReSRuleList();
        for (Rule rule : ruleList) {

            /**
             * The rule must be added to the ontology, so 1. an IRI is created from its name 2. the rule
             * in Stanbol syntax is added to the rule as a literal through the hasBobyAndHe data property. 3. the rule is
             * associated to the recipe by means of the hasRule object property, so that the triple <a_recipe
             * hasRule a_rule> is added to the rule ontology.
             * 
             */
            IRI ruleIRI = IRI.create(ruleNS + rule.getRuleName());
            OWLNamedIndividual ruleIndividual = factory.getOWLNamedIndividual(ruleIRI);

            OWLAxiom hasBodyAndHeadAxiom = factory.getOWLDataPropertyAssertionAxiom(hasBodyAndHead,
                ruleIndividual, rule.toKReSSyntax());
            manager.addAxiom(owlmodel, hasBodyAndHeadAxiom);

            OWLAxiom hasRuleAxiom = factory.getOWLObjectPropertyAssertionAxiom(hasRule, reipeIndividual,
                ruleIndividual);
            manager.addAxiom(owlmodel, hasRuleAxiom);

            /**
             * The Rule is added to the Recipe in-memory object.
             */
            recipe.addKReSRule(rule);
        }

        return recipe;
    }
    
    
    /**
     * 
     * @param recipe
     *            the recipe
     * @param rulesStream
     *            the rule in Rule syntax
     * 
     * @return the recipe we the new rule.
     */
    @Override
    public Recipe addRuleToRecipe(Recipe recipe, InputStream rulesStream) {
        log.debug("Adding rule to recipe " + recipe);

        /**
         * Get the OWLDataFactory.
         */
        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        /**
         * Add the rule to the recipe in the rule ontology managed by the RuleStore. First we define the
         * object property hasRule and then we add the literal that contains the rule in Rule Syntax to the
         * recipe individual.
         */
        String ruleNS = "http://incubator.apache.org/stanbol/ontology/rmi.owl#";
        OWLObjectProperty hasRule = factory.getOWLObjectProperty(IRI.create(ruleNS + "hasRule"));
        OWLDataProperty hasBodyAndHead = factory.getOWLDataProperty(IRI.create(ruleNS + "hasBodyAndHead"));

        /**
         * The IRI of the recipe is fetched from the recipe object itself. From that IRI is obtained the
         * recipe owl individual.
         */
        IRI recipeIRI = recipe.getRecipeID();
        OWLNamedIndividual reipeIndividual = factory.getOWLNamedIndividual(recipeIRI);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        /**
         * Finally also the in-memory representation of the Recipe passed as input is modified.
         */
        KB kb = RuleParserImpl.parse(rulesStream);
        RuleList ruleList = kb.getkReSRuleList();
        for (Rule rule : ruleList) {

            /**
             * The rule must be added to the ontology, so 1. an IRI is created from its name 2. the rule
             * in Stanbol syntax is added to the rule as a literal through the hasBobyAndHe data property. 3. the rule is
             * associated to the recipe by means of the hasRule object property, so that the triple <a_recipe
             * hasRule a_rule> is added to the rule ontology.
             * 
             */
            IRI ruleIRI = IRI.create(ruleNS + rule.getRuleName());
            OWLNamedIndividual ruleIndividual = factory.getOWLNamedIndividual(ruleIRI);

            OWLAxiom hasBodyAndHeadAxiom = factory.getOWLDataPropertyAssertionAxiom(hasBodyAndHead,
                ruleIndividual, rule.toKReSSyntax());
            manager.addAxiom(owlmodel, hasBodyAndHeadAxiom);

            OWLAxiom hasRuleAxiom = factory.getOWLObjectPropertyAssertionAxiom(hasRule, reipeIndividual,
                ruleIndividual);
            manager.addAxiom(owlmodel, hasRuleAxiom);

            /**
             * The Rule is added to the Recipe in-memory object.
             */
            recipe.addKReSRule(rule);
        }

        return recipe;
    }

    /**
     * 
     * @param recipeIRI
     *            the IRI of the recipe
     * @param stanbolRule
     *            the rule in Rule syntax
     */
    @Override
    public Recipe addRuleToRecipe(String recipeID, String stanbolRule) throws NoSuchRecipeException {

        Recipe recipe = getRecipe(IRI.create(recipeID));
        return addRuleToRecipe(recipe, stanbolRule);

    }
    
    
    /**
     * 
     * @param recipeIRI
     *            the IRI of the recipe
     * @param rulesStream
     *            the rule in Rule syntax
     */
    @Override
    public Recipe addRuleToRecipe(String recipeID, InputStream rulesStream) throws NoSuchRecipeException {

        Recipe recipe = getRecipe(IRI.create(recipeID));
        return addRuleToRecipe(recipe, rulesStream);

    }

    @Override
    public void createRecipe(String recipeID, String stanbolRule) {
        log.debug("Create recipe " + recipeID + " with rules in Stanbol sytnax " + stanbolRule, this);
        KB kb = RuleParserImpl.parse(stanbolRule);
        RuleList rules = kb.getkReSRuleList();

        AddRule addRule = new AddRule(this);

        Vector<IRI> ruleVectorIRIs = new Vector<IRI>();
        log.debug("Rules are " + rules.size());
        for (Rule rule : rules) {
            log.debug("Creating rule " + rule.getRuleName());
            String stanbolSyntax = rule.toKReSSyntax();

            log.debug("Rule in Stanbol Syntax : " + stanbolSyntax);
            addRule.addRule(IRI.create(rule.getRuleName()), stanbolSyntax, null);
            ruleVectorIRIs.add(IRI.create(rule.getRuleName()));
        }

        if (ruleVectorIRIs.size() > 0) {
            log.debug("Adding rules: " + ruleVectorIRIs.size());
            AddRecipe addRecipe = new AddRecipe(this);
            addRecipe.addRecipe(IRI.create(recipeID), ruleVectorIRIs, null);
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + RuleStoreImpl.class + " deactivate with context " + context);
    }

    private RuleList generateKnowledgeBase(String stanbolRule) {
        KB kb = RuleParserImpl.parse(stanbolRule);
        return kb.getkReSRuleList();
    }

    /**
     * To get the file path usde to load the ontology.
     * 
     * @return {A string contains the complete file path.}
     */
    @Override
    public String getFilePath() {
        return this.ruleOntologyLocation;
    }

    /**
     * Get the owl ontology model.
     * 
     * @return {An OWLOntology object where to store the rules and the recipes.}
     */
    @Override
    public OWLOntology getOntology() {
        return this.owlmodel;
    }

    @Override
    public Recipe getRecipe(IRI recipeIRI) throws NoSuchRecipeException {
        log.debug("Called get recipe for id: " + recipeIRI.toString());
        Recipe recipe = null;

        if (recipeIRI != null) {
            OWLDataFactory factory = OWLManager.getOWLDataFactory();
            OWLIndividual recipeIndividual = factory.getOWLNamedIndividual(recipeIRI);
            if (recipeIndividual != null) {
                // String ruleNS =
                // owlmodel.getOntologyID().toString().replace("<","").replace(">","")+"#";

                // OWLObjectProperty objectProperty =
                // factory.getOWLObjectProperty(IRI.create(ruleNS + "hasRule"));

                String ruleNS = "http://incubator.apache.org/stanbol/ontology/rmi.owl#";

                /**
                 * First get the recipe description in the rule/recipe ontology.
                 */
                OWLDataProperty hasDescription = factory.getOWLDataProperty(IRI.create(ruleNS
                                                                                       + "hasDescription"));

                String recipeDescription = null;

                Set<OWLLiteral> descriptions = recipeIndividual.getDataPropertyValues(hasDescription,
                    owlmodel);
                for (OWLLiteral description : descriptions) {
                    recipeDescription = description.getLiteral();
                }

                /**
                 * Then retrieve the rules associated to the recipe in the rule store.
                 */
                OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI
                        .create(ruleNS + "hasRule"));
                Set<OWLIndividual> rules = recipeIndividual.getObjectPropertyValues(objectProperty, owlmodel);

                String stanbolRule = "";

                log.debug("The recipe " + recipeIRI.toString() + " has " + rules.size() + " rules.");

                /**
                 * Fetch the rule content expressed as a literal in Rule Syntax.
                 */
                boolean firstLoop = true;
                OWLDataProperty hasBodyAndHead = factory.getOWLDataProperty(IRI.create(ruleNS
                                                                                       + "hasBodyAndHead"));

                for (OWLIndividual rule : rules) {
                    log.debug("Getting rule : " + rule.toStringID(), this);

                    Set<OWLLiteral> stanbolRuleLiterals = rule.getDataPropertyValues(hasBodyAndHead, owlmodel);

                    if (!firstLoop) {
                        stanbolRule += " . ";
                    } else {
                        firstLoop = false;
                    }

                    for (OWLLiteral stanbolRuleLiteral : stanbolRuleLiterals) {
                        String ruleTmp = stanbolRuleLiteral.getLiteral().replace("&lt;", "<");
                        ruleTmp = ruleTmp.replace("&gt;", ">");
                        log.debug("Rule is: " + ruleTmp);
                        stanbolRule += ruleTmp;
                    }
                }

                /**
                 * Create the Recipe object.
                 */
                log.debug("Recipe in Stanbol Syntax : " + stanbolRule);

                RuleList ruleList = null;

                if (!stanbolRule.isEmpty()) {
                    ruleList = generateKnowledgeBase(stanbolRule);
                }

                recipe = (Recipe) new RecipeImpl(recipeIRI, recipeDescription, ruleList);

            } else {
                throw new NoSuchRecipeException(recipeIRI);
            }
        }

        return recipe;

    }

    @Override
    public String getRuleStoreNamespace() {
        return owlmodel.getOntologyID().getOntologyIRI() + "#";
    }

    @Override
    public Set<IRI> listIRIRecipes() {
        Set<IRI> recipeIRIs = null;
        String ruleNS = owlmodel.getOntologyID().toString().replace("<", "").replace(">", "") + "#";

        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        OWLClass recipeOWLClass = factory.getOWLClass(IRI.create(ruleNS + "Recipe"));
        Set<OWLIndividual> recipeIndividuals = recipeOWLClass.getIndividuals(owlmodel);

        if (recipeIndividuals != null && recipeIndividuals.size() > 0) {
            recipeIRIs = new HashSet<IRI>();
            for (OWLIndividual recipeIndividual : recipeIndividuals) {
                if (recipeIndividual instanceof OWLNamedIndividual) {
                    recipeIRIs.add(((OWLNamedIndividual) recipeIndividual).getIRI());
                }
            }
        }

        return recipeIRIs;
    }

    @Override
    public RecipeList listRecipes() {
        RecipeList recipies = null;

        Set<IRI> recipeIRIs = listIRIRecipes();

        if (recipeIRIs != null && recipeIRIs.size() > 0) {

            recipies = new RecipeList();
            for (IRI recipeIRI : recipeIRIs) {
                try {
                    recipies.add(getRecipe(recipeIRI));
                } catch (NoSuchRecipeException e) {
                    log.error("Recipe missing: " + recipeIRI.toString(), e);
                }
            }
        }
        return recipies;
    }

    @Override
    public boolean removeRecipe(IRI recipeIRI) {
        Recipe recipe;
        try {
            recipe = getRecipe(recipeIRI);
            return removeRecipe(recipe);
        } catch (NoSuchRecipeException ex) {
            log.error("Exception cougth: ", ex);
            return false;
        }
    }

    @Override
    public boolean removeRecipe(Recipe recipe) {

        OWLOntologyManager mng = owlmodel.getOWLOntologyManager();
        OWLDataFactory factory = mng.getOWLDataFactory();

        // Create the remover to be used to delete the recipe from the ontology.
        OWLEntityRemover remover = new OWLEntityRemover(mng, Collections.singleton(owlmodel));

        // Create the recipe axiom
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipe.getRecipeID());

        // Remove the recipe
        ontoind.accept(remover);
        mng.applyChanges(remover.getChanges());
        remover.reset();

        // Check if the recipe ahs been removed
        if (owlmodel.containsIndividualInSignature(recipe.getRecipeID())) return false;
        else return true;

    }

    @Override
    public boolean removeRule(Rule rule) {

        OWLOntologyManager mng = owlmodel.getOWLOntologyManager();
        OWLDataFactory factory = mng.getOWLDataFactory();
        String ruleNS = "http://incubator.apache.org/stanbol/ontology/rmi.owl#";

        // Create the remover to be used to delete the rule from the ontology.
        OWLEntityRemover remover = new OWLEntityRemover(mng, Collections.singleton(owlmodel));

        // Create the rule axiom
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create((ruleNS + rule.getRuleName())));

        // Remove the rule
        ontoind.accept(remover);
        mng.applyChanges(remover.getChanges());
        remover.reset();

        // Check if the recipe ahs been removed
        if (owlmodel.containsIndividualInSignature(IRI.create((ruleNS + rule.getRuleName())))) return false;
        else return true;

    }

    /**
     * To save some change to the ontology loaded in the store.
     * 
     * FIXME: save using the Clerezza TcManager, or the Stanbol wrapper for it
     */
    @Override
    public void saveOntology() {
        try {
            FileOutputStream fos;
            if (this.ruleOntologyLocation.isEmpty()) {
                String sep = System.getProperty("file.separator");
                this.ruleOntologyLocation = System.getProperty("user.dir") + sep + "RuleConf" + sep
                                            + "rmi_config.owl";
                try {
                    fos = new FileOutputStream(ruleOntologyLocation);
                    OWLManager.createOWLOntologyManager().saveOntology(owlmodel,
                        owlmodel.getOWLOntologyManager().getOntologyFormat(owlmodel), fos);
                } catch (FileNotFoundException e) {
                    log.error("Cannot store the ontology ", e);
                } catch (OWLOntologyStorageException e) {
                    log.error("Cannot store the ontology ", e);
                }
            } else {
            	log.info("Rule Ontology Location: " + this.ruleOntologyLocation, this);
                fos = new FileOutputStream(ruleOntologyLocation);
                this.owlmodel.getOWLOntologyManager().saveOntology(owlmodel, fos);
            }

        } catch (OWLOntologyStorageException ex) {
            log.error("Cannot store the ontology ", ex);
        } catch (FileNotFoundException ex) {
            log.error("Cannot store the ontology ", ex);
        }

    }

    /**
     * To set new OWLOntology with stored rules and recipes.
     * 
     * @param owl
     *            {OWLOntology with new changes.}
     */
    @Override
    public void setStore(OWLOntology owl) {
        this.owlmodel = owl;
    }

}
