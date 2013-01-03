package org.apache.stanbol.commons.solr.extras.paoding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.paoding.analysis.Constants;
import net.paoding.analysis.knife.PaodingMaker;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This BundleActivator copies the paoding dictionary to the persistent storage
 * of the bundle ( {@link BundleContext#getDataFile(String)} with argument "dict").
 * This is necessary because this library can not read the dictionary from within 
 * a jar file.<p>
 * 
 * @author Rupert Westenthaler
 *
 */
public class Activator implements BundleActivator {

    public static final String DICT_ARCHIVE = "paoding-dict.zip";
    private static Logger log = LoggerFactory.getLogger(Activator.class);
    
    @Override
    public void start(BundleContext ctx) throws Exception {
        
        File paodingDict = ctx.getDataFile("dict");
        if(paodingDict.isFile()){
            log.warn("Paoding dictionary root exists but is a File");
            log.warn("   ... try to delete");
            if(!paodingDict.delete()){
                throw new IllegalStateException("Unable to initialise paoding dictionary because "
                    + paodingDict.getAbsolutePath() +" exists, is a file and can not be deleted!");
            }
        }
        if(!paodingDict.isDirectory()){
            log.info("initialise paoding dictionary in {}",paodingDict.getAbsolutePath());
            InputStream in = Activator.class.getClassLoader().getResourceAsStream(DICT_ARCHIVE);
            if(in == null){
                throw new IllegalStateException("Unable to load paoding dictionary data from bundle "
                    + "using name "+DICT_ARCHIVE);
            }
            initPaodingDictionary(paodingDict,in);
        }
        initPaodingDictHomeProperty(paodingDict);
    }

    /**
     * Copies the dictionary from the parsed {@link InputStream} to the
     * parsed directory
     * @param paodingDict
     * @throws IOException
     */
    public static void initPaodingDictionary(File paodingDict, InputStream in) throws IOException {
        if(in == null){
            throw new IllegalArgumentException("The parsed InputStream MUST NOT be NULL");
        }
        if(paodingDict == null){
            throw new IllegalArgumentException("The parsed poading dictionary MUST NOT be NULL");
        }
        if(paodingDict.isFile()){
            throw new IllegalArgumentException("The parsed paoding dictionary MUST NOT be a File");
        }
        ZipArchiveInputStream zin = new ZipArchiveInputStream(in);
        ZipArchiveEntry entry;
        try {
            while((entry = zin.getNextZipEntry()) != null){
                if(!entry.isDirectory()){
                    File file = new File(paodingDict,entry.getName());
                    if(!file.isFile()){
                        //copy the entry
                        log.debug("   > copy {}",entry.getName());
                        IOUtils.copy(zin, FileUtils.openOutputStream(file));
                    } else {
                        log.debug("   < {} already present",entry.getName());
                    }
                }
            }
            log.info("  ... paoding dictionaly initialised");
        } catch (IOException e) {
            log.error("Unable to initialise paoding dictionary in "
                +paodingDict,e);
            if(paodingDict.exists() && !paodingDict.delete()){
                log.error("Unable to delete incomplete paoding dictionary "
                    +paodingDict+"! Please delete this directory manually before "
                    + " the next start of this Bundle!");
            }
            throw e;
        }
    }

    /**
     * @param paodingDict
     */
    public static void initPaodingDictHomeProperty(File paodingDict) {
        //set the Dictionary home to the PaodingMaker. This is somewhat a workaround as
        //setting the home directory is only supported via
        // a) an properties file loaded via the classpath (can not be used)
        // b) an System environment variable (Systen#getenv(..) NOT System#getProperty(..)!)
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        try{
            Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());
            Properties p = PaodingMaker.getProperties();
            p.setProperty(Constants.DIC_HOME, paodingDict.getAbsolutePath());
            //actually in the already initialised properties file the dictionary home is read
            //from the following key
            p.setProperty("paoding.dic.home.absolute.path", paodingDict.getAbsolutePath());
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        // TODO Auto-generated method stub
        
    }

}
