package org.apache.stanbol.commons.solr.managed;

public final class ManagedIndexConstants {
    
    private ManagedIndexConstants() {/*do not create instances*/}
    
    /**
     * Key used to configure the names for the Index-Archive(s) to be used for
     * this core. Multiple archives can be parsed by using , as separator.<p>
     * Note: that a single Archive can also be provided as second parameter. 
     * If this is the case it will override this value if present in the third 
     * parameter of 
     * {@link ManagedSolrServer#createSolrIndex(String, String, java.util.Properties) createIndex} and
     * {@link ManagedSolrServer#updateIndex(String, String, java.util.Properties) updateIndex}.
     */
    public static final String INDEX_ARCHIVES = "Index-Archive";
    /**
     * Key used to specify if an index is synchronized with the provided
     * Index-Archive. If <code>false</code> the index will be initialised
     * by the provided Archive and than stay independent (until explicit calls
     * to {@link ManagedSolrServer#updateIndex(String, String, java.util.Properties)}.
     * If synchronised the index will stay connected with the Archive.
     * So deleting the archive will also cause the index to get inactive and 
     * making the Index-Archive available again (e.g. a newer version) will
     * case an re-initalisation of the index based on the new archive.
     */
    public static final String SYNCHRONIZED = "Synchronized";
    /**
     * The name of the index. Note that the name can be also specified as the
     * first parameter of 
     * {@link ManagedSolrServer#createSolrIndex(String, String, java.util.Properties) createIndex} and
     * {@link ManagedSolrServer#updateIndex(String, String, java.util.Properties) updateIndex}.
     * If this is the case it will override the value provided in the property
     * file.
     */
    public static final String INDEX_NAME = "Index-Name";
    /**
     * The name of the server to install this index to. If present the
     * server will check that the name corresponds to its own name.
     */
    public static final String SERVER_NAME = "Server-Name";

}
