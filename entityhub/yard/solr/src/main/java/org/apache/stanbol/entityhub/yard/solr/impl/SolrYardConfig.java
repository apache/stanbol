package org.apache.stanbol.entityhub.yard.solr.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.osgi.service.cm.ConfigurationException;


/**
 * Used for the configuration of a SolrYard. Especially if the SolrYard is
 * not running within an OSGI context, than an instance of this class must
 * be configured and than parsed to the constructor of {@link SolrYard}.<p>
 * When running within an OSGI context, the configuration is provided by the
 * OSGI environment. I that case this class is used as a wrapper for easy
 * access to the configuration.
 * @author Rupert Westenthaler
 *
 */
public final class SolrYardConfig extends YardConfig {

    /**
     * Creates a new config with the minimal set of required properties
     * @param id the ID of the Yard
     * @param solrServer the base URL of the {@link SolrServer}
     * @throws IllegalArgumentException if the parsed valued do not fulfil the
     * requirements.
     */
    public SolrYardConfig(String id, URL solrServer) throws IllegalArgumentException{
        super(id);
        setSolrServerUrl(solrServer);
        try {
            isValid();
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage(),e);
        }
    }
    /**
     * Initialise the Yard configuration based on a parsed configuration. Usually
     * used on the context of an OSGI environment in the activate method.
     * @param config the configuration usually parsed within an OSGI activate
     * method
     * @throws ConfigurationException if the configuration is incomplete of
     * some values are not valid
     * @throws IllegalArgumentException if <code>null</code> is parsed as
     * configuration
     */
    protected SolrYardConfig(Dictionary<String, Object> config) throws IllegalArgumentException, ConfigurationException {
        super(config);
    }
    /**
     * Setter for the base URL of the SolrServer
     * @param url the base URL of the SolrServer. Required, NOT NULL.
     */
    public void setSolrServerUrl(URL url){
        if(url != null) {
            config.put(SolrYard.SOLR_SERVER_URI, url);
        } else {
            config.remove(SolrYard.SOLR_SERVER_URI);
        }
    }
    /**
     * Getter for the base URL of the SolrServer
     * @return the base URL of the configured SolrServer
     * @throws IllegalStateException Internally a Object is used to store values
     *   and if the configuration is provided by the OSGI environment the URL
     *   is configured by using a {@link String}. When it fails to parse a
     *   {@link URL} object based on the provided string, than the
     *   {@link MalformedURLException} is wrapped by
     *   an {@link IllegalStateException}.
     */
    public URL getSolrServerUrl() throws IllegalStateException {
        Object value = config.get(SolrYard.SOLR_SERVER_URI);
        if(value != null){
            if(value instanceof URL){
                return (URL)value;
            } else {
                try {
                    return new URL(value.toString());
                } catch (MalformedURLException e) {
                    throw new IllegalStateException("Unable to parse URL from value "+value,e);
                }
            }
        } else {
            return null;
        }
    }
    /**
     * Setter for the multi yard index layout state.<p>
     * The multi layout state. If data of multiple yards are stored in the same
     * Solr Index, than the YardID MUST be stored within all indexed documents.
     * In addition the to all queries a fq (filterQuery) must be added that
     * restricts results to the current yard
     */
    public void setMultiYardIndexLayout(Boolean multiYardIndexLayoutState){
        if(multiYardIndexLayoutState != null){
            config.put(SolrYard.MULTI_YARD_INDEX_LAYOUT, multiYardIndexLayoutState);
        } else {
            config.remove(SolrYard.MULTI_YARD_INDEX_LAYOUT);
        }
    }
    /**
     * Getter for the multi yard index layout state.<p>
     * If data of multiple yards are stored in the same
     * Solr Index, than the YardID MUST be stored within all indexed documents.
     * In addition the to all queries a fq (filterQuery) must be added that
     * restricts results to the current yard.<p>
     * The default value is <code>false</code>
     * @return the multi yard index layout state
     */
    public boolean isMultiYardIndexLayout() {
        Object value = config.get(SolrYard.MULTI_YARD_INDEX_LAYOUT);
        if(value != null){
            if(value instanceof Boolean){
                return (Boolean) value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else {
            return false;
        }
    }
    /**
     * Getter for the maximum number of boolean clauses allowed for queries
     * @return The configured number of <code>null</code> if not configured or
     * the configured value is not an valid Integer.
     */
    public Integer getMaxBooleanClauses(){
        Object value = config.get(SolrYard.MAX_BOOLEAN_CLAUSES);
        if(value != null){
            if(value instanceof Integer){
                return (Integer)value;
            } else {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    public void setMaxBooleanClauses(Integer integer){
        if( integer == null || integer.intValue()<=0){
            config.remove(SolrYard.MAX_BOOLEAN_CLAUSES);
        } else {
            config.put(SolrYard.MAX_BOOLEAN_CLAUSES, integer);
        }
    }
    public void setDocumentBoostFieldName(String fieldName){
        if(fieldName == null || fieldName.isEmpty()){
            config.remove(SolrYard.DOCUMENT_BOOST_FIELD);
        } else {
            config.put(SolrYard.DOCUMENT_BOOST_FIELD, fieldName);
        }
    }
    public String getDocumentBoostFieldName(){
        Object name = config.get(SolrYard.DOCUMENT_BOOST_FIELD);
        return name == null?null:name.toString();
    }
    public void setFieldBoosts(Map<String,Float> fieldBoosts){
        if(fieldBoosts != null){
            config.put(SolrYard.FIELD_BOOST_MAPPINGS, fieldBoosts);
        } else {
            config.remove(SolrYard.FIELD_BOOST_MAPPINGS);
        }
    }
    public Map<String,Float> getFieldBoosts(){
        Object fieldBoosts = config.get(SolrYard.FIELD_BOOST_MAPPINGS);
        if(fieldBoosts == null){
            return null;
        } else if(fieldBoosts instanceof Map<?, ?>){
            return (Map<String,Float>)fieldBoosts;
        } else {
            //TODO: add support for parsing from String[] and Collection<String>
            return null;
        }
    }
    /**
     * checks for the {@link SolrYard#SOLR_SERVER_URI}
     */
    @Override
    protected void validateConfig() throws ConfigurationException{
        try {
            URL solrServer = getSolrServerUrl();
            if(solrServer == null){
                throw new ConfigurationException(SolrYard.SOLR_SERVER_URI, "The URL of the Solr server MUST NOT be NULL!");
            }
        } catch (IllegalStateException e) {
            throw new ConfigurationException(SolrYard.SOLR_SERVER_URI, e.getMessage(),e.getCause());
        }

    }
}
