package org.apache.stanbol.cmsadapter.cmis.repository;

//Used for properties like cmis:localname, cmis:localnamespace
public enum CMISProperty {

//    LOCAL_NAME("cmis:localname"),
//    LOCAL_NAMESPACE("cmis:localnamespace"),
    PATH("cmis:path"),
    ID("cmis:id");

    private final String name;

    CMISProperty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
