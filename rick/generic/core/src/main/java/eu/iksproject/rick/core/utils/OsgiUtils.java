package eu.iksproject.rick.core.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;

/**
 * This class contains some utilities for osgi
 * TODO: Check if they are not available in some std. library
 * @author Rupert Westenthaler
 *
 */
public final class OsgiUtils {

    //private static final Logger log = LoggerFactory.getLogger(OsgiUtils.class);

    private OsgiUtils() {/* do not create instances of utility classes*/}

    /**
     * Checks if a value is present
     * @param propertyName The key for the property
     * @return the value
     * @throws ConfigurationException if the property is not present
     */
    public final static Object checkProperty(Dictionary<?, ?> properties, String propertyName) throws ConfigurationException{
        return checkProperty(properties, propertyName, null);
    }
    /**
     * Checks if the value is present. If not it returns the parse defaultValue.
     * If the value and the default value is null, it throws an {@link ConfigurationException}
     * @param properties the properties to search
     * @param propertyName the name of the proeprty
     * @param defaultValue the default value or <code>null</code> if none
     * @return the value of the property (guaranteed NOT <code>null</code>)
     * @throws ConfigurationException In case the property is not present and no default value was parsed
     */
    public final static Object checkProperty(Dictionary<?, ?> properties, String propertyName,Object defaultValue) throws ConfigurationException{
        Object value = properties.get(propertyName);
         if(value == null){
             if(defaultValue != null){
                 return defaultValue;
             } else {
                 throw new ConfigurationException(propertyName,"No value found for this required property");
             }
         } else {
             return value;
         }
    }

    /**
     * Checks if the property is present and the value can be converted to an {@link URI}
     * @param propertyName The key for the property
     * @return the value
     * @throws ConfigurationException if the property is not present or the
     * configured value is no valid URI
     */
    public final static URI checkUriProperty(Dictionary<?, ?> properties,String propertyName) throws ConfigurationException {
        Object uri = checkProperty(properties,propertyName);
        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            throw new ConfigurationException(propertyName,"Property needs to be a valid URI", e);
        }
    }
    /**
     * Checks if the property is present and the value can be converted to an {@link URL}
     * @param propertyName The key for the property
     * @return the value
     * @throws ConfigurationException if the property is not present or the
     * configured value is no valid URL
     */
    public final static URL checkUrlProperty(Dictionary<?, ?> properties,String propertyName) throws ConfigurationException {
        Object uri = checkProperty(properties,propertyName);
        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            throw new ConfigurationException(propertyName,"Property value needs to be a valid URL", e);
        }
    }
    /**
     * Checks if the value of a property is a member of the parsed Enumeration
     * @param <T> the Enumeration
     * @param enumeration the class of the enumeration
     * @param properties the configuration
     * @param propertyName the name of the property to check
     * @return the member of the enumeration
     * @throws ConfigurationException if the property is missing or the value is
     * not a member of the parsed enumeration
     */
    public final static <T extends Enum<T>> T checkEnumProperty(Class<T> enumeration,Dictionary<?, ?> properties,String propertyName) throws ConfigurationException{
        Object value =checkProperty(properties, propertyName);
        try {
            return Enum.valueOf(enumeration,value.toString());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(propertyName,String.format("Property value %s is not a member of Enumeration %s!",value,enumeration.getName()), e);
        }
    }
//    /**
//     * search for a {@link ComponentFactory} that has the component.name property
//     * as configured by {@link ConfiguredSite#DEREFERENCER_TYPE}. Than creates
//     * an new instance of an {@link EntityDereferencer} and configures it with
//     * all the properties present for this instance of {@link ReferencedSite} (
//     * only component.* and service.* properties are ignored).<br>
//     * The {@link ComponentInstance} and the {@link EntityDereferencer} are
//     * stored in the according memeber variables.
//     * @return the ComponentInstance of <code>null</code> if no ComponentFactory
//     *    was found for the parsed componentService
//     * @throws ConfigurationException if the {@link ConfiguredSite#DEREFERENCER_TYPE}
//     * is not present or it's value does not allow to create a {@link EntityDereferencer}
//     * instance.
//     */
//    public static ComponentInstance createComonentInstance(ComponentContext context, String property,Object componentName,Class<?> componentService) throws ConfigurationException {
//        //Object value = checkProperty(DEREFERENCER_TYPE);
//        final ServiceReference[] refs;
//        try {
//            refs = context.getBundleContext().getServiceReferences(
//                    ComponentFactory.class.getName(),
//                    "(component.name="+componentName+")");
//
//        } catch (InvalidSyntaxException e) {
//            throw new ConfigurationException(property, "Unable to get ComponentFactory for parsed value "+componentName.toString(),e);
//        }
//        if(refs != null && refs.length>0){
//            if(refs.length>1){ //log some warning if more than one Service Reference was found by the query!
//                log.warn("Multiple ComponentFactories found for the property "+property+"="+componentName+"! -> First one was used to instantiate the "+componentService+" Service");
//            }
//            Object dereferencerFactorySerivceObject = context.getBundleContext().getService(refs[0]);
//            if(dereferencerFactorySerivceObject != null){
//                try {
//                    // I trust the OSGI framework, that the returned service implements the requested Interface
//                    ComponentFactory dereferencerFactory = (ComponentFactory)dereferencerFactorySerivceObject;
//                    //log.debug("build configuration for "+EntityDereferencer.class.getSimpleName()+" "+componentName.toString());
//                    Dictionary<String, Object> config = copyConfig(context.getProperties());
//                    ComponentInstance dereferencerComponentInstance = dereferencerFactory.newInstance(config);
//                    dereferencerFactory = null;
//                    //now
//                    if(dereferencerComponentInstance == null){
//                        throw new IllegalStateException("Unable to create ComponentInstance for Property value "+componentName+"!");
//                    }
//                    if(componentService.isAssignableFrom(dereferencerComponentInstance.getInstance().getClass())){
//                        return dereferencerComponentInstance;
//                    } else {
//                        dereferencerComponentInstance.dispose(); //we can not use it -> so dispose it!
//                        dereferencerComponentInstance = null;
//                        throw new IllegalStateException("ComponentInstance created for Property value "+componentName+" does not provide the "+componentService+" Service!");
//                    }
//                } finally {
//                    //we need to unget the ComponentFactory!
//                    context.getBundleContext().ungetService(refs[0]);
//                    dereferencerFactorySerivceObject=null;
//                }
//            } else {
//                return null;
//            }
//        } else {
//            return null;
//        }
//    }

    /**
     * Copy all properties excluding "{@value Constants#OBJECTCLASS}",
     * "component.*" and "service.*" to the returned Dictionary
     * @param source the source
     * @return the target
     */
    public static Dictionary<String, Object> copyConfig(Dictionary<String, Object> source) {
        Dictionary<String, Object> config = new Hashtable<String, Object>();
        for(Enumeration<?> keys = source.keys();keys.hasMoreElements();){
            String key = keys.nextElement().toString();
            if(!key.startsWith("component.") &&
                    !key.startsWith("service.") &&
                    !key.equals(Constants.OBJECTCLASS)){
                //log.debug(" > copy key" + key);
                config.put(key, source.get(key));
            } else {
                //log.debug(" > ignore key" + key);
            }
        }
        return config;
    }

}
