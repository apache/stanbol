package org.apache.stanbol.cmsadapter.jcr.repository;

public class JCRQueryRepresentation {

    private String queryString;
    private String queryType;

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public JCRQueryRepresentation(String queryString, String queryType) {
        super();
        this.queryString = queryString;
        this.queryType = queryType;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

}
