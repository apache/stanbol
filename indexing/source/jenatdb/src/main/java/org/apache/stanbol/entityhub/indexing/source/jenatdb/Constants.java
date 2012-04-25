package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;

public final class Constants {
    
    private Constants(){}

    
    /**
     * The default name of the folder used to initialise the 
     * {@link DatasetGraphTDB Jena TDB dataset}.
     */
    public static final String DEFAULT_MODEL_DIRECTORY = "tdb";
    /**
     * Parameter used to configure the name of the directory used to store the
     * RDF model (a Jena TDB dataset). The default name is
     * {@link #DEFAULT_MODEL_DIRECTORY}
     */
    public static final String PARAM_MODEL_DIRECTORY = "model";

}
