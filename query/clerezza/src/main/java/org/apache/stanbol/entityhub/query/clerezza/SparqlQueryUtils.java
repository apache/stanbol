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
package org.apache.stanbol.entityhub.query.clerezza;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.util.W3CDateFormat;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.entityhub.core.utils.AdaptingIterator;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.RangeConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.apache.stanbol.entityhub.servicesapi.util.PatternUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility Class to create SPARQL Queries for {@link SparqlFieldQuery} instances.<p>
 * Thanks to ogrisel for pointing me to his <a href="http://hg.nuxeo.org/sandbox/scribo/raw-file/b57ada956947/scribo-annotator-recognizer-sparql-ep/src/main/java/ws/scribo/annotators/recognizer/SparqlEndpointInstanceRecognizer.java">
 * SparqlEndpointInstanceRecognizer</a> implementation for the query optimisations
 * for Virtuoso and LARQ!
 * @author Rupert Westenthaler
 *
 */
public final class SparqlQueryUtils {
    
    private static final Logger log = LoggerFactory.getLogger(SparqlQueryUtils.class);
    
    public static enum EndpointTypeEnum {
        Standard,
        Virtuoso(true),
        LARQ,
        ARQ;
        boolean supportsSparql11SubSelect;
        /**
         * Default feature set (SPARQL 1.0)
         */
        EndpointTypeEnum(){
            this(false);
        }
        /**
         * Allows to enable SPARQL 1.1 features
         * @param supportsSparql11SubSelect
         */
        EndpointTypeEnum(boolean supportsSparql11SubSelect){
            this.supportsSparql11SubSelect = supportsSparql11SubSelect;
        }
        
        public final boolean supportsSubSelect() {
            return supportsSparql11SubSelect;
        }
    }

    private static final String XSD_DATE_TIME = "http://www.w3.org/2001/XMLSchema#dateTime";
    private static final DateFormat DATE_FORMAT = new W3CDateFormat();
    private static final RdfValueFactory valueFavtory = RdfValueFactory.getInstance();
    /**
     * {@link UriRef} constant for {@link RdfResourceEnum#queryResult}
     * @see RdfResourceEnum.fieldQueryResult
     */
    public static final UriRef FIELD_QUERY_RESULT = new UriRef(RdfResourceEnum.queryResult.getUri());
    /**
     * {@link UriRef} constant for {@link RdfResourceEnum#QueryResultSet}
     * @see RdfResourceEnum.FieldQueryResultSet
     */
    public static final UriRef FIELD_QUERY_RESULT_SET = new UriRef(RdfResourceEnum.QueryResultSet.getUri());
    private SparqlQueryUtils(){}

    /**
     * Creates a SPARWL CONSTRUCT query that creates triples for all the selected
     * fields of representations in the result set. <p>
     * In addition the query also constructs <code>entityhub-query:ieldQueryResultSet
     * entityhub-query:fieldQueryResult ?representation </code> triples that can be
     * used to create an iterator over the results of the query
     * @param query the field query
     * @param endpointType The type of the Endpoint (used to write optimized
     *    queries for endpoint type specific extensions
     * @param additionalFields This allows to parse additional fields that are
     *    optionally selected in the data set and added to the CONSTRUCT part
     *    of the query
     * @return the SPARQL CONSTRUCT Query
     */
    public static String createSparqlConstructQuery(SparqlFieldQuery query,EndpointTypeEnum endpointType,String...additionalFields){
        return createSparqlConstructQuery(query,-1,endpointType,additionalFields);
    }
    /**
     * Creates a SPARWL CONSTRUCT query that creates triples for all the selected
     * fields of representations in the result set. <p>
     * In addition the query also constructs <code>entityhub-query:ieldQueryResultSet
     * entityhub-query:fieldQueryResult ?representation </code> triples that can be
     * used to create an iterator over the results of the query
     * @param query the field query
     * @param limit if a value > 0 is parsed, than this value overwrites the limit
     *    defined by the query.
     * @param endpointType The type of the Endpoint (used to write optimized
     *    queries for endpoint type specific extensions
     * @param additionalFields This allows to parse additional fields that are
     *    optionally selected in the data set and added to the CONSTRUCT part
     *    of the query
     * @return the SPARQL CONSTRUCT Query
     */
    public static String createSparqlConstructQuery(SparqlFieldQuery query,int limit,EndpointTypeEnum endpointType,String...additionalFields){
        //1)INIT
        final StringBuilder queryString = new StringBuilder();
        //clone the query and reconfigure the clone 
        query = initLocalQuery(query, limit, additionalFields);
        final Map<String,String> selectedFields = initSelectedFieldsMap(query);
        //2)CONSTRUCT
        createConstruct(queryString, selectedFields);
        //3)WHERE
        queryString.append("WHERE { \n");
        addFieldConstraint(queryString, query, selectedFields,endpointType);
        queryString.append("} \n");
        //5) Limit and Offset
        if(!isSubSelectState(endpointType,selectedFields)){
            //4)add Entity Ranking (if needed)
            addRankingOrder(endpointType, queryString,selectedFields.get(null),"");
            addLimit(query.getLimit()!=null?query.getLimit():0, queryString);
            addOffset(query, queryString);
        }
        return queryString.toString();
    }
    /**
     * Determines if the current query uses sub selects. Activated if the
     * SPARQL endpoint supports the SPARQL 1.1 sub select feature and the query
     * selects more than the enttiy id.
     * @param endpoint the used endpoint type
     * @param selectedFields the map with the selected fields
     * @return the state
     */
    private static boolean isSubSelectState(EndpointTypeEnum endpoint, Map<String,String> selectedFields){
        return endpoint.supportsSubSelect() && selectedFields.size() > 1;
    }

