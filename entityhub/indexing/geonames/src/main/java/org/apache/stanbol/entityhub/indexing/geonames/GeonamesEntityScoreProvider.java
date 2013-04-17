package org.apache.stanbol.entityhub.indexing.geonames;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

public class GeonamesEntityScoreProvider implements EntityScoreProvider {

    private static final String FCLASS_A = GeonamesConstants.GEONAMES_ONTOLOGY_NS +"A";
    private static final String FCLASS_P = GeonamesConstants.GEONAMES_ONTOLOGY_NS +"P";
    private static final int MAX_POPULATION = 1000000;
    private static final double FACT = Math.log1p(1000000);
    private static final Float DEFAULT_SCORE = Float.valueOf(0.3f);
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean needsData() {
        return true;
    }

    @Override
    public Float process(String id) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This implementation requries data to process the score");
    }

    @Override
    public Float process(Representation entity) throws UnsupportedOperationException {
        Reference ref = entity.getFirstReference(GeonamesPropertyEnum.gn_featureClass.toString());
        String fclass = ref == null ? null : ref.getReference();
        //ref = entity.getFirstReference(GeonamesPropertyEnum.gn_featureCode.toString());
        //String fCode = ref == null ? null : ref.getReference();
        
        if(FCLASS_A.equals(fclass)){
            return Float.valueOf(1f);
        } else if(FCLASS_P.equals(fclass)){
            Long population = entity.getFirst(GeonamesPropertyEnum.gn_population.toString(), Long.class);
            if(population == null){
                return Float.valueOf(0.2f); //min population score
            } else {
                long p = Math.min(MAX_POPULATION, population.longValue());
                double fact = Math.log1p(p);
                //Normalised the score based on the population in the range
                // [0.2..1.0]
                return Float.valueOf((float)((fact/FACT*0.8)+0.2));
            }
        } else {
            return DEFAULT_SCORE;
        }
    }

}
