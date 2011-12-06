/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.factstore.derby;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.factstore.api.FactStore;
import org.apache.stanbol.factstore.model.Fact;
import org.apache.stanbol.factstore.model.FactResult;
import org.apache.stanbol.factstore.model.FactResultSet;
import org.apache.stanbol.factstore.model.FactSchema;
import org.apache.stanbol.factstore.model.Query;
import org.apache.stanbol.factstore.model.WhereClause;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the FactStore interface based on an Apache Derby relational
 * database.
 * 
 * @author Fabian Christ
 */
@Component(immediate = true)
@Service
public class DerbyFactStore implements FactStore {

	private static Logger logger = LoggerFactory
			.getLogger(DerbyFactStore.class);

	private static int MAX_FACTSCHEMAURN_LENGTH = 96;

	private static final String CreateTableFactSchemata = "CREATE TABLE factschemata ( id INT GENERATED ALWAYS AS IDENTITY CONSTRAINT factschema_id PRIMARY KEY, name VARCHAR(128) NOT NULL )";
	private static final String CreateTableFactRoles = "CREATE TABLE factroles ( id INT GENERATED ALWAYS AS IDENTITY CONSTRAINT factrole_id PRIMARY KEY, factschema_id INT NOT NULL CONSTRAINT factschema_foreign_key REFERENCES factschemata ON DELETE CASCADE ON UPDATE RESTRICT, name VARCHAR(128) NOT NULL, type VARCHAR(512) NOT NULL )";
	private static final String CreateTableFactContexts = "CREATE TABLE factcontexts ( id INT GENERATED ALWAYS AS IDENTITY CONSTRAINT context_id PRIMARY KEY, created TIMESTAMP, updated TIMESTAMP, validFrom TIMESTAMP, validTo TIMESTAMP, contextURN VARCHAR(1024) )";

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
						sqls.add(CreateTableFactContexts);

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
		String factSchemaB64 = Base64.encodeBase64URLSafeString(factSchemaURN
				.getBytes());
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

	private boolean existsTable(String tableName, Connection con)
			throws Exception {
		boolean exists = false;

		ResultSet res = null;
		try {
			con = DriverManager.getConnection(DB_URL);
			DatabaseMetaData meta = con.getMetaData();
			res = meta.getTables(null, null, null, new String[] { "TABLE" });
			while (res.next()) {
				if (res.getString("TABLE_NAME").equalsIgnoreCase(tableName)) {
					exists = true;
					break;
				}
			}
		} catch (SQLException e) {
			logger
					.error(
							"Error while reading tables' metadata to check if table '{}' exists",
							tableName);
			throw new Exception("Error while reading tables' metadata", e);
		} finally {
			try {
				res.close();
			} catch (Throwable t) { /* ignore */
			}
		}

		return exists;
	}

	@Override
	public FactSchema getFactSchema(String factSchemaURN) {
		FactSchema factSchema = null;

		Connection con = null;
		try {
			con = DriverManager.getConnection(DB_URL);
			factSchema = loadFactSchema(factSchemaURN, con);
		} catch (Exception e) {
			logger.error("Error while loading fact schema", e);
			factSchema = null;
		} finally {
			try {
				con.close();
			} catch (Throwable t) { /* ignore */
			}
		}

		return factSchema;
	}