    /**
     * Creates a clone of the parsed query and applies the parsed limit and
     * additional fields
     * @param query the query
     * @param limit the limit (if &gt; 0)
     * @param additionalFields additional fields to select
     * @return a clone of the parsed query with the set limit and added fields
     */
    private static SparqlFieldQuery initLocalQuery(SparqlFieldQuery query,
                                                   int limit,
                                                   String... additionalFields) {
        query = query.clone();
        if(limit > 0){
            query.setLimit(limit);
        }
        //We need a copy to delete all fields that are already covered by some added
        //graph pattern.
        if(additionalFields!=null && additionalFields.length>0){
            query.addSelectedFields(Arrays.asList(additionalFields));
        }
        return query;
    }

    /**
     * Creates the CONSTRUCT part of the query including the
     * <code>entityhub-query:ieldQueryResultSet entityhub-query:fieldQueryResult ?representation </code>
     * triples that are used to build the iterator over the results
     * @param queryString The query to add the construct fields
     * @param selectedFields the field name 2 variable name mapping used by the
     *    query. This mapping MUST also contain the <code>null</code> key that
     *    is mapped to the variable name used for the representations to be selected
     */
    private static void createConstruct(final StringBuilder queryString, final Map<String, String> selectedFields) {
        queryString.append("CONSTRUCT { \n");
        String rootVar = selectedFields.get(null);//the null element has the root variable mapping
        for(Entry<String,String> mapping :selectedFields.entrySet()){
            if(mapping.getKey() != null){
                queryString.append("  ?").append(rootVar).append(" <");
                queryString.append(mapping.getKey()).append("> ?");
                queryString.append(mapping.getValue()).append(" .\n");
            }
        }
        //add the triples for the Representation type
        //add the triples that form the result set
        queryString.append("  <").append(RdfResourceEnum.QueryResultSet).append("> <");
        queryString.append(RdfResourceEnum.queryResult).append("> ?");
        queryString.append(rootVar).append(" . \n");

        queryString.append("} ");
    }
    /**
     * Creates the SPARQL representation of the parse field query.
     * @param query A field query implementation that additionally supports a field
     *    to variable mapping
     * @param endpointType The type of the Endpoint (used to write optimized
     *    queries for endpoint type specific extensions
     * @return the SPARQL query as String
     */
    public static String createSparqlSelectQuery(SparqlFieldQuery query,EndpointTypeEnum endpointType) {
        return createSparqlSelectQuery(query,true,-1,endpointType);
    }
    /**
     * Creates the SPARQL representation of the parse field query.
     * @param query A field query implementation that additionally supports a field
     *    to variable mapping
     * @param limit If > 0, than the limit parsed by the query is overriden by this
     *    value
     * @param endpointType The type of the Endpoint (used to write optimized
     *    queries for endpoint type specific extensions
     * @return the SPARQL query as String
     */
    public static String createSparqlSelectQuery(SparqlFieldQuery query,int limit,EndpointTypeEnum endpointType) {
        return createSparqlSelectQuery(query,true,limit,endpointType);
    }
    /**
     * Creates the SPARQL representation of the parse field query.
     * @param query A field query implementation that additionally supports a field
     *    to variable mapping
     * @param includeFields if <code>false</code> only the root is selected (selected fields are ignored)
     * @param endpointType The type of the Endpoint (used to write optimized
     *    queries for endpoint type specific extensions
     * @return the SPARQL query as String
     */
    public static String createSparqlSelectQuery(SparqlFieldQuery query,boolean includeFields, EndpointTypeEnum endpointType) {
        return createSparqlSelectQuery(query,includeFields,-1,endpointType);
    }
    /**
     * Creates the SPARQL representation of the parse field query.
     * @param query A field query implementation that additionally supports a field
     *    to variable mapping
     * @param includeFields if <code>false</code> only the root is selected (selected fields are ignored)
     * @param limit if > 0 than the limit defined by the query is overridden by
     *    the parsed value
     * @param endpointType The type of the Endpoint (used to write optimized
     *    queries for endpoint type specific extensions
     * @return the SPARQL query as String
     */
    public static String createSparqlSelectQuery(SparqlFieldQuery query,boolean includeFields,int limit, EndpointTypeEnum endpointType) {
        //1) INIT
        final StringBuilder queryString = new StringBuilder();
        query = initLocalQuery(query, limit);
        final Map<String,String> selectedFields = initSelectedFieldsMap(query);
        //2) SELECT
        createSelect(queryString, includeFields, selectedFields);
        //3) WHERE
        queryString.append("WHERE { \n");
        addFieldConstraint(queryString, query, selectedFields,endpointType);
        queryString.append("} \n");
        if(!isSubSelectState(endpointType, selectedFields)){
            //4) Add Stuff to rank results based on the "page rank" of entities
            addRankingOrder(endpointType, queryString,selectedFields.get(null),"");
            //5) Limit and Offset
            addLimit(query.getLimit()!=null?query.getLimit():0, queryString);
            addOffset(query, queryString);
        }
        return queryString.toString();
    }

