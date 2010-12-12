package eu.iksproject.rick.servicesapi.yard;

import java.util.Collection;

/**
 * Manages the different active Yards
 * TODO: Who is responsible of initialising the RickYard (the yard storing
 * Symbols and Mapped Entities)?
 * @author Rupert Westenthaler
 */
public interface YardManager {

    /**
     * Getter for the IDs of Yards currently managed by this Manager
     * @return the Ids of the currently active Yards
     */
    Collection<String> getYardIDs();
    /**
     * Returns if there is a Yard for the parsed ID
     * @param id the id
     * @return <code>true</code> if a {@link Yard} with the parsed ID is managed
     * by this YardManager.
     */
    boolean isYard(String id);
    /**
     * Getter for the Yard based on the parsed Id
     * @param id the ID
     * @return The Yard or <code>null</code> if no Yard with the parsed ID is
     * active.
     */
    Yard getYard(String id);

}
