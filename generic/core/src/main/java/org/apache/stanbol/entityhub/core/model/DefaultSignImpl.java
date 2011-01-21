package org.apache.stanbol.entityhub.core.model;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSignImpl implements Sign{

    Logger log = LoggerFactory.getLogger(DefaultSignImpl.class);

    protected final Representation representation;
    protected final String TYPE = RdfResourceEnum.signType.getUri();
    protected final String signSite;

//    public DefaultSignImpl(Representation representation) {
//        if(representation == null){
//            throw new IllegalArgumentException("NULL value ist not allowed for the Representation");
//        }
//        if(representation.getFirstReference(SIGN_SITE) == null){
//            throw new IllegalStateException("Parsed Representation does not define the required field"+SIGN_SITE+"!");
//        }
//        this.representation = representation;
//    }
    public DefaultSignImpl(String siteId,Representation representation) {
        if(representation == null){
            throw new IllegalArgumentException("NULL value ist not allowed for the Representation");
        }
        if(siteId == null || siteId.isEmpty()){
            throw new IllegalStateException("Parsed SiteId MUST NOT be NULL nor empty!");
        }
        this.signSite = siteId;
        this.representation = representation;
    }

    @Override
    public String getSignSite() {
        return signSite;
    }

    @Override
    public String getId() {
        return representation.getId();
    }

    @Override
    public Representation getRepresentation() {
        return representation;
    }
//    @Override
//    public SignTypeEnum getType() {
//        Reference ref = representation.getFirstReference(TYPE);
//        if(ref == null){
//            return DEFAULT_SIGN_TYPE;
//        } else {
//            SignTypeEnum type = ModelUtils.getSignType(ref.getReference());
//            if(type == null){
//                log.warn("Sign "+getId()+" is set to an unknown SignType "+ref.getReference()+"! -> return default type (value is not reseted)");
//                return DEFAULT_SIGN_TYPE;
//            } else {
//                return type;
//            }
//        }
//    }

}
