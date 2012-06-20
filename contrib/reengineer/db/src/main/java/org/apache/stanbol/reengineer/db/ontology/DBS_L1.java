package org.apache.stanbol.reengineer.db.ontology;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

/**
 * Vocabulary definitions from http://andriry.altervista.org/tesiSpecialistica/dbs_l1.owl 
 * @author andrea.nuzzolese
 */

public class DBS_L1 {
	
	
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://andriry.altervista.org/tesiSpecialistica/dbs_l1.owl#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    public static final Property bindToColumn = m_model.createProperty( NS+"bindToColumn" );
    
    public static final Property hasColumn = m_model.createProperty( NS+"hasColumn" );
    
    public static final Property isColumnOf = m_model.createProperty( NS+"isColumnOf" );
    
    public static final Property hasDatum = m_model.createProperty( NS+"hasDatum" );
    
    public static final Property isDatumOf = m_model.createProperty( NS+"isDatumOf" );
    
    public static final Property hasRow = m_model.createProperty( NS+"hasRow" );
    
    public static final Property isRowOf = m_model.createProperty( NS+"isRowOf" );
    
    public static final Property isComposedBy = m_model.createProperty( NS+"isComposedBy" );
    
    public static final Property composes = m_model.createProperty( NS+"composes" );
    
    public static final Property hasKey = m_model.createProperty( NS+"hasKey" );
    
    public static final Property isKeyOf = m_model.createProperty( NS+"isKeyOf" );
    
    public static final Property hasPrimaryKey = m_model.createProperty( NS+"hasPrimaryKey" );
    
    public static final Property isPrimaryKeyOf = m_model.createProperty( NS+"isPrimaryKeyOf" );
    
    public static final Property hasForeignKey = m_model.createProperty( NS+"hasForeignKey" );
    
    public static final Property isForeignKeyOf = m_model.createProperty( NS+"isForeignKeyOf" );
    
    public static final Property hasSQLType = m_model.createProperty( NS+"hasSQLType" );
    
    public static final Property hasTable = m_model.createProperty( NS+"hasTable" );
    
    public static final Property isTableOf = m_model.createProperty( NS+"isTableOf" );
    
    public static final Property isBoundBy = m_model.createProperty( NS+"isBoundBy" );
    
    public static final Property isJoinedBy = m_model.createProperty( NS+"isJoinedBy" );
    
    public static final Property joinsOn = m_model.createProperty( NS+"joinsOn" );
    
    public static final Property isDumped = m_model.createProperty( NS+"isDumped" );
    
    public static final Property hasContent = m_model.createProperty( NS+"hasContent" );
    
    public static final Property hasName = m_model.createProperty( NS+"hasName" );
    
    public static final Property hasPhysicalName = m_model.createProperty( NS+"hasPhysicalName" );
    
    public static final Property hasJDBCDriver = m_model.createProperty( NS+"hasJDBCDriver" );
    
    public static final Property hasJDBCDns = m_model.createProperty( NS+"hasJDBCDns" );
    
    public static final Property hasUsername = m_model.createProperty( NS+"hasUsername" );
    
    public static final Property hasPassword = m_model.createProperty( NS+"hasPassword" );
    
    public static final Property hasPrimaryKeyMember = m_model.createProperty( NS+"hasPrimaryKeyMember" );
    
    public static final Property isPrimaryKeyMemberOf = m_model.createProperty( NS+"isPrimaryKeyMemberOf" );
    
    public static final Property hasForeignKeyMember = m_model.createProperty( NS+"hasForeignKeyMember" );
    
    public static final Property isForeignKeyMemberOf = m_model.createProperty( NS+"isForeignKeyMemberOf" );
    
    public static final Resource Datum = m_model.createResource( NS+"Datum" );
    
    public static final Resource DatabaseConnection = m_model.createResource( NS+"DatabaseConnection" );
    
    public static final Resource DataObject = m_model.createResource( NS+"DataObject" );
    
    public static final Resource SchemaObject = m_model.createResource( NS+"SchemaObject" );
    
    public static final Resource Row = m_model.createResource( NS+"Row" );
    
    public static final Resource Column = m_model.createResource( NS+"Column" );
    
    public static final Resource Key = m_model.createResource( NS+"Key" );
    
    public static final Resource PrimaryKey = m_model.createResource( NS+"PrimaryKey" );
    
    public static final Resource ForeignKey = m_model.createResource( NS+"ForeignKey" );
    
    public static final Resource Table = m_model.createResource( NS+"Table" );
    
    public static final Resource NotNullableColumn = m_model.createResource( NS+"NotNullableColumn" );
    
    public static final Resource NullableColumn = m_model.createResource( NS+"NullableColumn" );
    
    public static Model getModel(){
    	//return FileManager.get().loadModel("http://andriry.altervista.org/tesiSpecialistica/dbs_l1.owl");
    	return FileManager.get().loadModel("rdf/dbs_l1.owl");
    }
    
    public static String getSPARQLPrefix(){
    	Map<String, String> prefixMap = getModel().getNsPrefixMap();
		Set<String> prefixSet = prefixMap.keySet();
		Iterator<String> it = prefixSet.iterator();
		
		String sparqlPrefix = "";
		
		
		while(it.hasNext()){
			String prefix = it.next();
			try {
				String uri = prefixMap.get(prefix);
				uri = uri.replace("\\", "/");
				sparqlPrefix += "PREFIX "+prefix+": <"+(new URI(uri).toString())+">"+System.getProperty("line.separator");
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		return sparqlPrefix;
    }
    
    public static String getSPARQLPrefix(Model model){
    	Map<String, String> prefixMap = model.getNsPrefixMap();
		Set<String> prefixSet = prefixMap.keySet();
		Iterator<String> it = prefixSet.iterator();
		
		String sparqlPrefix = "";
		
		
		while(it.hasNext()){
			String prefix = it.next();
			try {
				String uri = prefixMap.get(prefix);
				uri = uri.replace("\\", "/");
				sparqlPrefix += "PREFIX "+prefix+": <"+(new URI(uri).toString())+">"+System.getProperty("line.separator");
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		return sparqlPrefix;
    }
    
    public static ResultSet executeQuery(String querySPARQL){
    	Query query = QueryFactory.create(querySPARQL); 
		return QueryExecutionFactory.create(query, getModel()).execSelect();
    }
    
    public static ResultSet executeQuery(Model model, String querySPARQL){
    	Query query = QueryFactory.create(querySPARQL); 
		return QueryExecutionFactory.create(query, model).execSelect();
    }
    
}
