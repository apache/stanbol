package eu.iksproject.rick.model.clerezza;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.NoConvertorException;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.rick.core.utils.AdaptingIterator;
import eu.iksproject.rick.core.utils.ModelUtils;
import eu.iksproject.rick.core.utils.TypeSaveIterator;
import eu.iksproject.rick.model.clerezza.impl.Literal2TextAdapter;
import eu.iksproject.rick.model.clerezza.impl.LiteralAdapter;
import eu.iksproject.rick.model.clerezza.impl.NaturalLanguageLiteralIterator;
import eu.iksproject.rick.model.clerezza.impl.ReferenceIterator;
import eu.iksproject.rick.model.clerezza.impl.Resource2ValueAdapter;
import eu.iksproject.rick.model.clerezza.impl.UriRefAdapter;
import eu.iksproject.rick.model.clerezza.utils.Resource2StringAdapter;
import eu.iksproject.rick.servicesapi.model.Reference;
import eu.iksproject.rick.servicesapi.model.Representation;
import eu.iksproject.rick.servicesapi.model.Text;
import eu.iksproject.rick.servicesapi.model.UnsupportedTypeException;
import eu.iksproject.rick.servicesapi.model.rdf.RdfResourceEnum;

public class RdfRepresentation implements Representation{

    private static final Logger log = LoggerFactory.getLogger(RdfRepresentation.class);

    private static final UriRef REPRESENTATION_TYPE_PROPERTY = new UriRef(RdfResourceEnum.signType.getUri());

    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    private final GraphNode graphNode;

    protected final GraphNode getGraphNode() {
        return graphNode;
    }

    protected RdfRepresentation(UriRef resource, TripleCollection graph) {
        this.graphNode = new GraphNode(resource, graph);
    }

    /**
     * Getter for the read only view onto the RDF data of this representation.
     *
     * @return The RDF graph of this Representation
     */
    public TripleCollection getRdfGraph(){
        return graphNode.getGraph();
    }

    protected UriRef getRepresentationType(){
        Iterator<UriRef> it = this.graphNode.getUriRefObjects(REPRESENTATION_TYPE_PROPERTY);
        return it.hasNext()?it.next():null;
    }
    @Override
    public void add(String field, Object value) {
        if(field == null) {
            throw new IllegalArgumentException("Parameter \"String field\" MUST NOT be NULL!");
        }
        if(value == null){
            log.warn("NULL parsed as value in add method for symbol "+getId()
                    +" and field "+field+" -> call ignored");
            return;
        }
        UriRef fieldUriRef = new UriRef(field);
        Collection<Object> values = new ArrayList<Object>();
        //process the parsed value with the Utility Method ->
        // this converts Objects as defined in the specification
        ModelUtils.checkValues(valueFactory, value, values);
        //We still need to implement support for specific types supported by this implementation
        for (Object current : values){
            if (current instanceof Resource){ //native support for Clerezza types!
                graphNode.addProperty(fieldUriRef, (Resource)current);
            } else if (current instanceof RdfReference){
                //treat RDF Implementations special to avoid creating new instances
                graphNode.addProperty(fieldUriRef, ((RdfReference) current).getUriRef());
            } else if (current instanceof Reference){
                graphNode.addProperty(fieldUriRef, new UriRef(((Reference) current).getReference()));
                addReference(field,((Reference)current).getReference());
            } else if (current instanceof RdfText){
                //treat RDF Implementations special to avoid creating new instances
                graphNode.addProperty(fieldUriRef,((RdfText) current).getLiteral());
            } else if (current instanceof Text){
                addNaturalText(fieldUriRef, ((Text)current).getText(), ((Text)current).getLanguage());
            } else { //else add an typed Literal!
                addTypedLiteral(fieldUriRef, current);
            }
        }
    }

