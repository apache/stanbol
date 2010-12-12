package eu.iksproject.rick.yard.solr.impl.constraintEncoders;

import java.util.Arrays;
import java.util.Collection;

import eu.iksproject.rick.yard.solr.query.ConstraintTypePosition;
import eu.iksproject.rick.yard.solr.query.EncodedConstraintParts;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEncoder;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEnum;
import eu.iksproject.rick.yard.solr.query.ConstraintTypePosition.PositionType;

public class WildcardEncoder implements IndexConstraintTypeEncoder<String>{

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.value);

    @Override
    public void encode(EncodedConstraintParts constraint, String value) {
        if(value == null){
            throw new IllegalArgumentException("This encoder does not support the NULL IndexValue!");
        } else {
            //TODO: Use toLoverCase here, because I had problems with Solr that
            //     Queries where not converted to lower case even that the
            //     LowerCaseFilterFactory was present in the query analyser :(
            constraint.addEncoded(POS, value.toLowerCase());
        }
    }

    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Arrays.asList(IndexConstraintTypeEnum.EQ);
    }

    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.WILDCARD;
    }

    @Override
    public boolean supportsDefault() {
        return false;
    }

    @Override
    public Class<String> acceptsValueType() {
        return String.class;
    }

}
