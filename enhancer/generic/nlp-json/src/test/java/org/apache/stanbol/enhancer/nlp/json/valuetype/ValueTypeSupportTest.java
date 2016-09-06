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

package org.apache.stanbol.enhancer.nlp.json.valuetype;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.json.AnalyzedTextParser;
import org.apache.stanbol.enhancer.nlp.json.AnalyzedTextSerializer;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.Assert;

public abstract class ValueTypeSupportTest {
    /**
     * The line separator used by the Environment running this test
     */
    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");
	/**
     * Empty AnalysedText instance created before each test
     */
    protected static AnalysedText at;

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static final AnalysedTextFactory atFactory = AnalysedTextFactory.getDefaultInstance();
    
    private static ContentItem ci;

    private static Entry<IRI,Blob> textBlob;
	
	protected static void setupAnalysedText(String text) throws IOException {
		ci = ciFactory.createContentItem(new StringSource(text));
        textBlob = ContentItemHelper.getBlob(ci, Collections.singleton("text/plain"));
        at = atFactory.createAnalysedText(textBlob.getValue());
	}
	
	protected String getSerializedString() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
        AnalyzedTextSerializer serializer = AnalyzedTextSerializer.getDefaultInstance();
        serializer.serialize(at, bout, null);
        byte[] data = bout.toByteArray();
        
        return new String(data,Charset.forName("UTF-8"));
	}
	
	protected AnalysedText getParsedAnalysedText(String serializedData) throws IOException {
		AnalyzedTextParser parser = AnalyzedTextParser.getDefaultInstance();
		byte[] bytes = serializedData.getBytes();
		
        return parser.parse(new ByteArrayInputStream(bytes), null, 
            atFactory.createAnalysedText(textBlob.getValue()));
	}
	
	protected void assertAnalysedTextEquality(AnalysedText parsedAt) {
		Assert.assertEquals(at, parsedAt);
        Iterator<Span> origSpanIt = at.getEnclosed(EnumSet.allOf(SpanTypeEnum.class));
        Iterator<Span> parsedSpanIt = parsedAt.getEnclosed(EnumSet.allOf(SpanTypeEnum.class));
        while(origSpanIt.hasNext() && parsedSpanIt.hasNext()){
            Span orig = origSpanIt.next();
            Span parsed = parsedSpanIt.next();
            Assert.assertEquals(orig, parsed);
            Set<String> origKeys = orig.getKeys();
            Set<String> parsedKeys = parsed.getKeys();
            Assert.assertEquals(origKeys, parsedKeys);
            for(String key : origKeys){
                List<Value<?>> origValues = orig.getValues(key);
                List<Value<?>> parsedValues = parsed.getValues(key);
                Assert.assertEquals(origValues, parsedValues);
            }
        }
	}
}
