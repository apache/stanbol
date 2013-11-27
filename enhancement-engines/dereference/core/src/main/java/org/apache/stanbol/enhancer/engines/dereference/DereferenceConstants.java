package org.apache.stanbol.enhancer.engines.dereference;

/**
 * Define configuration parameters for Dereference engines
 * @author Rupert Westenthaler
 *
 */
public interface DereferenceConstants {
    /**
     * Property used to configure the fields that should be dereferenced.<p>
     * DereferenceEngines need to support a list of URIs but may also support more
     * complex syntax (such as the Entityhub FiedMapping). However parsing a
     * list of properties URIs MUST BE still valid.<p>
     * Support for Namespace prefixes via the Stanbol Namespace Prefix Service
     * is optional. If unknown prefixes are used or prefixes are not supported
     * the Engine is expected to throw a 
     * {@link org.osgi.service.cm.ConfigurationException} during activation
     */
    String DEREFERENCE_ENTITIES_FIELDS = "enhancer.engines.dereference.fields";
    /**
     * Property used to configure LDPath statements. Those are applied using
     * each referenced Entity as Context.<p>
     * DereferenceEngines that can not support LDPath are expected to throw a
     * {@link org.osgi.service.cm.ConfigurationException} if values are set
     * for this property.
     */
    String DEREFERENCE_ENTITIES_LDPATH = "enhancer.engines.dereference.ldpath";

}
