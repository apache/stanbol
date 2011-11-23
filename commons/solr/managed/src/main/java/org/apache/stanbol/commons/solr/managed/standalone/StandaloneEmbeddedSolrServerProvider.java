package org.apache.stanbol.commons.solr.managed.standalone;

import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.SolrServerProvider;
import org.apache.stanbol.commons.solr.SolrServerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides {@link EmbeddedSolrServer} instances based on the
 * {@link StandaloneManagedSolrServer} implementation. Only intended to be used
 * outside of an OSGI environment. If running within an OSGI Environment
 * this functionality is provided by an {@link SolrServerProvider} implementation
 * that uses {@link CoreContainer}s and {@link SolrCore}s registered as
 * OSGI services.
 * @see StandaloneManagedSolrServer
 * @author Rupert Westenthaler
 *
 */
public class StandaloneEmbeddedSolrServerProvider implements SolrServerProvider {
    
    private final Logger log = LoggerFactory.getLogger(StandaloneEmbeddedSolrServerProvider.class);
    /**
     * Default constructor used by the {@link ServiceLoader} utility used
     * outside of an OSGI environment to instantiate {@link SolrServerProvider}
     * implementations for the different {@link SolrServerTypeEnum}. 
     */
    public StandaloneEmbeddedSolrServerProvider() {
        
    }
    
    @Override
    public SolrServer getSolrServer(SolrServerTypeEnum type, String uriOrPath, String... additional) throws IllegalArgumentException {
        if(type != SolrServerTypeEnum.EMBEDDED){
            throw new IllegalArgumentException("The parsed SolrServerType '"+
                type+"' is not supported (supported: '"+SolrServerTypeEnum.EMBEDDED+"')");
        }
        IndexReference indexRef = IndexReference.parse(uriOrPath);
        StandaloneManagedSolrServer server;
        log.debug("Create EmbeddedSolrServer for Server: {}, Index: {}",
            indexRef.getServer(),indexRef.getIndex());
        if(indexRef.getServer() == null){
            server = StandaloneManagedSolrServer.getManagedServer();
        } else {
            server = StandaloneManagedSolrServer.getManagedServer(indexRef.getServer());
        }
        if(server == null){
            log.debug("  > Managed Solr server with name {} not found -> return null",
                indexRef.getServer());
            return null;
        }
        log.debug("  > use managed Solr server with name {}",server.getServerName());

        String coreName;
        if(indexRef.getIndex() == null){
            coreName = server.getDefaultCore();
        } else if(indexRef.isPath()){
            coreName = server.getCoreForDirectory(indexRef.getIndex());
        } else {
            coreName = indexRef.getIndex();
        } 
        if(coreName != null){
            return new EmbeddedSolrServer(server.getCoreContainer(), coreName);
        } else {
            return null;
        }
    }

    /**
     * Outside an OSGI environment this also is used as {@link SolrServerProvider}
     * for the type {@link SolrServerTypeEnum#EMBEDDED}
     * @see org.apache.stanbol.commons.solr.SolrServerProvider#supportedTypes()
     */
    @Override
    public Set<SolrServerTypeEnum> supportedTypes() {
        return Collections.singleton(SolrServerTypeEnum.EMBEDDED);
    }
}
