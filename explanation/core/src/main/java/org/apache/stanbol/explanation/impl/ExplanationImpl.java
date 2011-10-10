package org.apache.stanbol.explanation.impl;

import java.util.Collection;

import org.apache.stanbol.explanation.api.Explainable;
import org.apache.stanbol.explanation.api.Explanation;
import org.apache.stanbol.explanation.api.ExplanationTypes;

public class ExplanationImpl implements Explanation {

    Explainable<?> object;
    
    ExplanationTypes type;
    
    public ExplanationImpl(Explainable<?> explainable, ExplanationTypes type) {
        this.object = explainable;
        this.type = type;
    }
    
    @Override
    public Explainable<?> getObject() {
        return object;
    }

    @Override
    public Collection<?> getGrounding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExplanationTypes getType() {
        return type;
    }

}
