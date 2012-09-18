/*
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

package org.apache.stanbol.enhancer.engines.metaxa.core.mail.simple;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.semanticdesktop.aperture.extractor.Extractor;
import org.semanticdesktop.aperture.extractor.ExtractorFactory;

public class SimpleMailExtractorFactory implements ExtractorFactory {
    
    private static final Set MIME_TYPES;
    
    static {
        HashSet set = new HashSet();
        set.add("message/rfc822");
        set.add("message/news");
        
        MIME_TYPES = Collections.unmodifiableSet(set);
    }
    
    public Extractor get() {
        return new SimpleMailExtractor();
    }
    
    public Set getSupportedMimeTypes() {
        return MIME_TYPES;
    }
}
