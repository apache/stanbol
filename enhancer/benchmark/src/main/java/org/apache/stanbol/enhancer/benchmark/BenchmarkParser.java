/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.enhancer.benchmark;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public interface BenchmarkParser {
    
    // Marker strings in the benchmark text input
    String COMMENT_MARKER = "#";
    String FIELD_SEPARATOR = ":";
    String ENHANCEMENT_CHAIN = "= CHAIN =";
    String INPUT_SECTION_MARKER = "= INPUT =";
    String EXPECT_SECTION_MARKER = "= EXPECT =";
    String COMPLAIN_SECTION_MARKER = "= COMPLAIN =";
    String DESCRIPTION_FIELD = "Description";
    
    /** Parse the supplied text in a List of Benchmark
     * 
     *  @param r is closed by this method after parsing or failure
     *  @throws IOException on I/O or syntax error
     */
    List<? extends Benchmark> parse(Reader r) throws IOException;
}
