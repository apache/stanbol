package org.apache.stanbol.factstore.model;

public class WhereClause {

    private CompareOperator compareOperator;
    private String comparedRole;
    private String searchedValue;

    public CompareOperator getCompareOperator() {
        return compareOperator;
    }

    public void setCompareOperator(CompareOperator compareOperator) {
        this.compareOperator = compareOperator;
    }

    public String getComparedRole() {
        return comparedRole;
    }

    public void setComparedRole(String comparedValue) {
        this.comparedRole = comparedValue;
    }

    public String getSearchedValue() {
        return searchedValue;
    }

    public void setSearchedValue(String searchedValue) {
        this.searchedValue = searchedValue;
    }

}
