package eu.iksproject.kres.rules.arqextention;

import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase1;

public class CreateStandardLabel extends FunctionBase1 {

	public CreateStandardLabel() {
		super();
	}
	
	@Override
	public NodeValue exec(NodeValue nodeValue) {
		String value = nodeValue.getString();
		
		String[] split = value.split("(?=\\p{Upper})");
		
		int i = 0;
		
		if(split[i].isEmpty()){
			i += 1;
		}
		
		String newString = split[i].substring(0, 1).toUpperCase() + split[i].substring(1, split[i].length());
		
		if(split.length > 1){
			for(i+=1; i<split.length; i++){
				newString += " "+split[i].substring(0, 1).toLowerCase() + split[i].substring(1, split[i].length());
			}
		}
		
		return NodeValue.makeString(newString);
	}
	

}
