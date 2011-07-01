package org.apache.stanbol.factstore.model;

public enum CompareOperator {

    EQ("=");
    
    private String literal;
    
    private CompareOperator(String name) {
        this.literal = name;
    }

    public String getLiteral() {
        return literal;
    }
    
}
