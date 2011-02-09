package org.apache.stanbol.entityhub.yard.solr.provider;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.entityhub.yard.solr.provider.SolrServerProvider.Type;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for different {@link SolrServerProvider} present in the current
 * environment.
 * This manager works both within an OSGI Environment by defining an Reference
 * and outside by using {@link #getInstance()}.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true)
@Service(SolrServerProviderManager.class)
public final class SolrServerProviderManager {

    /**
     * Used for the singleton pattern, but also init within the OSGI environment
     * when {@link #activate(ComponentContext)} is called.
     */
    private static SolrServerProviderManager solrServerProviderManager;
    
    private final static Logger log = LoggerFactory.getLogger(SolrServerProviderManager.class);
    @Reference(
        referenceInterface=SolrServerProvider.class,
        strategy=ReferenceStrategy.EVENT,
        policy=ReferencePolicy.DYNAMIC,
        cardinality=ReferenceCardinality.MANDATORY_MULTIPLE,
        bind="addSolrProvider",unbind="removeSolrProvider")
    protected Map<Type,List<SolrServerProvider>> solrServerProviders = Collections.synchronizedMap(new EnumMap<Type,List<SolrServerProvider>>(Type.class));

    public static SolrServerProviderManager getInstance(){
        if(solrServerProviderManager == null){
            SolrServerProviderManager manager = new SolrServerProviderManager();
            Iterator<SolrServerProvider> providerIt = ServiceLoader.load(SolrServerProvider.class,SolrServerProviderManager.class.getClassLoader()).iterator();
            while(providerIt.hasNext()){
                SolrServerProvider provider = providerIt.next();
                log.info("load provider "+provider.getClass()+" supporting "+provider.supportedTypes());
                manager.addSolrProvider(provider);
            }
            solrServerProviderManager = manager;
        }
        return solrServerProviderManager;
    }
        
    @Activate
    protected void activate(ComponentContext context) {
        log.debug("Activate SolrServerProviderManager");
        if(solrServerProviderManager == null){
            solrServerProviderManager = this;
        }
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.debug("Activate SolrServerProviderManager");
        solrServerProviderManager = null;
    }
    public SolrServer getSolrServer(Type type, String uriOrPath, String...additionalServerLocations){
        List<SolrServerProvider> providers = solrServerProviders.get(type);
        if(providers == null){
            throw new IllegalArgumentException("No Provider for type "+type+" available");
        }
        for(SolrServerProvider provider : providers){
            try {
                return provider.getSolrServer(type, uriOrPath, additionalServerLocations);
            } catch (RuntimeException e) {
                log.warn("Unable to create SolrServer by using Provider "+provider,e);
            }
        }
        throw new IllegalArgumentException(String.format("Unable to create SolrServer for type %s and service location %s",
            type,uriOrPath));
    }
    
    protected void addSolrProvider(SolrServerProvider provider){
        log.info("add SolrProvider "+provider+" types "+provider.supportedTypes());
        for(Type type : provider.supportedTypes()){
            List<SolrServerProvider> providers = solrServerProviders.get(type);
            if(providers == null){
                providers = new CopyOnWriteArrayList<SolrServerProvider>();
                solrServerProviders.put(type, providers);
            }
            providers.add(provider);
        }
    }
    
    protected void removeSolrProvider(SolrServerProvider provider){
        log.info("remove SolrProvider "+provider+" types "+provider.supportedTypes());
        for(Type type : provider.supportedTypes()){
            List<SolrServerProvider> providers = solrServerProviders.get(type);
            if(providers != null){
                if(providers.remove(provider) && providers.isEmpty()){
                    //last element removed -> remove the mapping
                    solrServerProviders.remove(type); 
                }
            }
        }
    }
}
