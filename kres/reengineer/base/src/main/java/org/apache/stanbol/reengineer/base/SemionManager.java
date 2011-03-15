package org.apache.stanbol.reengineer.base;

import java.util.Collection;

import org.apache.stanbol.ontologymanager.store.api.NoSuchOntologyInStoreException;
import org.apache.stanbol.rules.refactor.api.SemionRefactorer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * 
 * The {@code SemionManager} is responsible of the coordination of all the tasks performed by Semion in KReS
 * 
 * @author andrea.nuzzolese
 *
 */

public interface SemionManager {

	/**
	 * The {@link SemionManager} can add a new reengineer to the list of available reengineers. This is performed through the method
	 * {@cod bindReengineer}.
	 * 
	 * @param semionReengineer {@link SemionReengineer}
	 * @return true if the reengineer is bound, false otherwise
	 */
	public boolean bindReengineer(SemionReengineer semionReengineer);
	
	/**
	 * The {@link SemionManager} can remove a reengineer from the list of available reengineers. This is performed through the method
	 * {@cod unbindReengineer}.
	 * 
	 * @param semionReengineer {@link SemionReengineer}
	 * @return true if the reengineer is unbound, false otherwise
	 */
	public boolean unbindReengineer(SemionReengineer semionReengineer);
	
	/**
	 * The {@link SemionManager} can remove a reengineer from the list of available reengineers. This is performed through the method
	 * {@cod unbindReengineer}.
	 * 
	 * @param reenginnerType {@code int}
	 * @return true if the reengineer is unbound, false otherwise
	 */
	public boolean unbindReengineer(int reenginnerType);
	
	
	/**
	 * The {@link SemionManager} can register a single instance of {@link SemionRefactorer}.
	 * 
	 * @param semionRefactorer {@link SemionRefactorer}
	 */
	public void registerRefactorer(SemionRefactorer semionRefactorer);
	
	/**
	 * Unregisters the instance of {@link SemionRefactorer}. After the call of this method Semion has no refactorer.
	 */
	public void unregisterRefactorer();
	
	/**
	 * The instance of the refactored is returned back if it exists.
	 * 
	 * @return the active {@link SemionRefactorer}
	 */
	public SemionRefactorer getRegisteredRefactorer();
	
	
	
	/**
	 * Gets the active reengineers of KReS.
	 * 
	 * @return the {@link Collection< SemionReengineer >} of active reengineers. 
	 */
	public Collection<SemionReengineer> listReengineers();
	
	/**
	 * Gets the number of active reengineers.
	 * 
	 * @return the number of active reengineers.
	 */
	public int countReengineers();
	
	public OWLOntology performReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException;
	
	public OWLOntology performSchemaReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException;
	
	public OWLOntology performDataReengineering(String graphNS, IRI outputIRI, DataSource dataSource, IRI schemaOntologyIRI) throws ReengineeringException, NoSuchOntologyInStoreException;
	
	public OWLOntology performDataReengineering(String graphNS, IRI outputIRI, DataSource dataSource, OWLOntology schemaOntology) throws ReengineeringException;
	
}
