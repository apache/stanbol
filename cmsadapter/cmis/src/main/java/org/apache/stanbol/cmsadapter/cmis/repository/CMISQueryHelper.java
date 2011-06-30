package org.apache.stanbol.cmsadapter.cmis.repository;

public class CMISQueryHelper {

    public static final String CMIS_FOLDER_BY_NAME = "SELECT cmis:id FROM cmis:folder WHERE cmis:name = '%s'";
    public static final String CMIS_DOCUMENT_BY_NAME = "SELECT cmis:id FROM cmis:document WHERE cmis:name = '%s'";

    public static String[] getCMISIdByNameQuery(String name) {
        return new String[] {String.format(CMIS_FOLDER_BY_NAME, name),
                             String.format(CMIS_DOCUMENT_BY_NAME, name)};
    }
}