    /**
     * Initialise the field -&gt; variable name mappings including the root
     * variable name by adding <code>null</code> as key
     * @param query the query
     * @return the mappings
     */
    private static Map<String,String> initSelectedFieldsMap(SparqlFieldQuery query) {
        //We need a copy to delete all fields that are already covered by some added
        //graph pattern.
        final Map<String,String> selectedFields = new HashMap<String, String>();
        selectedFields.putAll(query.getFieldVariableMappings());
        //also add the root variable
        selectedFields.put(null, query.getRootVariableName());
        return selectedFields;
    }

    /**
     * For some {@link EndpointTypeEnum SPARQL endpoint types} we need to add
     * an additional constraint to determine the ranking information based on
     * incomming relations to the Entities.<p>
     * Currently this done for {@link EndpointTypeEnum#ARQ ARQ} and
     * {@link EndpointTypeEnum#LARQ LARQ}. 
     * @param endpointType the endpoint type
     * @param queryString the SPARQL query string to add the ranking constraint
     * @param rootFieldName the variable name used to select entities
     */
    private static void addRankingConstraints(EndpointTypeEnum endpointType, final StringBuilder queryString, final String rootFieldName) {
        if(endpointType == EndpointTypeEnum.ARQ || endpointType == EndpointTypeEnum.LARQ){
            queryString.append(String.format("   { ?incoming ?p ?%s . } \n",rootFieldName));
        } //else ... for Virtuoso we need not count incoming links, because it has a
          //page rank like feature we can use to rank entities!
        //all others do not support sorting
    }
    /**
     * @param endpointType
     * @param queryString
     */
    private static void addRankingOrder(EndpointTypeEnum endpointType, final StringBuilder queryString,String rootVarName,String intend) {
        if(endpointType == EndpointTypeEnum.Virtuoso){
            //is that still SPARQL ... wondering about the syntax ^
            queryString.append(String.format("%sORDER BY DESC ( <LONG::IRI_RANK> (?%s) ) \n",
                intend!=null?intend:"",rootVarName));
        } else if(endpointType == EndpointTypeEnum.ARQ || endpointType == EndpointTypeEnum.LARQ){
            //TODO: COUNT is not part of the SPARQL 1.0 specification!
            // see http://www.w3.org/2009/sparql/wiki/Feature:AggregateFunctions
            queryString.append(String.format("%sORDER BY DESC (COUNT (?incoming) ) \n",
                intend!=null?intend:""));
        } //else not supported ... add nothing
    }

    /**
     * @param query
     * @param queryString
     */
    private static void addOffset(SparqlFieldQuery query, final StringBuilder queryString) {
        if(query.getOffset() > 0){
            queryString.append(String.format("OFFSET %d \n",query.getOffset()));
        }
    }

    /**
     * @param query
     * @param queryString
     */
    private static void addLimit(Integer limit, final StringBuilder queryString) {
        if(limit != null && limit > 0){
            queryString.append(String.format("LIMIT %d \n", limit));
        }
    }

    /**
     * Adds the SELECT part to the SPARQL query
     * @param queryString
     * @param query
     * @param includeFields
     * @param selectedFields
     */
    private static void createSelect(final StringBuilder queryString, boolean includeFields, final Map<String, String> selectedFields) {
        queryString.append("SELECT DISTINCT");
        // REMOVED: The root variable is already in the selected fields map!
        // queryString.append(" ?"+query.getRootVariableName()); //select the representation ID

        //now the variables for the selected fields!
        if(includeFields){
            for(String varName : selectedFields.values()){
                queryString.append(" ?");
                queryString.append(varName);
            }
        } else {
            //else add only the root variable (stored under key null)
            queryString.append(" ?");
            queryString.append(selectedFields.get(null));
        }
        queryString.append(" \n");
    }

