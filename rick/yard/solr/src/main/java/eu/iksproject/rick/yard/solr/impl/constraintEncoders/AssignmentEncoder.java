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
import eu.iksproject.rick.yard.solr.utils.SolrUtil;

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
