package eu.iksproject.kres.manager.registry.model.impl;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.OWLAxiomFilter;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stlab.xd.registry.io.XDRegistrySource;
import org.stlab.xd.registry.models.AbstractRegistryItem;
import org.stlab.xd.registry.models.Registry;
import org.stlab.xd.registry.models.RegistryContentException;
import org.stlab.xd.registry.models.RegistryItem;
import org.stlab.xd.registry.models.RegistryLibrary;
import org.stlab.xd.registry.models.RegistryOntology;
import org.stlab.xd.utils.RDFSLabelGetter;
import org.stlab.xd.vocabulary.CODOVocabulary;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.registry.KReSRegistryLoader;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.registry.cache.ODPRegistryCacheException;
import eu.iksproject.kres.manager.registry.cache.ODPRegistryCacheManager;
import eu.iksproject.kres.manager.registry.cache.URIUnresolvableException;
import eu.iksproject.kres.manager.util.OntologyLoaderPrinter;

public class RegistryLoader implements KReSRegistryLoader {

	private final OWLClass cRegistryLibrary;

	private KReSONManager onm;

	private Logger log = LoggerFactory.getLogger(getClass());

	private final OWLObjectProperty isPartOf, isOntologyOf;

	private final IRI mergedOntologyIRI = IRI
			.create(CODOVocabulary.REPOSITORY_MERGED_ONTOLOGY);

	private Map<URI, OWLOntology> registryOntologiesCache = new HashMap<URI, OWLOntology>();

	/**
	 */
	public RegistryLoader(KReSONManager onm) {
		this.onm = onm;
		OWLDataFactory factory = OWLManager.getOWLDataFactory();
		cRegistryLibrary = factory.getOWLClass(IRI
				.create(CODOVocabulary.CODD_OntologyLibrary));
		isPartOf = factory.getOWLObjectProperty(IRI
				.create(CODOVocabulary.PARTOF_IsPartOf));
		isOntologyOf = factory.getOWLObjectProperty(IRI
				.create(CODOVocabulary.ODPM_IsOntologyOf));
	}

	public Set<OWLOntology> gatherOntologies(RegistryItem registryItem,
			OWLOntologyManager manager, boolean recurseRegistries)
			throws OWLOntologyCreationException {

		Set<OWLOntology> result = new HashSet<OWLOntology>();

		if (registryItem instanceof Registry)
			for (RegistryItem item : ((Registry) registryItem).getChildren())
				try {
					result.addAll(gatherOntologies(item, manager,
							recurseRegistries));
				} catch (OWLOntologyCreationException e) {
					log
							.warn(
									"KReS :: [NONFATAL] Could not gather ontologies for registry "
											+ registryItem.getName()
											+ ". Skipping.", e);
					continue;
				}
		else if (registryItem.isOntology())
			try {
				result.add(manager.loadOntologyFromOntologyDocument(IRI
						.create(((RegistryOntology) registryItem).getURL())));
			} catch (OWLOntologyAlreadyExistsException ex) {
				// E chissenefrega, ce la aggiungiamo lo stesso.
				result.add(manager.getOntology(ex.getOntologyID()));
			} catch (OWLOntologyCreationIOException ex) {
				// Che ce potemo fa'?
			} catch (URISyntaxException e) {
				log.warn("KReS :: [NONFATAL] Malformed URI for ontology "
						+ registryItem.getName() + ". Skipping.", e);
			}
		else if (registryItem.isLibrary())
			for (RegistryItem item : ((RegistryLibrary) registryItem)
					.getChildren()) {
				result
						.addAll(gatherOntologies(item, manager,
								recurseRegistries));
			}
		return result;
	}

	public RegistryLibrary getLibrary(Registry reg, IRI libraryID) {
		for (RegistryItem child : reg.getChildren()) {
			try {
				if (child.isLibrary()
						&& IRI.create(child.getURL()).equals(libraryID))
					return (RegistryLibrary) child;
			} catch (URISyntaxException e) {
				// If some URL is not well-formed here and there, sticazzi
				continue;
			}
		}
		return null;
	}

	private OWLOntology getMergedOntology(IRI registryLocation)
			throws RegistryContentException {
		try {
			return getMergedOntology(registryLocation.toURI().toURL());
		} catch (MalformedURLException e) {
			log.warn(
					"KReS :: [NONFATAL] Malformed URI for merged ontology from registry "
							+ registryLocation, e);
			return null;
		}
	}

