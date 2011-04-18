package org.apache.stanbol.entityhub.core.model;

import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.Sign.SignTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;

/**
 * Factory for the default implementations of {@link Sign} and its subclasses
 * {@link EntityMapping} and {@link Symbol}.
 * @author Rupert Westenthaler
 *
 */
public class DefaultSignFactory {

    /**
     * Singleton instance
     */
    private static DefaultSignFactory instance;

    public static DefaultSignFactory getInstance(){
        if(instance == null){
            instance = new DefaultSignFactory();
        }
        return instance;
    }
    /**
     * Reads the {@link Sign#SIGN_TYPE} value in the parsed 
     * {@link Representation} and creates the Sign of the according type
     * @param signSite the site for the sign (not stored in the Representation
     * @param rep the representation of the sign
     * @return the sign instance based on the data in the Representation
     * @throws IllegalArgumentException if any of the parsed parameter is 
     * <code>null</code>
     */
    public Sign getSign(String signSite,Representation rep) throws IllegalArgumentException {
        if(signSite == null){
            throw new IllegalArgumentException("The Site for the Sign MUST NOT be NULL");
        }
        if(rep == null){
            throw new IllegalArgumentException("The Representation for the Sign MUST NOT be NULL");
        }
        SignTypeEnum signType = DefaultSignImpl.parseSignType(rep);
        return createInstance(signSite, signType, rep);
    }

    /**
     * Creates a Sign of the given type. The {@link Sign#SIGN_TYPE} is set to
     * the parsed uri of the parsed {@link SignTypeEnum}.
     * @param signSite the site of the sign
     * @param signType the type of the sign
     * @param rep the Representation for the sign
     * @return the created sign instance ({@link Sign}, {@link EntityMapping} or
     * {@link Symbol} depending on the parsed signType).
     * @throws IllegalArgumentException If the parsed signSite or representation
     * is <code>null</code>.
     */
    public Sign createSign(String signSite,Sign.SignTypeEnum signType,Representation rep) throws IllegalArgumentException{
        if(signSite == null){
            throw new IllegalArgumentException("The Site for the Sign MUST NOT be NULL");
        }
        if(rep == null){
            throw new IllegalArgumentException("The Representation for the Sign MUST NOT be NULL");
        }
        if(signType == null){ //if null is parsed as type
            signType = Sign.SignTypeEnum.Sign; //set to default
        }
        rep.setReference(Sign.SIGN_TYPE, signType.getUri());
        return createInstance(signSite, signType, rep);
    }
    /**
     * Internally used to create the sign instance.
     * @param signSite the site for the sign
     * @param signType the type for the sign. <code>null</code> defaults to Sign
     * @param rep the Representation to use
     * @return the {@link Symbol}, {@link EntityMapping} or {@link Sign} depending
     * on the parsed signType
     * @throws IllegalArgumentException if the parsed signType has an other value
     * as {@link SignTypeEnum#Sign}, {@link SignTypeEnum#EntityMapping} or
     * {@link SignTypeEnum#Symbol}. Meaning that an additional type was added to
     * the enumeration without adapting this implementation!)
     */
    private Sign createInstance(String signSite,Sign.SignTypeEnum signType,Representation rep){
        if(signType == null){
            signType = Sign.SignTypeEnum.Sign;
        }
        switch (signType) {
            case Sign:
                return new DefaultSignImpl(signSite, rep);
            case EntityMapping:
                return new DefaultEntityMappingImpl(signSite, rep);
            case Symbol:
                return new DefaultSymbolImpl(signSite, rep);
            default:
                throw new IllegalStateException("Unknown SignType "+signType+". " +
                        "This implementation is outdated and need to be adapted to the new Sign Type!");
        }
    }
    /**
     * Creates a sign by using the parsed type. The {@link Sign#SIGN_TYPE} according
     * to the parsed signType is set to the parsed {@link Representation}.
     * @param <T> the type of the sign.
     * @param signSite the site for the sign
     * @param signType one of {@link Sign}, {@link EntityMapping} or {@link Symbol}
     * @param rep the representation of the sign
     * @return The sign of the requested type
     * @throws IllegalArgumentException if any of the parsed Parameter is 
     * <code>null</code> or the signType is not one of {@link Sign}, 
     * {@link EntityMapping} or {@link Symbol}
     */
    @SuppressWarnings("unchecked")
    public <T extends Sign> T createSign(String signSite,Class<T> signType, Representation rep){
        if(signSite == null){
            throw new IllegalArgumentException("The Site for the sign MUST NOT be NULL");
        }
        if(rep == null){
            throw new IllegalArgumentException("The Representation for the sign MUST NOT be NULL");
        }
        if(signType == null){
            throw new IllegalArgumentException("The Class representing the type of the sign MUST NOT be NULL");
        }
        if(Symbol.class.getName().equals(signType.getName())){
            return (T)createSign(signSite, Sign.SignTypeEnum.Symbol, rep);
        } else if(EntityMapping.class.getName().equals(signType.getName())){
            return (T)createSign(signSite, Sign.SignTypeEnum.EntityMapping, rep);
        } else if(Sign.class.getName().equals(signType.getName())){
            return (T)createSign(signSite, Sign.SignTypeEnum.Sign, rep);
        } else {
            throw new IllegalArgumentException("The parsed Class representing the sign type MUST BE one of Sign, EntityMapping or Symbol");
        }
    }
}
