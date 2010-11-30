package eu.iksproject.rick.servicesapi;
/**
 * Provides the Configuration needed by the {@link Rick}.
 * @author Rupert Westenthaler
 *
 */
public interface RickConfiguration {
	/**
	 * The property used to configure the id of the RickYard
	 */
	String RICK_YARD_ID = "eu.iksproject.rick.yard.rickYardId";
	/**
	 * The default ID for the Yard used for the Rick
	 */
	String DEFAULT_RICK_YARD_ID = "urn:eu.iksproject:rick:rickYard";
	/**
	 * This is the ID of the Yard used by the Rick to store its data
	 * @return the Rick-Yard id
	 */
	String getRickYardId();
}
