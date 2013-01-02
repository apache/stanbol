package org.apache.stanbol.commons.solr.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solr {@link ResourceLoader} implementation that supports adding an parent as
 * well as parsing the classloader used for 
 * {@link #newInstance(String, String...)}.<p>
 * This implementation can be used in combination with the 
 * {@link DataFileResourceLoader} to allow providing resources via the
 * Stanbol {@link DataFileProvider} infrastructure.<p>
 * The {@link #newInstance(String, String...)} method uses the same algorithm as
 * the {@link SolrResourceLoader#newInstance(String, String...)} method to 
 * build candidate class names. It also supports the default packages if
 * <code>null</code> or an empty array is parsed as second parameter.
 * @author Rupert Westenthaler
 *
 */
public class StanbolResourceLoader implements ResourceLoader {

    private Logger log = LoggerFactory.getLogger(StanbolResourceLoader.class);
    
    static final String project = "solr";
    static final String base = "org.apache" + "." + project;
    static final String[] packages = {"","analysis.","schema.","handler.","search.","update.","core.","response.","request.","update.processor.","util.", "spelling.", "handler.component.", "handler.dataimport." };
    
    protected final ClassLoader classloader;
    protected final ResourceLoader parent;
    
    public StanbolResourceLoader(){
        this(null,null);
    }
    
    public StanbolResourceLoader(ClassLoader classloader){
        this(classloader,null);
    }
    
    public StanbolResourceLoader(ResourceLoader parent){
        this(null,parent);
    }
    
    public StanbolResourceLoader(ClassLoader classloader, ResourceLoader parent){
        this.classloader = classloader == null ? StanbolResourceLoader.class.getClassLoader() : classloader;
        this.parent = parent;
    }
    
    
    @Override
    public InputStream openResource(String resource) throws IOException {
        InputStream in;
        String parentMessage = null;
        if(parent != null){
            try {
                in = parent.openResource(resource);
            } catch (IOException e) {
                in = null;
                parentMessage = e.getMessage();
            } catch (SecurityException e) { //do not catch security related exceptions
                throw e;
            } catch (RuntimeException e) {
                in = null;
                parentMessage = e.getMessage();
            }
        } else {
            in = null;
        }
        if(in == null){
            in = classloader.getResourceAsStream(resource);
        }
        if(in == null){
            throw new IOException("Unable to load Resource '"+resource+"' from "
                + (parent != null ? ("parent (message: "+parentMessage+") and from") : "")
                + "classpath!");
        }
        return in;
    }

    @Override
    public List<String> getLines(String resource) throws IOException {
        List<String> lines = new ArrayList<String>();
        LineIterator it = IOUtils.lineIterator(openResource(resource), "UTF-8");
        while(it.hasNext()){
            String line = it.nextLine();
            if(line != null && !line.isEmpty() && line.charAt(0) != '#'){
                lines.add(line);
            }
        }
        return lines;
    }

    @Override
    public Object newInstance(String cname, String... subpackages) {
        String parentMessage = null;
        if(parent != null){
            try {
                return parent.newInstance(cname, subpackages);
            } catch (SecurityException e) { //do not catch security related exceptions
                throw e;
            } catch (RuntimeException e) {
                parentMessage = e.getMessage();
            }
        }
        if (subpackages == null || subpackages.length == 0 || subpackages == packages) {
            subpackages = packages;
        }
        Class clazz = null;
        // first try cname == full name
        try {
            clazz = classloader.loadClass(cname);
        } catch (ClassNotFoundException e) {
            String newName = cname;
            if (newName.startsWith(project)) {
                newName = cname.substring(project.length() + 1);
            }
            for (String subpackage : subpackages) {
                try {
                    String name = base + '.' + subpackage + newName;
                    log.trace("Trying class name " + name);
                    clazz = classloader.loadClass(name);
                    break;
                } catch (ClassNotFoundException e1) {
                    // ignore... assume first exception is best.
                }
            }
        }
        if(clazz == null){
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, 
                "Error loading class '" + cname + "' " + (parent != null ? 
                        ("from parent (message: "+parentMessage+") and ") : "")
                        + "via Classloader "+classloader);

        }
        try {
          return clazz.newInstance();
        } catch (Exception e) {
            throw new SolrException( SolrException.ErrorCode.SERVER_ERROR,
                "Error instantiating class: '" + clazz.getName()+"'", e);
        }

    }
}
