package org.apache.stanbol.reengineer.db.vocab;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;


/**
 * Vocabulary definitions from http://ontologydesignpatterns.org/ont/iks/dbs_l1.owl 
 * @author andrea.nuzzolese
 */

public class DBS_L1 {
	
	
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://ontologydesignpatterns.org/ont/iks/dbs_l1.owl#";
    
    public static final String URI = "http://ontologydesignpatterns.org/ont/iks/dbs_l1.owl";
    
        
    private static MGraph mGraph = new SimpleMGraph();
    
        
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return URI;}
    
    
    public static final UriRef RDF_TYPE = new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#" + "type");
    
    public static final UriRef RDFS_LABEL = new UriRef("http://www.w3.org/2000/01/rdf-schema#" + "label");
    
    public static final UriRef NAMESPACE = new UriRef(NS);
    
    public static final UriRef bindToColumn = new UriRef( NS+"bindToColumn" );
    
    public static final UriRef hasColumn = new UriRef( NS+"hasColumn" );
    
    public static final UriRef isColumnOf = new UriRef( NS+"isColumnOf" );
    
    public static final UriRef hasDatum = new UriRef( NS+"hasDatum" );
    
    public static final UriRef isDatumOf = new UriRef( NS+"isDatumOf" );
    
    public static final UriRef hasRow = new UriRef( NS+"hasRow" );
    
    public static final UriRef isRowOf = new UriRef( NS+"isRowOf" );
    
    public static final UriRef isComposedBy = new UriRef( NS+"isComposedBy" );
    
    public static final UriRef composes = new UriRef( NS+"composes" );
    
    public static final UriRef hasKey = new UriRef( NS+"hasKey" );
    
    public static final UriRef isKeyOf = new UriRef( NS+"isKeyOf" );
    
    public static final UriRef hasPrimaryKey = new UriRef( NS+"hasPrimaryKey" );
    
    public static final UriRef isPrimaryKeyOf = new UriRef( NS+"isPrimaryKeyOf" );
    
    public static final UriRef hasForeignKey = new UriRef( NS+"hasForeignKey" );
    
    public static final UriRef isForeignKeyOf = new UriRef( NS+"isForeignKeyOf" );
    
    public static final UriRef hasSQLType = new UriRef( NS+"hasSQLType" );
    
    public static final UriRef hasTable = new UriRef( NS+"hasTable" );
    
    public static final UriRef isTableOf = new UriRef( NS+"isTableOf" );
    
    public static final UriRef isBoundBy = new UriRef( NS+"isBoundBy" );
    
    public static final UriRef isJoinedBy = new UriRef( NS+"isJoinedBy" );
    
    public static final UriRef joinsOn = new UriRef( NS+"joinsOn" );
    
    public static final UriRef isDumped = new UriRef( NS+"isDumped" );
    
    public static final UriRef hasContent = new UriRef( NS+"hasContent" );
    
    public static final UriRef hasName = new UriRef( NS+"hasName" );
    
    public static final UriRef hasPhysicalName = new UriRef( NS+"hasPhysicalName" );
    
    public static final UriRef hasJDBCDriver = new UriRef( NS+"hasJDBCDriver" );
    
    public static final UriRef hasJDBCDns = new UriRef( NS+"hasJDBCDns" );
    
    public static final UriRef hasUsername = new UriRef( NS+"hasUsername" );
    
    public static final UriRef hasPassword = new UriRef( NS+"hasPassword" );
    
    public static final UriRef hasPrimaryKeyMember = new UriRef( NS+"hasPrimaryKeyMember" );
    
    public static final UriRef isPrimaryKeyMemberOf = new UriRef( NS+"isPrimaryKeyMemberOf" );
    
    public static final UriRef hasForeignKeyMember = new UriRef( NS+"hasForeignKeyMember" );
    
    public static final UriRef isForeignKeyMemberOf = new UriRef( NS+"isForeignKeyMemberOf" );
    
    public static final UriRef Datum = new UriRef( NS+"Datum" );
    
    public static final UriRef DatabaseConnection = new UriRef( NS+"DatabaseConnection" );
    
    public static final UriRef DataObject = new UriRef( NS+"DataObject" );
    
    public static final UriRef SchemaObject = new UriRef( NS+"SchemaObject" );
    
    public static final UriRef Record = new UriRef( NS+"Record" );
    
    public static final UriRef Row = new UriRef( NS+"Row" );
    
    public static final UriRef Column = new UriRef( NS+"Column" );
    
    public static final UriRef Key = new UriRef( NS+"Key" );
    
    public static final UriRef PrimaryKey = new UriRef( NS+"PrimaryKey" );
    
    public static final UriRef ForeignKey = new UriRef( NS+"ForeignKey" );
    
    public static final UriRef Table = new UriRef( NS+"Table" );
    
    public static final UriRef NotNullableColumn = new UriRef( NS+"NotNullableColumn" );
    
    public static final UriRef NullableColumn = new UriRef( NS+"NullableColumn" );
    
    public static Model getModel(){
    	//return FileManager.get().loadModel("http://andriry.altervista.org/tesiSpecialistica/dbs_l1.owl");
    	return FileManager.get().loadModel(URI);
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
