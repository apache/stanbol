package eu.iksproject.fise.engines.autotagging.impl;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.commons.io.FileUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.store.FSDirectory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.autotagging.Autotagger;
import eu.iksproject.autotagging.jena.ModelIndexer;
import eu.iksproject.fise.engines.autotagging.AutotaggerProvider;

/**
 * Simple OSGi component to look up a configured autotagger instance.
 */
@Component(immediate = true, metatype = true)
@Service
public class ConfiguredAutotaggerProvider implements AutotaggerProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = "")
    public static final String LUCENE_INDEX_PATH = "eu.iksproject.fise.engines.autotagging.indexPath";

    private Autotagger autotagger;

    protected BundleContext bundleContext;

    public Autotagger getAutotagger() {
        return autotagger;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @SuppressWarnings("unchecked")
    public void activate(ComponentContext ce) throws IOException {
        bundleContext = ce.getBundleContext();
        Dictionary<String, String> properties = ce.getProperties();
        String directoryPath = properties.get(LUCENE_INDEX_PATH);
        if (directoryPath == null || directoryPath.trim().length() == 0) {
            File dataFolder = bundleContext.getDataFile("fise-engines-autotagging");
            if (!dataFolder.isDirectory()) {
                FileUtils.deleteQuietly(dataFolder);
                dataFolder.mkdirs();
            }
            directoryPath = ModelIndexer.buildDefaultIndex(dataFolder, false).getAbsolutePath();
        }
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            throw new IOException("Lucene index directory not found: "
                    + directory.getAbsolutePath());
        }
        log.info("Loading the autotagging index from {}", directoryPath);
        autotagger = new Autotagger(FSDirectory.open(directory));
    }

    public void deactivate(ComponentContext ce) {
        autotagger = null;
    }
}
