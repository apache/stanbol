/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.enhancer.test.helper;

import java.io.FileNotFoundException;
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
        } else if(check instanceof FileNotFoundException){
            //FileNotFoundException is thrown in case of 404 NOT FOUND responses
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
            } else if(message.contains("Server returned HTTP response code: 401") ){
                log.warn("deactivate Test because Server returned HTTP Error 401 Unauthorized (Message: '"
                        +check.getMessage()+"')", e);
                return;
            } else if(message.contains("Server returned HTTP response code: 402") ){
                log.warn("deactivate Test because Server returned HTTP Error 402 Payment Required (Message: '"
                        +check.getMessage()+"')", e);
                return;
            } else if(message.contains("Server returned HTTP response code: 403") ){
                log.warn("deactivate Test because Server returned HTTP Error 403 Forbidden (Message: '"
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