    /**
     * Adds the WHERE clause of the SPARQL query. <p>
     * If the {@link EndpointTypeEnum SPARQL endpoint} supports SPARQL 1.1 
     * subqueries, than this adds also the LIMIT and OFFSET to in inner SELECT 
     * that only selects the id. 
     * @param queryString the SPARQL query string to add the WHERE
     * @param query the query
     * @param selectedFields the selected fields
     * @param endpointType The type of the endpoint (used to write optimised
     *    queries for endpoint type specific extensions
     */
    private static void addFieldConstraint(final StringBuilder queryString, SparqlFieldQuery query, Map<String, String> selectedFields,EndpointTypeEnum endpointType) {
        //we need temporary variables with unique names
        String varPrefix = "tmp";
        int[] varNum = new int []{1};
        //used to open brackets for the select part of the constraints
        boolean first = true;
        //determine if sub-selects are supported and if we need a sub-select
        //(more than the id is selected)
        boolean subSelectState = isSubSelectState(endpointType, selectedFields);
        //if we uses a sub query to select the ids, we need to add the graph pattern
        //of all selected fields outside of the sub query
        Map<String,String> tmpSelectedFields = subSelectState ?
                new HashMap<String,String>(selectedFields) :
                    null;
        String intend;
        if(subSelectState){
            intend = "      "; //additional intend because of sub query (3*2)
        } else {
            intend = "    "; //normal intend (2*2)
        }
        for(Entry<String,Constraint> fieldConstraint : query){
            if(first){
                queryString.append("  { \n");
                if(subSelectState){
                    String rootVarName = selectedFields.get(null);
                    queryString.append("    SELECT ?").append(rootVarName).append(" \n");
                    queryString.append("    WHERE { \n");
                }
                first = false;
            }
            String field = fieldConstraint.getKey();
            Constraint constraint = fieldConstraint.getValue();
            switch (constraint.getType()) {
            case value:
                addValueConstraint(queryString,field, (ValueConstraint)constraint,selectedFields, varPrefix,varNum,intend);
                break;
            case text:
                String var = addFieldGraphPattern(queryString, field, selectedFields, varPrefix, varNum,intend);
                addTextConstraint(queryString, var, (TextConstraint)constraint,endpointType,intend);
                break;
            case range:
                var = addFieldGraphPattern(queryString, field, selectedFields, varPrefix, varNum,intend);
                addRangeConstriant(queryString,var,(RangeConstraint)constraint,intend);
                break;
            default:
                log.warn("Please update this Implementation to support the Constraint Type "+fieldConstraint.getValue().getType());
                break;
            }
            queryString.append(" . \n");
        }
        //for some endpoints we need to add an additional constraints used for
        //ranking. If sub-queries are used this need to be in the select part
        //of the query (to rank results of the inner query)
        //otherwise it is better to have it in outside if the select part to only
        //rank the graph selected by the query
        if(subSelectState){
            addRankingConstraints(endpointType, queryString, selectedFields.get(null));
        }
        if(!first){
            if(subSelectState){
                queryString.append("    } \n");
                //re-add all selected fields to be added as selects because in
                //the sub-query we only select the ID!
                selectedFields = tmpSelectedFields;
                //ranking needs also to be added to the sub-query (to correctly
                //process LIMIT and OFFSET
                addRankingOrder(endpointType, queryString,selectedFields.get(null),"    ");
                //add LIMIT and OFFSET to the sub-query!
                //TODO: add link to the email
                queryString.append("    ");
                addLimit(query.getLimit(), queryString);
                queryString.append("    ");
                addOffset(query, queryString);
                queryString.append("    ");
            }
            queryString.append("} \n");
        }
        //All the followig Graphpattern are only processed for the parts selected
        //by the above constraints
        //if no subqueries are used we need now to add the ranking constraints
        if(!subSelectState){
            addRankingConstraints(endpointType, queryString, selectedFields.get(null));
        }
        //we need to add graph pattern for selected field that are not covered by
        //graph pattern written for the constraint.
        //Implementation Note: selectedFields contains the null key for the root variable
        while(selectedFields.size()>1){ //if this is the only left element we are done
            Iterator<String> it = selectedFields.keySet().iterator();
            String actField; //we need to get a non null value from the map
            do {
                //the outer while ensures an non null value so we need not to use hasNext
                actField = it.next();
            } while(actField == null);
            queryString.append("  OPTIONAL { ");
            // NOTE the following Method removes the written mapping from the Map
            addFieldGraphPattern(queryString, actField, selectedFields, varPrefix, varNum,"");
            queryString.append(". } \n");
        }
    }

