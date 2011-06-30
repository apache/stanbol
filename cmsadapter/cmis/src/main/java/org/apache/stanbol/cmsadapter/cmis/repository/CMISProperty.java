package org.apache.stanbol.cmsadapter.cmis.repository;

public enum CMISProperty {

    ID("cmis:id");

    private final String name;

    CMISProperty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
