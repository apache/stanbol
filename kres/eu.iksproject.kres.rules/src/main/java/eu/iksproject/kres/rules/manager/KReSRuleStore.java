/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.rules.manager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.rules.KReSRule;
import eu.iksproject.kres.api.rules.NoSuchRecipeException;
import eu.iksproject.kres.api.rules.Recipe;
import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.api.rules.util.KReSRuleList;
import eu.iksproject.kres.api.semion.util.RecipeList;
import eu.iksproject.kres.rules.KReSKB;
import eu.iksproject.kres.rules.parser.KReSRuleParser;

/**
 * This class creates an OWLOntology object where to store rules and recipes.
 * 
 * @author elvio
 * @author andrea.nuzzolese
 * 
 */

@Component(immediate = true, metatype = true)
@Service(RuleStore.class)
public class KReSRuleStore implements RuleStore {

	@Reference
	KReSONManager onManager;

	@Property(value = "")
	public static final String RULE_ONTOLOGY = "rule.ontology";

	@Property(value = "http://kres.iks-project.eu/ontology/meta/rmi.owl#")
	public static final String RULE_ONTOLOGY_NAMESPACE = "rule.ontology.namespace";

	private final Logger log = LoggerFactory.getLogger(getClass());
	private OWLOntology owlmodel;
	private String file;
	private String alias;
	private RuleStore ruleStore;
	private File owlfile;
	private String ruleOntologyNS;

	private KReSRuleParser kReSRuleParser;

	/**
	 * This construct returns KReSRuleStore object with inside an ontology where
	 * to store the rules.
	 * 
	 * This default constructor is <b>only</b> intended to be used by the OSGI
	 * environment with Service Component Runtime support.
	 * <p>
	 * DO NOT USE to manually create instances - the KReSRuleStore instances do
	 * need to be configured! YOU NEED TO USE
	 * {@link #KReSRuleStore(KReSONManager, Dictionary)} or its overloads, to
	 * parse the configuration and then initialise the rule store if running
	 * outside a OSGI environment.
	 */
	public KReSRuleStore() {

		/*
		 * The constructor should be empty as some issue derives from a filled
		 * one. The old version can be invoked with KReSRuleStore(null)
		 */
	}

	public KReSRuleStore(KReSONManager onm,
			Dictionary<String, Object> configuration) {
		this();
		this.onManager = onm;
		activate(configuration);
	}

	/**
	 * This construct returns an ontology where to store the rules.
	 * 
	 * @param filepath
	 *            {Ontology file path previously stored.}
	 */
	public KReSRuleStore(KReSONManager onm,
			Dictionary<String, Object> configuration, String filepath) {
		/*
		 * FIXME : This won't work if the activate() method is called at the end
		 * of the constructor, like it is for other components. Constructors
		 * should not override activate().
		 */
		// This recursive constructor call will also invoke activate()
		this(onm, configuration);

		if (filepath.isEmpty()) {
			try {
				Properties configProps = System.getProperties();
				String userdir = configProps.getProperty("user.dir");
				String respath = "src/main/resources/";
				userdir = userdir
						.substring(0, userdir.lastIndexOf("kres.") + 5)
						+ "rules/";
				String filepath2 = "RuleOntology/rmi_config.owl";
				this.file = userdir + respath + filepath2;

				this.owlfile = new File(file);

				OWLOntologyManager owlmanager = OWLManager
						.createOWLOntologyManager();
				owlfile.setWritable(true);
				this.owlmodel = owlmanager
						.loadOntologyFromOntologyDocument(owlfile);

			} catch (Exception io) {
				log.error(io.getLocalizedMessage(), io);
				this.owlmodel = null;
			}
		} else {
			this.file = filepath;
			this.owlfile = new File(file);
			owlfile.setWritable(true);
			OWLOntologyManager owlmanager = OWLManager
					.createOWLOntologyManager();
			try {
				this.owlmodel = owlmanager
						.loadOntologyFromOntologyDocument(owlfile);
			} catch (OWLOntologyCreationException oce) {
				log.error(oce.getLocalizedMessage(), oce);
				this.owlmodel = null;
			}

		}
		// activate(configuration);

	}

