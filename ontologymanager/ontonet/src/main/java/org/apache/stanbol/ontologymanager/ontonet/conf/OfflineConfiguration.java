package org.apache.stanbol.ontologymanager.ontonet.conf;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for configuring the Ontology Network Manager offline mode.
 * 
 * @author alessandro
 * 
 */
public class OfflineConfiguration {

    /**
     * The paths of local directories to be searched for ontologies.
     */
    private Set<File> localDirs = new HashSet<File>();

    public void addDirectory(File directory) {
        if (directory.isDirectory()) localDirs.add(directory);
    }

    public void clearDirectories() {
        localDirs.clear();
    }

    public Set<File> getDirectories() {
        return localDirs;
    }

    public void removeDirectory(File directory) {
        if (directory.isDirectory()) localDirs.remove(directory);
    }

}
