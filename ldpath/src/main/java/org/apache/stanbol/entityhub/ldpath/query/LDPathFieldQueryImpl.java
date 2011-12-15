package org.apache.stanbol.entityhub.ldpath.query;

import org.apache.stanbol.entityhub.core.query.FieldQueryImpl;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;

/**
 * FieldQuery implementation that also implements  {@link LDPathSelect}
 * @author Rupert Westenthaler
 *
 */
public class LDPathFieldQueryImpl extends FieldQueryImpl implements FieldQuery, LDPathSelect {

        
    private String ldpath;

    public void setLDPathSelect(String ldpath) {
        this.ldpath = ldpath;
    }
    
    @Override
    public String getLDPathSelect() {
        return ldpath;
    }

}
