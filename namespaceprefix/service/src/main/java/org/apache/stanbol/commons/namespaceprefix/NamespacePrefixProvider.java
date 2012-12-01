package org.apache.stanbol.commons.namespaceprefix;

import java.util.List;

public interface NamespacePrefixProvider {

    /**
     * Getter for the namespace for the parsed prefix
     * @param prefix the prefix. '' for the default namepace
     * @return the namespace or <code>null</code> if the prefix is not known
     */
    String getNamespace(String prefix);
    /**
     * Getter for the prefix for a namespace. Note that a namespace might be 
     * mapped to multiple prefixes
     * @param namespace the namespace
     * @return the prefix or <code>null</code>
     */
    String getPrefix(String namespace);
    /**
     * Getter for all prefixes for the parsed namespace
     * @param namespace the namespace
     * @return the prefixes. An empty list if none
     */
    List<String> getPrefixes(String namespace);
}
