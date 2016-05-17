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
package org.apache.stanbol.enhancer.nlp.ner;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.nlp.model.tag.Tag;

public class NerTag extends Tag<NerTag> {

    private IRI type;
    
    public NerTag(String tag) {
        super(tag);
    }
    public NerTag(String tag,IRI type) {
        super(tag);
        this.type = type;
    }

    /**
     * The <code>dc:type</code> of the Named Entity
     * @return the <code>dc:type</code> of the Named Entity
     * as also used by the <code>fise:TextAnnotation</code>
     */
    public IRI getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return type == null ? super.toString() : 
            String.format("%s %s (type: %s)", getClass().getSimpleName(),tag,type);
    }
    
}
