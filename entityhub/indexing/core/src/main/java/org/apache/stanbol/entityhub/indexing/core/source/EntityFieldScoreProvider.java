package org.apache.stanbol.entityhub.indexing.core.source;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

public class EntityFieldScoreProvider implements EntityScoreProvider {
    
    public static final String PRAM_FIELD_NAME = "field";
    
    public static final String DEFAULT_FIELD_NAME = RdfResourceEnum.signRank.getUri();
    
    private String fieldName;
    
    /**
     * Creates an instance that uses the field as specified by
     * {@link RdfResourceEnum#signRank} to retrieve the score for an entity
     */
    public EntityFieldScoreProvider(){
        this(null);
    }
    /**
     * Creates an instance that uses the parsed field to retrieve the score for 
     * an entity or {@link RdfResourceEnum#signRank} in case <code>null</code>
     * is parsed.
     * @param fieldName the field used to retrieve the score from parsed
     * {@link Representation}s or <code>null</code> to use the
     * {@link RdfResourceEnum#signRank} field.
     */
    public EntityFieldScoreProvider(String fieldName){
        if(fieldName == null){
            this.fieldName = DEFAULT_FIELD_NAME;
        }
    }
    
    @Override
    public boolean needsData() {
        return true;
    }

    @Override
    public Float process(String id) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Float process(Representation entity) throws UnsupportedOperationException {
        Object value = entity.getFirst(fieldName);
        if(value instanceof Float){
            return (Float)value;
        } else if(value instanceof Number) {
            return Float.valueOf(((Number)value).floatValue());
        } else {
            if(value != null){
                try {
                    return Float.valueOf(value.toString());
                } catch(NumberFormatException e){
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public void close() {
        //nothing to do
    }

    @Override
    public void initialise() {
        //no initialisation needed
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        Object value = config.get(PRAM_FIELD_NAME);
        if(value != null && !value.toString().isEmpty()){
            fieldName = value.toString();
        }
    }
}
