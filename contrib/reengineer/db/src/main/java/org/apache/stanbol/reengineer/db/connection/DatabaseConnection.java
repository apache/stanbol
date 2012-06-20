package org.apache.stanbol.reengineer.db.connection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.stanbol.commons.owl.transformation.JenaToOwlConvert;
import org.apache.stanbol.reengineer.db.vocab.DBS_L1;
import org.semanticweb.owlapi.model.OWLOntology;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.vocabulary.RDF;

public class DatabaseConnection {

    private OWLOntology schemaOntology;
    private Connection connection;

    private java.sql.Statement stmt;
    private PreparedStatement preparedStatement;

    public DatabaseConnection(OWLOntology schemaOntology) {

        this.schemaOntology = schemaOntology;
    }

    public void openDBConnection() {

        String sparql = "SELECT ?dbName ?jdbcDNS ?jdbcDriver ?username ?password " + "WHERE ?db <"
                        + RDF.type.getURI() + "> <" + DBS_L1.DatabaseConnection.toString() + "> . " + "?db <"
                        + DBS_L1.hasPhysicalName.toString() + "> ?dbName . " + "?db <"
                        + DBS_L1.hasJDBCDns.toString() + "> ?jdbcDNS . " + "?db <"
                        + DBS_L1.hasJDBCDriver.toString() + "> ?jdbcDriver . " + "?db <"
                        + DBS_L1.hasUsername.toString() + "> ?username . " + "?db <"
                        + DBS_L1.hasPassword.toString() + "> ?password ";

        JenaToOwlConvert jenaToOwlConvert = new JenaToOwlConvert();
        OntModel ontModel = jenaToOwlConvert.ModelOwlToJenaConvert(schemaOntology, "RDF/XML");

        Query sparqlQuery = QueryFactory.create(sparql);
        QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, ontModel);
        com.hp.hpl.jena.query.ResultSet jenaRs = qexec.execSelect();
        if (jenaRs.hasNext()) {
            QuerySolution qs = jenaRs.next();
            String jdbcDNS = qs.getLiteral("jdbcDNS").getLexicalForm();
            String jdbcDriver = qs.getLiteral("jdbcDriver").getLexicalForm();
            String username = qs.getLiteral("username").getLexicalForm();
            String password = qs.getLiteral("password").getLexicalForm();

            if (jdbcDNS != null && username != null && password != null && jdbcDriver != null) {
                try {
                    Class.forName(jdbcDriver);
                    connection = java.sql.DriverManager.getConnection(jdbcDNS, username, password);
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }

    }

    public ResultSet executeQuery(String query) {
        try {

            openDBConnection();

            System.out.println(query);
            preparedStatement = connection.prepareStatement(query);

            return preparedStatement.executeQuery();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public synchronized ResultSet executeQuerySafeMemory(String query) {
        try {

            openDBConnection();

            stmt = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                java.sql.ResultSet.CONCUR_READ_ONLY);
            System.out.println("Integer.MIN_VALUE: " + Integer.MIN_VALUE);
            stmt.setFetchSize(Integer.MIN_VALUE);

            return stmt.executeQuery(query);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public void closeDBConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void closeStatement() {
        try {
            stmt.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void closePreparedStatement() {
        try {
            preparedStatement.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
