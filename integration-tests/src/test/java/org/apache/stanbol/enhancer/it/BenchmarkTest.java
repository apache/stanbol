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
package org.apache.stanbol.enhancer.it;

import java.net.URLEncoder;

import org.junit.Test;

public class BenchmarkTest extends EnhancerTestBase {

    final String BENCHMARK =
        "= INPUT = \n"
        + "Bob Marley was born in Kingston, Jamaica. Marley's music was heavily "
        + "influenced by the social issues of his homeland.\n"
        + "= EXPECT = \n"
        + "Description: Jamaica must be found \n"
        + "http://fise.iks-project.eu/ontology/entity-reference URI http://dbpedia.org/resource/Jamaica\n"
        + "\n"
        + "Description: Bob Marley must be found as a musical artist\n"
        + "http://fise.iks-project.eu/ontology/entity-type URI http://dbpedia.org/ontology/MusicalArtist\n"
        + "http://fise.iks-project.eu/ontology/entity-reference URI http://dbpedia.org/resource/Bob_Marley\n"
        + "\n"
        + "= COMPLAIN =\n"
        + "Description: Miles Davis must not be found\n"
        + "http://fise.iks-project.eu/ontology/entity-type URI http://dbpedia.org/ontology/MusicalArtist\n"
        + "http://fise.iks-project.eu/ontology/entity-reference URI http://dbpedia.org/resource/Miles_Davis\n"
        ;
        
    @Test
    public void testBenchmark() throws Exception {
        executor.execute(
                builder.buildPostRequest("/benchmark?content=" + URLEncoder.encode(BENCHMARK, "UTF-8"))
                .withContent(BENCHMARK)
            ).assertStatus(200)
            .assertContentContains("SUCCESSFUL");
    }
}
