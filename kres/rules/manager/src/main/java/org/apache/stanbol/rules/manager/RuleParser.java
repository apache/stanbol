package org.apache.stanbol.rules.manager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.ontology.EnumeratedClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.CollectionFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import org.apache.stanbol.rules.base.SWRL;

public class RuleParser {

	public static String ruleNS = "http://www.prova.org/rules.rdf#";
	
	
	private Model sourceModel;
	private Model destinationModel;
	
	private OntModel ruleOntology;
	
	public RuleParser(Model sourceModel, Model destinationModel, Model ruleOntology) {
		this.sourceModel = sourceModel;
		this.destinationModel = destinationModel;
		this.ruleOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, ruleOntology);
	}
	
	
	public RuleParser(OntModel inputOntology, Map<String, String>prefixMap, Model ruleOntology) {
		this.sourceModel = inputOntology;
		this.ruleOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, ruleOntology);
		Set<String> keys = prefixMap.keySet();
		Iterator<String> it = keys.iterator();
		while(it.hasNext()){
			String key = it.next();
			String ns = prefixMap.get(key);
			ruleOntology.setNsPrefix(key, ns);
		}
	}
	
	public RuleParser(OntModel inputOntology, Map<String, String>prefixMap) {
		this.sourceModel = inputOntology;
		this.ruleOntology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		ruleOntology.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#"); 
		ruleOntology.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"); 
		ruleOntology.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#"); 
		ruleOntology.setNsPrefix("swrl", "http://www.w3.org/2003/11/swrl#");
		ruleOntology.setNsPrefix("rule", ruleNS);
		
		Set<String> keys = prefixMap.keySet();
		Iterator<String> it = keys.iterator();
		while(it.hasNext()){
			String key = it.next();
			String ns = prefixMap.get(key);
			ruleOntology.setNsPrefix(key, ns);
		}
	}
	
	public Resource parse(String ruleName, String ruleString){
		
		Resource imp = null;
		
		System.out.println("INPUT");
		//inputOntology.write(System.out);
		
		String[] ruleSplit = ruleString.split("->");
		if(ruleSplit.length == 2){
			
			imp = ruleOntology.createResource(ruleNS+ruleName, SWRL.Imp);
			
			//rule divided into body and head
			String bodyString = ruleSplit[0];
			String headString = ruleSplit[1];
			System.out.println("BODY : "+bodyString);
			System.out.println("HEAD : "+headString);
			
			//RDFList list = createList(bodyString);
			
			imp.addProperty(SWRL.body, createList(bodyString));
			
			imp.addProperty(SWRL.head, createList(headString));
			
		}
		else{
			ruleSplit = ruleString.split("<-");
			
			if(ruleSplit.length == 2){
				
				imp = ruleOntology.createResource(ruleNS+ruleName, SWRL.Imp);
				
				//rule divided into body and head
				
				String headString = ruleSplit[0];				
				String bodyString = ruleSplit[1];
				

				RDFList list = createList(bodyString);
				if(list != null){
					imp.addProperty(SWRL.head, list.getHead());
				}

				list = createList(headString);
				if(list != null){
					imp.addProperty(SWRL.body, list);
				}
				
			}
		}
		
		return imp;
		
	}
	
	
	
	public OntModel getRuleOntology() {
		return ruleOntology;
	}
	
	private RDFNode getSWRLArgument(String argument){
		RDFNode rdfNode = null;
		String[] argumentComposition = argument.split(":");
		if(argumentComposition.length == 2){
			String prefix = argumentComposition[0];
			String resourceName = argumentComposition[1];
			
			String namespaceURI = ruleOntology.getNsPrefixURI(prefix);
			
			rdfNode = sourceModel.getResource(namespaceURI+resourceName);
			if(rdfNode == null){
				rdfNode = destinationModel.getResource(namespaceURI+resourceName);
			}
			
		}
		else if(argument.startsWith("\"") && argument.endsWith("\"")){
			Model tmpModel = ModelFactory.createDefaultModel();
			rdfNode = tmpModel.createLiteral(argument);
		}
		return rdfNode;
	}
	
	private Resource getSWRLVariable(String argument){
		Resource variableResource = null;
		String variableString = argument.substring(1);
		StmtIterator stmtIterator = ruleOntology.listStatements(null, RDF.type, SWRL.Variable);
		boolean found = false;
		
		String variable = ruleNS+variableString;
		while(stmtIterator.hasNext() && !found){
			Statement statement = stmtIterator.next();
			variableResource = statement.getSubject();
			if(variableResource.getURI().equals(variable)){
				found = true;
			}
			
		}
		if(!found){
			variableResource = ruleOntology.createResource(variable, SWRL.Variable);
			System.out.println("LA CREO");
		}
		
		
		return variableResource;
	}
	
	private Resource createSameAsAtom(String token, int openPar, int closePar){
		
		Resource sameASAtom = ruleOntology.createResource(SWRL.SameIndividualAtom);
		String argumentString = token.substring(openPar+1, closePar);
		System.out.println("ARGUMENT STRING : "+argumentString);
		
		String[] arguments = argumentString.split(",");
		if(arguments.length == 2){
			
			for(int j=0; j<arguments.length; j++){
				String argument = arguments[j];
				argument = argument.replaceAll(" ", "");
				System.out.println("ARGUMENT : "+argument);
				RDFNode argRes;
				if(argument.startsWith("?")){
					argRes = getSWRLVariable(argument);
					
				}
				else{
					argRes = getSWRLArgument(argument);
				}
				
				switch(j){
				case 0:
					sameASAtom.addProperty(SWRL.argument1, argRes);
					break;
				case 1:
					sameASAtom.addProperty(SWRL.argument2, argRes);
					break;
				}
			}
		}
		
		return sameASAtom;
		
	}
	
	
	private Resource createDifferentAtom(String token, int openPar, int closePar){
		
		Resource differentIndividualAtom = ruleOntology.createResource(SWRL.DifferentIndividualsAtom);
		String argumentString = token.substring(openPar+1, closePar);
		System.out.println("ARGUMENT STRING : "+argumentString);
		
		String[] arguments = argumentString.split(",");
		if(arguments.length == 2){
			
			for(int j=0; j<arguments.length; j++){
				String argument = arguments[j];
				argument = argument.replaceAll(" ", "");
				System.out.println("ARGUMENT : "+argument);
				RDFNode argRes;
				if(argument.startsWith("?")){
					argRes = getSWRLVariable(argument);
					
				}
				else{
					argRes = getSWRLArgument(argument);
				}
				
				switch(j){
				case 0:
					differentIndividualAtom.addProperty(SWRL.argument1, argRes);
					break;
				case 1:
					differentIndividualAtom.addProperty(SWRL.argument2, argRes);
					break;
				}
			}
		}
		
		return differentIndividualAtom;
		
	}
	
	private Resource createClassAtom(OntResource ontResource, String token, int openPar, int closePar){
		
		Resource classAtom = ruleOntology.createResource(SWRL.ClassAtom);
		String argumentString = token.substring(openPar+1, closePar);
		System.out.println("ARGUMENT STRING : "+argumentString);
		
		classAtom.addProperty(SWRL.classPredicate, ontResource);
		
		String[] arguments = argumentString.split(",");
		if(arguments.length == 1){
			
			
			String argument = arguments[0];
			argument = argument.replaceAll(" ", "");
			System.out.println("ARGUMENT : "+argument);
			RDFNode argRes;
			if(argument.startsWith("?")){
				argRes = getSWRLVariable(argument);
				
			}
			else{
				argRes = getSWRLArgument(argument);
			}
			
			classAtom.addProperty(SWRL.argument1, argRes);
		}
		
		return classAtom;
		
	}
	
	private Resource createIndividualAtom(OntResource ontResource, String token, int openPar, int closePar){
		
		
		
		Resource individualPropertyAtom = ruleOntology.createResource(SWRL.IndividualPropertyAtom);
		String argumentString = token.substring(openPar+1, closePar);
		System.out.println("ARGUMENT STRING INDIVIDUAL PROPERTY ATOM: "+argumentString);
		
		individualPropertyAtom.addProperty(SWRL.propertyPredicate, ontResource);
		
		String[] arguments = argumentString.split(",");
		if(arguments.length == 2){
			
			for(int j=0; j<arguments.length; j++){
				String argument = arguments[j];
				argument = argument.replaceAll(" ", "");
				System.out.println("ARGUMENT : "+argument);
				RDFNode argRes;
				if(argument.startsWith("?")){
					argRes = getSWRLVariable(argument);
					
				}
				else{
					argRes = getSWRLArgument(argument);
				}
				
				switch(j){
				case 0:
					individualPropertyAtom.addProperty(SWRL.argument1, argRes);
					break;
				case 1:
					individualPropertyAtom.addProperty(SWRL.argument2, argRes);
					break;
				}
			}
		}
		
		return individualPropertyAtom;
		
	}
	
	private Resource createDatavaluedPropertyAtom(OntResource ontResource, String token, int openPar, int closePar){
		
		Resource databaluedPropertyAtom = ruleOntology.createResource(SWRL.DatavaluedPropertyAtom);
		String argumentString = token.substring(openPar+1, closePar);
		System.out.println("ARGUMENT STRING : "+argumentString);
		
		databaluedPropertyAtom.addProperty(SWRL.propertyPredicate, ontResource);
		
		String[] arguments = argumentString.split(",");
		if(arguments.length == 2){
			
			for(int j=0; j<arguments.length; j++){
				String argument = arguments[j];
				argument = argument.replaceAll(" ", "");
				System.out.println("ARGUMENT : "+argument);
				RDFNode argRes;
				if(argument.startsWith("?")){
					argRes = getSWRLVariable(argument);
					
				}
				else{
					argRes = getSWRLArgument(argument);
				}
				
				switch(j){
				case 0:
					databaluedPropertyAtom.addProperty(SWRL.argument1, argRes);
					break;
				case 1:
					databaluedPropertyAtom.addProperty(SWRL.argument2, argRes);
					break;
				}
			}
		}
		
		return databaluedPropertyAtom;
		
	}
	
	private Resource createDataRangeAtom(OntResource ontResource, String token, int openPar, int closePar){
		
		Resource dataRangeAtom = ruleOntology.createResource(SWRL.DataRangeAtom);
		String argumentString = token.substring(openPar+1, closePar);
		System.out.println("ARGUMENT STRING : "+argumentString);
		
		dataRangeAtom.addProperty(SWRL.propertyPredicate, ontResource);
		
		String[] arguments = argumentString.split(",");
		if(arguments.length == 1){
			
		
			String argument = arguments[0];
			argument = argument.replaceAll(" ", "");
			System.out.println("ARGUMENT : "+argument);
			RDFNode argRes;
			if(argument.startsWith("?")){
				argRes = getSWRLVariable(argument);
				
			}
			else{
				argRes = getSWRLArgument(argument);
			}
			
			
			dataRangeAtom.addProperty(SWRL.argument1, argRes);
			
		}
		
		return dataRangeAtom;
		
	}
	
	private RDFList createList(String atomString){
		RDFList list = ruleOntology.createList();
		
		String[] tokenArray = atomString.split(" AND ");
		for(int i=0; i<tokenArray.length; i++){
			if(!tokenArray.equals("")){
				String token = tokenArray[i];
				while(token.startsWith(" ")){
					token = token.substring(1);
				}
				int openPar = token.indexOf("(");
				int closePar = token.indexOf(")");
				
				String atom = token.substring(0, openPar);
				System.out.println("ATOM : "+atom);
				
				Resource atomResource = null;
				
				
				
				if(atom.equals("same")){						
					atomResource = createSameAsAtom(token, openPar, closePar);						
				}
				else if(atom.equals("different")){
					atomResource = createDifferentAtom(token, openPar, closePar);
				}
				else{
					String[] atomComponents = atom.split(":");
					if(atomComponents.length == 2){
						String atomNSPrefix = atomComponents[0];
						String atomName = atomComponents[1];

						OntResource ontResource = null;
						System.out.println("atomNSPrefix : "+atomNSPrefix);
						//ruleOntology.write(System.out);
						String namespaceURI = ruleOntology.getNsPrefixURI(atomNSPrefix);
						
						System.out.println("SEMION RULE PARSER : ontology "+namespaceURI.replace("#", ""));
						
						OntModel ontModelExternal = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
						ontModelExternal.read(namespaceURI.replace("#", ""));
						
						ontResource = ontModelExternal.getOntResource(namespaceURI+atomName);
						
						System.out.println("NAME ATOOOOM: "+namespaceURI+atomName);
						
						if(ontResource != null){
							System.out.println("QUIIIIIIIIIIIIIIII");
							if(ontResource.isClass()){
								atomResource = createClassAtom(ontResource, token, openPar, closePar);
								System.out.println(ontResource.getURI()+ " CLASS ");
							}
							else if(ontResource.isObjectProperty()){
								atomResource = createIndividualAtom(ontResource, token, openPar, closePar);
								System.out.println(ontResource.getURI()+ " OBJECT PROPERTY");
							}
							else if(ontResource.isDatatypeProperty()){
								atomResource = createDatavaluedPropertyAtom(ontResource, token, openPar, closePar);
								System.out.println(ontResource.getURI()+ " OBJECT DATATYPE PROPERTY");
							}
							else if(ontResource.isDataRange()){
								atomResource = createDataRangeAtom(ontResource, token, openPar, closePar);
								System.out.println(ontResource.getURI()+ " DATA RANGE");
							}
							else{
								if(ontResource.isProperty()){
									atomResource = createIndividualAtom(ontResource, token, openPar, closePar);
									System.out.println(ontResource.getURI()+ " PROPERTY");
								}
								else{
									System.out.println(ontResource.getURI()+ " nil");
								}
							}
							
						}
						else{
							System.out.println("SONO UNA MINCHIA");
						}
						
						
					}
				}
				
				if(atomResource != null){
					
					list = list.cons(atomResource);
					System.out.println("ENTRO QUI "+list.size());
					
				}
				
			}
		}
		
		return list;
	}
}
