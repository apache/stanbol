package org.apache.stanbol.enhancer.contentitem.file;

import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.test.ContentItemFactoryTest;

public class DefaultFileContentItemFactoryTest extends ContentItemFactoryTest {

    @Override
    protected ContentItemFactory createContentItemFactory() {
        return FileContentItemFactory.getInstance();
    }

}