	private OWLOntology getMergedOntology(URL registryLocation)
			throws RegistryContentException {
		OWLOntology ontology = null;

		try {
			IRI mergedOntology = mergedOntologyIRI.resolve("#"
					+ URLEncoder.encode(registryLocation.toString(), "UTF-8"));
			if (!ODPRegistryCacheManager.registryContains(mergedOntology
					.toURI())) {

				// final OWLOntology ont =
				// getOntologyForRegistryLocation(registryLocation
				// .toURI());

				final OWLOntology ont = getOntologyForRegistryLocationNoCached(registryLocation
						.toURI());
				if (ont == null)
					throw new RegistryContentException(
							new NullPointerException("Registry unavailable: "
									+ registryLocation.toURI()));

				OWLOntologySetProvider provider = new OWLOntologySetProvider() {
					public Set<OWLOntology> getOntologies() {
						return ODPRegistryCacheManager.getManager()
								.getImportsClosure(ont);
					}
				};
				final OWLDataFactory factory = ODPRegistryCacheManager
						.getManager().getOWLDataFactory();

				// We filter only interesting axioms
				OWLAxiomFilter filter = new OWLAxiomFilter() {
					public boolean passes(OWLAxiom arg0) {
						if (arg0
								.getSignature()
								.contains(
										factory
												.getOWLClass(IRI
														.create(CODOVocabulary.CODD_OntologyLibrary)))
								|| arg0
										.getSignature()
										.contains(
												factory
														.getOWLClass(IRI
																.create(CODOVocabulary.CODK_Ontology)))
								|| arg0
										.getSignature()
										.contains(
												factory
														.getOWLObjectProperty(IRI
																.create(CODOVocabulary.ODPM_IsOntologyOf)))
								|| arg0
										.getSignature()
										.contains(
												factory
														.getOWLObjectProperty(IRI
																.create(CODOVocabulary.PARTOF_IsPartOf)))
								|| arg0
										.getSignature()
										.contains(
												factory
														.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL
																.getIRI()))) {
							return true;
						}
						return false;
					}
				};

				OWLOntologyMerger merger = new OWLOntologyMerger(provider,
						filter);
				OWLOntology merged = merger.createMergedOntology(
						ODPRegistryCacheManager.getManager(), mergedOntology);
				ODPRegistryCacheManager.addResource(merged, mergedOntology
						.toURI());
				ontology = merged;
			} else {
				ontology = ODPRegistryCacheManager.getOntology(mergedOntology
						.toURI());
			}
		} catch (URIUnresolvableException e) {
			throw new RegistryContentException(e);
		} catch (OWLOntologyCreationException e) {
			throw new RegistryContentException(e);
		} catch (OWLOntologyChangeException e) {
			throw new RegistryContentException(e);
		} catch (ODPRegistryCacheException e) {
			throw new RegistryContentException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RegistryContentException(e);
		} catch (URISyntaxException e) {
			throw new RegistryContentException(e);
		}
		return ontology;
	}

	private OWLOntology getOntologyForRegistryLocation(URI location) {
		return registryOntologiesCache.get(location);
	}

	private OWLOntology getOntologyForRegistryLocationNoCached(URI location) {
		try {
			return OWLManager.createOWLOntologyManager()
					.loadOntologyFromOntologyDocument(IRI.create(location));
		} catch (OWLOntologyCreationException e) {
			log.error("KReS :: Registry loader failed to load ontology at "
					+ location, e);
			return null;
		}
	}

	public Object getParent(Object child) {
		if (child instanceof AbstractRegistryItem) {
			return ((RegistryItem) child).getParent();
		}
		return null;
	}

	private Set<OWLIndividual> getParentContainer(OWLNamedIndividual child,
			OWLOntology ontology) {

		if (child.getObjectPropertyValues(ontology).containsKey(isPartOf)
				|| child.getObjectPropertyValues(ontology).containsKey(
						isOntologyOf)) {
			Set<OWLIndividual> partOfSet = child.getObjectPropertyValues(
					ontology).get(isPartOf);
			Set<OWLIndividual> ontologyOfSet = child.getObjectPropertyValues(
					ontology).get(isOntologyOf);

			Set<OWLIndividual> mergedSet = new HashSet<OWLIndividual>();
			if (partOfSet != null)
				mergedSet.addAll(partOfSet);
			if (ontologyOfSet != null)
				mergedSet.addAll(ontologyOfSet);
			return mergedSet;
		} else
			return new HashSet<OWLIndividual>();
	}

	private Set<OWLNamedIndividual> getParts(OWLIndividual parent,
			OWLOntology ontology) {
		Set<OWLNamedIndividual> indies = ontology.getIndividualsInSignature();
		Iterator<OWLNamedIndividual> iter = indies.iterator();
		Set<OWLNamedIndividual> tor = new HashSet<OWLNamedIndividual>();
		// For each individual in this ontology
		while (iter.hasNext()) {
			OWLNamedIndividual n = iter.next();
			// Get its parent wrt to isPartOf or isOntologyOf relationships
			for (OWLIndividual i : this.getParentContainer(n, ontology)) {
				if (i.equals(parent)) {
					tor.add(n);
					break;
				}
			}
		}
		return tor;
	}

	private List<Registry> getRegistries() {
		List<Registry> registries = new ArrayList<Registry>();
		// String storedStringValue = XDRegistryPlugin.getDefault()
		// .getPreferenceStore().getString(
		// PreferenceConstants.P_ODP_REGISTRIES);
		String[] regs = new String[] {}/*
										 * URLListEditor
										 * .parsePreferenceStoreValue
										 * (storedStringValue)
										 */;

		for (int i = 0; i < regs.length; i++) {
			Registry registry1 = null;
			try {
				// TODO Find a way to obtain registry names
				String registryName = ""/*
										 * URLListEditor
										 * .parseNameValueString(regs[i])[0]
										 */;
				// TODO Find a way to obtain registry locations
				String registryLocation = ""/*
											 * URLListEditor
											 * .parseNameValueString(regs[i])[1]
											 */;
				registry1 = new Registry(registryName);
				registry1.setURL(new URL(registryLocation));
			} catch (Exception e) {
				if (registry1 != null) {
					registry1.setError(e.getLocalizedMessage());
					log.error("KReS :: Error on ODP registry: "
									+ registry1.getName(), e);
				}
			}
			if (registry1 != null)
				registries.add(registry1);
			else
				log.error("KReS :: Cannot load ODP registry: " + regs[i]);
		}
		return registries;
	}

	private List<Registry> getRegistries(XDRegistrySource source) {

		List<Registry> registries = new ArrayList<Registry>();

		if (source.getPhysicalIRI() != null) {

		} else if (source.isInputStreamAvailable()) {

		} else if (source.isReaderAvailable()) {

		}

		return registries;
	}

	private RegistryLibrary getTree(OWLNamedIndividual i, OWLOntology ontology) {

		RegistryLibrary to = new RegistryLibrary(new RDFSLabelGetter(ontology,
				i.getIRI(), false).getPreferred());
		try {
			Set<OWLNamedIndividual> children = this.getParts(i, ontology);
			if (children.size() == 0)
				return to;
			for (OWLNamedIndividual childIndividual : children) {
				if (isLibrary(childIndividual, ontology)) {
					RegistryLibrary t = this.getTree(childIndividual, ontology);
					to.addChild(t);
				} else if (isOntology(childIndividual, ontology)) {
					RegistryOntology t = new RegistryOntology(
							new RDFSLabelGetter(ontology, childIndividual
									.getIRI(), false).getPreferred());
					t.setURL(childIndividual.getIRI().toURI().toURL());
					to.addChild(t);
				}
			}
		} catch (MalformedURLException e) {
			log.error(
					"KReS :: MalformedURLException caught while getting tree for "
							+ i.getIRI(), e);

		}
		return to;
	}

	public boolean hasChildren(Object parent) {
		if (parent instanceof RegistryLibrary)
			return ((RegistryLibrary) parent).hasChildren();
		return false;
	}

	public boolean hasLibrary(Registry reg, IRI libraryID) {
		for (RegistryItem child : reg.getChildren()) {
			try {
				if (child.isLibrary()
						&& IRI.create(child.getURL()).equals(libraryID))
					return true;
			} catch (URISyntaxException e) {
				// If some URL is not well-formed here and there, sticazzi
				continue;
			}
		}
		return false;
	}

	private boolean isLibrary(OWLIndividual indy, OWLOntology ontology) {
		OWLClass folderClass = OWLManager.getOWLDataFactory().getOWLClass(
				IRI.create(CODOVocabulary.CODD_OntologyLibrary));
		return (folderClass.getIndividuals(ontology).contains(indy));
	}

	private boolean isOntology(OWLIndividual indy, OWLOntology ontology) {
		OWLClass ontologyClass = OWLManager.getOWLDataFactory().getOWLClass(
				IRI.create(CODOVocabulary.CODK_Ontology));
		return (ontologyClass.getIndividuals(ontology).contains(indy));
	}

	public void loadLocations() throws RegistryContentException {

		try {

			registryOntologiesCache.clear();
			List<Registry> registries = getRegistries();

			int regsize = registries.size();
			int c = 0;
			for (Registry current : registries) {
				c++;
				log.debug("Loading " + current.toString() + " [" + c + "/"
						+ regsize + "]");
				if (!ODPRegistryCacheManager.registryContains(current.getURL()
						.toURI())) {
					try {
						log.debug("Fetching: " + current.getURL().toURI());
						registryOntologiesCache.put(current.getURL().toURI(),
								ODPRegistryCacheManager.getOntology(current
										.getURL().toURI()));
					} catch (URIUnresolvableException e) {
						log.error("KReS :: could not resolve URI "
										+ current.getURL().toURI(), e);
						registryOntologiesCache.put(current.getURL().toURI(),
								null);
					} catch (ODPRegistryCacheException e) {
						log.error("KReS :: failed to cache ontology "
										+ current.getURL().toURI(), e);
						registryOntologiesCache.put(current.getURL().toURI(),
								null);
					}
				}
			}
			c = 0;
			for (Registry registry : registries) {
				c++;
				try {
					registry = setupRegistry(registry);
				} catch (RegistryContentException e) {
					registry.setError(" [Unable to load from location "
							+ registry.getURL().toString() + "]");
				}
			}
		} catch (Throwable th) {
			log
					.error(
							"KreS :: Exception occurred while trying to get registry locations.",
							th);
		}
	}

	/**
	 * The ontology at <code>physicalIRI</code> may in turn include more than
	 * one registry.
	 * 
	 * @param physicalIRI
	 * @return
	 */
	public Set<Registry> loadRegistriesEager(IRI physicalIRI) {

		Set<Registry> results = new HashSet<Registry>();
		OWLOntologyManager mgr = onm.getOwlCacheManager();// getManager();

		try {
			OWLOntology ontology = mgr.loadOntology(physicalIRI);
			for (OWLIndividual ind : cRegistryLibrary.getIndividuals(ontology))
				if (ind.isNamed()) {
					OWLNamedIndividual nind = ind.asOWLNamedIndividual();
					IRI regiri = nind.getIRI();
					Registry registry = new Registry(regiri.getFragment());
					try {
						// TODO: avoid using toURL crap
						registry.setURL(regiri.toURI().toURL());
					} catch (MalformedURLException e) {
						// Why should a well-formed IRI be a malformed URL
						// anyway ?
						log.warn("KReS :: ontology document IRI " + physicalIRI
								+ " matches a malformed URI pattern.", e);
					}
					// Find the ontologies in this registry
					// If this is individual is not "ontology of" or "part of",
					// then proceed.
					if (!nind.getObjectPropertyValues(ontology).containsKey(
							isPartOf)
							&& !nind.getObjectPropertyValues(ontology)
									.containsKey(isOntologyOf)) {
						registry.addChild(this.getTree(
								(OWLNamedIndividual) nind, ontology));
					}
					results.add(registry);
				}
		} catch (OWLOntologyAlreadyExistsException e) {
			log.warn("KReS :: ontology " + e.getOntologyID()
							+ " exists and will not be reloaded.", e);
			// Do nothing. Existing ontologies are fine.
		} catch (OWLOntologyCreationException e) {
			log.error("KReS :: Could not load ontology " + physicalIRI + " .",
					e);
		} finally {
		}
		return results;
	}

	/**
	 * Requires that Registry objects are created earlier. Problem is, we might
	 * not know their names a priori.
	 * 
	 * @param registry
	 * @return
	 * @throws RegistryContentException
	 */
	private Registry setupRegistry(Registry registry)
			throws RegistryContentException {

		// For each registry:
		registry.removeChildren();
		OWLOntology ontology = getMergedOntology(registry.getURL());

		// TODO: Restore ODP cache manager.
		// setManager(ODPRegistryCacheManager.getManager());
		Set<OWLIndividual> folderSet = cRegistryLibrary
				.getIndividuals(ontology);

		// Look for first level elements;
		Iterator<OWLIndividual> iter = folderSet.iterator();
		while (iter.hasNext()) {
			OWLIndividual i = iter.next();
			if (i instanceof OWLNamedIndividual) {
				if (!i.getObjectPropertyValues(ontology).containsKey(isPartOf)
						&& !i.getObjectPropertyValues(ontology).containsKey(
								isOntologyOf)) {
					registry.addChild(this.getTree((OWLNamedIndividual) i,
							ontology));
				}
			}
		}

		return registry;
	}

}
