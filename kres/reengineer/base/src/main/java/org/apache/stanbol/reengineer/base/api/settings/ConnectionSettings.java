package org.apache.stanbol.reengineer.base.api.settings;

import java.io.Serializable;

/**
 * A {@code ConnectionSettings} contains all the information that are needed in order to open a connection with a relational
 * database through JDBC.
 * 
 * @author andrea.nuzzolese
 *
 */
public interface ConnectionSettings extends Serializable{

	
	/**
	 * Get the URL of the connection.
	 * 
	 * @return the URL of the connection as a {@link String}.
	 */
	public String getUrl();
	
	/**
	 * Get the name of the server on which the DB is running.
	 * 
	 * @return the name of the server as a {@link String}.
	 */
	public String getServerName();
	
	/**
	 * Get the port of the server on which the DB is running.
	 * 
	 * @return the port of the server as a {@link String}.
	 */
	public String getPortNumber();
	
	/**
	 * Get the name of the database.
	 * 
	 * @return the port of the server as a {@link String}.
	 */
	public String getDatabaseName();
	
	/**
	 * Get the user name for the autenthication.
	 * 
	 * @return the user name as a {@link String}.
	 */
	public String getUserName();
	
	/**
	 * Get the password for the autenthication.
	 * 
	 * @return the password as a {@link String}.
	 */
	public String getPassword();
	
	/**
	 * Get the select method for querying.
	 * 
	 * @return the select method as a {@link String}.
	 */
	public String getSelectMethod();
	
	/**
	 * Get the JDBC driver of the database.
	 * 
	 * @return the JDBC driver as a {@link String}.
	 */
	public String getJDBCDriver();
	
}
