package eu.iksproject.kres.semion.manager.datasources;

import org.apache.stanbol.reengineer.base.IdentifiedDataSource;
import org.apache.stanbol.reengineer.base.settings.ConnectionSettings;
import org.apache.stanbol.reengineer.base.util.ReengineerType;
import org.apache.stanbol.rules.refactor.api.util.URIGenerator;


/**
 * An object representing a relational database in Semion.
 *  
 * @author andrea.nuzzolese
 *
 */
public class RDB extends IdentifiedDataSource {

	
	
	private ConnectionSettings connectionSettings;
	
	/**
	 * The constructor requires all the parameters in order to establish a connection with the physical DB.
	 * Those information regarding the connection with the DB are passed to the constructor in the {@link ConnectionSettings}.
	 *  
	 * @param connectionSettings {@link ConnectionSettings}
	 */
	public RDB(ConnectionSettings connectionSettings) {
		String dbId = connectionSettings.getUrl() + connectionSettings.getServerName() + ":" + connectionSettings.getPortNumber() + "/" + connectionSettings.getDatabaseName();
		id = URIGenerator.createID("urn:datasource-", dbId.getBytes());
		this.connectionSettings = connectionSettings;
	}
	
	
	
	/**
	 * Return the physical data source. In this specific case, as the data source is an RDB, a {@link ConnectionSettings} object containing
	 * the information in order to establish a connection with the DB via JDBC is returned
	 * 
	 * @return the information for establishing the connection with the DB
	 */
	@Override
	public Object getDataSource() {
		return connectionSettings;
	}

	
	/**
	 * Return the {@code int} representing the data source type in Semion.
	 * In the case of relationa databases the value returned is {@link ReengineerType.RDB}, namely 0.
	 * 
	 * @return the value assigned to the relational databases by Semion
	 */
	
	@Override
	public int getDataSourceType() {
		return ReengineerType.RDB;
	}

	
}
