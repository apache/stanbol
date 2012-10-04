package org.apache.stanbol.enhancer.test.helper;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility that provides utilities for Unit Tests that need to access remote
 * services
 * 
 * @author Rupert Westenthaler
 *
 */
public final class RemoteServiceHelper {

    private static final Logger log = LoggerFactory.getLogger(RemoteServiceHelper.class);
    
    private RemoteServiceHelper(){}
    
    
    /**
     * Catches {@link IOException}s or {@link EngineException} that are caused
     * by {@link IOException}s indicating that a remote service is not available
     * @param e the Exception to check
     * @param containedInMessage Optionally an Err
     * @throws T the parsed Exception if it was not caused by an External service
     * that is not available
     */
    public static <T extends Exception> void checkServiceUnavailable(T e, String...containedInMessage) throws T {
        Throwable check;
        if(e instanceof EngineException){
            check = e.getCause(); //check the cuase
        } else {
            check = e;
        }
        if (check instanceof UnknownHostException) {
            log.warn("deactivate Test because of "+check.getMessage(), e);
            return;
        } else if (check instanceof SocketTimeoutException) {
            log.warn("deactivate Test because of "+check.getMessage(), e);
            return;
        } else if (check instanceof IOException){
            String message = check.getMessage();
            //check for typical messates
            if(message != null && message.contains("Connection refused")) {
                log.warn("deactivate Test because connection to remote service was refused (Message: '"
                        +check.getMessage()+"')", e);
                return;
            } else if(message.contains("Server returned HTTP response code: 50")){
                log.warn("deactivate Test because Internal Error of remote serivce (Message: '"
                        +check.getMessage()+"')", e);
                return;
            } else if(containedInMessage != null){
                for(String contained : containedInMessage){
                    if(message.contains(contained)){
                        log.warn("deactivate Test because IOException of remote Service contained '"
                            + contained+"' (Message: '"+check.getMessage()+"')", e);
                        return;
                    }
                }
            }
        }
        throw e;
    }
    
}
