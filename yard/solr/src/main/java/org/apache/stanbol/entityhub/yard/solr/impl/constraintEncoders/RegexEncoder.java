package org.apache.stanbol.entityhub.yard.solr.impl.constraintEncoders;

import java.util.Arrays;
import java.util.Collection;

import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;


/**
 * TODO: This encoder is not functional! It would need to convert the REGEX
 * Pattern to the according WildCard search!
 * Need to look at http://lucene.apache.org/java/2_4_0/api/org/apache/lucene/search/regex/RegexQuery.html
 * @author Rupert Westenthaler
 *
 */
public class RegexEncoder implements IndexConstraintTypeEncoder<String>{

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.value);

    @Override
    public void encode(EncodedConstraintParts constraint, String value) {
        if(value == null){
            throw new IllegalArgumentException("This encoder does not support the NULL IndexValue!");
        } else {
            //TODO: Implement some REGEX to WILDCard conversion for Solr
            constraint.addEncoded(POS, value.toLowerCase());
        }
    }

    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Arrays.asList(IndexConstraintTypeEnum.EQ);
    }

    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.REGEX;
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
