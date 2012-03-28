package org.apache.stanbol.rules.adapters.swrl.atoms;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;

/**
 * It adapts any MultiplicationAtom to the op:numeric-divide XPath function call in SWRL.
 * 
 * @author anuzzolese
 * 
 */
public class DivisionAtom extends AbstractAdaptableAtom {

    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption {
        throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
    }

}
