package org.apache.stanbol.reengineer.base.api.settings;

public class DBConnectionSettings implements ConnectionSettings {

	private String url;
    private String serverName;
    private String portNumber;
    private String databaseName;
    private String userName;
    private String password;
    private String selectMethod;
    private String jdbcDriver;
    
    public DBConnectionSettings() {
    	
	}
    
    
    /**
     * 
     * Create a new {@link DBConnectionSettings} that contain all the information that enable to Semion to open a connection with the
     * specified database. 
     * 
     * @param url {@link String}
     * @param serverName {@link String}
     * @param portNumber {@link String}
     * @param databaseName {@link String}
     * @param userName {@link String}
     * @param password {@link String}
     * @param selectMethod {@link String}
     * @param jdbcDriver {@link String}
     */
    public DBConnectionSettings(String url, String serverName, String portNumber, String databaseName, String userName, String password, String selectMethod, String jdbcDriver) {
    	this.url = url;
    	this.serverName = serverName;
    	this.portNumber = portNumber;
    	this.databaseName = databaseName;
    	this.userName = userName;
    	this.password = password;
    	this.selectMethod = selectMethod;
    	this.jdbcDriver = jdbcDriver;
	}
    
    
	public String getUrl() {
		return url;
	}
	public String getServerName() {
		return serverName;
	}
	public String getPortNumber() {
		return portNumber;
	}
	public String getDatabaseName() {
		return databaseName;
	}
	public String getUserName() {
		return userName;
	}
	public String getPassword() {
		return password;
	}
	public String getSelectMethod() {
		return selectMethod;
	}

	
	public String getJDBCDriver() {
		return jdbcDriver;
	} 
}
