package eu.iksproject.rick.yard.solr.impl.constraintEncoders;

import java.util.Arrays;
import java.util.Collection;

import eu.iksproject.rick.servicesapi.defaults.NamespaceEnum;
import eu.iksproject.rick.yard.solr.model.FieldMapper;
import eu.iksproject.rick.yard.solr.model.IndexDataType;
import eu.iksproject.rick.yard.solr.query.ConstraintTypePosition;
import eu.iksproject.rick.yard.solr.query.EncodedConstraintParts;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEncoder;
import eu.iksproject.rick.yard.solr.query.IndexConstraintTypeEnum;
import eu.iksproject.rick.yard.solr.query.ConstraintTypePosition.PositionType;
import eu.iksproject.rick.yard.solr.utils.SolrUtil;

public class LangEncoder implements IndexConstraintTypeEncoder<Collection<String>> {

    private static final ConstraintTypePosition PREFIX = new ConstraintTypePosition(PositionType.prefix);
    private static final ConstraintTypePosition SUFFIX = new ConstraintTypePosition(PositionType.suffux);
    private static final IndexDataType STRING_DATATYPE =  new IndexDataType(NamespaceEnum.xsd+"string");
    private FieldMapper fieldMapper;
    public LangEncoder(FieldMapper fieldMapper){
        this.fieldMapper = fieldMapper;
    }
    @Override
    public void encode(EncodedConstraintParts constraint,Collection<String> value) {
        if(value != null && !value.isEmpty()){ //null indicates the default language!
            for(String prefix : fieldMapper.encodeLanguages(value)){
                constraint.addEncoded(PREFIX, SolrUtil.escapeSolrSpecialChars(prefix));
            }
        } else { //default
            //search in the language merger field of the default language
            constraint.addEncoded(PREFIX, SolrUtil.escapeSolrSpecialChars(fieldMapper.getLanguageMergerField(null)));
            String[] prefixSuffix = fieldMapper.encodeDataType(STRING_DATATYPE);
            if(prefixSuffix[0] != null && !prefixSuffix[0].isEmpty()){
                constraint.addEncoded(PREFIX, prefixSuffix[0]);
            }
            if(prefixSuffix[1] != null && !prefixSuffix[1].isEmpty()){
                constraint.addEncoded(SUFFIX, prefixSuffix[1]);
            }
        }
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
        return IndexConstraintTypeEnum.LANG;
    }
    @SuppressWarnings("unchecked")
    @Override
    public Class<Collection<String>> acceptsValueType() {
        //generic types are ereased anyway!
        return (Class<Collection<String>>)(Class<?>)Collection.class;
    }

}
