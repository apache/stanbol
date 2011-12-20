package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.URIResource;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.vocabulary.XSD;

public abstract class AbstractRuleAtom implements RuleAtom {

    protected Node getTypedLiteral(Object argument) {

        Node literal;
        if (argument instanceof TypedLiteralAtom) {
            TypedLiteralAtom typedLiteralAtom = (TypedLiteralAtom) argument;

            URIResource xsdType = typedLiteralAtom.getXsdType();

            if (xsdType.getURI().equals(XSD.xboolean)) {
                literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDboolean);
            } else if (xsdType.getURI().equals(XSD.xdouble)) {
                literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDdouble);
            } else if (xsdType.getURI().equals(XSD.xfloat)) {
                literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDfloat);
            } else if (xsdType.getURI().equals(XSD.xint)) {
                literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDint);
            } else {
                literal = Node_RuleVariable.createLiteral(argument.toString());
            }
        } else if (argument instanceof String) {
            literal = Node_RuleVariable.createLiteral((String) argument, null, XSDDatatype.XSDstring);
        } else if (argument instanceof Integer) {
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDinteger);
        } else if (argument instanceof Double) {
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDdouble);
        } else if (argument instanceof Float) {
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDfloat);
        } else if (argument instanceof Boolean) {
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDboolean);
        } else {
            literal = Node_RuleVariable.createLiteral((String) argument);
        }

        return literal;
    }

}
