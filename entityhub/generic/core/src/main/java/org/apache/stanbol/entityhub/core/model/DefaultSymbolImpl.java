package org.apache.stanbol.entityhub.core.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.stanbol.entityhub.core.utils.ToStringIterator;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Symbol Interface based on the parsed {@link Representation}.<br>
 * Specific implementations of the entity hub models might want to use this implementation
 * so that they need only to implement the {@link Representation} interface.
 * However implementations might also decides to provide its own implementation
 * of the {@link Symbol} as well as the other interfaces defined by the
 * entityhub model
 * @author Rupert Westenthaler
 *
 */
public class DefaultSymbolImpl extends DefaultSignImpl implements Symbol {

    private static final Logger log = LoggerFactory.getLogger(DefaultSymbolImpl.class);

    private String defaultLanguage;
    private static final String[] ALT_LABEL_LANGUAGES = new String[]{null,"en"};
    public static final Map<String,SymbolState> SYMBOL_STATE_MAP;
    static{
        Map<String,SymbolState> tmp = new HashMap<String, SymbolState>();
        for(SymbolState state : SymbolState.values()){
            tmp.put(state.getUri(), state);
        }
        SYMBOL_STATE_MAP = Collections.unmodifiableMap(tmp);
    }

    /**
     * Creates a Symbol Wrapper over the parsed representation with
     * <code>null</code> as default language
     * @param entityhubId the ID of the Entityhub that manages this Symbol
     * @param representation the representation holding the state of the symbol
     * @throws IllegalArgumentException If the Symbol instance can not be initialised based on the parsed parameter.
     * This includes: <ul>
     * <li> the Entityhub ID is <code>null</code> or empty
     * <li> the parsed representation does not define a label
     *      (provide a value for the {@link Symbol#LABEL} field)
     * <li> the parsed representation does not define a valid symbol state
     *      (provide a value of {@link SymbolState} for the {@link Symbol#STATE} field)
     * <li> the representation is <code>null</code>
     * </ul>
     */
    public DefaultSymbolImpl(String entityhubId,Representation representation){
        this(entityhubId,representation,null);
    }
    /**
     * Creates a Symbol Wrapper over the parsed representation
     * @param entityhubId the ID of the Entityhub that manages this Symbol
     * @param representation the representation holding the state of the symbol
     * @param defaultLanguage the language for requests without an language
     * (e.g. methods like {@link #getLabel()})
     * @throws IllegalArgumentException If the Symbol instance can not be initialised based on the parsed parameter.
     * This includes: <ul>
     * <li> the Entityhub ID is <code>null</code> or empty
     * <li> the parsed representation does not define a label
     *      (provide a value for the {@link Symbol#LABEL} field)
     * <li> the parsed representation does not define a valid symbol state
     *      (provide a value of {@link SymbolState} for the {@link Symbol#STATE} field)
     * <li> the representation is <code>null</code>
     * </ul>
     */
    public DefaultSymbolImpl(String entityhubId,Representation representation,String defaultLanguage) throws IllegalArgumentException {
        super(entityhubId,representation);
        if(getLabel() == null){
            throw new IllegalArgumentException("Representation "+getId()+" does not define required field "+Symbol.LABEL);
        }
        if(getState() == null){
            throw new IllegalArgumentException("Representation "+getId()+" does not define required field "+Symbol.STATE);
        }
        this.defaultLanguage = defaultLanguage;
    }
    @Override
    public void addDescription(String description) {
        representation.addNaturalText(Symbol.DESCRIPTION, description,defaultLanguage);
    }

    @Override
    public void addDescription(String description, String lanugage) {
        representation.addNaturalText(Symbol.DESCRIPTION, description,lanugage);
    }

    @Override
    public void addPredecessor(String predecessor) {
        representation.addReference(Symbol.PREDECESSOR, predecessor);
    }

    @Override
    public void addSuccessor(String successor) {
        representation.addReference(Symbol.SUCCESSOR, successor);

    }

    @Override
    public Iterator<Text> getDescriptions() {
        return representation.getText(Symbol.DESCRIPTION);
    }

    @Override
    public Iterator<Text> getDescriptions(String lang) {
        return representation.get(Symbol.DESCRIPTION, lang);
    }

    @Override
    public String getLabel() {
        String label = getLabel(defaultLanguage);
        if(label == null){ //no label for the default language
            //search labels in other languages
            Text altLabel = representation.getFirst(Symbol.LABEL, ALT_LABEL_LANGUAGES);
            if(altLabel == null){
                Iterator<Text> labels = representation.getText(Symbol.LABEL);
                if(labels.hasNext()){
                    altLabel = labels.next();
                }
            }
            return altLabel!=null?altLabel.getText():null;
        } else {
            return label;
        }
    }

    @Override
    public String getLabel(String lang) {
        Text label = representation.getFirst(Symbol.LABEL, lang);
        return label!=null?label.getText():null;
    }

    @Override
    public Iterator<String> getPredecessors() {
        return new ToStringIterator(representation.get(Symbol.PREDECESSOR));
    }

    @Override
    public SymbolState getState() {
        Reference stateUri = representation.getFirstReference(Symbol.STATE);
        SymbolState state;
        if(stateUri != null){
            state = SYMBOL_STATE_MAP.get(stateUri.getReference());
        } else {
            state = null;
        }
        if(state == null){
            log.warn("Value "+stateUri+" for field "+Symbol.STATE+" is not a valied SymbolState! -> return null");
            return null;
        } else {
            return state;
        }
    }

    @Override
    public Iterator<String> getSuccessors() {
        return new ToStringIterator(representation.get(Symbol.SUCCESSOR));
    }

    @Override
    public boolean isPredecessors() {
        return getPredecessors().hasNext();
    }

    @Override
    public boolean isSuccessor() {
        return getSuccessors().hasNext();
    }

    @Override
    public void removeDescription(String description) {
        representation.removeNaturalText(Symbol.DESCRIPTION,description,defaultLanguage);
    }

    @Override
    public void removeDescription(String description, String language) {
        representation.removeNaturalText(Symbol.DESCRIPTION,description,language);
    }

    @Override
    public void removePredecessor(String predecessor) {
        representation.removeReference(Symbol.PREDECESSOR, predecessor);
    }

    @Override
    public void removeSuccessor(String successor) {
        representation.removeReference(Symbol.SUCCESSOR, successor);
    }

    @Override
    public void setLabel(String label) {
        representation.setNaturalText(Symbol.LABEL, label, defaultLanguage);
    }

    @Override
    public void setLabel(String label, String language) {
        representation.setNaturalText(Symbol.LABEL, label, language);
    }

    @Override
    public void setState(SymbolState state) throws IllegalArgumentException {
        representation.setReference(Symbol.STATE, state.getUri());
    }
}
