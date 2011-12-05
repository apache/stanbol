package org.apache.stanbol.entityhub.ldpath.impl;

import org.apache.stanbol.commons.solr.managed.standalone.ClassPathDataFileProvider;

public class DbPediaDatadefaultDataFileProvider extends ClassPathDataFileProvider {

    private static final String DATA_FILES_DIR = "org/apache/stanbol/data/site/dbpedia/default/index/";
    
    public DbPediaDatadefaultDataFileProvider() {
        super(null,DATA_FILES_DIR);
    }
}
