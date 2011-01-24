package org.apache.stanbol.entityhub.yard.solr.impl.constraintEncoders;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.stanbol.entityhub.yard.solr.model.FieldMapper;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.utils.SolrUtil;


public class FieldEncoder implements IndexConstraintTypeEncoder<List<String>> {

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.field);
    private final FieldMapper fieldMapper;

    public FieldEncoder(FieldMapper fieldMapper){
        if(fieldMapper == null){
            throw new IllegalArgumentException("The parsed FieldMapper instance MUST NOT be NULL!");
        }
        this.fieldMapper = fieldMapper;
    }
    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Collections.emptySet();
    }

    @Override
    public void encode(EncodedConstraintParts constraint, List<String> value) throws IllegalArgumentException {
        if(value == null){
            throw new IllegalArgumentException("This encoder does not support the NULL value");
        }
        constraint.addEncoded(POS, SolrUtil.escapeSolrSpecialChars(fieldMapper.encodePath(value)));
    }

    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.FIELD;
    }

    @Override
    public boolean supportsDefault() {
        return false;
    }
    @SuppressWarnings("unchecked")
    @Override
    public Class<List<String>> acceptsValueType() {
        //NOTE: Generic types are erased at compile time anyways!
        return (Class<List<String>>)(Class<?>)List.class;
    }

}