    private void addTypedLiteral(UriRef field, Object literalValue){
        Literal literal;
        try {
            literal = RdfResourceUtils.createLiteral(literalValue);
        } catch (NoConvertorException e){
            log.info("No Converter for value type "+literalValue.getClass()
                    +" (parsed for field "+field+") use toString() to get String representation");
            literal = RdfResourceUtils.createLiteral(literalValue.toString(), null);
        }
        graphNode.addProperty(field, literal);
    }
    @Override
    public void addReference(String field, String reference) {
        if(field == null) {
            throw new IllegalArgumentException("Parameter \"String field\" MUST NOT be NULL!");
        }
        if(reference == null){
            log.warn("NULL parsed as value in add method for symbol "+getId()
                    +" and field "+field+" -> call ignored");
        }
        graphNode.addProperty(new UriRef(field), new UriRef(reference));
    }
    @Override
    public void addNaturalText(String field, String text, String...languages) {
        if(field == null) {
            throw new IllegalArgumentException("Parameter \"String field\" MUST NOT be NULL!");
        }
        if(text == null){
            log.warn("NULL parsed as value in add method for symbol "+getId()
                    +" and field "+field+" -> call ignored");
        }
        this.addNaturalText(new UriRef(field), text, languages);
    }
    private void addNaturalText(UriRef field, String text, String...languages) {
        if(languages == null){
            log.debug("NULL parsed as languages -> replacing with \"new String []{null}\"" +
                    " -> assuming a missing explicit cast to (Stirng) in the var arg");
            languages = new String []{null};
        }
        for(String language : languages){
            graphNode.addProperty(field, RdfResourceUtils.createLiteral(text, language));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Iterator<T> get(String field, final Class<T> type) throws UnsupportedTypeException {
        UriRef fieldUriRef = new UriRef(field);
        if(Resource.class.isAssignableFrom(type)){ //native support for Clerezza types
            return new TypeSaveIterator<T>(graphNode.getObjects(fieldUriRef), type);
        } else if(type.equals(String.class)){ //support to convert anything to String
            return (Iterator<T>) new AdaptingIterator<Resource,String>(
                    graphNode.getObjects(fieldUriRef),
                    new Resource2StringAdapter<Resource>(),
                    String.class);
        } else if(type.equals(URI.class) || type.equals(URL.class)){ //support for References
            return new AdaptingIterator<UriRef, T>(
                    graphNode.getUriRefObjects(fieldUriRef),
                    new UriRefAdapter<T>(),
                    type);
        } else if(Reference.class.isAssignableFrom(type)){
            return (Iterator<T>) new ReferenceIterator(
                    graphNode.getUriRefObjects(fieldUriRef));
        } else if(Text.class.isAssignableFrom(type)){
            return (Iterator<T>)new AdaptingIterator<Literal, Text>(
                    graphNode.getLiterals(fieldUriRef),
                    new Literal2TextAdapter<Literal>(),
                    Text.class);
        } else { //support for Literals -> Type conversions
            return new AdaptingIterator<Literal, T>(
                    graphNode.getLiterals(fieldUriRef),
                    new LiteralAdapter<Literal, T>(),
                    type);
        }
    }

    @Override
    public Iterator<Reference> getReferences(String field) {
        Iterator<UriRef> it = graphNode.getUriRefObjects(new UriRef(field));
        return new ReferenceIterator(it);
    }

    @Override
    public Iterator<Text> getText(String field) {
        return new AdaptingIterator<Literal, Text>(
                graphNode.getLiterals(new UriRef(field)),
                new Literal2TextAdapter<Literal>(),
                Text.class);
    }

    @Override
    public Iterator<Object> get(String field) {
        return new AdaptingIterator<Resource, Object>(graphNode.getObjects(new UriRef(field)),
                new Resource2ValueAdapter<Resource>(),Object.class);
    }

    @Override
    public Iterator<Text> get(String field, String...languages) {
        if(languages == null){
            log.debug("NULL parsed as languages -> replacing with \"new String []{null}\"" +
                    " -> assuming a missing explicit cast to (String) in the var arg");
            languages = new String []{null};
        }
        return new AdaptingIterator<Literal, Text>(
                graphNode.getLiterals(new UriRef(field)),
                new Literal2TextAdapter<Literal>(languages),
                Text.class);
    }

    @Override
    public Iterator<String> getFieldNames() {
        return new AdaptingIterator<UriRef, String>(graphNode.getProperties(),
                new Resource2StringAdapter<UriRef>(), String.class);
    }

    @Override
    public <T> T getFirst(String field, Class<T> type) throws UnsupportedTypeException {
        Iterator<T> it = get(field,type);
        if(it.hasNext()){
            return it.next();
        } else {
            return null;
        }
    }

    @Override
    public Object getFirst(String field) {
        Iterator<Object> it = get(field);
        if(it.hasNext()){
            return it.next();
        } else {
            return null;
        }
    }
    @Override
    public Reference getFirstReference(String field) {
        Iterator<Reference> it = getReferences(field);
        return it.hasNext()?it.next():null;
    }
    @Override
    public Text getFirst(String field, String...languages) {
        if(languages == null){
            log.debug("NULL parsed as languages -> replacing with \"new String []{null}\"" +
                    " -> assuming a missing explicit cast to (String) in the var arg");
            languages = new String []{null};
        }
        Iterator<Text> it = get(field,languages);
        if(it.hasNext()){
            return it.next();
        } else {
            return null;
        }
    }
    @Override
    public String getId() {
        return ((UriRef)graphNode.getNode()).getUnicodeString();
    }

    @Override
    public void remove(String field, Object value) {
        if(field == null) {
            throw new IllegalArgumentException("Parameter \"String field\" MUST NOT be NULL!");
        }
        if(value == null){
            log.warn("NULL parsed as value in remove method for symbol "+getId()
                    +" and field "+field+" -> call ignored");
        }
        UriRef fieldUriRef = new UriRef(field);
        if(value instanceof Resource){ //native support for Clerezza types!
            graphNode.deleteProperty(fieldUriRef, (Resource)value);
        } else if(value instanceof URI || value instanceof URL){
            removeReference(field, value.toString());
        } else if (value instanceof String[]){
            if(((String[])value).length>0){
                if(((String[])value).length>1){
                    removeNaturalText(field, ((String[])value)[0],((String[])value)[1]);
                } else {
                    removeNaturalText(field, ((String[])value)[0],(String)null);
                }
            }
        } else {
            removeTypedLiteral(fieldUriRef, value);
        }

    }

    @Override
    public void removeReference(String field, String reference) {
        if(field == null) {
            throw new IllegalArgumentException("Parameter \"String field\" MUST NOT be NULL!");
        }
        if(reference == null){
            log.warn("NULL parsed as value in remove method for symbol "+getId()+" and field "+field+" -> call ignored");
        }
        graphNode.deleteProperty(new UriRef(field), new UriRef(reference));
    }
    protected void removeTypedLiteral(UriRef field, Object object){
        Literal literal;
        try{
            literal = RdfResourceUtils.createLiteral(object);
        } catch (NoConvertorException e){
            log.info("No Converter for value type "+object.getClass()
                    +" (parsed for field "+field+") use toString() Method to get String representation");
            literal = RdfResourceUtils.createLiteral(object.toString(), null);
        }
        graphNode.deleteProperty(field,literal);
    }
    @Override
    public void removeNaturalText(String field, String value, String... languages) {
        if(field == null) {
            throw new IllegalArgumentException("Parameter \"String field\" MUST NOT be NULL!");
        }
        if(value == null){
            log.warn("NULL parsed as value in remove method for symbol "+getId()+" and field "+field+" -> call ignored");
        }
        if(languages == null){
            log.debug("NULL parsed as languages -> replacing with \"new String []{null}\"" +
                    " -> assuming a missing explicit cast to (Stirng) in the var arg");
            languages = new String []{null};
        }
        UriRef fieldUriRef = new UriRef(field);
        for(String language : languages){
            graphNode.deleteProperty(fieldUriRef,RdfResourceUtils.createLiteral(value, language));
        }
    }
    @Override
    public void removeAll(String field) {
        graphNode.deleteProperties(new UriRef(field));
    }
    @Override
    public void removeAllNaturalText(String field, String... languages) {
        if(field == null) {
            throw new IllegalArgumentException("Parameter \"String field\" MUST NOT be NULL!");
        }
        if(languages == null){
            log.debug("NULL parsed as languages -> replacing with \"new String []{null}\"" +
                    " -> assuming a missing explicit cast to (Stirng) in the var arg");
            languages = new String []{null};
        }
        UriRef fieldUriRef = new UriRef(field);
        Collection<Literal> literals = new ArrayList<Literal>();
        //get all the affected Literals
        for (Iterator<Literal> it =  new NaturalLanguageLiteralIterator(graphNode.getLiterals(fieldUriRef), languages);
            it.hasNext();
            literals.add(it.next())
        );
        //delete the found literals
        for(Literal literal:literals){
            graphNode.deleteProperty(fieldUriRef, literal);
        }

    }

    @Override
    public void set(String field, Object value) {
        removeAll(field);
        if(value != null){
            add(field,value);
        }
    }

    @Override
    public void setReference(String field, String reference) {
        removeAll(reference);
        if(reference != null){
            addReference(field, reference);
        }
    }

    @Override
    public void setNaturalText(String field, String text, String...languages) {
        removeAllNaturalText(field, languages);
        if(text != null){
            addNaturalText(field, text, languages);
        }
    }

}
