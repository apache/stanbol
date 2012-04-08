package org.apache.stanbol.enhancer.contentitem.file;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.test.ContentItemFactoryTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class CustomDirFileContentItemFactoryTest extends ContentItemFactoryTest {

    private static File customDir;
    private static ContentItemFactory factory;
    
    @BeforeClass
    public static void init(){
        String prefix = System.getProperty("basedir",".");
        File targetDir = new File(prefix,"target");
        customDir = new File(targetDir,"fileContentItem");
    }
    
    @Override
    protected ContentItemFactory createContentItemFactory() throws IOException {
        if(factory == null){
            factory = new FileContentItemFactory(customDir);
        }
        return factory;
    }

    /**
     * Tests that the specified directory is actually used!
     */
    @Test
    public void testCustomDir() throws IOException {
        assertTrue("The custom dir '"+customDir+"'MUST exist",
            customDir.exists());
        assertTrue("The custom dir '"+customDir+"'MUST be an directory",
            customDir.isDirectory());
        int numFiles = customDir.list().length;
        Blob blob = contentItemFactory.createBlob(new StringSource("ensure a file exist"));
        assertNotNull(blob);
        Assert.assertEquals("Creating a new Blob has not increased the " +
        		"number of files by one!",numFiles, customDir.list().length-1);
    }
}
