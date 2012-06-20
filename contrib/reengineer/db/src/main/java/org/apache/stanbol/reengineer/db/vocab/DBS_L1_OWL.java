package org.apache.stanbol.reengineer.db.vocab;

import org.semanticweb.owlapi.model.IRI;



public class DBS_L1_OWL {
	
	/** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://ontologydesignpatterns.org/ont/iks/dbs_l1.owl#";
    
    public static final String URI = "http://ontologydesignpatterns.org/ont/iks/dbs_l1.owl";
        
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    
    public static final IRI RDF_TYPE = IRI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#" + "type");
    
    public static final IRI RDFS_LABEL = IRI.create("http://www.w3.org/2000/01/rdf-schema#" + "label");
    
    public static final IRI NAMESPACE = IRI.create(NS);
    
    public static final IRI bindToColumn = IRI.create( NS+"bindToColumn" );
    
    public static final IRI hasColumn = IRI.create( NS+"hasColumn" );
    
    public static final IRI isColumnOf = IRI.create( NS+"isColumnOf" );
    
    public static final IRI hasDatum = IRI.create( NS+"hasDatum" );
    
    public static final IRI isDatumOf = IRI.create( NS+"isDatumOf" );
    
    public static final IRI hasRow = IRI.create( NS+"hasRow" );
    
    public static final IRI isRowOf = IRI.create( NS+"isRowOf" );
    
    public static final IRI isComposedBy = IRI.create( NS+"isComposedBy" );
    
    public static final IRI composes = IRI.create( NS+"composes" );
    
    public static final IRI hasKey = IRI.create( NS+"hasKey" );
    
    public static final IRI isKeyOf = IRI.create( NS+"isKeyOf" );
    
    public static final IRI hasPrimaryKey = IRI.create( NS+"hasPrimaryKey" );
    
    public static final IRI isPrimaryKeyOf = IRI.create( NS+"isPrimaryKeyOf" );
    
    public static final IRI hasForeignKey = IRI.create( NS+"hasForeignKey" );
    
    public static final IRI isForeignKeyOf = IRI.create( NS+"isForeignKeyOf" );
    
    public static final IRI hasSQLType = IRI.create( NS+"hasSQLType" );
    
    public static final IRI hasTable = IRI.create( NS+"hasTable" );
    
    public static final IRI isTableOf = IRI.create( NS+"isTableOf" );
    
    public static final IRI isBoundBy = IRI.create( NS+"isBoundBy" );
    
    public static final IRI isJoinedBy = IRI.create( NS+"isJoinedBy" );
    
    public static final IRI joinsOn = IRI.create( NS+"joinsOn" );
    
    public static final IRI isDumped = IRI.create( NS+"isDumped" );
    
    public static final IRI hasContent = IRI.create( NS+"hasContent" );
    
    public static final IRI hasName = IRI.create( NS+"hasName" );
    
    public static final IRI hasPhysicalName = IRI.create( NS+"hasPhysicalName" );
    
    public static final IRI hasJDBCDriver = IRI.create( NS+"hasJDBCDriver" );
    
    public static final IRI hasJDBCDns = IRI.create( NS+"hasJDBCDns" );
    
    public static final IRI hasUsername = IRI.create( NS+"hasUsername" );
    
    public static final IRI hasPassword = IRI.create( NS+"hasPassword" );
    
    public static final IRI hasPrimaryKeyMember = IRI.create( NS+"hasPrimaryKeyMember" );
    
    public static final IRI isPrimaryKeyMemberOf = IRI.create( NS+"isPrimaryKeyMemberOf" );
    
    public static final IRI hasForeignKeyMember = IRI.create( NS+"hasForeignKeyMember" );
    
    public static final IRI isForeignKeyMemberOf = IRI.create( NS+"isForeignKeyMemberOf" );
    
    public static final IRI Datum = IRI.create( NS+"Datum" );
    
    public static final IRI DatabaseConnection = IRI.create( NS+"DatabaseConnection" );
    
    public static final IRI DataObject = IRI.create( NS+"DataObject" );
    
    public static final IRI SchemaObject = IRI.create( NS+"SchemaObject" );
    
    public static final IRI Record = IRI.create( NS+"Record" );
    
    public static final IRI Row = IRI.create( NS+"Row" );
    
    public static final IRI Column = IRI.create( NS+"Column" );
    
    public static final IRI Key = IRI.create( NS+"Key" );
    
    public static final IRI PrimaryKey = IRI.create( NS+"PrimaryKey" );
    
    public static final IRI ForeignKey = IRI.create( NS+"ForeignKey" );
    
    public static final IRI Table = IRI.create( NS+"Table" );
    
    public static final IRI NotNullableColumn = IRI.create( NS+"NotNullableColumn" );
    
    public static final IRI NullableColumn = IRI.create( NS+"NullableColumn" );

}
