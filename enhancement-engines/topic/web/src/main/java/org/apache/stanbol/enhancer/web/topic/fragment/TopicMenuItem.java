/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.enhancer.web.topic.fragment;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

/**
 *
 */
@Component(immediate=true)
@Service(NavigationLink.class)
public class TopicMenuItem extends NavigationLink {
    
   private static final String htmlDescription = "<strong>Topic Classification" +
           "endpoint</strong> for Apache Stanbol. This allows to manage and train " +
           "topic classification models for the Topic classification engine of the " +
           "Stanbol Enhancer.";
        
    public TopicMenuItem() {
        super("topic", "/topic", htmlDescription , 15);
    }
    
}