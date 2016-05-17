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
package org.apache.stanbol.enhancer.nlp.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.junit.Test;

import junit.framework.Assert;

public class NIFHelperTest {

    static IRI base = new IRI("http://stanbol.apache.org/test/nif/nif-helper");
    static String text = "This is a test for the NLP Interchange format!";
    
    
    @Test
    public void testFragmentURI(){
        Assert.assertEquals(
            new IRI(base.getUnicodeString()+"#char=23,26"), 
            NIFHelper.getNifFragmentURI(base, 23, 26));
    }
    @Test
    public void testOffsetURI(){
        Assert.assertEquals(
            base.getUnicodeString()+"#offset_23_26", 
            NIFHelper.getNifOffsetURI(base, 23, 26).getUnicodeString());
    }
    @Test
    public void testHashURI() throws IOException {
        String selected = text.substring(23,26);
        String context = text.substring(13,23)+'('+selected+')'+text.substring(26,36);
        byte[] contextData = context.getBytes(Charset.forName("UTF8"));
        String md5 = ContentItemHelper.streamDigest(new ByteArrayInputStream(contextData), null, "MD5");
        IRI expected = new IRI(base.getUnicodeString()+"#hash_10_3_"+md5+"_NLP");
        Assert.assertEquals(expected, NIFHelper.getNifHashURI(base, 23, 26, text));
    }    
}
