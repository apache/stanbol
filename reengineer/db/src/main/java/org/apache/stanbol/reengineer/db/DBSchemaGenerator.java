package org.apache.stanbol.reengineer.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.stanbol.reengineer.base.api.settings.ConnectionSettings;
import org.apache.stanbol.reengineer.base.api.util.ReengineerUriRefGenerator;
import org.apache.stanbol.reengineer.db.vocab.DBS_L1_OWL;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;


/**
 * The {@code DBSchemaGenerator} is responsible of the generation of the RDF of the schema of a relational database.
 * 
 * @author andrea.nuzzolese
 */
public class DBSchemaGenerator extends ReengineerUriRefGenerator implements Serializable {

	private String graphNS; 
	private IRI outputIRI;
	private ConnectionSettings connectionSettings;

	/**
	 * Creates a new standard {@code DBSchemaGenerator}
	 */
	public DBSchemaGenerator() {
		
	}
	
	/**
	 * Creates a new {@code DBSchemaGenerator} that can generate the RDF of the database's schema. The database is available
	 * thanks to the {@code connectionSettings} passed as input. The URI of the RDF will be that one passed as actual parameter to the
	 * formal parameter {@code databaseURI}.
	 * 
	 * @param databaseURI {@link String}
	 * @param connectionSettings {@link ConnectionSettings}
	 */
	public DBSchemaGenerator(IRI outputIRI, ConnectionSettings connectionSettings){
		this.connectionSettings = connectionSettings;
		this.outputIRI = outputIRI;
	}

