package eu.iksproject.rick.yard.solr.impl.constraintEncoders;

import java.util.Arrays;
import java.util.Collection;

import eu.iksproject.rick.yard.solr.model.IndexValue;
import eu.iksproject.rick.yard.solr.model.IndexValueFactory;
import eu.iksproject.rick.yard.solr.query.ConstraintTypePosition;
import eu.iksproject.rick.yard.solr.query.EncodedConstraintParts;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEncoder;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEnum;
import eu.iksproject.rick.yard.solr.query.ConstraintTypePosition.PositionType;


public class GtEncoder implements IndexConstraintTypeEncoder<Object>{
    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.value,1);
    private static final String DEFAULT = "*";

    private IndexValueFactory indexValueFactory;
    public GtEncoder(IndexValueFactory indexValueFactory) {
        if(indexValueFactory == null){
            throw new IllegalArgumentException("The parsed IndexValueFactory MUST NOT be NULL!");
        }
        this.indexValueFactory = indexValueFactory;
    }
    @Override
    public void encode(EncodedConstraintParts constraint, Object value) {
        IndexValue indexValue;
        if(value == null){
            indexValue = null; //default value
        } else if(value instanceof IndexValue){
            indexValue = (IndexValue)value;
        } else {
            indexValue = indexValueFactory.createIndexValue(value);
        }
        String geConstraint = String.format("{%s ",
                indexValue !=null && indexValue.getValue() != null && !indexValue.getValue().toString().isEmpty() ?
                        indexValue.getValue() : DEFAULT);
        constraint.addEncoded(POS, geConstraint);
    }

    @Override
    public boolean supportsDefault() {
        return true;
    }

    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Arrays.asList(IndexConstraintTypeEnum.EQ,IndexConstraintTypeEnum.LT);
    }

    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.GT;
    }
    @Override
    public Class<Object> acceptsValueType() {
        return Object.class;
    }

}