    private static void addValueConstraint(StringBuilder queryString,String field,ValueConstraint constraint,Map<String, String> selectedFields,String varPrefix,int[] varNum,String intend){
        String rootVarName = selectedFields.get(null);
        Collection<String> dataTypes = constraint.getDataTypes();
        if(dataTypes == null){
            //we need not to distinguish between empty and null
            //because SPARQL includes support for automatic detection of dataTypes
            //see http://www.w3.org/TR/rdf-sparql-query/#QSynLiterals
            dataTypes = Collections.emptySet();
        }
        if(constraint.getValue() != null){
            if(dataTypes.size()<=1){
                addDataTypeValueConstraint(queryString, rootVarName, field,
                        dataTypes.isEmpty()?null:dataTypes.iterator().next(),
                                constraint.getValue(),intend);
            } else { //we have multiple dataTypes -> ned to use union!
                boolean first = true;
                for(Iterator<String> it = dataTypes.iterator();it.hasNext();){
                    String dataType = it.next();
                    if(first){
                        queryString.append(intend);
                        first = false;
                    } else {
                        queryString.append(" UNION \n  ").append(intend);
                    }
                    addDataTypeValueConstraint(queryString, rootVarName, field, dataType, constraint.getValue(),"");
                }
                //queryString.append('}');
            }
        } else { // no constraint for the value
            // filter all instances that define any value for the given dataTypes
            // see http://www.w3.org/TR/rdf-sparql-query/#func-datatype
            //first we need to select the Variable to filter
            String var = addFieldGraphPattern(queryString, field, selectedFields, varPrefix, varNum,intend);
            queryString.append(". \n").append(intend);
            //now we need to write the filter
            if(dataTypes.size()==1){
                addDataTypeFilter(queryString, var, dataTypes.iterator().next());
            } else {
                boolean first = true;
                for(Iterator<String> it = dataTypes.iterator();it.hasNext();) {
                    String dataType = it.next();
                    if(first){
                        queryString.append("( \n  ").append(intend);
                        first = false;
                    } else{
                        queryString.append(" || \n  ").append(intend);
                    }
                    addDataTypeFilter(queryString, var, dataType);
                }
                queryString.append(" \n").append(intend).append(")");
            }
        }
    }

    /**
     * Adds a filter that restricts the data type to an variable
     * @param queryString the query String to add the filter. MUST NOT be <code>null</code>
     * @param var the variable to add the filter. MUST NOT be <code>null</code>
     * @param dataTypes the data type uri for the filter. MUST NOT be <code>null</code>
     */
    private static void addDataTypeFilter(StringBuilder queryString, String var, String dataType) {
        queryString.append(String.format("FILTER(datatype(?%s) = <%s>)",
                var,dataType));
    }

