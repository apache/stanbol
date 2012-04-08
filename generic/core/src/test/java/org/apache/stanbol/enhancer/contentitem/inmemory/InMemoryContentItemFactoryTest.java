package org.apache.stanbol.enhancer.contentitem.inmemory;

import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.test.ContentItemFactoryTest;

public class InMemoryContentItemFactoryTest extends ContentItemFactoryTest {

    @Override
    protected ContentItemFactory createContentItemFactory() {
        return InMemoryContentItemFactory.getInstance();
    }

}