	/**
	 * This construct returns an ontology where to store the rules.
	 * 
	 * @param owl
	 *            {OWLOntology object contains rules and recipe}
	 */
	public KReSRuleStore(KReSONManager onm,
			Dictionary<String, Object> configuration, OWLOntology owl) {
		/*
		 * FIXME : This won't work if the activate() method is called at the end
		 * of the constructor, like it is for other components. Constructors
		 * should not override activate().
		 */
		// This recursive constructor call will also invoke activate()
		this(onm, configuration);

		try {
			this.owlmodel = owl;
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
			this.owlmodel = null;
		}
		// activate(configuration);
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

	/**
	 * Used to configure an instance within an OSGi container.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Activate
	protected void activate(ComponentContext context) throws IOException {
		log.info("in " + KReSRuleStore.class + " activate with context "
				+ context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	protected void activate(Dictionary<String, Object> configuration) {

		this.file = (String) configuration.get(RULE_ONTOLOGY);
		this.ruleOntologyNS = (String) configuration
				.get(RULE_ONTOLOGY_NAMESPACE);

		if (file == null || file.equals("")) {
			// InputStream ontologyStream =
			// this.getClass().getResourceAsStream("/META-INF/conf/KReSOntologyRules.owl");
			IRI inputontology = IRI
					.create("http://ontologydesignpatterns.org/ont/iks/kres/rmi_config.owl");
			// if (inputontology == null) {
			// log.err("Input ontology is null");
			// }
			try {
				owlmodel = OWLManager.createOWLOntologyManager()
						.loadOntologyFromOntologyDocument(inputontology);
			} catch (OWLOntologyCreationException e) {
				log.error("Cannot create the ontology "
						+ inputontology.toString(), e);
			} catch (Exception e) {
				log.error("Rule Store: no rule ontology available.");
			}

			if (owlmodel != null) {

				File dirs = new File("./KReSConf");
				if (!dirs.exists())
					dirs.mkdir();
				file = "./KReSConf/rmi_config.owl";

				FileOutputStream fos;
				try {
					fos = new FileOutputStream(file);
					OWLManager.createOWLOntologyManager().saveOntology(
							owlmodel,
							owlmodel.getOWLOntologyManager().getOntologyFormat(
									owlmodel), fos);
				} catch (FileNotFoundException e) {
					log.error("Cannot save the RMI configuration ontology", e);
				} catch (OWLOntologyStorageException e) {
					log.error("Cannot save the RMI configuration ontology", e);
				}
			}
		} else {
			IRI pathIri = IRI.create(file);
			try {
				owlmodel = OWLManager.createOWLOntologyManager()
						.loadOntologyFromOntologyDocument(pathIri);
			} catch (OWLOntologyCreationException e) {
				log.error("Cannot load the RMI configuration ontology", e);
			} catch (Exception e) {
				log.error("Rule Store: no rule ontology available.");
			}
		}

	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		log.info("in " + KReSRuleStore.class + " deactivate with context "
				+ context);
	}

	@Override
	public Set<IRI> listIRIRecipes() {
		Set<IRI> recipeIRIs = null;
		String ruleNS = owlmodel.getOntologyID().toString().replace("<", "")
				.replace(">", "")
				+ "#";
                
		OWLDataFactory factory = onManager.getOwlFactory();
		OWLClass recipeOWLClass = factory.getOWLClass(IRI.create(ruleNS
				+ "Recipe"));
		Set<OWLIndividual> recipeIndividuals = recipeOWLClass
				.getIndividuals(owlmodel);

		if (recipeIndividuals != null && recipeIndividuals.size() > 0) {
			recipeIRIs = new HashSet<IRI>();
			for (OWLIndividual recipeIndividual : recipeIndividuals) {
				if (recipeIndividual instanceof OWLNamedIndividual) {
					recipeIRIs.add(((OWLNamedIndividual) recipeIndividual)
							.getIRI());
				}
			}
		}

		return recipeIRIs;
	}

	@Override
	public Recipe getRecipe(IRI recipeIRI) throws NoSuchRecipeException {
		log.debug("Called get recipe for id: "+recipeIRI.toString());
		Recipe recipe = null;

		if (onManager != null && recipeIRI != null) {
			OWLDataFactory factory = onManager.getOwlFactory();
			OWLIndividual recipeIndividual = factory
					.getOWLNamedIndividual(recipeIRI);
			if (recipeIndividual != null) {
				// String ruleNS =
				// owlmodel.getOntologyID().toString().replace("<","").replace(">","")+"#";

				// OWLObjectProperty objectProperty =
				// factory.getOWLObjectProperty(IRI.create(ruleNS + "hasRule"));

				String ruleNS = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";

				/**
				 * First get the recipe description in the rule/recipe ontology.
				 */
				OWLDataProperty hasDescription = factory.getOWLDataProperty(IRI
						.create(ruleNS + "hasDescription"));

				String recipeDescription = null;

				Set<OWLLiteral> descriptions = recipeIndividual
						.getDataPropertyValues(hasDescription, owlmodel);
				for (OWLLiteral description : descriptions) {
					recipeDescription = description.getLiteral();
				}

				/**
				 * Then retrieve the rules associated to the recipe in the rule
				 * store.
				 */
				OWLObjectProperty objectProperty = factory
						.getOWLObjectProperty(IRI.create(ruleNS + "hasRule"));
				Set<OWLIndividual> rules = recipeIndividual
						.getObjectPropertyValues(objectProperty, owlmodel);

				String kReSRulesInKReSSyntax = "";

				log.debug("The recipe " + recipeIRI.toString() + " has "
						+ rules.size() + " rules.");
			
				/**
				 * Fetch the rule content expressed as a literal in KReSRule
				 * Syntax.
				 */
				boolean firstLoop = true;
				OWLDataProperty hasBodyAndHead = factory.getOWLDataProperty(IRI
						.create(ruleNS + "hasBodyAndHead"));
				
				for (OWLIndividual rule : rules) {
					log.debug("Getting rule : " + rule.toStringID(),this);
					
					Set<OWLLiteral> kReSRuleLiterals = rule
							.getDataPropertyValues(hasBodyAndHead, owlmodel);

					if (!firstLoop) {
						kReSRulesInKReSSyntax += " . ";
					} else {
						firstLoop = false;
					}

					for (OWLLiteral kReSRuleLiteral : kReSRuleLiterals) {
						String ruleTmp = kReSRuleLiteral.getLiteral().replace(
								"&lt;", "<");
						ruleTmp = ruleTmp.replace("&gt;", ">");
						log.debug("Rule is: " + ruleTmp);
						kReSRulesInKReSSyntax += ruleTmp;
					}
				}

				/**
				 * Create the Recipe object.
				 */
				log.debug("Recipe in KReS Syntax : " + kReSRulesInKReSSyntax);

				KReSRuleList ruleList = null;

				if (!kReSRulesInKReSSyntax.isEmpty()) {
					ruleList = generateKnowledgeBase(kReSRulesInKReSSyntax);
				}

				recipe = new RecipeImpl(recipeIRI, recipeDescription, ruleList);

			} else {
				throw new NoSuchRecipeException(recipeIRI);
			}
		}

		return recipe;

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
					log.error("Recipe missing: "+recipeIRI.toString(),e);
				}
			}
		}
		return recipies;
	}

	/**
	 * To get the file path usde to load the ontology.
	 * 
	 * @return {A string contains the complete file path.}
	 */
	@Override
	public String getFilePath() {
		return this.file;
	}

	/**
	 * To save some change to the ontology loaded in the store.
	 */
	@Override
	public void saveOntology() {
		try {
			FileOutputStream fos;
			if (this.file.isEmpty()) {
				File dirs = new File("./KReSConf");
				if (!dirs.exists())
					dirs.mkdir();
				file = "./KReSConf/rmi_config.owl";
				try {
					fos = new FileOutputStream(file);
					OWLManager.createOWLOntologyManager().saveOntology(
							owlmodel,
							owlmodel.getOWLOntologyManager().getOntologyFormat(
									owlmodel), fos);
				} catch (FileNotFoundException e) {
					log.error("Cannot store the ontology ",e);
				} catch (OWLOntologyStorageException e) {
					log.error("Cannot store the ontology ",e);
				}
			} else {
				fos = new FileOutputStream(file);
				this.owlmodel.getOWLOntologyManager().saveOntology(owlmodel,
						fos);
			}

		} catch (OWLOntologyStorageException ex) {
			log.error("Cannot store the ontology ",ex);
		} catch (FileNotFoundException ex) {
			log.error("Cannot store the ontology ",ex);
		}

	}

	@Override
	public String getRuleStoreNamespace() {
		return owlmodel.getOntologyID().getOntologyIRI() + "#";
	}

	/*
	 * Moved form KReSAddRecipe class. The KReSAddRecipe should not be used
	 * anymore.
	 */
	@Override
	public boolean addRecipe(IRI recipeIRI, String recipeDescription) {
		boolean ok = false;
		log.debug(
				"Adding recipe " + recipeIRI + " [" + recipeDescription + "]",
				this);
		OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = OWLManager.getOWLDataFactory();

		String owlIDrmi = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";

		OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
		OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipeIRI);
		OWLDataProperty description = factory.getOWLDataProperty(IRI
				.create(owlIDrmi + "hasDescription"));
		OWLDataPropertyAssertionAxiom dataPropAssertion;

		if ((recipeIRI != null)) {
			if (!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(
					ontocls, ontoind))) {

				// Add the recipe istance
				OWLClassAssertionAxiom classAssertion = factory
						.getOWLClassAssertionAxiom(ontocls, ontoind);
				owlmanager.addAxiom(owlmodel, classAssertion);

				// Add description
				if ((recipeDescription != null) && !recipeDescription.isEmpty()) {
					// Add the rule description
					dataPropAssertion = factory
							.getOWLDataPropertyAssertionAxiom(description,
									ontoind, recipeDescription);
					owlmanager.addAxiom(owlmodel, dataPropAssertion);
					ok = true;
				}
			} else {
				log.error("The recipe with name " + recipeIRI
						+ " already exists. Please check the name.");
				ok = false;
				return (ok);
			}

		} else {
			log
					.error("The recipe with name and the set of rules cannot be empity or null.");
			ok = false;
			return (ok);
		}

		if (ok) {
			setStore(owlmodel);
		}

		return (ok);
	}

	/**
	 * 
	 * @param recipeIRI
	 *            the IRI of the recipe
	 * @param kReSRule
	 *            the rule in KReSRule syntax
	 */
	@Override
	public Recipe addRuleToRecipe(String recipeID, String kReSRuleInKReSSyntax)
			throws NoSuchRecipeException {

		Recipe recipe = getRecipe(IRI.create(recipeID));
		return addRuleToRecipe(recipe, kReSRuleInKReSSyntax);

	}

	/**
	 * 
	 * @param recipe
	 *            the recipe
	 * @param kReSRule
	 *            the rule in KReSRule syntax
	 * 
	 * @return the recipe we the new rule.
	 */
	@Override
	public Recipe addRuleToRecipe(Recipe recipe, String kReSRuleInKReSSyntax) {
		log.debug("Adding rule to recipe "+recipe);
		
		/**
		 * Get the OWLDataFactory.
		 */
		OWLDataFactory factory = OWLManager.getOWLDataFactory();

		/**
		 * Add the rule to the recipe in the rule ontology managed by the
		 * RuleStore. First we define the object property hasRule and then we
		 * add the literal that contains the rule in KReSRule Syntax to the
		 * recipe individual.
		 */
		String ruleNS = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";
		OWLObjectProperty hasRule = factory.getOWLObjectProperty(IRI
				.create(ruleNS + "hasRule"));
		OWLDataProperty hasBodyAndHead = factory.getOWLDataProperty(IRI
				.create(ruleNS + "hasBodyAndHead"));

		/**
		 * The IRI of the recipe is fetched from the recipe object itself. From
		 * that IRI is obtained the recipe owl individual.
		 */
		IRI recipeIRI = recipe.getRecipeID();
		OWLNamedIndividual reipeIndividual = factory
				.getOWLNamedIndividual(recipeIRI);

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		/**
		 * Finally also the in-memory representation of the Recipe passed as
		 * input is modified.
		 */
		KReSKB kReSKB = KReSRuleParser.parse(kReSRuleInKReSSyntax);
		KReSRuleList ruleList = kReSKB.getkReSRuleList();
		for (KReSRule rule : ruleList) {

			/**
			 * The rule must be added to the ontology, so 1. an IRI is created
			 * from its name 2. the KReS syntax is added to the rule as a
			 * literal through the hasBobyAndHe data property. 3. the rule is
			 * associated to the recipe by means of the hasRule object property,
			 * so that the triple <a_recipe hasRule a_rule> is added to the rule
			 * ontology.
			 * 
			 */
			IRI ruleIRI = IRI.create(ruleNS + rule.getRuleName());
			OWLNamedIndividual ruleIndividual = factory
					.getOWLNamedIndividual(ruleIRI);

			OWLAxiom hasBodyAndHeadAxiom = factory
					.getOWLDataPropertyAssertionAxiom(hasBodyAndHead,
							ruleIndividual, rule.toKReSSyntax());
			manager.addAxiom(owlmodel, hasBodyAndHeadAxiom);

			OWLAxiom hasRuleAxiom = factory.getOWLObjectPropertyAssertionAxiom(
					hasRule, reipeIndividual, ruleIndividual);
			manager.addAxiom(owlmodel, hasRuleAxiom);

			/**
			 * The KReSRule is added to the Recipe in-memory object.
			 */
			recipe.addKReSRule(rule);
		}

		return recipe;
	}

	@Override
	public void createRecipe(String recipeID, String rulesInKReSSyntax) {
		log.debug("Create recipe " + recipeID + " with rules in kres sytnax "
				+ rulesInKReSSyntax, this);
		KReSKB kb = KReSRuleParser.parse(rulesInKReSSyntax);
		KReSRuleList rules = kb.getkReSRuleList();

		KReSAddRule addRule = new KReSAddRule(this);

		Vector<IRI> ruleVectorIRIs = new Vector<IRI>();
		log.debug("Rules are " + rules.size());
		for (KReSRule rule : rules) {
			log.debug("Creating rule " + rule.getRuleName());
			String kReSSyntax = rule.toKReSSyntax();

			log.debug("Rule in KReS Syntax : " + kReSSyntax);
			addRule.addRule(IRI.create(rule.getRuleName()), kReSSyntax, null);
			ruleVectorIRIs.add(IRI.create(rule.getRuleName()));
		}

		if (ruleVectorIRIs.size() > 0) {
			log.debug("Adding rules: " + ruleVectorIRIs.size());
			KReSAddRecipe addRecipe = new KReSAddRecipe(this);
			addRecipe.addRecipe(IRI.create(recipeID), ruleVectorIRIs, null);
		}
		
	}

	private KReSRuleList generateKnowledgeBase(String kReSRulesInKReSSyntax) {
		KReSKB kb = KReSRuleParser.parse(kReSRulesInKReSSyntax);
		return kb.getkReSRuleList();
	}

        @Override
        public boolean removeRecipe(Recipe recipe) {

            OWLOntologyManager mng = owlmodel.getOWLOntologyManager();
            OWLDataFactory factory = mng.getOWLDataFactory();

            //Create the remover to be used to delete the recipe from the ontology.
		OWLEntityRemover remover = new OWLEntityRemover(mng, Collections
				.singleton(owlmodel));

            //Create the recipe axiom
		OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipe
				.getRecipeID());

            //Remove the recipe
            ontoind.accept(remover);
            mng.applyChanges(remover.getChanges());
            remover.reset();

            //Check if the recipe ahs been removed
            if(owlmodel.containsIndividualInSignature(recipe.getRecipeID()))
                return false;
            else
                return true;

        }

        @Override
        public boolean removeRecipe(IRI recipeIRI){
            Recipe recipe;
            try {
                recipe = getRecipe(recipeIRI);
                return removeRecipe(recipe);
            } catch (NoSuchRecipeException ex) {
                log.error("Exception cougth: ",ex);
                return false;
            }
        }

    @Override
    public boolean removeRule(KReSRule rule) {

            OWLOntologyManager mng = owlmodel.getOWLOntologyManager();
            OWLDataFactory factory = mng.getOWLDataFactory();
            String ruleNS = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";

            //Create the remover to be used to delete the rule from the ontology.
		OWLEntityRemover remover = new OWLEntityRemover(mng, Collections
				.singleton(owlmodel));

            //Create the rule axiom
		OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI
				.create((ruleNS + rule.getRuleName())));

            //Remove the rule
            ontoind.accept(remover);
            mng.applyChanges(remover.getChanges());
            remover.reset();

            //Check if the recipe ahs been removed
		if (owlmodel.containsIndividualInSignature(IRI.create((ruleNS + rule
				.getRuleName()))))
                return false;
            else
                return true;

    }

}
