package org.apache.stanbol.entityhub.yard.solr.impl.constraintEncoders;

import java.util.Arrays;
import java.util.Collection;

import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValueFactory;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.utils.SolrUtil;


/**
 * Encodes the Assignment of the field to an value. If a value is parsed, than
 * it encodes that the field must be equals to this value.
 * @author Rupert Westenthaler
 *
 */
public class AssignmentEncoder implements IndexConstraintTypeEncoder<Object>{

    private final static ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.assignment);
    private final static String EQ = ":";
    private final IndexValueFactory indexValueFactory;
    public AssignmentEncoder(IndexValueFactory indexValueFactory) {
        if(indexValueFactory == null){
            throw new IllegalArgumentException("The indexValueFactory MUST NOT be NULL");
        }
        this.indexValueFactory = indexValueFactory;
    }
    @Override
    public void encode(EncodedConstraintParts constraint, Object value) {
        IndexValue indexValue;
        if(value == null){
            indexValue = null;
        } else if(value instanceof IndexValue){
            indexValue = (IndexValue)value;
        } else {
            indexValue = indexValueFactory.createIndexValue(value);
        }
        String eqConstraint = EQ;
        if(value != null){
            eqConstraint = EQ+(SolrUtil.escapeSolrSpecialChars(indexValue.getValue()));
        }
        constraint.addEncoded(POS, eqConstraint);
    }

    @Override
    public boolean supportsDefault() {
        return true;
    }
    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Arrays.asList(IndexConstraintTypeEnum.FIELD);
    }
    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.EQ;
    }

    @Override
    public Class<Object> acceptsValueType() {
        return Object.class;
    }


}
