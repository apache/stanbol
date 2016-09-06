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

package org.apache.stanbol.rules.web;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

/**
 * The menue item for the Stanbol Rules component
 */
@Component
@Service(value=NavigationLink.class)
public class RulesMenueItem extends NavigationLink {

    private static final String NAME = "rules";

    private static final String htmlDescription = 
            "This is the implementation of Stanbol Rules which can be used both "+
            "for <strong>reasoning</strong> and <strong>refactoring</strong>";

	public RulesMenueItem() {
		super(NAME, "/"+NAME, htmlDescription, 50);
	}
	
}
