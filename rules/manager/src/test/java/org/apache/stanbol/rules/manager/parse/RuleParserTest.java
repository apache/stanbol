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
package org.apache.stanbol.rules.manager.parse;

import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author andrea.nuzzolese
 * 
 */
public class RuleParserTest {

    private static String kReSRule;

    private Logger log = LoggerFactory.getLogger(getClass());

    @BeforeClass
    public static void setup() {
        kReSRule = "/* example of " + System.getProperty("line.separator") + "rule */"
                   + "ProvaParent = <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#> . "
                   + "rule1[ has(ProvaParent:hasParent, ?x, ?y) . has(ProvaParent:hasBrother, ?y, ?z) -> "
                   + "has(ProvaParent:hasUncle, ?x, ?z) ]";
    }

    @Test
    public void testParser() {
        try {
            KB kReSKB = RuleParserImpl.parse("http://incubator.apache.org/stanbol/rules/test/", kReSRule);
            if (kReSKB != null) {
                RuleList kReSRuleList = kReSKB.getRuleList();
                if (kReSRuleList != null) {
                    for (Rule kReSRule : kReSRuleList) {
                        log.debug("RULE : " + kReSRule.toString());
                    }
                }
                log.debug("RULE LIST IS NULL");
            } else {
                log.debug("KB IS NULL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