    /**
     * Adds a value constraint for a field including the dataType
     * @param queryString the query string to add the constraint.  MUST NOT be <code>null</code>
     * @param rootVarName the variable name of the subject.  MUST NOT be <code>null</code>
     * @param field the property name of the field. MUST NOT be <code>null</code>
     * @param dataType the dataType constraint or <code>null</code> if none
     * @param value the value. MUST NOT be <code>null</code>.
     */
    private static void addDataTypeValueConstraint(StringBuilder queryString, String rootVarName, String field, String dataType, Object value, String intend) {
        if(DataTypeEnum.Reference.getUri().equals(dataType) ||
                value instanceof Reference){
            queryString.append(String.format("%s?%s <%s> <%s>", 
                intend,rootVarName,field,value));
        } else {
            queryString.append(String.format("%s?%s <%s> \"%s\"%s",
                    intend, rootVarName,field,value,
                    dataType!=null?String.format("^^<%s>",dataType):""));
        }
    }
    /**
     * Adds an text constraint to the SPARQL query string
     * @param queryString the query string to add the constraint
     * @param var the variable name to constrain
     * @param constraint the constraint
     * @param endpointType The type of the Endpoint (used to write optimized
     *    queries for endpoint type specific extensions
     */
    private static void addTextConstraint(StringBuilder queryString,String var,TextConstraint constraint,EndpointTypeEnum endpointType,String intend){
        boolean filterAdded = false;
        boolean isTextValueConstraint = constraint.getTexts() != null && !constraint.getTexts().isEmpty();
        if(isTextValueConstraint){
            if(constraint.getPatternType() == PatternType.regex){
                queryString.append(" \n").append(intend).append("  FILTER(");
                filterAdded = true;
                addRegexFilter(queryString,var,constraint.getTexts(),constraint.isCaseSensitive());
            } else {
                //TODO: This optimised versions for Virtuoso and LARQ might not
                //      respect case sensitive queries. Need more testing!
                if (EndpointTypeEnum.Virtuoso == endpointType) {
                    queryString.append(". \n  ").append(intend);
                    queryString.append(String.format("?%s bif:contains '%s'",
                        var,createFullTextQueryString(constraint.getTexts())));
                } else if (EndpointTypeEnum.LARQ == endpointType) {
                    queryString.append(". \n  ").append(intend);
                    queryString.append(String.format("?%s <http://jena.hpl.hp.com/ARQ/property#textMatch> '%s'", 
                        var, createFullTextQueryString(constraint.getTexts())));
                } else {
                    queryString.append(" \n").append(intend).append("  FILTER(");
                    filterAdded = true;
                    if(constraint.getPatternType() == PatternType.none){
                        if(constraint.isCaseSensitive()){
                            boolean first = true;
                            for(String textConstraint : constraint.getTexts()){
                                if(first){
                                    first = false;
                                } else {
                                    queryString.append(" || ");
                                }
                                if(textConstraint != null && !textConstraint.isEmpty()){
                                    queryString.append(String.format("(str(?%s) = \"%s\")", var,textConstraint));
                                }
                            }
                        } else {
                            Collection<String> regexQueryTexts = new ArrayList<String>(constraint.getTexts().size());
                            for(String textConstraint : constraint.getTexts()){
                                if(textConstraint != null && !textConstraint.isEmpty()){
                                    regexQueryTexts.add(PatternUtils.value2Regex(textConstraint));
                                }
                            }
                            addRegexFilter(queryString,var,regexQueryTexts,constraint.isCaseSensitive());
                        }
                    } else if(constraint.getPatternType() == PatternType.wildcard){
                        //parse false, because that is more in line with the expectations of users!
                        Collection<String> regexQueryTexts = new ArrayList<String>(constraint.getTexts().size());
                        for(String textConstraint : constraint.getTexts()){
                            if(textConstraint != null && !textConstraint.isEmpty()){
                                regexQueryTexts.add(PatternUtils.wildcardToRegex(textConstraint,false));
                            }
                        }
                        addRegexFilter(queryString,var,regexQueryTexts,constraint.isCaseSensitive());
                    } else {
                        log.warn("Unspported Patterntype "+constraint.getPatternType()+"! Change this impplementation to support this type! -> treat constaint \""+constraint.getTexts()+"\"as REGEX");
                        addRegexFilter(queryString,var,constraint.getTexts(),constraint.isCaseSensitive());
                    }
                }
            }
        } //else nothing to do
        //add language Filters
        //TODO check if FILTER ( is already written!
        if(constraint.getLanguages() != null && !constraint.getLanguages().isEmpty()){
            if(!filterAdded){
                queryString.append(" . \n").append(intend).append("  FILTER(");
                filterAdded = true;
                writeLanguagesFilter(queryString, constraint.getLanguages(), var,null);
            } else {
                writeLanguagesFilter(queryString, constraint.getLanguages(), var," && ");
            }
        }
        if(filterAdded){
            queryString.append(")"); //close the FILTER and the graph pattern
        }
    }

    /**
     * (Creates AND Text) OR (Query AND String) like queries based on the
     * parsed TextConstraint as used by {@link EndpointTypeEnum#LARQ LARQ} and
     * {@link EndpointTypeEnum#Virtuoso VIRTUOSO} SPARQL endpoints to speed up
     * full text queries.
     * @param constraints the as returned by {@link TextConstraint#getTexts()}
     * @return the full text query string 
     */
    protected static String createFullTextQueryString(Collection<String> constraints) {
        StringBuilder textQuery = new StringBuilder();
        boolean firstText = true;
        for(String constraintText : constraints){
            if(constraintText != null && !constraintText.isEmpty()){
                if(firstText){
                    firstText = false;
                } else {
                    textQuery.append(" OR ");
                }
                //TODO: maybe we should use a word tokenizer here
                String[] words = constraintText.split("\\s");
                if(words.length>1){
                    //not perfect because words might contain empty string, but
                    //it will eliminate most unnecessary brackets .
                    textQuery.append('(');
                }
                boolean firstAndWord = true;
                for(String word : words){
                    word = word.trim();
                    boolean hasAlphaNumeric = false;
                    for(int i = 0; i < word.length() && !hasAlphaNumeric;i++){
                        char ch = word.charAt(i);
                        if(Character.isLetter(ch) || Character.isDigit(ch)){
                            hasAlphaNumeric = true;
                        }
                    }
                    if(hasAlphaNumeric){
                        if(firstAndWord){
                            firstAndWord = false;
                        } else {
                            textQuery.append(" AND ");
                        }
                        // we need to double the backslashes because of replaceAll takes a regular expression
                        // as input.
                        String escapedWord = word.replaceAll("\\\"", "\\\\\"");
                        textQuery.append('"').append(escapedWord).append('"');
                    }
                }
                if (words.length > 1) {
                    textQuery.append(')');
                }
            } //end if not null and not empty
        }
        return textQuery.toString();
    }

