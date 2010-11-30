package eu.iksproject.kres.api.semion;

import java.util.Observer;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.OWL;

import eu.iksproject.kres.api.semion.util.UnsupportedReengineerException;

/**
 * 
 * A SemionReengineer provides methods for transforming in KReS both the schema and the data of a non-RDF data source into RDF.
 * <br>
 * <br>
 * Accepted data sources are:
 * <ul>
 * <li> 0 - Relational Databases
 * <li> 1 - XML
 * <li> 2 - iCalendar
 * <li> 3 - RSS
 * </ul>
 * 
 * @author andrea.nuzzolese
 *
 */

public interface SemionReengineer {

	/**
	 * The method returns one of the following values related to a particular data souce:
	 * <li> 0 - Relational Databases
	 * <li> 1 - XML
	 * <li> 2 - iCalendar
	 * <li> 3 - RSS
	 * </ul>
	 * 
	 * @return {@code int}
	 */
	public int getReengineerType();
	
	/**
	 * The method enables to test if the Reengineer can perform the reengineering of a particular data source given as input.
	 * 
	 * @param dataSource {@link DataSource}
	 * @return true if the Reengineer can perform the reengineering, false otherwise
	 */
	public boolean canPerformReengineering(DataSource dataSource);
	
	/**
	 * The method enables to test if the Reengineer can perform the reengineering of a particular data source type given as input.
	 * 
	 * @param the data source type {@code int}
	 * @return true if the Reengineer can perform the reengineering, false otherwise
	 */
	public boolean canPerformReengineering(int dataSourceType);
	
	public boolean canPerformReengineering(OWLOntology schemaOntology);
	
	/**
	 * The method enables to test if the Reengineer can perform the reengineering of a particular data source type given as input.
	 * 
	 * @param the data source type {@code String}
	 * @return true if the Reengineer can perform the reengineering, false otherwise
	 */
	public boolean canPerformReengineering(String dataSourceType) throws UnsupportedReengineerException;
	
	/**
	 * The data source (non-RDF) provided is reengineered to RDF. This operation produces an RDF data set that contains information
	 * both about the data and about the schema of the original data source.
	 * 
	 * @param graphNS {@link String}
	 * @param outputIRI {@link IRI}
	 * @param dataSource {@link DataSource}
	 * @return the reengineered data set - {@link OWLOntology}
	 */
	public OWLOntology reengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException;
	
	/**
	 * The generation of the RDF containing the information about the schema of the data source is obtained passing to this method
	 * the data source object as it is represented in Semion (i.e. {@link DataSource}). An {@link OWLOntology} is returned
	 * 
	 * @param graphNS {@link String}
	 * @param outputIRI {@link IRI}
	 * @param dataSource {@link DataSource}
	 * @return the {@link OWLOntology} of the data source shema
	 */
	public OWLOntology schemaReengineering(String graphNS, IRI outputIRI, DataSource dataSource);
	
	
	/**
	 * The generation of the RDF containing the information about the data of the data source is obtained passing to this method
	 * the data source object as it is represented in Semion (i.e. {@link DataSource}). An {@link OWLOntology} is returned
	 * 
	 * @param graphNS {@link String}
	 * @param outputIRI {@link IRI}
	 * @param dataSource {@link DataSource}
	 * @return the {@link OWLOntology} of the data source shema
	 */
	public OWLOntology dataReengineering(String graphNS, IRI outputIRI, DataSource dataSource, OWLOntology schemaOntology) throws ReengineeringException;
	
	
}
