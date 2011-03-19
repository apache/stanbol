package org.apache.stanbol.rules.refactor;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.sparql.QueryEngine;
import org.apache.clerezza.rdf.jena.sparql.JenaSparqlEngine;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManager;
import org.apache.stanbol.reasoners.base.impl.KReSReasonerImpl;
import org.apache.stanbol.rules.base.api.KReSRule;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.KReSRuleList;
import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.apache.stanbol.rules.manager.changes.RecipeImpl;
import org.apache.stanbol.rules.manager.parse.KReSRuleParser;
import org.apache.stanbol.rules.refactor.api.SemionRefactorer;
import org.apache.stanbol.rules.refactor.api.SemionRefactoringException;
import org.apache.stanbol.rules.refactor.impl.SemionRefactorerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;



public class SemionRefactoringTest {

	static RuleStore ruleStore;
	static OWLOntology ontology;
	static IRI recipeIRI;

	@BeforeClass
	public static void setup() {

		recipeIRI = IRI
				.create("http://kres.iks-project.eu/ontology/meta/rmi_config.owl#MyTestRecipe");

		InputStream ontologyStream = SemionRefactoringTest.class
				.getResourceAsStream("/META-INF/test/testKReSOnt.owl");
		InputStream recipeStream = SemionRefactoringTest.class
				.getResourceAsStream("/META-INF/test/rmi.owl");

		try {
			final OWLOntology recipeModel = OWLManager
					.createOWLOntologyManager()
					.loadOntologyFromOntologyDocument(recipeStream);
			ontology = OWLManager.createOWLOntologyManager()
					.loadOntologyFromOntologyDocument(ontologyStream);

			ruleStore = new RuleStore() {

				@Override
				public void setStore(OWLOntology owl) {
					// TODO Auto-generated method stub

				}

				@Override
				public void saveOntology() throws OWLOntologyStorageException {
					// TODO Auto-generated method stub

				}

				@Override
				public RecipeList listRecipes() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Set<IRI> listIRIRecipes() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String getRuleStoreNamespace() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Recipe getRecipe(IRI recipeIRI)
						throws NoSuchRecipeException {
					Recipe recipe = null;

					if (recipeIRI != null) {
						OWLDataFactory factory = OWLManager.getOWLDataFactory();
						OWLIndividual recipeIndividual = factory
								.getOWLNamedIndividual(recipeIRI);
						if (recipeIndividual != null) {
							String ruleNS = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";

							/**
							 * First get the recipe description in the
							 * rule/recipe ontology.
							 */
							OWLDataProperty hasDescription = factory
									.getOWLDataProperty(IRI.create(ruleNS
											+ "hasDescription"));

							String recipeDescription = null;

							Set<OWLLiteral> descriptions = recipeIndividual
									.getDataPropertyValues(hasDescription,
											recipeModel);
							for (OWLLiteral description : descriptions) {
								recipeDescription = description.getLiteral();
							}

							/**
							 * Then retrieve the rules associated to the recipe
							 * in the rule store.
							 */
							OWLObjectProperty objectProperty = factory
									.getOWLObjectProperty(IRI.create(ruleNS
											+ "hasRule"));
							Set<OWLIndividual> rules = recipeIndividual
									.getObjectPropertyValues(objectProperty,
											recipeModel);

							String kReSRulesInKReSSyntax = "";

							/**
							 * Fetch the rule content expressed as a literal in
							 * KReSRule Syntax.
							 */
							OWLDataProperty hasBodyAndHead = factory
									.getOWLDataProperty(IRI.create(ruleNS
											+ "hasBodyAndHead"));
							for (OWLIndividual rule : rules) {

								Set<OWLLiteral> kReSRuleLiterals = rule
										.getDataPropertyValues(hasBodyAndHead,
												recipeModel);

								for (OWLLiteral kReSRuleLiteral : kReSRuleLiterals) {
									String ruleTmp = kReSRuleLiteral
											.getLiteral().replace("&lt;", "<");
									ruleTmp = ruleTmp.replace("&gt;", ">");
									kReSRulesInKReSSyntax += ruleTmp
											+ System
													.getProperty("line.separator");
								}
							}

							/**
							 * Create the Recipe object.
							 */

							KReSRuleList ruleList = KReSRuleParser.parse(
									kReSRulesInKReSSyntax).getkReSRuleList();
							recipe = new RecipeImpl(recipeIRI,
									recipeDescription, ruleList);
						} else {
							throw new NoSuchRecipeException(recipeIRI);
						}
					}

					return recipe;
				}

				@Override
				public OWLOntology getOntology() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String getFilePath() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public boolean addRecipe(IRI recipeIRI, String recipeDescription) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public Recipe addRuleToRecipe(String recipeID,
						String kReSRuleInKReSSyntax)
						throws NoSuchRecipeException {
					return null;

				}

				@Override
				public Recipe addRuleToRecipe(Recipe recipe,
						String kReSRuleInKReSSyntax) {
					return null;
					// TODO Auto-generated method stub

				}

				@Override
				public void createRecipe(String recipeID,
						String rulesInKReSSyntax) {
					// TODO Auto-generated method stub

				}

				@Override
				public boolean removeRecipe(Recipe recipe) {
					throw new UnsupportedOperationException(
							"Not supported yet.");
				}

				@Override
				public boolean removeRecipe(IRI recipeIRI) {
					throw new UnsupportedOperationException(
							"Not supported yet.");
				}

				@Override
				public boolean removeRule(KReSRule rule) {
					throw new UnsupportedOperationException(
							"Not supported yet.");
				}
			};
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void refactoringTest() throws Exception {
		Dictionary<String, Object> emptyConfig = new Hashtable<String, Object>();


		class SpecialTcManager extends TcManager {
			public SpecialTcManager(QueryEngine qe, WeightedTcProvider wtcp) {
				super();
				bindQueryEngine(qe);
				bindWeightedTcProvider(wtcp);
			}
		}

		QueryEngine qe = new JenaSparqlEngine();
		WeightedTcProvider wtcp = new SimpleTcProvider();
		TcManager tcm = new SpecialTcManager(qe, wtcp);

	      KReSONManager onm = new ONManager(tcm,wtcp, emptyConfig);
		SemionRefactorer refactorer = new SemionRefactorerImpl(null,
				new Serializer(), tcm, onm,
				ruleStore, new KReSReasonerImpl(emptyConfig), emptyConfig);
		try {
			refactorer.ontologyRefactoring(ontology, recipeIRI);
		} catch (SemionRefactoringException e) {
			fail("Error while refactoring.");
		} catch (NoSuchRecipeException e) {
			fail("Error while refactoring: no such recipe");
		}
	}

}
