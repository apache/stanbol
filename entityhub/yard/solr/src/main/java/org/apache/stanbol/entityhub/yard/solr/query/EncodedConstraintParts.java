package org.apache.stanbol.entityhub.yard.solr.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * This class is used to store the encoded parts of the index field constraints
 * created by the {@link IndexConstraintTypeEncoder}.<p>
 * The processing of this parts is specific to the used Index, therefore such
 * processing is not implemented by this Class.<p>
 * Constraints parts can be encoded in the following part of an index field:
 * <code><per>
 *    &lt;prefix&gt;.field.&lt;prefix&gt;&lt;assignment&gt;&lt;value&gt;
 * </pre></code>
 * The: <ul>
 * <li> <code>prefix</code> is used for the data type and language constraints
 * <li> <code>field</code> is predefined by by the field of the constraint
 * <li> <code>suffix</code> is currently unused
 * <li> <code>assignment</code> is used for checking static values or just adding
 *      ':' in the case that values of that field are filtered.
 * <li> <code>value</code> is used to define filters like value ranges or
 *      wildcard searches.
 * </ul>
 * The {@link ConstraintTypePosition} defines such position in ordinal numbers
 * from left to right. This ordinal numbers are also used sort the elements
 * of the {@link Iterable} interface implemented by this class.
 *
 * @author Rupert Westenthaler
 *
 */
public class EncodedConstraintParts implements Iterable<Entry<ConstraintTypePosition,Set<String>>>{
    /**
     * This maps contains all the encoded parts of the query.
     */
    private SortedMap<ConstraintTypePosition,Set<String>> constraintMap = new TreeMap<ConstraintTypePosition, Set<String>>();
    /**
     * Adds an constraint type
     * @param pos
     * @param values
     */
    public void addEncoded(ConstraintTypePosition pos,String...values){
        if(values == null || values.length<1){
            return;
        } else {
            Set<String> constraints = constraintMap.get(pos);
            if(constraints == null){
                constraints = new HashSet<String>();
                constraintMap.put(pos, constraints);
            }
            constraints.addAll(Arrays.asList(values));
        }
    }

    @Override
    public String toString() {
        return "Encoded Constraints: "+constraintMap.toString();
    }

    @Override
    public Iterator<Entry<ConstraintTypePosition, Set<String>>> iterator() {
        return constraintMap.entrySet().iterator();
    }
    @Override
    public int hashCode() {
        return constraintMap.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof EncodedConstraintParts && ((EncodedConstraintParts)obj).constraintMap.equals(constraintMap);
    }
}