    /**
     * Adds a SPARQL regex filter to the parsed query string
     * @param queryString the string builder to add the constraint
     * @param var the variable to constrain
     * @param regexContraints the regex encoded search strings (connected with '||' (OR))
     * @param isCasesensitive if the constraint is case sensitive or not
     */
    private static void addRegexFilter(StringBuilder queryString, String var, Collection<String> regexContraints,boolean isCasesensitive) {
        boolean first = true;
        for(String regex : regexContraints){
            if(regex != null && !regex.isEmpty()){
                if(first){
                    first = false;
                } else {
                    queryString.append(" || ");
                }
                queryString.append(String.format("regex(str(?%s),\"%s\"%s)", var,regex,isCasesensitive?"":",\"i\""));
            }
        }
    }

    /**
     * Adds an RangeConstraint to the parsed query String
     * @param queryString the query to add the constraint
     * @param var the variable to constrain
     * @param constraint the constraint
     */
    private static void addRangeConstriant(StringBuilder queryString, String var, RangeConstraint constraint,String intend) {
        queryString.append("\n").append(intend).append("FILTER "); //start the FILTER
        boolean closedRange = constraint.getLowerBound() != null && constraint.getUpperBound() != null;
        if(closedRange){
            queryString.append("(");
        }
        //write lower and upper bounds
        if(constraint.getLowerBound()!= null){
            addRangeBound(queryString, var, true, constraint.isInclusive(), constraint.getLowerBound());
        }
        if(closedRange){
            queryString.append(" && ");
        }
        if(constraint.getUpperBound() != null){
            addRangeBound(queryString, var, false, constraint.isInclusive(), constraint.getUpperBound());
        }
        if(closedRange){
            queryString.append(")");
        }
    }

    /**
     * Adds a lower/upper bound constraint to the query String
     * @param queryString the query string
     * @param var the variable
     * @param lowerBound <code>true</code> to add the lower bound and
     *                   <code>false</code> to add the upper bound
     * @param inclusive if the bound is inclusive (>= or <=)
     * @param value the value representing the bound.
     */
    private static void addRangeBound(StringBuilder queryString, String var, boolean lowerBound, boolean inclusive, Object value) {
        String stringValue;
        if(value instanceof Date){//for dates add the data type!
            stringValue = String.format("%s^^<%s>", DATE_FORMAT.format((Date)value),XSD_DATE_TIME);
        } else { //add additional  "if" for special types if necessary
            stringValue =value.toString();
        }
        //adds (?var >/<[=] valueString)
        queryString.append(String.format("(?%s %c%s %s)", var,lowerBound?'>':'<',inclusive?"=":"",stringValue));
    }

    /**
     * Adds a S P O pattern to the query by using the root as subject, the parsed
     * field as predicate and the returned variable as object. This method doese
     * not open a '{' nor close the pattern with any of '.', ',' or ';'
     * @param queryString the {@link StringBuilder} to add the pattern
     * @param field the field
     * @param selectedFields the map field -> var of the selected variables. If
     *    the parsed field is selected, the field is removed from the list and
     *    the mapped variable name is returned
     * @param varPrefix the default prefix for newly created variable names
     * @param varNum The first element of the array is used to get the number of
     *    the created variable. If one is created the value of the first element is
     *    increased by one
     * @return The variable name used for the object of the pattern
     */
    private static String addFieldGraphPattern(StringBuilder queryString, String field, Map<String, String> selectedFields, String varPrefix, int[] varNum,String intend) {
        String var = selectedFields.remove(field); //check if the field is selected
        if(var == null){ //this field is not selected
            //we need to generate a temp var
            var = varPrefix+varNum[0];
            varNum[0]++;
        }
        queryString.append(String.format("%s?%s <%s> ?%s ", 
            intend,selectedFields.get(null),field,var));
        return var;
    }

    /**
     * Writes the SPARQL FILTER for the parsed languages.
     * This Method writes
     * <code><pre>
     *      prefix ((lang(?var) = "lang1") [|| (lang(?var) = "lang2..n")])
     * </pre></code>
     * @param queryString the query string to add the FILTER
     * @param languages the languages to filter for (may contain <code>null</code> as element)
     * @param var the name of the variable to filter.
     * @param prefix The prefix is written in front of the filter expression (if any is created).
     *        Typically this will be <ul>
     *        <li> FILTER if this is the only filter for an variable
     *        <li> && if this filter is combined with AND to an other filter or
     *        <li> || if this filter is combined wit OR to an other filter
     *        </ul>
     */
    private static void writeLanguagesFilter(StringBuilder queryString, Collection<String> languages, String var, String prefix) {
        if(languages != null && !languages.isEmpty()){
            if(prefix != null){
                queryString.append(prefix);
            }
            if(languages.size()>1){
                queryString.append("(");
            }
            boolean first = true;
            for(String language : languages){
                if(first){
                    first = false;
                } else {
                    queryString.append(" || ");
                }
                queryString.append(String.format("(lang(?%s) = \"%s\")", var,language!=null?language:""));
                /*
                 * NOTE: the lang() returns "" for literals without an language
                 *       tag. Because of that if the language == null we need
                 *       to parse "" as an argument
                 */
            }
            if(languages.size()>1){
                queryString.append(")");
            }
        }
    }


