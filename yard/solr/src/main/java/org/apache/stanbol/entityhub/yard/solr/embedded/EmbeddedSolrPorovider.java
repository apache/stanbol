package org.apache.stanbol.entityhub.yard.solr.embedded;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.stanbol.entityhub.yard.solr.provider.SolrServerProvider;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
/**
 * Support for the use of {@link EmbeddedSolrPorovider} in combination with the
 * SolrYard implementation. This implements the {@link SolrServerProvider}
 * interface for the {@link Type#EMBEDDED}.<p>
 * TODO: Describe the configuration of the embedded SolrServer
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true)
@Service
public class EmbeddedSolrPorovider implements SolrServerProvider {
    private final Logger log = LoggerFactory.getLogger(EmbeddedSolrPorovider.class);
    /**
     * internally used to keep track of active {@link CoreContainer}s for
     * requested paths.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, CoreContainer> coreContainers = new ReferenceMap(); 
    
    @Property
    public static final String SOLR_HOME = "solr.solr.home";
    
    public EmbeddedSolrPorovider() {
    }
    
    @Override
    public SolrServer getSolrServer(Type type, String uriOrPath, String... additional) throws NullPointerException, IllegalArgumentException {
        log.debug(String.format("getSolrServer Request for %s and path %s",type,uriOrPath));
        if(uriOrPath == null){
            throw new NullPointerException("The Path to the Index MUST NOT be NULL!");
        }
        File index = new File(uriOrPath);
        if(!index.exists()){
            try {
                URI fileUri = new URI(uriOrPath);
                index = new File(fileUri);
            } catch (URISyntaxException e) {
                //also not an URI -> ignore
            }
            if(!index.exists()){
                throw new IllegalArgumentException(String.format("The parsed Index Path %s does not exist",uriOrPath));
            }
        }
        if(index.isDirectory()){
            File solr = getFile(index, "solr.xml");
            String coreName;
            if(solr != null){
                //in that case we assume that this is a single core installation
                coreName = "";
            } else {
                solr = getFile(index.getParentFile(), "solr.xml");
                if(solr != null){
                    //assume this is a multi core
                    coreName = index.getName();
                    index = index.getParentFile(); //set the index dir to the parent
                } else {
                    throw new IllegalArgumentException(String.format("The parsed Index Path %s is not an Solr " +
                    		"Index nor a Core of an Multi Core Configuration " +
                    		"(no \"solr.xml\" was found in this nor the parent directory!)",uriOrPath));
                }
            }
            //now init the EmbeddedSolrServer
            log.info(String.format("Create EmbeddedSolrServer for index %s and core %s",index.getAbsolutePath(),coreName));
            return new EmbeddedSolrServer(getCoreContainer(index.getAbsolutePath(), solr), coreName);
        } else {
            throw new IllegalArgumentException(String.format("The parsed Index Path %s is no Directory",uriOrPath));
        }
    }
    
    protected final CoreContainer getCoreContainer(String solrDir, File solrConf) throws IllegalArgumentException, IllegalStateException {
        CoreContainer container = coreContainers.get(solrDir);
        if(container == null){
            container = new CoreContainer(solrDir);
            coreContainers.put(solrDir, container);
            /*
             * NOTE:
             * We need to reset the ContextClassLoader to the one used for this
             * Bundle, because Solr uses this ClassLoader to load all the
             * plugins configured in the solr.xml and schema.xml.
             * The finally block resets the context class loader to the previous
             * value. (Rupert Westenthaler 20010209)
             */
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(EmbeddedSolrPorovider.class.getClassLoader());
            try {
                container.load(solrDir, solrConf);
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Unable to parse Solr Configuration",e);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to access Solr Configuration",e);
            } catch (SAXException e) {
                throw new IllegalStateException("Unable to parse Solr Configuration",e);
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }
        return container;
    }
    
    @Override
    public Set<Type> supportedTypes() {
        return Collections.singleton(Type.EMBEDDED);
    }
    @Activate
    protected void activate(ComponentContext context) {
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        //should we remove the coreContainers -> currently I don't because
        // (1) activate deactivate do not have any affect
        // (2) it are soft references anyway.
    }
    /**
     * Checks if the parsed directory contains a file that starts with the parsed
     * name. Parsing "hallo" will find "hallo.all", "hallo.ween" as well as "hallo".
     * @param dir the Directory. This assumes that the parsed File is not
     * <code>null</code>, exists and is an directory
     * @param name the name. If <code>null</code> any file is accepted, meaning
     * that this will return true if the directory contains any file 
     * @return the state
     */
    private boolean hasFile(File dir, String name){
        return dir.listFiles(new SimpleFileNameFilter(name)).length>0;
    }
    /**
     * Returns the first file that matches the parsed name.
     * Parsing "hallo" will find "hallo.all", "hallo.ween" as well as "hallo".
     * @param dir the Directory. This assumes that the parsed File is not
     * <code>null</code>, exists and is an directory.
     * @param name the name. If <code>null</code> any file is accepted, meaning
     * that this will return true if the directory contains any file 
     * @return the first file matching the parsed name.
     */
    private File getFile(File dir, String name){
        File[] files =  dir.listFiles(new SimpleFileNameFilter(name));
        return files.length>0?files[0]:null;
    }
    /**
     * Could not find a simple implementation of {@link FilenameFilter} that
     * can be used if a file exists. If someone knows one, feel free to replace
     * this one! 
     * @author Rupert Westenthaler
     *
     */
    private static class SimpleFileNameFilter implements FilenameFilter {

        private String name;
        public SimpleFileNameFilter(String name) {
            this.name = name;
        }
        @Override
        public boolean accept(File dir, String name) {
            return this.name == null?true:name.startsWith(this.name);
        }
        
    }
}
