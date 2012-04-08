package org.apache.stanbol.enhancer.contentitem.file;

import java.io.IOException;

import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.test.BlobTest;

public class FileBlobTest extends BlobTest {

    private ContentItemFactory factory = FileContentItemFactory.getInstance();

    @Override
    protected Blob createBlob(ContentSource cs) throws IOException {
        return factory.createBlob(cs);
    }

}
