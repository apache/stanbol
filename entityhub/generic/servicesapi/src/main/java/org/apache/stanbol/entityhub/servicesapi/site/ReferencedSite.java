package org.apache.stanbol.entityhub.servicesapi.site;

import java.io.InputStream;

import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;

public interface ReferencedSite extends ConfiguredSite {

    /**
     * Searches for entities based on the parsed {@link FieldQuery} and returns
     * the references (ids). Note that selected fields of the query are ignored.
     * @param query the query
     * @return the references of the found entities
     * @throws ReferencedSiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    QueryResultList<String> findReferences(FieldQuery query) throws ReferencedSiteException;
    /**
     * Searches for entities based on the parsed {@link FieldQuery} and returns
     * representations as defined by the selected fields of the query. Note that
     * if the query defines also {@link Constraint}s for selected fields, that
     * the returned representation will only contain values selected by such
     * constraints.
     * @param query the query
     * @return the found entities as representation containing only the selected
     * fields and there values.
     * @throws ReferencedSiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    QueryResultList<Representation> find(FieldQuery query) throws ReferencedSiteException;
    /**
     * Searches for Signs based on the parsed {@link FieldQuery} and returns
     * the selected Signs including the whole representation. Note that selected
     * fields of the query are ignored.
     * @param query the query
     * @return All Entities selected by the Query.
     * @throws ReferencedSiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    QueryResultList<Sign> findSigns(FieldQuery query) throws ReferencedSiteException;

    /**
     * Getter for the Sign by the id
     * @param id the id of the entity
     * @return the entity or <code>null</code> if not found
     * @throws ReferencedSiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    Sign getSign(String id) throws ReferencedSiteException;
    /**
     * Getter for the Content of the Entity
     * @param id the id of the Entity
     * @param contentType the requested contentType
     * @return the content or <code>null</code> if no entity with the parsed id
     * was found or the parsed ContentType is not supported for this Entity
     * @throws ReferencedSiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    InputStream getContent(String id,String contentType) throws ReferencedSiteException;
    /**
     * Getter for the FieldMappings configured for this Site
     * @return The {@link FieldMapping} present for this Site.
     */
    FieldMapper getFieldMapper();
    /**
     * Getter for the QueryFactory implementation preferable used with this Site.
     * Note that Site MUST support query instances regardless of there specific
     * implementation. However specific implementations might have performance
     * advantages for query processing and may be even execution. Therefore
     * if one creates queries that are specifically executed on this specific
     * site, that it is best practice to use the instance provided by this
     * method.
     * @return The query factory of this site.
     */
    FieldQueryFactory getQueryFactory();

}
