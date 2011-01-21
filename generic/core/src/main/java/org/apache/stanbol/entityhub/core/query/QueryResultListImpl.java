package org.apache.stanbol.entityhub.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;

public class QueryResultListImpl<T> implements Iterable<T>, QueryResultList<T>{


    private final Collection<T> results;
    private final FieldQuery query;
    private Class<T> type;

    /**
     * Constructs an QueryResultList by iterating over all elements in the parsed
     * {@link Iterator} and storing all elements that are NOT <code>null</code>.
     * @param query The query uses to select the results
     * @param resultIterator The Iterator containing the results of the Query
     * @throws IllegalArgumentException if the parsed {@link FieldQuery} is <code>null</code>
     */
    public QueryResultListImpl(FieldQuery query,Iterator<T> resultIterator,Class<T> type) throws IllegalArgumentException {
//        if(query == null){
//            throw new IllegalArgumentException("Query MUST NOT be NULL");
//        }
        this.query = query;
        if(type == null){
            throw new IllegalArgumentException("The type of the results MUST NOT be NULL");
        }
        this.type = type;
        if(resultIterator == null || !resultIterator.hasNext()){
            this.results = Collections.emptyList();
        } else {
            List<T> results = new ArrayList<T>();
            while(resultIterator.hasNext()){
                results.add(resultIterator.next());
            }
            this.results = Collections.unmodifiableList(results);
        }
    }
    @Override
    public Class<T> getType(){
        return type;
    }
    /**
     * Constructs an QueryResultList with the parsed Query and Results
     * @param query The query uses to select the results
     * @param results The results of the query
     * @throws IllegalArgumentException if the parsed {@link FieldQuery} is <code>null</code>
     */
    public QueryResultListImpl(FieldQuery query,Collection<T> results,Class<T> type)
            throws IllegalArgumentException {
//        if(query == null){
//            throw new IllegalArgumentException("Query MUST NOT be NULL");
//        }
        this.query = query;
        if(type == null){
            throw new IllegalArgumentException("The type of the results MUST NOT be NULL");
        }
        this.type = type;
        if(results == null){
            this.results = Collections.emptyList();
        } else {
            this.results = Collections.unmodifiableCollection(results);
        }
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.core.query.ResultList#getQuery()
     */
    public FieldQuery getQuery(){
        return query;
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.core.query.ResultList#getSelectedFields()
     */
    public Set<String> getSelectedFields(){
        return query.getSelectedFields();
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.core.query.ResultList#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return results.iterator();
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.core.query.ResultList#isEmpty()
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.core.query.ResultList#size()
     */
    public int size() {
        return results.size(); //not supported :(
    }
}
