package org.apache.stanbol.explanation.impl;

import org.apache.stanbol.explanation.heuristics.IDTypes;
import org.apache.stanbol.explanation.heuristics.Identifier;

public class AbstractIdentifier implements Identifier {

    private IDTypes type;
    
    private String mnemonic;
    
    public AbstractIdentifier(IDTypes type, String mnemonic) {
        this.type = type;
        this.mnemonic = mnemonic;
    }
    
    @Override
    public String getMnemonic() {
        return mnemonic;
    }

    @Override
    public IDTypes getType() {
        return type;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this==obj) return true;
        if (!(obj instanceof Identifier)) return false;
        return getType().equals(((Identifier)obj).getType()) && getMnemonic().equals(((Identifier)obj).getMnemonic());
    }

}
