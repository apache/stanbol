/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.entityhub.core.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint.ConstraintType;
import org.apache.stanbol.entityhub.servicesapi.util.PatternUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is only an intermediate solution just to have the functionality.
 * This needs to be refactored! This is something similar to a semantic lifting
 * work flow that could used schema translation, reasoning ... <p>
 * The goal is to build a simple Module that supports basics things (like the
 * stuff provided by this implementation) and that allow other implementations
 * to do the advanced stuff.<p>
 * Currently I hope, that when the functionality is in place it is easier to
 * see what a good design for this part of the Entityhub would be.
 * TODO: refactoring (see above comment)
 * @author Rupert Westenthaler
 *
 */
public class DefaultFieldMapperImpl implements FieldMapper, Cloneable {
    private final Logger log = LoggerFactory.getLogger(DefaultFieldMapperImpl.class);
    private final Set<FieldMapping> mappings;
//    private final Map<String,Collection<FieldMapping>> ignoreFieldMap;
//    private final Map<Pattern,Collection<FieldMapping>> ignoreWildcardMap;
    private final Map<String,Set<FieldMapping>> fieldMap;
    private final Map<Pattern,Set<FieldMapping>> wildcardMap;
    private Collection<FieldMapping> unmodMappings;
    private ValueConverterFactory valueConverter;
    //private Map<String,FieldMapping> mappings = Collections.synchronizedMap(new HashMap<String, FieldMapping>());
    public DefaultFieldMapperImpl(ValueConverterFactory valueConverter) {
        super();
        mappings = new HashSet<FieldMapping>();
        unmodMappings = Collections.unmodifiableCollection(mappings);
        fieldMap = new HashMap<String, Set<FieldMapping>>();
        wildcardMap = new HashMap<Pattern, Set<FieldMapping>>();
        if(valueConverter == null){
            throw new IllegalArgumentException("The parsed ValueConverterFactory MUST NOT be NULL");
        }
        this.valueConverter = valueConverter;
//        ignoreFieldMap = new HashMap<String, Collection<FieldMapping>>();
//        ignoreWildcardMap = new HashMap<Pattern, Collection<FieldMapping>>();
    }
    /**
     * Internally used by clone
     * @param fieldMap
     * @param wildcardMap
     */
    private DefaultFieldMapperImpl(ValueConverterFactory valueConverter,Set<FieldMapping> mappings,Map<String,Set<FieldMapping>> fieldMap, Map<Pattern,Set<FieldMapping>> wildcardMap){
        this(valueConverter);
        this.mappings.addAll(mappings);
        this.fieldMap.putAll(fieldMap);
        this.wildcardMap.putAll(wildcardMap);
    }
    /**
     * Getter for all the defined Mappings for a given field name
     * @param field the name of the field
     * @return all the active Mappings
     */
    protected List<FieldMapping> getMappings(String field){
        final List<FieldMapping> fieldMappings = new ArrayList<FieldMapping>();
        //first search the fieldMappings
        Collection<FieldMapping> tmp = fieldMap.get(field);
        if(tmp != null){
            fieldMappings.addAll(tmp);
        }
        //now iterate over the Wildcard Mappings
        for(Entry<Pattern,Set<FieldMapping>> entry : wildcardMap.entrySet()){
            if(entry.getKey().matcher(field).find()){
                fieldMappings.addAll(entry.getValue());
            }
        }
        Collections.sort(fieldMappings, FieldMappingUtils.FIELD_MAPPING_COMPARATOR);
        return fieldMappings;
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper#addMapping(org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping)
     */
    public void addMapping(FieldMapping mapping){
        if(mapping == null){
            return;
        }
        if(mappings.add(mapping)){
            if(mapping.usesWildcard()){
                Pattern fieldPattern = mapping.getRegexPattern();
                synchronized (wildcardMap) {
                    Set<FieldMapping> fieldPatternMappings = wildcardMap.get(fieldPattern);
                    if(fieldPatternMappings == null){
                        fieldPatternMappings = new HashSet<FieldMapping>();//new TreeSet<FieldMapping>(FieldMappingUtils.FIELD_MAPPING_COMPARATOR);
                        wildcardMap.put(fieldPattern, fieldPatternMappings);
                    }
                    fieldPatternMappings.add(mapping);
                }
            } else {
                String fieldName = mapping.getFieldPattern();
                synchronized (fieldMap) {
                    Set<FieldMapping> fieldPatternMappings = fieldMap.get(fieldName);
                    if(fieldPatternMappings == null){
                        fieldPatternMappings = new HashSet<FieldMapping>();//new TreeSet<FieldMapping>(FieldMappingUtils.FIELD_MAPPING_COMPARATOR);
                        fieldMap.put(fieldName, fieldPatternMappings);
                    }
                    fieldPatternMappings.add(mapping);
                }
            }
        } //else already present -> nothing todo
    }
    public Collection<FieldMapping> getMappings(){
        return unmodMappings;
    }
//    private static String getPrefix(String fieldPattern){
//        return fieldPattern.split("[\\?\\*]")[0];
//    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper#removeFieldMapping(org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping)
     */
    public void removeFieldMapping(FieldMapping mapping){
        if(mapping == null){
            return;
        }
        if(mappings.remove(mapping)){
            if(mapping.usesWildcard()){
                Pattern fieldPattern = mapping.getRegexPattern();
                synchronized (wildcardMap) {
                    Collection<FieldMapping> fieldPatternMappings = wildcardMap.get(fieldPattern);
                    if(fieldPatternMappings != null){
                        if(fieldPatternMappings.remove(mapping) && fieldPatternMappings.isEmpty()){
                            //clean up the prefix if last value is removed
                            wildcardMap.remove(fieldPattern);
                        }
                    }
                }
            } else {
                String fieldPattern = mapping.getFieldPattern();
                synchronized (fieldMap) {
                    Collection<FieldMapping> fieldPatternMappings = fieldMap.get(fieldPattern);
                    if(fieldPatternMappings != null){
                        if(fieldPatternMappings.remove(mapping) && fieldPatternMappings.isEmpty()){
                            //clean up the prefix if last value is removed
                            fieldMap.remove(fieldPattern);
                        }
                    }
                }
            }
        } //else nothing todo
    }
    /**
     * Removes the FieldMapping based on the fieldPattern
     * @param fieldPattern the field pattern
     */
    public void removeFieldMapping(String fieldPattern){
        if(fieldPattern == null || fieldPattern.length()<1){
            return;
        }
        if(PatternUtils.usesWildCard(fieldPattern)){
            Pattern pattern = Pattern.compile(PatternUtils.wildcardToRegex(fieldPattern,true));
            synchronized (wildcardMap) {
                wildcardMap.remove(pattern);
            }
        } else {
            synchronized (fieldMap) {
                fieldMap.remove(fieldPattern);
            }
        }
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper#applyMappings(org.apache.stanbol.entityhub.servicesapi.model.Representation, org.apache.stanbol.entityhub.servicesapi.model.Representation)
     */
    public Representation applyMappings(Representation source, Representation target, ValueFactory valueFactory) {
        Collection<String> fields = new HashSet<String>();
        for(Iterator<String> fieldIt = source.getFieldNames();fieldIt.hasNext();){
            fields.add(fieldIt.next());
        }
        for(String field : fields){
//            log.info("  > process field: "+field);
            //get the active Mappings
            List<FieldMapping> activeMappings = getMappings(field);
            if(!activeMappings.isEmpty()){
                //get all the values (store them in an Collection, because we need them more than once)
                Collection<Object> values = new ArrayList<Object>();
                for(Iterator<Object> valueIt = source.get(field);valueIt.hasNext();){
                    values.add(valueIt.next());
                }
                //only to be sure, that this is not changed by Filters!
                values = Collections.unmodifiableCollection(values);
                /*
                 * (1) Before working with the values first analyse the active
                 * mappings and filters. Two things
                 * a) Init Wildcard Filters:
                 *    Language filters set on namespaces are executed on all field
                 *    mappings that define no language filter
                 * b) calculate the mapped fields. Possible there are no mappings
                 *    left. Than we need not to process all the values
                 */
                Set<String> targetFields = new HashSet<String>();
                TextConstraint globalFilter = null;
                Collection<Object> globalFiltered = null;
                /*
                 * NOTE: the mappings are sorted in the way, that the most
                 *   prominent one will be at index 0. The wildcard "*" will
                 *   be always the last.
                 *   So we need to parse backwards because than more prominent
                 *   things will overwrite and win!
                 */
                for(int i=activeMappings.size()-1;i>=0;i--){
                    FieldMapping mapping = activeMappings.get(i);
                    if(mapping.usesWildcard() //if wildcard
                            && !mapping.ignoreField() && //and not ignore
                            mapping.getFilter() != null && //and a filter is present
                            mapping.getFilter().getType() == ConstraintType.text){ //and of type text
                        //set the global text filter.
                        //NOTE: the active mappings are sorted in that way, that
                        //      the most specific one is set last
                        globalFilter = (TextConstraint)mapping.getFilter();
                    }
                    for(String targetField : mapping.getMappings()){
                        if(mapping.ignoreField()){
                            targetFields.remove(targetField);
                        } else {
                            targetFields.add(targetField);
                        }
                    }
                }
//                log.info("    o targets: "+targetFields);
//                log.info("    o global text filter: "+globalFilter);
                if(globalFilter != null){
                    globalFiltered = new HashSet<Object>(values);
                    //parse false ass third argument, because we need not to filter
                    //non-Text values for wildcard filter!
                    processFilter(globalFilter, globalFiltered,false);
                }
                //now process the mappings
                for(FieldMapping mapping : activeMappings){
                    if(!mapping.ignoreField() &&
                            !Collections.disjoint(targetFields, mapping.getMappings())){
                        processMapping(mapping, valueFactory, field,  values,globalFiltered, targetFields, target);
//                    } else if(!mapping.ignoreField()) {
//                        log.info(String.format("  << ignore mapping %s ",mapping));
//                    } else {
//                        log.info(String.format("  << %s ",mapping));
                    }
                }
            }
        }
        /*
         * TODO: return a "MappingReport"
         * All mapping activities should be documented and stored with the
         * MappedEntity as MappingActivity!
         */
        return target;
    }
    /**
     *
     * @param mapping
     * @param valueFactory The value factory used to create converted values
     * @param field
     * @param values
     * @param globalFiltered
     * @param targets
     */
    private void processMapping(FieldMapping mapping, ValueFactory valueFactory,String field,  Collection<Object> values, Collection<Object> globalFiltered, Set<String> activeTargets,Representation targetRepresentation) {
        //parsed mappings are all !ignore and some mappings are active
        Collection<Object> filtered; //this collection will be modified by the filters later on
        if(globalFiltered == null || //if no global filter is present and therefore globalFiltered == null or
                //there is a more special text filter defined in this mapping
                mapping.getFilter() != null && mapping.getFilter().getType() == ConstraintType.text){
            filtered = new HashSet<Object>(values);//start with all values
        } else { //start with the values filtered by the global filter
            filtered = new HashSet<Object>(globalFiltered);
        }
        if(mapping.getFilter()!=null){
            switch (mapping.getFilter().getType()) {
            case value:
                ValueConstraint valueConstraint = (ValueConstraint)mapping.getFilter();
                processFilter(valueConstraint,filtered,valueFactory);
                break;
            case text:
                TextConstraint textConstraint = (TextConstraint)mapping.getFilter();
                //for wildcard mappings only filter TextValues. if the mapping is
                //for a specific field filter also non text values.
                processFilter(textConstraint,filtered,!mapping.usesWildcard());
                break;
            default:
                log.warn(String.format("Filter of type %s are not supported -> select all values! (Constraint=%s)",
                        mapping.getFilter().getType(),mapping.getFilter()));
                break;
            }
            /*
             * TODO: add general purpose functionality to apply Constraints.
             * Currently this is done by the specific Query Implementations :(
             *  - use the constraint to filter the values collection!
             */

        } //nothing to do
        for(String mappedField : mapping.getMappings()){
            //activeTargets still uses null for the current field
            // -> this is because wildcard filters can not know the actual field name
            if(activeTargets.contains(mappedField)){ //so use null to match
                if(mappedField == null){ //and than replace null with the field name
                    mappedField = field;
                }
//                log.info(String.format("  >> copy%s to %s &d values",
//                        mappedField.equals(field)?"":" from "+field,mappedField,filtered.size()));
                targetRepresentation.add(mappedField, filtered);
//            } else {
//                log.info(String.format("  << ignore%s %s",
//                        mappedField.equals(field)?"":"mapping from "+field+"to",mappedField));
            }
        }

    }
    /**
     * This method filters the parsed {@link Text} values based on the languages
     * parsed in the {@link TextConstraint}.
     * This method modifies the parsed collection by using the
     * {@link Iterator#remove()} method.
     * @param textConstraint the text constraint containing the active languages
     * @param values the values to filter. This method modifies this collection
     * @return the modified collection to allow nested calls
     */
    private Collection<Object> processFilter(TextConstraint textConstraint, Collection<Object> values,boolean filterNonTextValues) {
        if(textConstraint.getTexts() != null){
            log.warn("Filtering based on values is not implemented");
        }
        /*
         * TODO: If filterNonTextValues=true and acceptDefaultLanguate=true
         *       we could also try to convert non-Text values to Text (by using
         *       the valueConverter.
         */
        Set<String> langs = textConstraint.getLanguages();
        boolean acceptDefaultLanguage = textConstraint.getLanguages().contains(null);
        for(Iterator<Object> it = values.iterator();it.hasNext();){
            Object value = it.next();
            if(value instanceof Text){
                if(!langs.contains(((Text)value).getLanguage())){
                    it.remove();
//                    log.info(String.format("   - value %s(type:%s) rejected by text filter",value,value.getClass()));
//                } else {
//                    log.info(String.format("   + value %s(type:%s) accepted by text filter",value,value.getClass()));
                }
            } else if(filterNonTextValues && value instanceof String){
                //Strings only if the default language is enabled
                if(!acceptDefaultLanguage){
                    it.remove();
//                    log.info(String.format("   - value %s(type:%s) rejected by text filter",value,value.getClass()));
//                } else {
//                    log.info(String.format("   + value %s(type:%s) accepted by text filter",value,value.getClass()));
                }
            } else if(filterNonTextValues){
                it.remove();
//                log.info(String.format("   - value %s(type:%s) rejected by text filter",value,value.getClass()));
            } //else non text value and filterNonTextValues=false -> nothing to do
        }
        return values;
    }
    /**
     * This method converts - or if not possible filters the parsed values based
     * on the parsed constraint
     * @param valueConstraint
     * @param values
     * @return
     */
    private Collection<Object> processFilter(ValueConstraint valueConstraint, Collection<Object> values,ValueFactory valueFactory) {
        if(valueConstraint.getValues() != null){
            log.warn("Filtering based on values is not yet implemented");
        }
        //1) collect all active dataTypes
        //first a EnumSet for really fast containsAll ... operations
        Set<DataTypeEnum> activeDataTypes = EnumSet.noneOf(DataTypeEnum.class);
        //second a List to keep track of the ordering of the dataTypes in the
        //constraint for later conversions!
        List<DataTypeEnum> sortedActiveDataTypes = new ArrayList<DataTypeEnum>(valueConstraint.getDataTypes().size());
        //NOTE: using a LinkedHashSet would slow down this code, because EnumSet
        //  gives constant processing time even for bulk operations!
        for(String dataTypeUri : valueConstraint.getDataTypes()){
            DataTypeEnum dataType = DataTypeEnum.getDataType(dataTypeUri);
            if(dataType == null){
                log.warn(String.format("DataType %s not supported"));
            } else {
                if(activeDataTypes.add(dataType)){
                    //only of set has changed to avoid duplicates in the list
                    sortedActiveDataTypes.add(dataType);
                }
            }
        }
        //2) now process the values
//        log.info(" --- Filter values ---");
        //calculating acceptable and not acceptable types needs some processing time
        //and usually values will be only of very less different types.
        //Therefore it makes sense to cache accepted and rejected types!
        Set<Class<?>> accepted = new HashSet<Class<?>>();
        Set<Class<?>> rejected = new HashSet<Class<?>>();
        //Set that stores rejected values. Such will be converted later on!
        Set<Object> needConversion = new HashSet<Object>();
        for(Iterator<Object> it = values.iterator();it.hasNext();){
            Object value = it.next();
//            if(accepted.contains(value.getClass())){
//                log.info(String.format("   + value %s(type:%s) accepted by value filter",value,value.getClass()));
                //nothing to do
//            } else 
            if(rejected.contains(value.getClass())){
                it.remove(); //remove also the current value of that type
                needConversion.add(value); //save as value that need to be converted
//                log.info(String.format("   - value %s(type:%s) rejected by value filter",value,value.getClass()));
            } else { //new class ... calculate
                Set<DataTypeEnum> valueTypes = DataTypeEnum.getAllDataTypes(value.getClass());
                if(valueTypes.removeAll(activeDataTypes)){
                    accepted.add(value.getClass());
//                    log.info(String.format("   + value %s(type:%s) accepted by value filter",value,value.getClass()));
                } else {
                    rejected.add(getClass());
                    it.remove(); //remove the Item
                    needConversion.add(value); //save as value that need to be converted
//                    log.info(String.format("   - value %s(type:%s) rejected by value filter",value,value.getClass()));
                }
            }
        }
        //3) try to convert values to the active dataTypes
//        log.info(" --- Try to Convert rejected values ---");
        for(Object value : needConversion){
            Object converted = null;
            DataTypeEnum convertedTo = null;
            for(Iterator<DataTypeEnum> dataTypes = sortedActiveDataTypes.iterator(); //iterate over all active dataTypes
                converted == null && dataTypes.hasNext();){ //while converted still null and more dataTypes to try
                convertedTo = dataTypes.next();
                converted = valueConverter.convert(value, convertedTo.getUri(),valueFactory); //try the conversion
            }
            if(converted != null){
//                log.info(String.format("   + value %s(javaType=%s) successfully converted to %s(datatype=%s)",
//                        value,value.getClass().getSimpleName(),converted,convertedTo.getShortName()));
                values.add(converted);
//            } else {
//                log.info(String.format("   - value %s(javaType=%s) could not be converted"),
//                        value,value.getClass().getSimpleName());
            }
        }
        return values;
    }
    @Override
    public DefaultFieldMapperImpl clone() {
        return new DefaultFieldMapperImpl(this.valueConverter,this.mappings,this.fieldMap, this.wildcardMap);
    }
    @Override
    public int hashCode() {
        return mappings.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof DefaultFieldMapperImpl &&
            ((DefaultFieldMapperImpl)o).mappings.equals(mappings);
    }
}
