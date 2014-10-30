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
package org.apache.stanbol.entityhub.servicesapi.mapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.util.PatternUtils;

/**
 * A FieldMapping consisting of <ul>
 * <li> a required field pattern that is matched against source field. Wildcards
 *      can be used to define such patterns (e.g. http://myOntology.com/* to match
 *      all properties defined within this Ontology)
 * <li> a constraint that is used to filter values of the source field
 * <li> a set of mappings (target field names) to copy the filtered values to. If
 *      this set contains a <code>null</code> value, than a field with the same
 *      name as the source field is created for the source.
 * </ul>
 * Note that a Filter with the pattern '*' no constraint and only an <code>null</code>
 * value as mapping would create a 1:1 copy of the source.
 * TODO: Is it OK to keep an actual implementation in the Service API package?
 * @author Rupert Westenthaler
 *
 */
public class FieldMapping {

    /**
     * The '#' char is used for comments
     */
    public static final char COMMENT_CHAR = '#';
    private final String pattern;
    private final Pattern regex;
    private final boolean usesWildcard;
    private Set<String> mappings;
    private Constraint filter;
    private final boolean inverse;
    private final boolean global;
    /**
     * Returns <code>true</code> if fields that match the pattern are ignored.
     * This is can only be the case if no Filter is defined ({@link #getFilter()}
     * returns <code>null</code>).
     * @return the ignore field state
     */
    public final boolean ignoreField() {
        return inverse && filter == null;
    }
    /**
     * Creates a FieldMapping that matches all fields but does not map any field.
     * However it applies the filter to all other mappings if there is no more
     * specific Filter applied.
     * @param globalFilter The global filter. Typically a {@link TextConstraint}.
     * @throws IllegalArgumentException if the parsed Filter is <code>null</code>
     */
    public FieldMapping(Constraint globalFilter) throws IllegalArgumentException{
        this(null,false,globalFilter);
    }
    /**
     * Creates an 1:1 mapping for all values of fields that confirm the the
     * defined pattern.<p>
     * NOTE <ul>
     * <li> mappings are ignored if the fieldPattern uses a wildcard
     * <li> parsing <code>null</code> as fieldPattern matches any field, but does
     *      not map anything. This can be used to define global language filters-
     * </ul>
     * @param fieldPattern the pattern (typically the names pace followed by an *)
     * @param mappedTo the list of target fields (if the mappings contain <code>null</code>
     *    filtered values of the current field in the source {@link Representation}
     *    are copied to the same field name in the target {@link Representation}.
     * @throws IllegalArgumentException if <code>null</code> or an empty string is parsed as pattern
     */
    public FieldMapping(String fieldPattern,String...mappedTo) throws IllegalArgumentException{
        this(fieldPattern,null,mappedTo);
    }
    /**
     * Creates a Mapping the maps (<code>ignore = false)</code>) or ignores (
     * <code>ignore = true</code>) fields that match the defined pattern.
     * @param fieldPattern the pattern used to match field names
     * @param ignoreField if <code>false</code> (the default) than fields that match
     *   the parsed pattern are processed. If <code>true</code> than fields that
     *   match the pattern are ignored.
     * @param mappedTo the list of target fields (if the mappings contain <code>null</code>
     *    filtered values of the current field in the source {@link Representation}
     *    are copied to the same field name in the target {@link Representation}.
     * @throws IllegalArgumentException if <code>null</code> or an empty string is parsed as pattern
     */
    public FieldMapping(String fieldPattern,boolean ignoreField,String...mappedTo) throws IllegalArgumentException{
        this(fieldPattern,ignoreField,null,mappedTo);
    }
    /**
     * Creates an mapping based on the parsed parameter
     * @param fieldPattern the pattern used to select fields of the source representation
     * @param filter the constraint used to filter values of selected fields
     * @param mappedTo the list of target fields (if the mappings contain <code>null</code>
     *    filtered values of the current field in the source {@link Representation}
     *    are copied to the same field name in the target {@link Representation}.
     * @throws IllegalArgumentException if <code>null</code> or an empty string is parsed as pattern
     */
    public FieldMapping(String fieldPattern,Constraint filter,String...mappedTo) throws IllegalArgumentException {
        this(fieldPattern,false,filter,mappedTo);
    }
    /**
     * Private internal constructor that does all the initialisation stuff. This
     * is private because some combinations would not result in valid mappings!
     * The public constructors can only create valid field mappings.
     * See documentation of the public variants!
     */
    private FieldMapping(String fieldPattern,boolean ignore,Constraint filter,String...mappedTo){
        if(fieldPattern == null || fieldPattern.length()<1){
            if(filter == null){
                throw new IllegalArgumentException("The Filter MUST NOT be NULL for the global Fieldmapping!");
            }
            this.global = true;
            fieldPattern = "*";
        } else {
            this.global = false;
        }
        this.pattern = fieldPattern;
        this.inverse = ignore; //if ignore=true -> filter==null && mappedTo.lenght==0
        if(PatternUtils.usesWildCard(fieldPattern)){
            this.regex = Pattern.compile(PatternUtils.wildcardToRegex(fieldPattern,true));
            this.usesWildcard = true;
        } else {
            this.regex = null;
            usesWildcard = false;
        }
        this.filter = filter;
        if(this.global){
            mappedTo = new String[]{}; //set to empty -> if global than map nothing
        //NOTE: FieldMappings do now allow to map a Wildcard to an other field
        //      This is e.g. usefull for collecting all Literal values in a field
        //      holding the disambiguation context.
//        } else if(this.usesWildcard){
//            mappedTo = new String[]{null}; //wildcard always maps the selected field 1:1
        } else if(mappedTo == null || mappedTo.length<1){
                mappedTo = new String[]{null}; //if no mapping parse map the field 1:1
        } //else used the parsed one
        this.mappings = new HashSet<String>(Arrays.asList(mappedTo));
    }
    /**
     * Returns <code>true</code> if this fieldMapping maps any field. This
     * means that the {@link #getFieldPattern()}<code>.equals("*")</code>
     * @return if this is a global field mapping
     */
    public final boolean isGlobal() {
        return global;
    }
    /**
     * Getter for the RegexPettern representing the parsed wildcard.
     * @return The regex pattern or <code>null</code> if this mapping does not
     *     use wildcards within the field pattern.
     */
    public final Pattern getRegexPattern() {
        return regex;
    }
    /**
     * Returns <code>true</code> if the fieldPattern uses wildcards (? or *)
     * @return Returns <code>true</code> if the fieldPattern uses wildcards
     */
    public final boolean usesWildcard() {
        return usesWildcard;
    }
    /**
     * The Wildcard Pattern (*,?) used to match field name against.
     * @return the pattern
     */
    public String getFieldPattern(){
        return pattern;
    }
    /**
     * The target fields values of the source fields are copied to
     * @return the target fields
     */
    public Set<String> getMappings(){
        return mappings;
    }
    /**
     * The constraint used to filter values of the source field
     * @return the constraint used to filter values of the source field
     */
    public Constraint getFilter(){
        return filter;
    }
    /**
     * Setter for the filter used for values. If <code>null</code> is parsed
     * the filter is removed.
     * @param constraint the constraint or <code>null</code> to deactivate any
     *     filtering.
     */
    public void setFilter(Constraint constraint){
        this.filter = constraint;
    }
    /**
     * Removes any specified filter
     */
    public void removeFilter(){
        setFilter(null);
    }
    /**
     * Adds a mapping
     * @param mapping the Mapping (use <code>null</code> to configure a 1:1 Mapping)
     */
    public void addMapping(String mapping){
        mappings.add(mapping);
    }
    /**
     * Removes the mapping from the list. Please note, that if the last mapping
     * is removed, the <code>null</code> mapping is added to the list to preserve
     * the default 1:1 mapping.
     * @param mapping The mapping to remove
     */
    public void removeMapping(String mapping){
        if(mappings.remove(mapping) && mappings.isEmpty()){
            mappings.add(null); //if the last element is removed add null to
            //preserve the 1:1 mapping
        }
    }
    @Override
    public String toString() {
        return inverse?"!":""+pattern+(filter!=null?" | "+filter:"")+" > "+mappings;
    }
    @Override
    public int hashCode() {
        return pattern.hashCode()+mappings.hashCode()+(inverse?1:0)+(filter!=null?filter.hashCode():0);
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FieldMapping && // check type
            ((FieldMapping)obj).pattern.equals(pattern) && //check field pattern
            ((FieldMapping)obj).inverse == inverse && //check inverse
            ((FieldMapping)obj).mappings.equals(mappings) && //check mappings
            ( //check the optional value filter
                    (((FieldMapping)obj).filter == null && filter == null) ||
                    (((FieldMapping)obj).filter != null && ((FieldMapping)obj).filter.equals(filter))
            );
    }
}
