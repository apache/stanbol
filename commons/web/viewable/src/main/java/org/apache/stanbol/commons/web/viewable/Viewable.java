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
package org.apache.stanbol.commons.web.viewable;


/**
 * This is a replacement for the jersey Vieable that allows rendering an 
 * arbitrary object using a Freemarker template specified by path.
 * 
 * Usage of this class promotes a bad programming style where the 
 * application logic is not clearly separated from the presentation but 
 * where backend method are called by the presentation layer.
 * 
 * Users should consider migrate to RdfViewable instead where instead of
 * an arbitrary Object a GraphNode representing a node in a graph is passed,
 * this approach also allows the response to be rendered as RDF.
 *
 */
public class Viewable {
    

    /**
     * This uses the class name of Pojo to prefix the template
     * 
     * @param templatePath the templatePath
     * @param pojo the graphNode with the actual content
     */
    public Viewable(final String templatePath, final Object pojo) {
        this(templatePath, pojo, null);
    }
    
    /**
     * With this version of the constructor the templatePath is prefixed with
     * the slash-separated class name of clazz.
     *
     * @param templatePath the templatePath
     * @param pojo the graphNode with the actual content
     * @param clazz class which package name will be used as prefix 
     * 
     */
    public Viewable(final String templatePath, final Object pojo, final Class<?> clazz) {
        this.templatePath = templatePath;
        this.pojo = pojo;
        this.contextClass = clazz;
    }
    
    private String templatePath;
    private Object pojo;
    private Class contextClass;
    
    public String getTemplatePath() {
        return templatePath;
    }
    
    public Object getPojo() {
        return pojo;
    }

    public Class getContextClass() {
        return contextClass;
    }

}
