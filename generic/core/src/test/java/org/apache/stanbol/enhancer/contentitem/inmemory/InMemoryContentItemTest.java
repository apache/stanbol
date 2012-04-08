package org.apache.stanbol.enhancer.contentitem.inmemory;

import java.io.IOException;

import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.test.ContentItemTest;

public class InMemoryContentItemTest extends ContentItemTest {

    private static final ContentItemFactory factory  = InMemoryContentItemFactory.getInstance();

    @Override
    protected ContentItem createContentItem(ContentSource source) throws IOException {
        return factory.createContentItem(source);
    }

    @Override
    protected Blob createBlob(ContentSource source) throws IOException {
        return factory.createBlob(source);
    }

}
