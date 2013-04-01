package org.apache.stanbol.commons.solr.impl;

import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_ANALYZER_FACTORY_NAME;

import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.stanbol.commons.solr.SolrServerAdapter;
import org.apache.stanbol.commons.solr.utils.AbstractAnalyzerFoctoryActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Overrides the {@link SolrResourceLoader#findClass(String, Class, String...)}
 * method to look for {@link TokenFilterFactory}, {@link CharFilterFactory} and
 * {@link TokenizerFactory} registered as OSGI services.<p>
 * This is because Solr 4 uses SPI ("META-INF/services" files) to lookup
 * those factories and this does not work across bundles in OSGI.<p>
 * This {@link SolrResourceLoader} variant is intended to be used together
 * with Bundle-Activators based on the {@link AbstractAnalyzerFoctoryActivator}.
 * <p> The {@link SolrServerAdapter} does use this class as {@link SolrResourceLoader}
 * when creating {@link SolrCore}s.
 * 
 * @author Rupert Westenthaler
 *
 */
public class OsgiSolrResourceLoader extends SolrResourceLoader {

    /*
     * static members form the parent implementation that are not visible to subclasses in a different package
     */
    static final String project = "solr";
    static final String base = "org.apache" + "." + project;
    private static final Pattern legacyAnalysisPattern = Pattern.compile("((\\Q" + base
            + ".analysis.\\E)|(\\Q" + project
            + ".\\E))([\\p{L}_$][\\p{L}\\p{N}_$]+?)(TokenFilter|Filter|Tokenizer|CharFilter)Factory");

    protected final BundleContext bc;

    public OsgiSolrResourceLoader(BundleContext bc, String instanceDir) {
        super(instanceDir, OsgiSolrResourceLoader.class.getClassLoader());
        this.bc = bc;
    }

    public OsgiSolrResourceLoader(BundleContext bc, String instanceDir, ClassLoader parent) {
        super(instanceDir, parent);
        this.bc = bc;
    }

    public OsgiSolrResourceLoader(BundleContext bc, String instanceDir, ClassLoader parent,
            Properties coreProperties) {
        super(instanceDir, parent, coreProperties);
        this.bc = bc;
    }

    @Override
    public <T> Class<? extends T> findClass(String cname, Class<T> expectedType, String... subpackages) {
        Class<? extends T> clazz = null;
        RuntimeException parentEx = null;
        try {
            clazz = super.findClass(cname, expectedType, subpackages);
        } catch (RuntimeException e) {
            parentEx = e;
        }
        if (clazz != null) {
            return clazz;
        }
        final Matcher m = legacyAnalysisPattern.matcher(cname);
        if (m.matches()) {
            final String name = m.group(4);
            log.trace("Trying to load class from analysis SPI using name='{}'", name);
            ServiceReference[] referenced;
            try {
                referenced =
                        bc.getServiceReferences(expectedType.getName(),
                            String.format("(%s=%s)", PROPERTY_ANALYZER_FACTORY_NAME, name.toLowerCase(Locale.ROOT)));
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException("Unable to create Filter for Service with name '" + name
                        + "'!", e);
            }
            if (referenced != null && referenced.length > 0) {
                Object service = bc.getService(referenced[0]);
                if (service != null) {
                    clazz = (Class<? extends T>) service.getClass();
                    bc.ungetService(referenced[0]); //we return the class and do not use the service
                    return clazz;
                }
            }
        }
        if(parentEx != null) {
            throw parentEx;
        } else {
            throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, "Error loading class '" + cname + "'");
        }
    }

}
