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
package org.apache.stanbol.enhancer.nlp.model.impl;

import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.osgi.framework.Constants;

@Component(immediate=true)
@Service(value=AnalysedTextFactory.class)
@Properties(value={
    @Property(name=Constants.SERVICE_RANKING,intValue=Integer.MIN_VALUE)
})
public class AnalysedTextFactoryImpl extends AnalysedTextFactory {

    @Override
    public AnalysedText createAnalysedText(Blob blob) throws IOException {
        String text = ContentItemHelper.getText(blob);
        return new AnalysedTextImpl(blob,text);
    }
}
