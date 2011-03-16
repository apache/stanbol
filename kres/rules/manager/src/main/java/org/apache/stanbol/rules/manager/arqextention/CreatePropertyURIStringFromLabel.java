package org.apache.stanbol.rules.manager.arqextention;

import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase2;

public class CreatePropertyURIStringFromLabel  extends FunctionBase2 {

	@Override
	public NodeValue exec(NodeValue namespace, NodeValue label) {
		String argument1 = namespace.getString();
		String argument2 = label.getString();
		
		String[] argument2Splitted = argument2.split(" ");
		
		String localName = argument2Splitted[0].substring(0, 1).toLowerCase() + argument2Splitted[0].substring(1, argument2Splitted[0].length());
		
		
		for(int i=1; i<argument2Splitted.length; i++){
			localName += argument2Splitted[i].substring(0, 1).toUpperCase() + argument2Splitted[i].substring(1, argument2Splitted[i].length());
		}
	
		
		String newString = argument1 + localName;
		
		return NodeValue.makeString(newString);
	}

	
}