    public static void main(String[] args) {
        SparqlFieldQuery query = SparqlFieldQueryFactory.getInstance().createFieldQuery();
//        query.setConstraint("urn:field1", new ReferenceConstraint("urn:testReference"));
//        query.setConstraint("urn:field1a", new ValueConstraint(null, Arrays.asList(
//                DataTypeEnum.Float.getUri())));
//        query.addSelectedField("urn:field1a");
//        query.setConstraint("urn:field1b", new ValueConstraint(9, Arrays.asList(
//                DataTypeEnum.Float.getUri())));
//        query.setConstraint("urn:field1c", new ValueConstraint(null, Arrays.asList(
//                DataTypeEnum.Float.getUri(),DataTypeEnum.Double.getUri(),DataTypeEnum.Decimal.getUri())));
//        query.addSelectedField("urn:field1c");
//        query.setConstraint("urn:field1d", new ValueConstraint(9, Arrays.asList(
//                DataTypeEnum.Float.getUri(),DataTypeEnum.Double.getUri(),DataTypeEnum.Decimal.getUri())));
//        query.setConstraint("urn:field2", new TextConstraint("test value"));
        query.setConstraint("urn:field3", new TextConstraint(Arrays.asList(
            "text value","anothertest","some more values"),true));
//        query.setConstraint("urn:field2a", new TextConstraint(":-]")); //tests escaping of REGEX
//        query.setConstraint("urn:field3", new TextConstraint("language text","en"));
//        query.setConstraint("urn:field4", new TextConstraint("multi language text","en","de",null));
//        query.setConstraint("urn:field5", new TextConstraint("wildcar*",PatternType.wildcard,false,"en","de"));
//        query.addSelectedField("urn:field5");
//        query.setConstraint("urn:field6", new TextConstraint("^regex",PatternType.REGEX,true));
//        query.setConstraint("urn:field7", new TextConstraint("par*",PatternType.WildCard,false,"en","de",null));
//        query.setConstraint("urn:field8", new TextConstraint(null,"en","de",null));
//        query.setConstraint("urn:field9", new RangeConstraint((int)5, (int)10, true));
//        query.setConstraint("urn:field10", new RangeConstraint((int)5, (int)10, false));
//        query.setConstraint("urn:field11", new RangeConstraint(null, (int)10, true));
//        query.setConstraint("urn:field12", new RangeConstraint((int)5, null, true));
//        query.setConstraint("urn:field12", new RangeConstraint(new Date(), null, true));
        query.addSelectedField("urn:field2a");
        query.addSelectedField("urn:field3");
        query.setLimit(5);
        query.setOffset(5);
        System.out.println(createSparqlSelectQuery(query,true,0,EndpointTypeEnum.LARQ));
        System.out.println();
        System.out.println(createSparqlSelectQuery(query,true,0,EndpointTypeEnum.Virtuoso));
        System.out.println();
        System.out.println(createSparqlSelectQuery(query,true,0,EndpointTypeEnum.Standard));
        System.out.println();
        System.out.println(createSparqlConstructQuery(query,0,EndpointTypeEnum.Virtuoso));
    }

    /**
     * @param query
     * @param resultGraph
     * @return
     */
    public static Iterator<RdfRepresentation> parseQueryResultsFromMGraph(final TripleCollection resultGraph) {
        Iterator<Triple> resultTripleIterator = resultGraph.filter(FIELD_QUERY_RESULT_SET,FIELD_QUERY_RESULT,null);
        Iterator<RdfRepresentation> resultIterator = new AdaptingIterator<Triple, RdfRepresentation>(
                resultTripleIterator,new AdaptingIterator.Adapter<Triple, RdfRepresentation>() {
                    /*
                     * Anonymous implementation of an Adapter that converts the filtered
                     * Triples of the resulting graph to RdfRepresentations
                     */
                    @Override
                    public RdfRepresentation adapt(Triple value, Class<RdfRepresentation> type) {
                        Resource object = value.getObject();
                        if(object == null){
                            return null;
                        } else if(object instanceof UriRef){
                            return valueFavtory.createRdfRepresentation((UriRef)object, resultGraph);
                        } else {
                            log.warn("Unable to create representation for FieldQueryResult "+object+" because this Resource is not of Type UriRef (type: "+object.getClass()+") -> result gets ignored");
                            return null;
                        }
                    }
                },RdfRepresentation.class);
        return resultIterator;
    }

}
