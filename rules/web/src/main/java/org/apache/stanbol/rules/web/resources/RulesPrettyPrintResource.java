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
package org.apache.stanbol.rules.web.resources;
//import javax.servlet.ServletContext;
//import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

public class RulesPrettyPrintResource extends BaseStanbolResource {

	private Object result;

    public RulesPrettyPrintResource( UriInfo uriInfo, Object result) {
        this.result = result;
        this.uriInfo = uriInfo;
//        this.servletContext = servletContext;
    }

    public Object getResult() {
    	
        return this.result;
    }

}
