package eu.iksproject.rick.yard.solr.impl.constraintEncoders;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import eu.iksproject.rick.yard.solr.model.FieldMapper;
import eu.iksproject.rick.yard.solr.query.ConstraintTypePosition;
import eu.iksproject.rick.yard.solr.query.EncodedConstraintParts;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEncoder;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEnum;
import eu.iksproject.rick.yard.solr.query.ConstraintTypePosition.PositionType;
import eu.iksproject.rick.yard.solr.utils.SolrUtil;

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
