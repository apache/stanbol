package org.apache.stanbol.factstore.derby;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.factstore.api.FactStore;
import org.apache.stanbol.factstore.model.FactSchema;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the FactStore interface based on an Apache Derby relational database.
 * 
 * @author Fabian Christ
 */
@Component(immediate = true)
@Service
public class DerbyFactStore implements FactStore {

    private static Logger logger = LoggerFactory.getLogger(DerbyFactStore.class);

    private static int MAX_FACTSCHEMAURN_LENGTH = 96;

    private static final String CreateTableFactSchemata = "CREATE TABLE factschemata ( id INT GENERATED ALWAYS AS IDENTITY CONSTRAINT factschema_id PRIMARY KEY, name VARCHAR(128) NOT NULL )";
    private static final String CreateTableFactRoles = "CREATE TABLE factroles ( id INT GENERATED ALWAYS AS IDENTITY CONSTRAINT factrole_id PRIMARY KEY, factschema_id INT NOT NULL CONSTRAINT factschema_foreign_key REFERENCES factschemata ON DELETE CASCADE ON UPDATE RESTRICT, name VARCHAR(128) NOT NULL, type VARCHAR(512) NOT NULL )";

    public static final String DB_URL = "jdbc:derby:factstore;create=true";

    @Activate
    protected void activate(ComponentContext cc) throws Exception {
        logger.info("Activating FactStore...");

        logger.info("Connecting to Derby DB {}", DB_URL);
        Connection con = null;
        try {
            con = DriverManager.getConnection(DB_URL);

            if (con != null) {
                logger.info("Derby connection established.");

                try {
                    if (!existsTable("factschemata", con)) {
                        List<String> sqls = new ArrayList<String>();
                        sqls.add(CreateTableFactSchemata);
                        sqls.add(CreateTableFactRoles);

                        this.executeUpdate(sqls, con);

                        logger.info("Created FactStore meta tables.");
                    }
                } catch (Exception e) {
                    throw new Exception("Error creating meta data tables", e);
                }
            }
        } catch (Exception e) {
            throw new Exception("Derby DB error. Can't activate.", e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    /* ignore */
                }
            }
        }

