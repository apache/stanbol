package org.apache.stanbol.explanation.heuristics;

import java.util.Set;


public interface User extends Entity {

    /**
     * This method is used to obtain all the objects that identify the user in multiple contexts (e.g.
     * foaf:name, user name in the CMS, Social Security Number etc.).
     * 
     * @return
     */
    Set<Identifier> getIDs();

}
