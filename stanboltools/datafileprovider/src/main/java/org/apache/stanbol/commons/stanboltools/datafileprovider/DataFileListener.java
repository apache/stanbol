/**
 * 
 */
package org.apache.stanbol.commons.stanboltools.datafileprovider;

import java.io.InputStream;

/**
 * Callback used in case tracked DataFiles become available/unavailable.
 * @author westei
 *
 */
public interface DataFileListener {
    /**
     * Notifies that a requested resource is now available and provides the
     * name and optionally the InputStream for that resource.
     * @param resourceName the name of the now available resource
     * @param is Optionally the InputStream for that resource. If <code>null</code>
     * receiver of this notification need to requested the resource with the
     * parsed name by the DataFileProvider.
     * @return If <code>true</code> the registration for this event is 
     * automatically removed. Otherwise the registration is kept and can be used 
     * to track if the resource get unavailable. 
     */
    boolean available(String resourceName, InputStream is);
    
    /** 
     * Called as soon as a previous available resource is no longer available 
     * @param resource The name of the unavailable resource 
     * @return If <code>true</code> the registration for this event is 
     * automatically removed. Otherwise the component receiving this call needs 
     * to remove the registration them self if it dose not want to retrieve
     * further events (such as if the resource becomes available again)
     **/ 
    boolean unavailable(String resource);
}