	/**
	 * Performs the generation of the RDF of the database schema. The RDF graph is added to the {@link MGraph} passed as input.
	 *  
	 * 
	 * @param mGraph {@link OWLOntology}
	 * @return the {@link MGraph} containing the database schema into RDF.
	 */
	public OWLOntology getSchema(){
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		
		Connection con = openConnection(connectionSettings);
		
		/*
		Model schemaModel = ModelFactory.createDefaultModel();
		schemaModel.setNsPrefixes(DBS_L1.getModel().getNsPrefixMap());		
		schemaModel.setNsPrefix("dbSchema", namespace);
		schemaModel.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		schemaModel.setNsPrefix("dbs", DBS_L1.NS);
		*/

		OWLOntology schemaOntology = null;
		if(outputIRI != null){
			try {
				schemaOntology = manager.createOntology(outputIRI);
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		else{
			try {
				schemaOntology = manager.createOntology();
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		if(!graphNS.endsWith("#")){
			graphNS += "#";
		}
		
		IRI databaseConnectionInfoIRI = IRI.create(graphNS + "DBConnectionSettings");
		OWLClassAssertionAxiom databaseConnectionInfo = createOWLClassAssertionAxiom(factory, DBS_L1_OWL.DatabaseConnection, databaseConnectionInfoIRI);
		manager.applyChange(new AddAxiom(schemaOntology, databaseConnectionInfo));
		
		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasName, databaseConnectionInfoIRI, "database_"+outputIRI)));
		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasPhysicalName, databaseConnectionInfoIRI, connectionSettings.getDatabaseName())));
		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasJDBCDriver, databaseConnectionInfoIRI, connectionSettings.getJDBCDriver())));
		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasJDBCDns, databaseConnectionInfoIRI, getConnectionUrl(connectionSettings.getUrl(), connectionSettings.getServerName(), connectionSettings.getPortNumber(), connectionSettings.getDatabaseName()))));
		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasUsername, databaseConnectionInfoIRI, connectionSettings.getUserName())));
		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasPassword, databaseConnectionInfoIRI, connectionSettings.getPassword())));
		
		
		DatabaseMetaData md;
		try {
			md = con.getMetaData();
			
			ResultSet rs = md.getTables(null, null, "%", null);
			
			for(int k=0; rs.next(); k++) {
		    	String table = rs.getString(3);
		    	System.out.println("TABLE : "+table);
		    	
		    	IRI tableResourceIRI = IRI.create(graphNS + "table_"+table);
		    	OWLClassAssertionAxiom tableResource = createOWLClassAssertionAxiom(factory, DBS_L1_OWL.Table, tableResourceIRI);
		    	manager.applyChange(new AddAxiom(schemaOntology, tableResource));
		    	
		    	manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasName, databaseConnectionInfoIRI, "table_"+table)));
		    	manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.isTableOf, tableResourceIRI, databaseConnectionInfoIRI)));
		    	manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.isDumped, databaseConnectionInfoIRI, false)));
		    	manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.RDFS_LABEL, databaseConnectionInfoIRI, table)));
		    	
		    	manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.hasTable, databaseConnectionInfoIRI, tableResourceIRI)));
		    	
		    	String catalog = con.getCatalog();
		    	
		    	ResultSet fk = md.getImportedKeys(catalog, null, table);
		    	
		    	ResultSet pk = md.getPrimaryKeys(catalog, null, table);
		    	
		    	ResultSet columnsRs = md.getColumns(catalog, null, table, null);

		    	while(columnsRs.next()){
		    		String columnName = columnsRs.getString("COLUMN_NAME");
		    		String columnType = columnsRs.getString("TYPE_NAME");
		    		String columnNullable = columnsRs.getString("NULLABLE");
		    		System.out.println("COLUMN NAME : "+columnName);
		    		System.out.println("COLUMN TYPE : "+columnType);
		    		System.out.println("COLUMN NULLABLE : "+columnNullable);
		    		
		    		boolean nullable = true;
		    		try{
		    			int nullableInt = Integer.valueOf(columnNullable).intValue();
		    			nullable = nullableInt != 0;
		    		} catch (NumberFormatException e) {
						nullable = true;
					}

		    		IRI colummIRI = IRI.create(graphNS + table+"-column_"+columnName);
		    		OWLClassAssertionAxiom columm;
		    		
		    		if(nullable){
		    			columm = createOWLClassAssertionAxiom(factory, DBS_L1_OWL.NullableColumn, colummIRI);
		    		}
		    		else{
		    			columm = createOWLClassAssertionAxiom(factory, DBS_L1_OWL.NotNullableColumn, colummIRI);
		    		}
		    		manager.applyChange(new AddAxiom(schemaOntology, columm));
		    		
		    		if(columnType != null){
				    	manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasSQLType, colummIRI, columnType)));
		    		}
		    		if(columnName != null){
		    			manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.hasName, colummIRI, table+"-column_"+columnName)));
		    		}
		    		
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.isColumnOf, colummIRI, tableResourceIRI)));
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, DBS_L1_OWL.RDFS_LABEL, colummIRI, columnName)));
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.isColumnOf, tableResourceIRI, colummIRI)));
		    	}

		    	Hashtable<String, TablePKRelations> relationTable = new Hashtable<String, TablePKRelations>();
		    	
		    	while (fk.next()) {
		    		String fkTableName = fk.getString("FKTABLE_NAME");
		    		String fkColumnName = fk.getString("FKCOLUMN_NAME");
		    		String pkTableName = fk.getString("PKTABLE_NAME");
		    		String pkColumnName = fk.getString("PKCOLUMN_NAME");
		    		
		    		IRI foreignKeyResourceIRI = IRI.create(graphNS + table+"_fk_"+fkColumnName);
		    		OWLClassAssertionAxiom foreignKeyResource = createOWLClassAssertionAxiom(factory, DBS_L1_OWL.ForeignKey, foreignKeyResourceIRI);
		    		manager.applyChange(new AddAxiom(schemaOntology, foreignKeyResource));
		    		
		    		System.out.println("JOIN ON COLUMN "+pkColumnName);
		    		
		    		IRI columnIRI = IRI.create(graphNS + table+"-column_"+fkColumnName);
		    		
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.hasForeignKeyMember, foreignKeyResourceIRI, columnIRI)));
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.isForeignKeyMemberOf, columnIRI, foreignKeyResourceIRI)));
		    		
		    		IRI joinColumnIRI = IRI.create(graphNS + pkTableName+"-column_"+pkColumnName);
		    		OWLClassAssertionAxiom joinColumn = createOWLClassAssertionAxiom(factory, DBS_L1_OWL.NotNullableColumn, joinColumnIRI);
		    		manager.applyChange(new AddAxiom(schemaOntology, joinColumn));
		    		
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.joinsOn, columnIRI, joinColumnIRI)));
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.hasForeignKey, tableResourceIRI, foreignKeyResourceIRI)));
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.isForeignKeyOf, foreignKeyResourceIRI, tableResourceIRI)));

                    int fkSequence = fk.getInt("KEY_SEQ");
                    if (fkTableName != null) {
                        System.out.println("getExportedKeys(): fkTableName=" + fkTableName);
                    }
                    if (fkColumnName != null) {
                        System.out.println("getExportedKeys(): fkColumnName=" + fkColumnName);
                    }
                    if (pkTableName != null) {
                        System.out.println("getExportedKeys(): pkTableName=" + pkTableName);
                    }
                    if (pkColumnName != null) {
                        System.out.println("getExportedKeys(): pkColumnName=" + pkColumnName);
                    }
                    System.out.println("getExportedKeys(): fkSequence=" + fkSequence);

                    TablePKRelations tableRelations = relationTable.remove(fkTableName);
	    	       	System.out.println();
	    	       	System.out.println(pkTableName);
	    	       	if(tableRelations == null){
	    	       		tableRelations = new TablePKRelations(fkTableName, new ArrayList<String>(), new ArrayList<String>(), pkTableName);
	    	       		System.out.println("NULL");
	    	       	}
	    	       	else{
	    	       		System.out.println("tableRelations NOT NULL "+tableRelations.getPkTable());
	    	       	}
	    	       	System.out.println();
	    	       	System.out.println();
	    	       	
	    	       	ArrayList<String> fkArrayList = tableRelations.getFkColumns();
	    	       	ArrayList<String> pkArrayList = tableRelations.getPkColumns();
	    	       	fkArrayList.add(fkColumnName);
	    	       	pkArrayList.add(pkColumnName);
	    	       	
	    	       	tableRelations.setFkColumns(fkArrayList);
	    	       	tableRelations.setPkColumns(pkArrayList);
	    	       	
	    	       	relationTable.put(fkTableName, tableRelations);
		    	}

		    	fk.close();
		    	
		    	for(int i=0; pk.next(); i++){
		    		String colPk = pk.getString(4);
		    		
		    		IRI primaryKeyIRI = IRI.create(graphNS + table+"_pk_"+colPk);
		    		OWLClassAssertionAxiom primaryKey = createOWLClassAssertionAxiom(factory, DBS_L1_OWL.PrimaryKey, primaryKeyIRI);
		    		manager.applyChange(new AddAxiom(schemaOntology, primaryKey));
		    		
		    		IRI columnIRI = IRI.create(graphNS + table+"-column_"+colPk);
		    		
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.hasPrimaryKeyMember, primaryKeyIRI, columnIRI)));
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.isPrimaryKeyMemberOf, columnIRI, primaryKeyIRI)));
		    		
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.hasPrimaryKey, tableResourceIRI, primaryKeyIRI)));
		    		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.isPrimaryKeyOf, primaryKeyIRI, tableResourceIRI)));
		    	}

		    	pk.close();
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return schemaOntology;
	}

	/*
	public Model getSchemaL1(){
		
		Connection con = openConnection(connectionSettings);
		
		Model schemaModel = ModelFactory.createDefaultModel();
		
		schemaModel.setNsPrefixes(DBS_L1.getModel().getNsPrefixMap());		
		schemaModel.setNsPrefix("dbSchema", namespace);
		schemaModel.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		schemaModel.setNsPrefix("dbs", DBS_L1.NS);
		
		Resource database = schemaModel.createResource(namespace+databaseURI, DBS_L1.DatabaseConnection);
		database.addLiteral(DBS_L1.hasName, "database_"+databaseURI);
		database.addProperty(DBS_L1.hasPhysicalName, connectionSettings.getDatabaseName());
		database.addProperty(DBS_L1.hasJDBCDriver, connectionSettings.getJDBCDriver());
		database.addProperty(DBS_L1.hasJDBCDns, getConnectionUrl(connectionSettings.getUrl(), connectionSettings.getServerName(), connectionSettings.getPortNumber(), connectionSettings.getDatabaseName()));
		database.addProperty(DBS_L1.hasUsername, connectionSettings.getUserName());
		database.addProperty(DBS_L1.hasPassword, connectionSettings.getPassword());
		
		
		
		
		DatabaseMetaData md;
		try {
			md = con.getMetaData();
			
			ResultSet rs = md.getTables(null, null, "%", null);
			
			String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
			
			for(int k=0; rs.next(); k++) {
		    	String table = rs.getString(3);
		    	System.out.println("TABLE : "+table);
		    	
		    	Resource tableResource = schemaModel.createResource(namespace+"table_"+table, DBS_L1.Table);
		    	tableResource.addLiteral(DBS_L1.hasName, "table_"+table);
		    	tableResource.addProperty(DBS_L1.isTableOf, database);
		    	tableResource.addLiteral(DBS_L1.isDumped, false);
		    	tableResource.addLiteral(RDFS.label, table);
		    	database.addProperty(DBS_L1.hasTable, tableResource);
		    	
		    	String catalog = con.getCatalog();
		    	
		    	ResultSet fk = md.getImportedKeys(catalog, null, table);
		    	
		    	ResultSet pk = md.getPrimaryKeys(catalog, null, table);
		    	
		    	ResultSet columnsRs = md.getColumns(catalog, null, table, null);
		    	
		    	
		    	
		    	while(columnsRs.next()){
		    		String columnName = columnsRs.getString("COLUMN_NAME");
		    		String columnType = columnsRs.getString("TYPE_NAME");
		    		String columnNullable = columnsRs.getString("NULLABLE");
		    		System.out.println("COLUMN NAME : "+columnName);
		    		System.out.println("COLUMN TYPE : "+columnType);
		    		System.out.println("COLUMN NULLABLE : "+columnNullable);
		    		
		    		boolean nullable = true;
		    		try{
		    			int nullableInt = Integer.valueOf(columnNullable).intValue();
		    			
		    			nullable = nullableInt == 0 ? false : true;
		    			
		    			
		    		} catch (NumberFormatException e) {
						nullable = true;
					}
		    		
		    		Resource columm;
		    		if(nullable){
		    			columm = schemaModel.createResource(namespace+table+"-column_"+columnName, DBS_L1.NullableColumn);
		    		}
		    		else{
		    			columm = schemaModel.createResource(namespace+table+"-column_"+columnName, DBS_L1.NotNullableColumn);
		    		}
		    		
		    		if(columnType != null){
		    			columm.addLiteral(DBS_L1.hasSQLType, columnType);
		    		}
		    		if(columnName != null){
		    			columm.addLiteral(DBS_L1.hasName, table+"-column_"+columnName);
		    		}
		    		columm.addProperty(DBS_L1.isColumnOf, tableResource);
		    		
		    		columm.addLiteral(RDFS.label, columnName);
		    		
		    		//asserisco l'inversa di isFieldOf
		    		tableResource.addProperty(DBS_L1.hasColumn, columm);
		    		
		    	}
		    	
		    	
		    	Hashtable<String, TablePKRelations> relationTable = new Hashtable<String, TablePKRelations>();
		    	
		    	while (fk.next()) {
		    		String fkTableName = fk.getString("FKTABLE_NAME");
		    		String fkColumnName = fk.getString("FKCOLUMN_NAME");
		    		String pkTableName = fk.getString("PKTABLE_NAME");
		    		String pkColumnName = fk.getString("PKCOLUMN_NAME");
		    		
		    		Resource foreignKeyResource = schemaModel.createResource(namespace+table+"_fk_"+fkColumnName, DBS_L1.ForeignKey);
		    		
		    		
		    		System.out.println("JOIN ON COLUMN "+pkColumnName);
		    		Resource column = schemaModel.createResource(namespace+table+"-column_"+fkColumnName);
		    		foreignKeyResource.addProperty(DBS_L1.hasForeignKeyMember, column);
		    		column.addProperty(DBS_L1.isForeignKeyMemberOf, foreignKeyResource);
		    		
		    		column.addProperty(DBS_L1.joinsOn, schemaModel.createResource(namespace+pkTableName+"-column_"+pkColumnName, DBS_L1.NotNullableColumn));
		    		
		    		tableResource.addProperty(DBS_L1.hasForeignKey, foreignKeyResource);
		    		foreignKeyResource.addProperty(DBS_L1.isForeignKeyOf, tableResource);
		    		
		    		int fkSequence = fk.getInt("KEY_SEQ");
	    	       	if(fkTableName != null){
	    	    	   System.out.println("getExportedKeys(): fkTableName="+fkTableName);
	    	       	}
	    	       	if(fkColumnName != null){
	    	    	   System.out.println("getExportedKeys(): fkColumnName="+fkColumnName);
	    	       	}
	    	       	if(pkTableName != null){
	    	       		System.out.println("getExportedKeys(): pkTableName="+pkTableName);
	    	       	}
	    	       	if(pkColumnName != null){
	    	       		System.out.println("getExportedKeys(): pkColumnName="+pkColumnName);
	    	       	}
	    	       	System.out.println("getExportedKeys(): fkSequence="+fkSequence);
	    	       	
	    	       	
	    	       	TablePKRelations tableRelations = relationTable.remove(fkTableName);
	    	       	System.out.println();
	    	       	System.out.println(pkTableName);
	    	       	if(tableRelations == null){
	    	       		tableRelations = new TablePKRelations(fkTableName, new ArrayList<String>(), new ArrayList<String>(), pkTableName);
	    	       		System.out.println("NULL");
	    	       	}
	    	       	else{
	    	       		System.out.println("tableRelations NOT NULL "+tableRelations.getPkTable());
	    	       	}
	    	       	System.out.println();
	    	       	System.out.println();
	    	       	
	    	       	ArrayList<String> fkArrayList = tableRelations.getFkColumns();
	    	       	ArrayList<String> pkArrayList = tableRelations.getPkColumns();
	    	       	fkArrayList.add(fkColumnName);
	    	       	pkArrayList.add(pkColumnName);
	    	       	
	    	       	tableRelations.setFkColumns(fkArrayList);
	    	       	tableRelations.setPkColumns(pkArrayList);
	    	       	
	    	       	relationTable.put(fkTableName, tableRelations);
	    	       	
		    	}
		    	
		    	
		    	fk.close();
		    	
		    	for(int i=0; pk.next(); i++){
		    		String colPk = pk.getString(4);
		    		
		    		Resource primaryKey = schemaModel.createResource(namespace+table+"_pk_"+colPk, DBS_L1.PrimaryKey);
		    		
		    		Resource column = schemaModel.createResource(namespace+table+"-column_"+colPk);
		    		
		    		primaryKey.addProperty(DBS_L1.hasPrimaryKeyMember, column);
		    		column.addProperty(DBS_L1.isPrimaryKeyMemberOf, primaryKey);
		    		
		    		tableResource.addProperty(DBS_L1.hasPrimaryKey, primaryKey);
		    		primaryKey.addProperty(DBS_L1.isPrimaryKeyOf, tableResource);
		    	}
		    	
		    	
		    	
		    	pk.close();
		    	
		    	
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return schemaModel;
	}
	
	*/
	
	private String getConnectionUrl(String url, String serverName, String portNumber, String databaseName){
        return url+serverName+":"+portNumber+"/"+databaseName;
	}
	
	
	private Connection openConnection(ConnectionSettings connectionSettings){
		Connection con = null;
		try {
			Class.forName(connectionSettings.getJDBCDriver());
			System.out.println(getConnectionUrl(connectionSettings.getUrl(), connectionSettings.getServerName(), connectionSettings.getPortNumber(), connectionSettings.getDatabaseName()));
			con = DriverManager.getConnection(getConnectionUrl(connectionSettings.getUrl(), connectionSettings.getServerName(), connectionSettings.getPortNumber(), connectionSettings.getDatabaseName()), connectionSettings.getUserName(), connectionSettings.getPassword());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        
        return con;
	}
	
	private void closeConnection(Connection con){
		try {
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