        logger.info("FactStore activated.");
    }

    @Override
    public int getMaxFactSchemaURNLength() {
        return MAX_FACTSCHEMAURN_LENGTH;
    }

    @Override
    public boolean existsFactSchema(String factSchemaURN) throws Exception {
        String factSchemaB64 = Base64.encodeBase64URLSafeString(factSchemaURN.getBytes());
        boolean tableExists = false;
        Connection con = null;
        try {
            con = DriverManager.getConnection(DB_URL);
            tableExists = this.existsTable(factSchemaB64, con);
        } catch (Exception e) {
            throw new Exception("Error checking table existence", e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    /* ignore */
                }
            }
        }

        return tableExists;
    }

    private boolean existsTable(String tableName, Connection con) throws Exception {
        boolean exists = false;

        ResultSet res = null;
        try {
            con = DriverManager.getConnection(DB_URL);
            DatabaseMetaData meta = con.getMetaData();
            res = meta.getTables(null, null, null, new String[] {"TABLE"});
            while (res.next()) {
                if (res.getString("TABLE_NAME").equalsIgnoreCase(tableName)) {
                    exists = true;
                    break;
                }
            }
        } catch (SQLException e) {
            logger.error("Error while reading tables' metadata to check if table '{}' exists", tableName);
            throw new Exception("Error while reading tables' metadata", e);
        } finally {
            try {
                res.close();
            } catch (Throwable t) { /* ignore */}
        }

        return exists;
    }
    
    @Override
    public FactSchema getFactSchema(String factSchemaURN) {
        FactSchema factSchema = null;
        
        Connection con = null;
        try {
            con = DriverManager.getConnection(DB_URL);
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                String selectFactSchema = "SELECT factschemata.name AS schemaURN, factroles.name AS role, factroles.type AS type FROM factroles JOIN factschemata ON ( factschemata.id = factroles.factschema_id ) WHERE factschemata.name = ?";
                ps = con.prepareStatement(selectFactSchema);
                ps.setString(1, factSchemaURN);
                rs = ps.executeQuery();
                
                boolean first = true;
                while (rs.next()) {
                    if (first) {
                        factSchema = new FactSchema();
                        factSchema.setFactSchemaURN(rs.getString("schemaURN"));
                        first = false;
                    }
                    String typeFromDB = rs.getString("type");
                    String [] types = typeFromDB.split(",");
                    if (types.length > 0) {
                        for (String type : types) {
                            factSchema.addRole(rs.getString("role"), type);
                        }
                    }
                    else {
                        factSchema.addRole(rs.getString("role"), typeFromDB);
                    }
                }
            } catch (SQLException e) {
                throw new Exception("Error while selecting fact schema meta data", e);
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        /* ignore */
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException e) {
                        /* ignore */
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while creating fact schema", e);
            factSchema = null;
        } finally {
            try {
                con.close();
            } catch (Throwable t) { /* ignore */}
        }
        
        return factSchema;
    }

    @Override
    public void createFactSchema(FactSchema factSchema) throws Exception {
        // TODO Implement roll back behavior (transaction)

        String factSchemaB64 = Base64.encodeBase64URLSafeString(factSchema.getFactSchemaURN().getBytes());

        List<String> createFactSchemaTable = this.toSQLfromSchema(factSchemaB64, factSchema);

        Connection con = null;
        try {
            con = DriverManager.getConnection(DB_URL);
            this.executeUpdate(createFactSchemaTable, con);
            this.insertFactSchemaMetadata(factSchema, con);
        } catch (Exception e) {
            throw new Exception("Error while creating fact schema", e);
        } finally {
            try {
                con.close();
            } catch (Throwable t) { /* ignore */}
        }

        logger.info("Fact schema {} created as {}", factSchema.getFactSchemaURN(), factSchemaB64);
    }

    private void insertFactSchemaMetadata(FactSchema factSchema, Connection con) throws Exception {
        PreparedStatement ps = null;
        try {
            String insertFactSchema = "INSERT INTO factschemata (name) VALUES ( ? )";
            ps = con.prepareStatement(insertFactSchema, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, factSchema.getFactSchemaURN());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();

            int factSchemaId = -1;
            if (rs.next()) {
                factSchemaId = rs.getInt(1);
            }
            if (factSchemaId < 0) {
                throw new Exception("Could not obtain fact schema ID after insert");
            }

            logger
                    .info("Inserted new fact schema {} with ID {}", factSchema.getFactSchemaURN(),
                        factSchemaId);

            String insertFactRoles = "INSERT INTO factroles (factschema_id, name, type) VALUES ( ?, ?, ? )";
            ps = con.prepareStatement(insertFactRoles);
            for (String role : factSchema.getRoles()) {
                ps.setInt(1, factSchemaId);
                ps.setString(2, role);
                
                StringBuilder typeList = new StringBuilder();
                boolean first = true;
                for (String type : factSchema.getTypesOfRole(role)) {
                    if (!first) {
                        typeList.append(",");
                    }
                    typeList.append(type);
                    first = false;
                }
                ps.setString(3, typeList.toString());

                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new Exception("Error while inserting fact schema meta data", e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    /* ignore */
                }
            }
        }
    }

    protected List<String> toSQLfromSchema(String factSchemaB64, FactSchema factSchema) throws Exception {
        List<String> sqls = new ArrayList<String>();

        // TODO Add SQL command for index creation

        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE ");
        createTableSQL.append(factSchemaB64).append(' ');
        createTableSQL.append('(');
        createTableSQL.append("id INT GENERATED ALWAYS AS IDENTITY");

        for (String role : factSchema.getRoles()) {
            createTableSQL.append(", ");
            createTableSQL.append(role);
            createTableSQL.append(" VARCHAR(1024)");
        }
        createTableSQL.append(')');

        sqls.add(createTableSQL.toString());

        return sqls;
    }

    private void executeUpdate(List<String> sqls, Connection con) throws Exception {
        for (String sql : sqls) {
            int res = -1;
            Statement statement = null;
            try {
                statement = con.createStatement();
                res = statement.executeUpdate(sql);
                if (res < 0) {
                    logger.error("Negative result after executing SQL '{}'", sql);
                    throw new Exception("Negative result after executing SQL");
                }
            } catch (SQLException e) {
                logger.error("Error executing SQL '{}'", sql, e);
                throw new Exception("Error executing SQL", e);
            } finally {
                try {
                    statement.close();
                } catch (Throwable t) { /* ignore */}
            }
        }
    }

}
