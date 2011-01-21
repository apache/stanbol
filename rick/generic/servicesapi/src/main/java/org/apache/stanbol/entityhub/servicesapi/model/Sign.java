package org.apache.stanbol.entityhub.servicesapi.model;

import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
/**
 * A Sign links three things together
 * <ol>
 * <li>the <b>signifier</b> (ID) used to identify the sign
 * <li>the <b>description</b> (Representation) for the signified entity
 * <li>the <b>organisation</b> (Site) that provides this description
 * </ol>
 * 
 * @author Rupert Westenthaler
 *
 */
public interface Sign {
    /**
     * Enumeration over the different types of Signs defined by the Entityhub
     * @author Rupert Westenthaler
     *
     */
    public static enum SignTypeEnum {
        /**
         * The Sign - the default - type
         */
        Sign(RdfResourceEnum.Sign.getUri()),
        /**
         *  Symbols are Signs defined by this Entityhub instance
         */
        Symbol(RdfResourceEnum.Symbol.getUri()),
        /**
         * EntityMappings are signs that do map Signs defined/managed by referenced
         * Sites to Symbols.
         */
        EntityMapping(RdfResourceEnum.EntityMapping.getUri()),
        ;
        private String uri;
        SignTypeEnum(String uri){

        }
        public String getUri(){
            return uri;
        }
        @Override
        public String toString() {
            return uri;
        }
    }
    /**
     * The default type of a {@link Sign} (set to {@link SignTypeEnum#Sign})
     */
    SignTypeEnum DEFAULT_SIGN_TYPE = SignTypeEnum.Sign;
    /**
     * The id (signifier) of this  sign.
     * @return the id
     */
    String getId();

    String SIGN_SITE = RdfResourceEnum.signSite.getUri();
    /**
     * Getter for the id of the referenced Site that defines/manages this sign.<br>
     * Note that the Entityhub allows that different referenced Sites
     * provide representations for the same id ({@link Sign#getId()}).
     * Therefore there may be different entity instances of {@link Sign} with
     * the same id but different representations.<br>
     * In other word different referenced Sites may manage representations by
     * using the same id.<br>
     * Note also, that the Entityhub assumes that all such representations
     * are equivalent and interchangeable. Therefore Methods that searches for
     * Entities on different Sites will return the first hit without searching
     * for any others.
     * @return the site of this Sign
     */
    String getSignSite();
//    /**
//     * Getter for the type of a sign. Subclasses may restrict values of this
//     * property. (e.g. {@link #getType()} for {@link Symbol} always returns
//     * {@link SignTypeEnum#Symbol})
//     * @return the type
//     */
//    SignTypeEnum getType();

    /**
     * Getter for the {@link Representation} of that sign as defined/managed by the site
     * @return the representation
     */
    Representation getRepresentation();
}
