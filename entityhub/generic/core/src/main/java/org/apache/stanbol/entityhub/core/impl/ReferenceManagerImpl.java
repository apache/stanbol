package org.apache.stanbol.entityhub.core.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteException;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Component(immediate = true)
@Service
@Properties(value={
})
public class ReferenceManagerImpl implements ReferencedSiteManager {

    protected final Logger log;

    protected ComponentContext context;
    public ReferenceManagerImpl(){
        super();
        log = LoggerFactory.getLogger(ReferenceManagerImpl.class);
        log.info(" create instance of "+ReferenceManagerImpl.class);
    }
    @Reference(
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            referenceInterface=ReferencedSite.class,
            strategy=ReferenceStrategy.EVENT,
            policy=ReferencePolicy.DYNAMIC,
            bind="bindReferencedSites",
            unbind="unbindReferencedSites")
    List<ReferencedSite> referencedSites = new CopyOnWriteArrayList<ReferencedSite>();
    /**
     * Map holding the mapping of the site ID to the referencedSite Object
     * TODO: in principle it could be possible that two instances of
     * {@link ReferencedSite} could be configured to use the same ID
     */
    private final Map<String,ReferencedSite> idMap =
        Collections.synchronizedMap(new HashMap<String,ReferencedSite>());
    /**
     * Map holding the mappings between entityPrefixes and referenced sites
     */
    private final Map<String,Collection<ReferencedSite>> prefixMap =
        Collections.synchronizedMap(new TreeMap<String, Collection<ReferencedSite>>());
    /**
     * This List is used for binary searches within the prefixes to find the
     * {@link ReferencedSite} to search for a {@link #getSign(String)}
     * request.<b>
     * NOTE: Every access to this list MUST BE synchronised to {@link #prefixMap}
     * TODO: I am quite sure, that there is some ioUtils class that provides
     * both a Map and an sorted List over the keys!
     */
    private final List<String> prefixList = new ArrayList<String>();

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        log.info("Activate ReferenceManager with context" + context);
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("Deactivate ReferenceManager with context" + context);
        this.context = null;
        synchronized (prefixMap) {
            this.prefixList.clear();
            this.prefixMap.clear();
        }
        this.idMap.clear();
    }
    @Override
    public void addReferredSite(String baseUri, Dictionary<String,?> properties) {
        //TODO: implement
        throw new UnsupportedOperationException();

    }

    protected void bindReferencedSites(ReferencedSite referencedSite){
        log.info(" ... binding ReferencedSite "+referencedSite.getId());
        referencedSites.add(referencedSite);
        idMap.put(referencedSite.getId(), referencedSite);
        addEntityPrefixes(referencedSite);
    }
    protected void unbindReferencedSites(ReferencedSite referencedSite){
        log.info(" ... unbinding ReferencedSite "+referencedSite.getId());
        referencedSites.remove(referencedSite);
        idMap.remove(referencedSite.getId());
        removeEntityPrefixes(referencedSite);
    }
    /**
     * Adds the prefixes of the parsed Site to the Map holding the according mappings
     * @param referencedSite
     */
    private void addEntityPrefixes(ReferencedSite referencedSite) {
        for(String prefix : referencedSite.getEntityPrefixes()){
            synchronized (prefixMap) {
                Collection<ReferencedSite> sites = prefixMap.get(prefix);
                if(sites == null){
                    sites = new CopyOnWriteArrayList<ReferencedSite>();
                    prefixMap.put(prefix, sites);
                    //this also means that the prefix is not part of the prefixList
                    int pos = Collections.binarySearch(prefixList, prefix);
                    if(pos<0){
                        prefixList.add(Math.abs(pos)-1,prefix);
                    }
                    prefixList.add(Collections.binarySearch(prefixList, prefix)+1,prefix);
                }
                //TODO: Sort the referencedSites based on the ServiceRanking!
                sites.add(referencedSite);
            }
        }
    }
    /**
     * Removes the prefixes of the parsed Site to the Map holding the according mappings
     * @param referencedSite
     */
    private void removeEntityPrefixes(ReferencedSite referencedSite) {
        for(String prefix : referencedSite.getEntityPrefixes()){
            synchronized (prefixMap) {
                Collection<ReferencedSite> sites = prefixMap.get(prefix);
                if(sites != null){
                    sites.remove(referencedSite);
                }
                if(sites.isEmpty()){
                    //remove key from the Map
                    prefixMap.remove(prefix);
                    //remove also the prefix from the List
                    prefixList.remove(prefix);
                }
            }

        }
    }
    @Override
    public ReferencedSite getReferencedSite(String id) {
        return idMap.get(id);
    }
    @Override
    public Collection<String> getReferencedSiteIds() {
        return Collections.unmodifiableCollection(idMap.keySet());
    }

    @Override
    public boolean isReferred(String id) {
        return idMap.containsKey(id);
    }
    @Override
    public Collection<ReferencedSite> getReferencedSitesByEntityPrefix(String entityUri) {
        if(entityUri == null){
            log.warn("NULL value parsed for Parameter entityUri -> return emptyList!");
            return Collections.emptyList();
        }
        synchronized (prefixMap) { //sync is done via the Map (for both the list and the map)!
            int pos = Collections.binarySearch(prefixList, entityUri);
            final int prefixPos;
            if(pos < 0){
                /**
                 * Example:
                 * ["a","b"] <- "bc"
                 * binary search returns -3 (because insert point would be +2)
                 * to find the prefix we need the insert point-1 -> pos 1
                 *
                 * Example2:
                 * [] <- "bc"
                 * binary search returns -1 (because insert point would be 0)
                 * to find the prefix we need the insert point-1 -> pos -1
                 * therefore we need to check for negative prefixPos and return
                 * an empty list!
                 */
                prefixPos = Math.abs(pos)-2;
            } else {
                prefixPos = pos; //entityUri found in list
            }
            if(prefixPos<0){
                return Collections.emptySet();
            } else {
                String prefix = prefixList.get(prefixPos);
                if(entityUri.startsWith(prefix)){
                    log.debug("Found prefix "+prefix+" for Entity "+entityUri);
                    return prefixMap.get(prefix);
                } //else the parsed entityPrefix does not start with the found prefix
                // this may only happen, when the prefixPos == prefixList.size()
            }
        }
        log.info("No registered prefix for entity "+entityUri);
        return Collections.emptySet();
    }
    @Override
    public QueryResultList<String> findIds(FieldQuery query) {
        // We need to search all referenced Sites
        Set<String> entityIds = new HashSet<String>();
        for(ReferencedSite site : referencedSites){
            try {
                for(String entityId : site.findReferences(query)){
                    entityIds.add(entityId);
                }
            } catch (ReferencedSiteException e) {
                log.warn("Unable to access Site "+site.getName()+" (url = "+site.getAccessUri()+")",e);
            }
        }
        return new QueryResultListImpl<String>(query, entityIds.iterator(),String.class);
    }
    @Override
    public QueryResultList<Representation> find(FieldQuery query) {
        Set<Representation> representations = new HashSet<Representation>();
        for(ReferencedSite site : referencedSites){
            try {
                for(Representation rep : site.find(query)){
                    if(!representations.contains(rep)){ //do not override
                        representations.add(rep);
                    } else {
                        log.info("Entity "+rep.getId()+" found on more than one Referenced Site -> Representation of Site "+site.getName()+" is ignored");
                    }
                }
            } catch (ReferencedSiteException e) {
                log.warn("Unable to access Site "+site.getName()+" (url = "+site.getAccessUri()+")",e);
            }
        }
        return new QueryResultListImpl<Representation>(query, representations,Representation.class);
    }
    @Override
    public QueryResultList<Sign> findEntities(FieldQuery query) {
        Set<Sign> entities = new HashSet<Sign>();
        for(ReferencedSite site : referencedSites){
            try {
                for(Sign rep : site.findSigns(query)){
                    if(!entities.contains(rep)){ //do not override
                        entities.add(rep);
                    } else {
                        log.info("Entity "+rep.getId()+" found on more than one Referenced Site -> Representation of Site "+site.getName()+" is ignored");
                    }
                }
            } catch (ReferencedSiteException e) {
                log.warn("Unable to access Site "+site.getName()+" (url = "+site.getAccessUri()+")",e);
            }
        }
        return new QueryResultListImpl<Sign>(query, entities,Sign.class);
    }
    @Override
    public InputStream getContent(String entityId, String contentType) {
        Collection<ReferencedSite> sites = getReferencedSitesByEntityPrefix(entityId);
        if(sites.isEmpty()){
            log.info("No Referenced Site registered for Entity "+entityId+"");
            log.debug("Registered Prefixes "+prefixList);
            return null;
        }
        for(ReferencedSite site : sites){
            InputStream content;
            try {
                content = site.getContent(entityId, contentType);
                if(content != null){
                    log.debug("Return Content of type "+contentType+" for Entity "+entityId+" from referenced site "+site.getName());
                    return content;
                }
            } catch (ReferencedSiteException e) {
                log.warn("Unable to access Site "+site.getName()+" (url = "+site.getAccessUri()+")",e);
            }

        }
        log.debug("Entity "+entityId+" not found on any of the following Sites "+sites);
        return null;
    }
    @Override
    public Sign getSign(String entityId) {
        Collection<ReferencedSite> sites = getReferencedSitesByEntityPrefix(entityId);
        if(sites.isEmpty()){
            log.info("No Referenced Site registered for Entity "+entityId+"");
            log.debug("Registered Prefixes "+prefixList);
            return null;
        }
        for(ReferencedSite site : sites){
            Sign entity;
            try {
                entity = site.getSign(entityId);
                if(entity != null){
                    log.debug("Return Representation of Site "+site.getName()+" for Entity "+entityId);
                    return entity;
                }
            } catch (ReferencedSiteException e) {
                log.warn("Unable to access Site "+site.getName()+" (url = "+site.getAccessUri()+")",e);
            }

        }
        log.debug("Entity "+entityId+" not found on any of the following Sites "+sites);
        return null;
    }



}
