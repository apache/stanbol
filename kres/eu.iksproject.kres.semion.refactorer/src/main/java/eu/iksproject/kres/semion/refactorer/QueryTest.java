package eu.iksproject.kres.semion.refactorer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

import eu.iksproject.kres.api.rules.KReSRule;
import eu.iksproject.kres.api.rules.util.KReSRuleList;
import eu.iksproject.kres.rules.KReSKB;
import eu.iksproject.kres.rules.parser.KReSRuleParser;

public class QueryTest {

	
	public void query(String ruleText){
		System.out.println("PROVA");
		Model model = FileManager.get().loadModel("/Users/mac/weather.owl");
		KReSKB kReSKB = KReSRuleParser.parse(ruleText);
		KReSRuleList kReSRuleList = kReSKB.getkReSRuleList();
		
		System.out.println("RULE LIST SIZE : "+kReSRuleList.size());
		System.out.println("PROVA");
		
		for(KReSRule kReSRule : kReSRuleList){
			String sparql = kReSRule.toSPARQL();
			System.out.println("SPARQL : "+sparql);
			Query sparqlQuery = QueryFactory.create(sparql);
			QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, model) ;
			Model refactoredModel = qexec.execConstruct();
		
			refactoredModel.write(System.out);
			
		}
	}
	
	public void wrapPoints(String ruleText){
		
		String[] points = ruleText.split(" . ");
		
		ruleText = "";
		for(String point : points){
			ruleText += point + " . " + System.getProperty("line.separator");
		}
		System.out.println(ruleText);
	}
	
	public static void main(String[] args){
		/*String ruleText = "oxml = <http://ontologydesignpatterns.org/ont/iks/oxml.owl#> . schema = <http://kres.iks-project.eu/weather/san_francisco/schema#> . ami_context = <http://iks/ami-case/context#> . ami_content = <http://iks/ami-case/content#> . composite = <http://www.topbraid.org/2007/05/composite.owl#> . " +
				"weatherRule[has(oxml:hasElementDeclaration, ?x, schema:weather) . " +
				"has(composite:child, ?x, ?y) . " +
				"has(oxml:hasElementDeclaration, ?y, schema:weather-conditions) . " +
				"has(oxml:hasXMLAttribute,  ?y,  ?cond) . " +
				"values(oxml:nodeValue, ?cond, ?condition) . " +
				"has(oxml:hasXMLAttribute,  ?x,  ?z) . " +
				"has(oxml:hasAttributeDeclaration, ?z, schema:weather_time-layout) . " +
				"has(oxml:nodeValue, ?z, ?time) . " +
				"has(composite:child, ?time, ?timeStartEl) . " +
				"has(composite:child, ?time, ?timeEndEl) . " +
				"has(oxml:hasElementDeclaration, ?timeStartEl, schema:start-valid-time) . " +
				"has(oxml:hasElementDeclaration, ?timeEndEl, schema:end-valid-time) . " +
				"values(oxml:nodeValue, ?timeStartEl, ?startTime) . " +
				"values(oxml:nodeValue, ?timeEndEl, ?endTime) . " +
				"-> " +
				"is(ami_content:Weather, ?x) . " +
				"is(ami_content:WeatherCondition, ?y) . " +
				"has(ami_content:condition, ?x, ?y) . " +
				"values(ami_context:description, ?y, ?condition) . " +
				"has(ami_content:startTime, ?x, ?startTime) . " +
				"has(ami_content:endTime, ?x, ?endTime)]";
				*/
		
		String ruleText = "oxml = <http://ontologydesignpatterns.org/ont/iks/oxml.owl#> . schema = <http://kres.iks-project.eu/weather/san_francisco/schema#> . ami_context = <http://iks/ami-case/context#> . ami_content = <http://iks/ami-case/content#> . composite = <http://www.topbraid.org/2007/05/composite.owl#> . locationRule[is(oxml:XMLElement, ?x) . has(oxml:hasElementDeclaration, ?x, schema:location) . has(composite:child, ?x, ?y) . has(oxml:hasElementDeclaration, ?y, schema:location-key) . values(oxml:nodeValue, ?y, ?z) -> is(ami_context:Location, ?x) . values(ami_context:description, ?x, ?z)] . weatherRule[has(oxml:hasElementDeclaration, ?x, schema:weather) . has(composite:child, ?x, ?y) . has(oxml:hasElementDeclaration, ?y, schema:weather-conditions) . has(oxml:hasXMLAttribute,  ?y,  ?cond) . values(oxml:nodeValue, ?cond, ?condition) . has(oxml:hasXMLAttribute,  ?x,  ?z) . has(oxml:hasAttributeDeclaration, ?z, schema:weather_time-layout) . has(oxml:nodeValue, ?z, ?time) . has(composite:child, ?time, ?timeStartEl) . has(composite:child, ?time, ?timeEndEl) . has(oxml:hasElementDeclaration, ?timeStartEl, schema:start-valid-time) . has(oxml:hasElementDeclaration, ?timeEndEl, schema:end-valid-time) . values(oxml:nodeValue, ?timeStartEl, ?startTime) . values(oxml:nodeValue, ?timeEndEl, ?endTime) . -> is(ami_content:Weather, ?x) . is(ami_content:WeatherCondition, ?y) . has(ami_content:condition, ?x, ?y) . values(ami_context:description, ?y, ?condition) . has(ami_content:startTime, ?x, ?startTime) . has(ami_content:endTime, ?x, ?endTime)] . locationWeatherRule[has(oxml:hasElementDeclaration, ?x, schema:data) . has(composite:child, ?x, ?location) . has(composite:child, ?x, ?parameter) . has(oxml:hasElementDeclaration, ?location, schema:location) . has(oxml:hasElementDeclaration, ?parameter, schema:parameters) . has(composite:child, ?parameter, ?weather) . has(oxml:hasElementDeclaration, ?weather, schema:weather) -> has(ami_content:place, ?weather, ?location) ]";
		
		System.out.println(ruleText);
		QueryTest queryTest = new QueryTest();
		queryTest.wrapPoints(ruleText);
	}
	
	
	
}
