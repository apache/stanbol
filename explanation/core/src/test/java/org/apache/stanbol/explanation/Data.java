package org.apache.stanbol.explanation;

import org.apache.stanbol.explanation.heuristics.Entity;

public class Data {

    public static String _NS_ODP_REGISTRY = "http://www.ontologydesignpatterns.org/registry/";

    public static String _NS_TESTDATA = "http://www.iks-project.eu/ontologies/explanation_testdata.owl#";

    public static Entity dilbert, pointyHairedBoss, wally;

    public static String URI_BALABAM = _NS_TESTDATA + "Ducaconte_Balabam";

    public static String URI_FANTOZZI = _NS_TESTDATA + "Ugo_Fantozzi";

    public static String URI_FILINI = _NS_TESTDATA + "Renzo_Silvio_Filini";

    public static String URI_LIB_EXPLANATION_SCHEMA_MAPPINGS = _NS_ODP_REGISTRY
                                                               + "explanation-mappings.owl#ExplanationSchemaMappingCatalog";

    public static String URI_LIB_EXPLANATION_SCHEMAS = _NS_ODP_REGISTRY
                                                       + "explanation.owl#ExplanationSchemaCatalog";

    public static String URI_POINTYHAIRED = _NS_TESTDATA + "Pointy-haired_Boss";

    public static String URI_RICCARDELLI = _NS_TESTDATA + "Guidobaldo_Maria_Riccardelli";

}
