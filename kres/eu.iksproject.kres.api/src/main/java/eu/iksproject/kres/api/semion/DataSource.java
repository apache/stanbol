package eu.iksproject.kres.api.semion;

/**
 * A {@code DataSource} object represents a physical non-RDF data source in Semion.
 * <br>
 * <br>
 * Supported data sources are:
 * <ul>
 * <li>Relational databases
 * <li>XML
 * <li>iCalendar
 * <li>RSS
 * </ul>
 * 
 *  
 * @author andrea.nuzzolese
 *
 */

public interface DataSource {

	
	/**
	 * Get the ID of the data source as it is represented in Semion
	 * @return the {@link String} representing the ID of the physical data source in Semion
	 */
	public String getID();
	
	/**
	 * As a {@code DataSource} is only a representation of the data source in Semion, a method that returns the physical
	 * data source is provided.
	 * 
	 * @return the physical data source
	 */
	public Object getDataSource();
	
	/**
	 * Data sources that Semion is able to manage have an integer that identifies the type of the data source.
	 * 
	 * Valid values are:
	 * <ul>
	 * <li> 0 - Relational Databases
	 * <li> 1 - XML
	 * <li> 2 - iCalendar
	 * <li> 3 - RSS
	 * </ul>
	 * 
	 * @return the data source type
	 */
	public int getDataSourceType();
}
