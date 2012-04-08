package org.apache.stanbol.enhancer.contentitem.file;

import java.io.IOException;

import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.test.ContentItemTest;

public class FileContentItemTest extends ContentItemTest {

    private ContentItemFactory factory = FileContentItemFactory.getInstance();
   
    @Override
    protected ContentItem createContentItem(ContentSource source) throws IOException {
        return factory.createContentItem(source);
    }

    @Override
    protected Blob createBlob(ContentSource source) throws IOException {
        return factory.createBlob(source);
    }

}
