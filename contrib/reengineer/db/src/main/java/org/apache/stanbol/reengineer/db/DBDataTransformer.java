package org.apache.stanbol.reengineer.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import org.apache.stanbol.commons.owl.transformation.JenaToOwlConvert;
import org.apache.stanbol.reengineer.base.api.ReengineeringException;
import org.apache.stanbol.reengineer.base.api.util.ReengineerUriRefGenerator;
import org.apache.stanbol.reengineer.db.connection.DatabaseConnection;
import org.apache.stanbol.reengineer.db.vocab.DBS_L1;
import org.apache.stanbol.reengineer.db.vocab.DBS_L1_OWL;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyRenameException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Resource;

public class DBDataTransformer extends ReengineerUriRefGenerator {

    private OWLOntology schemaOntology;
    private DatabaseConnection databaseConnection;

    public DBDataTransformer(OWLOntology schemaOntology) {
        this.schemaOntology = schemaOntology;
        this.databaseConnection = new DatabaseConnection(schemaOntology);
    }

    public OWLOntology transformData(String graphNS, IRI ontologyIRI) throws ReengineeringException {

        OWLOntology dataOntology = null;

        if (schemaOntology != null) {

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLDataFactory factory = OWLManager.getOWLDataFactory();

            graphNS = graphNS.replace("#", "");
            String schemaNS = graphNS + "/schema#";

            if (ontologyIRI != null) {
                try {
                    dataOntology = manager.createOntology(ontologyIRI);
                } catch (OWLOntologyCreationException e) {
                    throw new ReengineeringException();
                }

            } else {
                try {
                    dataOntology = manager.createOntology();
                } catch (OWLOntologyCreationException e) {
                    throw new ReengineeringException();
                }

            }

            graphNS += "#";

            Hashtable<String,ArrayList<DBColumn>> tableColumnHash = new Hashtable<String,ArrayList<DBColumn>>();

            OWLClass databaseConnectionClass = factory.getOWLClass(DBS_L1_OWL.DatabaseConnection);
            Set<OWLIndividual> databaseConnections = databaseConnectionClass.getIndividuals(schemaOntology);
            if (databaseConnections != null && databaseConnections.size() == 1) {

                ArrayList<DBColumn> columnList = new ArrayList<DBColumn>();

                for (OWLIndividual dbConnection : databaseConnections) {
                    OWLObjectProperty hasTable = factory.getOWLObjectProperty(DBS_L1_OWL.hasTable);
                    Set<OWLIndividual> tables = dbConnection
                            .getObjectPropertyValues(hasTable, schemaOntology);
                    for (OWLIndividual tableIndividual : tables) {
                        OWLDataProperty hasName = factory.getOWLDataProperty(DBS_L1_OWL.hasName);
                        Set<OWLLiteral> tableNames = tableIndividual.getDataPropertyValues(hasName,
                            schemaOntology);

                        String tableName = null;
                        if (tableNames != null && tableNames.size() == 1) {
                            for (OWLLiteral tableNameLiteral : tableNames) {
                                tableName = tableNameLiteral.getLiteral();
                            }
                        }

                        OWLObjectProperty hasColumn = factory.getOWLObjectProperty(DBS_L1_OWL.hasColumn);
                        Set<OWLIndividual> tableColumns = tableIndividual.getObjectPropertyValues(hasColumn,
                            schemaOntology);

                        if (tableColumns != null) {
                            for (OWLIndividual tableColumn : tableColumns) {

                                String columnName = null;
                                String columnType = null;

                                Set<OWLLiteral> columnNameLiterals = tableColumn.getDataPropertyValues(
                                    hasName, schemaOntology);
                                if (columnNameLiterals != null && columnNameLiterals.size() == 1) {
                                    for (OWLLiteral columnNameLiteral : columnNameLiterals) {
                                        columnName = columnNameLiteral.getLiteral();
                                    }
                                }

                                OWLDataProperty hasSQLType = factory
                                        .getOWLDataProperty(DBS_L1_OWL.hasSQLType);
                                Set<OWLLiteral> columnTypeLiterals = tableColumn.getDataPropertyValues(
                                    hasSQLType, schemaOntology);
                                if (columnTypeLiterals != null && columnNameLiterals.size() == 1) {
                                    for (OWLLiteral columnTypeLiteral : columnNameLiterals) {
                                        columnType = columnTypeLiteral.getLiteral();
                                    }
                                }

                                if (columnName != null && columnType != null) {
                                    columnList.add(new DBColumn(columnName, columnType));
                                }
                            }
                        }

                        tableColumnHash.put(tableName, columnList);
                    }
                }
            }

            Set<String> tableNames = tableColumnHash.keySet();
            for (String tableName : tableNames) {
                databaseConnection.openDBConnection();

                String sqlQuery = "SELECT * FROM " + tableName;

                ResultSet resultSetSQL = databaseConnection.executeQuerySafeMemory(sqlQuery);

                try {
                    for (int z = 0; resultSetSQL.next(); z++) {

                        IRI recordIRI = IRI.create(graphNS + tableName + "_record_" + z);
                        OWLClassAssertionAxiom record = createOWLClassAssertionAxiom(factory, DBS_L1_OWL.Row,
                            recordIRI);
                        manager.applyChange(new AddAxiom(dataOntology, record));

                        Hashtable<Integer,String> foreignKeys = new Hashtable<Integer,String>();
                        ArrayList<Integer> notForeignKeys = new ArrayList<Integer>();

                        ArrayList<Integer> primaryKeys = new ArrayList<Integer>();
                        ArrayList<Integer> notPrimaryKeys = new ArrayList<Integer>();

                        ArrayList<DBColumn> dbColumns = tableColumnHash.get(tableName);

                        for (int j = 0, k = dbColumns.size(); j < k; j++) {
                            DBColumn dbColumn = dbColumns.get(j);
                            String dbColumnName = dbColumn.getName();
                            String dbColumnSQLType = dbColumn.getSqlType();

                            String value = resultSetSQL.getString(dbColumnName);

                            IRI valueResIRI;
                            String content = null;

                            if (primaryKeys.contains(Integer.valueOf(j))) {
                                valueResIRI = IRI.create(graphNS + tableName + "_" + dbColumnName + "_"
                                                         + value);
                                OWLClassAssertionAxiom valueRes = createOWLClassAssertionAxiom(factory,
                                    DBS_L1_OWL.Datum, valueResIRI);
                                manager.applyChange(new AddAxiom(dataOntology, valueRes));
                            } else if (notPrimaryKeys.contains(Integer.valueOf(j))) {
                                valueResIRI = IRI
                                        .create(graphNS + tableName + "_record_" + z + "_datum_" + j);
                                OWLClassAssertionAxiom valueRes = createOWLClassAssertionAxiom(factory,
                                    DBS_L1_OWL.Datum, valueResIRI);
                                manager.applyChange(new AddAxiom(dataOntology, valueRes));
                            } else {

                                JenaToOwlConvert jenaToOwlConvert = new JenaToOwlConvert();
                                OntModel ontModel = jenaToOwlConvert.ModelOwlToJenaConvert(schemaOntology,
                                    "RDF/XML");

                                String sparql = "SELECT ?c WHERE { " + "?c <" + DBS_L1.hasName + "> \""
                                                + tableName + "-column_" + dbColumnName + "\"^^xsd:string . "
                                                + "?c <" + DBS_L1.isPrimaryKeyMemberOf + "> ?p " + "}";

                                Query sparqlQuery = QueryFactory.create(sparql);
                                QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, ontModel);
                                com.hp.hpl.jena.query.ResultSet jenaRs = qexec.execSelect();
                                if (jenaRs != null && jenaRs.hasNext()) {
                                    valueResIRI = IRI.create(graphNS + tableName + "_" + dbColumnName + "_"
                                                             + value);
                                    OWLClassAssertionAxiom valueRes = createOWLClassAssertionAxiom(factory,
                                        DBS_L1_OWL.Datum, valueResIRI);
                                    manager.applyChange(new AddAxiom(dataOntology, valueRes));
                                    primaryKeys.add(Integer.valueOf(j));
                                } else {
                                    valueResIRI = IRI.create(graphNS + tableName + "_record_" + z + "_datum_"
                                                             + j);
                                    OWLClassAssertionAxiom valueRes = createOWLClassAssertionAxiom(factory,
                                        DBS_L1_OWL.Datum, valueResIRI);
                                    manager.applyChange(new AddAxiom(dataOntology, valueRes));
                                    notPrimaryKeys.add(Integer.valueOf(j));
                                }

                                IRI joinIRI;
                                String joinName;
                                if ((joinName = foreignKeys.get(Integer.valueOf(j))) != null) {
                                    joinIRI = IRI.create(graphNS + tableName + "_" + joinName + "_" + value);
                                    OWLClassAssertionAxiom join = createOWLClassAssertionAxiom(factory,
                                        DBS_L1_OWL.Datum, joinIRI);
                                    manager.applyChange(new AddAxiom(dataOntology, join));

                                    manager.applyChange(new AddAxiom(dataOntology,
                                            createOWLObjectPropertyAssertionAxiom(factory,
                                                DBS_L1_OWL.hasContent, valueResIRI, joinIRI)));
                                } else if (notForeignKeys.contains(Integer.valueOf(j))) {
                                    if (value != null && !value.toLowerCase().equals("null")) {
                                        manager.applyChange(new AddAxiom(dataOntology,
                                                createOWLDataPropertyAssertionAxiom(factory,
                                                    DBS_L1_OWL.hasContent, valueResIRI, value)));
                                    }
                                } else {

                                    sparql = "SELECT ?p ?n WHERE { " + "?c <" + DBS_L1.hasName + "> \""
                                             + tableName + "-column_" + dbColumnName + "\"^^xsd:string . "
                                             + "?c <" + DBS_L1.joinsOn + "> ?p . " + "?p <" + DBS_L1.hasName
                                             + "> ?n " + "}";

                                    Query sparqlQuery2 = QueryFactory.create(sparql);
                                    QueryExecution qexec2 = QueryExecutionFactory.create(sparqlQuery2,
                                        ontModel);
                                    com.hp.hpl.jena.query.ResultSet jenaRs2 = qexec.execSelect();

                                    if (jenaRs2.hasNext()) {
                                        QuerySolution qs = jenaRs2.next();
                                        Resource joinColumn = qs.getResource("?p");
                                        com.hp.hpl.jena.rdf.model.Literal joinNameNode = qs.getLiteral("?n");

                                        joinName = joinNameNode.getLexicalForm();

                                        joinIRI = IRI.create(graphNS + joinName + "_datum_" + value);

                                        OWLClassAssertionAxiom join = createOWLClassAssertionAxiom(factory,
                                            DBS_L1_OWL.Datum, joinIRI);
                                        manager.applyChange(new AddAxiom(dataOntology, join));

                                        manager.applyChange(new AddAxiom(dataOntology,
                                                createOWLObjectPropertyAssertionAxiom(factory,
                                                    DBS_L1_OWL.hasContent, valueResIRI, joinIRI)));
                                        foreignKeys.put(Integer.valueOf(j), joinName);

                                    } else {
                                        notForeignKeys.add(Integer.valueOf(j));
                                        if (value != null && !value.toLowerCase().equals("null")) {
                                            manager.applyChange(new AddAxiom(dataOntology,
                                                    createOWLDataPropertyAssertionAxiom(factory,
                                                        DBS_L1_OWL.hasContent, valueResIRI, value)));
                                        }
                                    }

                                }

                                manager.applyChange(new AddAxiom(dataOntology,
                                        createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.hasDatum,
                                            recordIRI, valueResIRI)));

                            }

                            IRI tableIRI = IRI.create(schemaNS + "table_" + tableName);
                            manager.applyChange(new AddAxiom(dataOntology,
                                    createOWLObjectPropertyAssertionAxiom(factory, DBS_L1_OWL.isRowOf,
                                        recordIRI, tableIRI)));

                        }

                    }
                } catch (OWLOntologyRenameException e) {
                    throw new ReengineeringException();
                } catch (SQLException e) {
                    throw new ReengineeringException();
                }
            }
        }

        return dataOntology;
    }

}

class DBColumn {
    private String name;
    private String sqlType;

    public DBColumn(String name, String sqlType) {
        this.name = name;
        this.sqlType = sqlType;
    }

    public String getName() {
        return name;
    }

    public String getSqlType() {
        return sqlType;
    }
}
