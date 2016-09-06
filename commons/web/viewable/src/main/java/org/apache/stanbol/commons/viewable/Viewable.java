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

package org.apache.stanbol.commons.viewable;
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
 * @deprecated Moved to {@link org.apache.stanbol.commons.web.viewable.Viewable}
 */
@Deprecated
public class Viewable extends org.apache.stanbol.commons.web.viewable.Viewable {
    
    public Viewable(String templatePath, Object pojo) {
        super(templatePath, pojo);
    }

    public Viewable(final String templatePath, final Object pojo, final Class<?> clazz) {
        super(templatePath, pojo, clazz);
    }
}
