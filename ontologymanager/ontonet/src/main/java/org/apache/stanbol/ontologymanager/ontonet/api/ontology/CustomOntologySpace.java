package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

/**
 * An ontology space that wraps the components that can be customized by CMS
 * developers, IKS customizers and the like. The custom ontology space becomes
 * read-only after bootstrapping (i.e. after a call to <code>setUp()</code>).
 * 
 * The ontologies in a custom space typically depend on those in the core space.
 * However, a custom space does <i>not</i> know which is the core space, it only
 * imports its ontologies. The core-custom-session relationship between spaces
 * is a scope is handled by external objects.
 * 
 * @author alessandro
 * 
 */
public interface CustomOntologySpace extends OntologySpace {

	/**
	 * Logically links this custom space with the supplied core ontology space,
	 * so that the top ontology in the former will import those in the latter.<br>
	 * <br>
	 * This relationship is expected to hold at all times once the space is
	 * active, however the method to set it is available in case implementations
	 * require to perform other operations between the creation of ontology
	 * spaces and their linking.
	 * 
	 * @param coreSpace
	 *            the core ontology space to be linked
	 * @param skipRoot
	 *            if true, the custom root ontology will not import the core
	 *            root ontology straight away, but instead all of its axioms and
	 *            import statements will be copied. Useful for implementations
	 *            that construct root ontologies in memory but do not store
	 *            them.
	 */
    void attachCoreSpace(CoreOntologySpace coreSpace, boolean skipRoot)
			throws UnmodifiableOntologySpaceException;

}
