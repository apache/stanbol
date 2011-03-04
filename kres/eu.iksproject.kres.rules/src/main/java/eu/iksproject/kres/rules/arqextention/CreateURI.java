package eu.iksproject.kres.rules.arqextention;

import eu.iksproject.kres.ontologies.XML_OWL;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.Binding0;
import com.hp.hpl.jena.sparql.engine.binding.Binding1;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArgType;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionEval;
import com.hp.hpl.jena.sparql.util.IterLib;

public class CreateURI extends PropertyFunctionEval {

	
	public CreateURI() {
		super(PropFuncArgType.PF_ARG_SINGLE, PropFuncArgType.PF_ARG_SINGLE);
	}

	@Override
	public QueryIterator execEvaluated(Binding binding, PropFuncArg argumentSubject,
			Node predicate, PropFuncArg argumentObject, ExecutionContext execCxt) {
		
		Binding b =  null;
		if(argumentObject.getArg().isLiteral()){
			Node ref = argumentSubject.getArg();
			if(ref.isVariable()){
				String argumentString = argumentObject.getArg().toString().replace("\"", "");
				
				b =  new Binding1(binding, Var.alloc(ref), Node.createURI(argumentString));
			}
		}
		
		if(b == null){
			b = binding;
		}
		
		return IterLib.result(b, execCxt);
	}
	
	
	 
}