	private FactSchema loadFactSchema(String factSchemaURN, Connection con)
			throws Exception {
		FactSchema factSchema = null;
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
				String[] types = typeFromDB.split(",");
				if (types.length > 0) {
					for (String type : types) {
						factSchema.addRole(rs.getString("role"), type);
					}
				} else {
					factSchema.addRole(rs.getString("role"), typeFromDB);
				}
			}
		} catch (SQLException e) {
			throw new Exception("Error while selecting fact schema meta data",
					e);
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

		return factSchema;
	}

	@Override
	public void createFactSchema(FactSchema factSchema) throws Exception {
		// TODO Implement roll back behavior (transaction)

		String factSchemaB64 = Base64.encodeBase64URLSafeString(factSchema
				.getFactSchemaURN().getBytes());

		List<String> createFactSchemaTable = this.toSQLfromSchema(
				factSchemaB64, factSchema);

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
			} catch (Throwable t) { /* ignore */
			}
		}

		logger.info("Fact schema {} created as {}", factSchema
				.getFactSchemaURN(), factSchemaB64);
	}

	protected List<String> toSQLfromSchema(String factSchemaB64,
			FactSchema factSchema) throws Exception {
		List<String> sqls = new ArrayList<String>();

		// TODO Add SQL command for index creation

		StringBuilder createTableSQL = new StringBuilder("CREATE TABLE ");
		createTableSQL.append(factSchemaB64).append(' ');
		createTableSQL.append('(');
		createTableSQL.append("id INT GENERATED ALWAYS AS IDENTITY");
		createTableSQL.append(", context_id INT NOT NULL CONSTRAINT ");
		createTableSQL.append(factSchemaB64).append("_foreign_key");
		createTableSQL.append(" REFERENCES factcontexts ON DELETE CASCADE ON UPDATE RESTRICT");

		for (String role : factSchema.getRoles()) {
			createTableSQL.append(", ");
			createTableSQL.append(role);
			createTableSQL.append(" VARCHAR(1024)");
		}

		// Append created time stamp
		//createTableSQL.append(", created TIMESTAMP NOT NULL WITH DEFAULT CURRENT TIMESTAMP");
		createTableSQL.append(")");

		sqls.add(createTableSQL.toString());

		return sqls;
	}

	private void insertFactSchemaMetadata(FactSchema factSchema, Connection con) throws Exception {
		PreparedStatement ps = null;
		try {
			String insertFactSchema = "INSERT INTO factschemata (name) VALUES ( ? )";
			ps = con.prepareStatement(insertFactSchema,	PreparedStatement.RETURN_GENERATED_KEYS);
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

			logger.info("Inserted new fact schema {} with ID {}", factSchema.getFactSchemaURN(), factSchemaId);

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
			throw new Exception("Error while inserting fact schema meta data",
					e);
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

	private void executeUpdate(List<String> sqls, Connection con)
			throws Exception {
		for (String sql : sqls) {
			int res = -1;
			Statement statement = null;
			try {
				statement = con.createStatement();
				res = statement.executeUpdate(sql);
				if (res < 0) {
					logger.error("Negative result after executing SQL '{}'",
							sql);
					throw new Exception("Negative result after executing SQL");
				}
			} catch (SQLException e) {
				logger.error("Error executing SQL '{}'", sql, e);
				throw new Exception("Error executing SQL", e);
			} finally {
				try {
					statement.close();
				} catch (Throwable t) { /* ignore */
				}
			}
		}
	}
	
    @Override
    public Fact getFact(int factId, String factSchemaURN) throws Exception {
        Fact fact = null;
        Connection con = null;
        try {
            con = DriverManager.getConnection(DB_URL);
            FactSchema factSchema = this.loadFactSchema(factSchemaURN, con);
            
            if (factSchema != null) {
                fact = this.getFact(factId, factSchema, con);
            }
            
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                con.close();
            } catch (Throwable t) { /* ignore */
            }
        }
        
        return fact;
    }

	private Fact getFact(int factId, FactSchema factSchema, Connection con) throws Exception {
	    Fact fact = null;
	    
	    String factSchemaB64 = Base64.encodeBase64URLSafeString(factSchema.getFactSchemaURN().getBytes());
	    logger.info("Loading fact {} from fact table {}", factId, factSchemaB64);
	    
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuilder selectBuilder = new StringBuilder("SELECT ");
            boolean firstRole = true;
            for (String role : factSchema.getRoles()) {
                if (!firstRole) {
                    selectBuilder.append(",");
                }
                selectBuilder.append(role);
                firstRole = false;
            }
            selectBuilder.append(" FROM " + factSchemaB64 + " WHERE id=?");
            
            // TODO Load the context, too
            
            ps = con.prepareStatement(selectBuilder.toString());
            ps.setInt(1, factId);
            rs = ps.executeQuery();

            if (rs.next()) {
                fact = new Fact();
                fact.setFactSchemaURN(factSchema.getFactSchemaURN());
                for (int col=1; col<=rs.getMetaData().getColumnCount(); col++) {
                    String role = factSchema.fixSpellingOfRole(rs.getMetaData().getColumnName(col));
                    fact.addRole(role, rs.getString(col));
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error while selecting fact", e);
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
	    
        return fact;
    }

    @Override
	public int addFact(Fact fact) throws Exception {
        int factId = -1; 
	    Connection con = null;
		try {
			con = DriverManager.getConnection(DB_URL);
			factId = this.addFact(fact, con);
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				con.close();
			} catch (Throwable t) { /* ignore */
			}
		}

		logger.info("Fact {} created for {}", factId, fact.getFactSchemaURN());
		return factId;
	}

    private int addFact(Fact fact, Connection con) throws Exception {
        int factId = -1;
        FactSchema factSchema = this.loadFactSchema(fact.getFactSchemaURN(), con);
        if (factSchema != null) {
            StringBuilder insertContext = new StringBuilder("INSERT INTO factcontexts (created, updated, validFrom, validTo, contextURN) VALUES (?, ?, ?, ?, ?)");
            
            // Create the fact
            String factSchemaB64 = Base64.encodeBase64URLSafeString(fact.getFactSchemaURN().getBytes());

            StringBuilder insertFact = new StringBuilder("INSERT INTO ").append(factSchemaB64).append(" (context_id");
            StringBuilder valueSB = new StringBuilder(" VALUES (?");
            
            Map<String,Integer> roleIndexMap = new HashMap<String,Integer>();
            int roleIndex = 1;
            for (String role : factSchema.getRoles()) {
                if (fact.getRoles().contains(role)) {
                    insertFact.append(',');
                    valueSB.append(',');

                    insertFact.append(role);
                    valueSB.append('?');
                    
                    roleIndex++;
                    roleIndexMap.put(role, roleIndex);
                }
            }
            insertFact.append(')').append(valueSB).append(')');
            
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement(insertContext.toString(), PreparedStatement.RETURN_GENERATED_KEYS);
                long nowStamp = Calendar.getInstance().getTimeInMillis();
                if (fact.getContext() != null) {
                    if (fact.getContext().getCreated() != null) {
                        ps.setTimestamp(1, new Timestamp(fact.getContext().getCreated().getTime()));
                    }
                    else {
                        ps.setTimestamp(1, new Timestamp(nowStamp));
                    }
                    
                    if (fact.getContext().getUpdated() != null) {
                        ps.setTimestamp(2, new Timestamp(fact.getContext().getUpdated().getTime()));
                    }
                    else {
                        ps.setTimestamp(2, new Timestamp(nowStamp));
                    }
                    
                    if (fact.getContext().getValidFrom() != null) {
                        ps.setTimestamp(3, new Timestamp(fact.getContext().getValidFrom().getTime()));
                    }
                    else {
                        ps.setTimestamp(3, new Timestamp(nowStamp));
                    }
                    
                    if (fact.getContext().getVaildTo() != null) {
                        ps.setTimestamp(4, new Timestamp(fact.getContext().getVaildTo().getTime()));
                    }
                    else {
                        ps.setTimestamp(4, null);
                    }
                    
                    if (fact.getContext().getContextURN() != null) {
                        ps.setString(5, fact.getContext().getContextURN());
                    }
                    else {
                        ps.setTimestamp(5, null);
                    }
                }
                else {
                    ps.setTimestamp(1, new Timestamp(nowStamp));
                    ps.setTimestamp(2, new Timestamp(nowStamp));
                    ps.setTimestamp(3, new Timestamp(nowStamp));
                    ps.setTimestamp(4, null);
                    ps.setTimestamp(5, null);
                }
                ps.executeUpdate();
                ResultSet rsContext = ps.getGeneratedKeys();
                int contextId = -1;
                if (rsContext.next()) {
                    contextId = rsContext.getInt(1);
                }
                if (contextId < 0) {
                    throw new Exception("Could not obtain context ID after insert");
                }
                logger.info("Inserted new context with ID {} into factcontexts table", contextId);
                
                ps = con.prepareStatement(insertFact.toString(), PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setInt(1, contextId);
                for (String role : fact.getRoles()) {
                    Integer roleIdx = roleIndexMap.get(role);
                    if (roleIdx == null) {
                        throw new Exception("Unknown role '" + role + "' for fact schema "
                                            + fact.getFactSchemaURN());
                    } else {
                        ps.setString(roleIdx, fact.getValueOfRole(role));
                    }
                }
                ps.executeUpdate();
                ResultSet rsFact = ps.getGeneratedKeys();
                if (rsFact.next()) {
                    factId = rsFact.getInt(1);
                }
                if (factId < 0) {
                    throw new Exception("Could not obtain fact ID after insert");
                }

                logger.info("Inserted new fact with ID {} into fact schema table {}", factId, factSchemaB64);
            } catch (SQLException e) {
                throw new Exception("Error while writing fact into database", e);
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException e) {
                        /* ignore */
                    }
                }
            }

        } else {
            throw new Exception("Unknown fact schema '" + fact.getFactSchemaURN() + "'");
        }

        return factId;
	}

	@Override
	public void addFacts(Set<Fact> factSet) throws Exception {

		// TODO Improve roll back behavior if single fact of set could not be
		// committed

		Connection con = null;
		try {
			con = DriverManager.getConnection(DB_URL);
			for (Fact fact : factSet) {
				this.addFact(fact, con);
				logger.info("Fact created for {}", fact.getFactSchemaURN());
			}
		} catch (Exception e) {
			throw new Exception("Error while inserting new facts", e);
		} finally {
			try {
				con.close();
			} catch (Throwable t) { /* ignore */
			}
		}
	}

	@Override
	public FactResultSet query(Query query) throws Exception {
		FactResultSet frs = null;
		if (query != null) {

			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = DriverManager.getConnection(DB_URL);

				FactSchema schema = validateQuery(query, con);

				// from here on we have valid data

				String factSchemaB64 = Base64.encodeBase64URLSafeString(query.getFromSchemaURN().getBytes());

				StringBuilder querySql = new StringBuilder("SELECT ");

				boolean firstRole = true;
				for (String role : query.getRoles()) {
					if (!firstRole) {
						querySql.append(",");
					}
					querySql.append(role);
					firstRole = false;
				}

				querySql.append(" FROM ").append(factSchemaB64);

				List<String> queryParams = new ArrayList<String>();
				querySql.append(" WHERE ");
				for (WhereClause wc : query.getWhereClauses()) {
					querySql.append('(');
					querySql.append(wc.getComparedRole());
					switch (wc.getCompareOperator()) {
					case EQ:
						querySql.append(" = ").append('?');
						queryParams.add(wc.getSearchedValue());
						break;
					}
				}
				querySql.append(')');

				// TODO load context, too
				
				logger.info("performing query {}", querySql);

				ps = con.prepareStatement(querySql.toString());
				for (int i = 0; i < queryParams.size(); i++) {
					ps.setString(i + 1, queryParams.get(i));
				}

				rs = ps.executeQuery();
				if (rs != null) {
					List<String> header = new ArrayList<String>();
					for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
						header.add(schema.fixSpellingOfRole(rs.getMetaData().getColumnName(i)));
					}

					frs = new FactResultSet();
					frs.setHeader(header);

					while (rs.next()) {
						FactResult result = new FactResult();
						List<String> values = new ArrayList<String>();
						for (String head : header) {
							values.add(rs.getString(head));
						}
						result.setValues(values);
						frs.addFactResult(result);
					}
				}

			} catch (Exception e) {
				throw e;
			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (Throwable t) {
						// ignore
					}
				}
				if (ps != null) {
					try {
						ps.close();
					} catch (Throwable t) {
						// ignore
					}

				}
				if (con != null) {
					try {
						con.close();
					} catch (Throwable t) {
						// ignore
					}
				}
			}

		}

		return frs;
	}

	private FactSchema validateQuery(Query query, Connection con) throws Exception {
		FactSchema schema = this.loadFactSchema(query.getFromSchemaURN(), con);

		if (schema == null) {
			throw new Exception("Fact schema " + query.getFromSchemaURN() + " does not exist.");
		}

		StringBuilder unknownRoles = new StringBuilder();
		for (String role : query.getRoles()) {
			if (!schema.hasRole(role)) {
				if (!unknownRoles.toString().isEmpty()) {
					unknownRoles.append(',');
				}
				unknownRoles.append("role");
			}
		}
		if (!unknownRoles.toString().isEmpty()) {
			throw new Exception(
					"The following roles are unknown for the fact schema '"
							+ query.getFromSchemaURN() + "': "
							+ unknownRoles.toString());
		}
		
		return schema;
	}

}
