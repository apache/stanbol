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
package org.apache.stanbol.enhancer.engines.metaxa.core.mp3;

import java.util.Collections;
import java.util.Set;

import org.semanticdesktop.aperture.extractor.FileExtractor;
import org.semanticdesktop.aperture.extractor.FileExtractorFactory;

/**
 * A FileExtractorFactory implementation for the MP3FileExtractors
 */
public class MP3FileExtractorFactory implements FileExtractorFactory {

    private static final Set MIME_TYPES = Collections.singleton("audio/mpeg");
    
    /**
     * @see FileExtractorFactory#get()
     */
    public FileExtractor get() {
        return new MP3FileExtractor();
    }

    /**
     * @see FileExtractorFactory#getSupportedMimeTypes()
     */
    public Set getSupportedMimeTypes() {
        return MIME_TYPES;
    }

}

