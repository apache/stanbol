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
package org.apache.stanbol.rules.manager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.Symbols;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A KB is the result of the parsing of a set of rules in Stanbol syntax. It provides methods for accessing
 * parsed rules available as {@link Rule} objects.
 * 
 * @author anuzzolese
 * 
 */
public class KB {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Hashtable<String,String> prefixes;

    private RuleList ruleList;

    public KB(String ruleNamespace) {
        log.debug("Setting up a KReSKB");
        prefixes = new Hashtable<String,String>();
        prefixes.put("var", Symbols.variablesPrefix);
        prefixes.put("rmi2", ruleNamespace);
        ruleList = new RuleList();
    }

    public void addPrefix(String prefixString, String prefixURI) {
        prefixes.put(prefixString, prefixURI);
    }

    public String getPrefixURI(String prefixString) {
        return prefixes.get(prefixString);
    }

    public void addRule(Rule rule) {
        ruleList.add(rule);
    }

    public RuleList getRuleList() {
        return ruleList;
    }

    public void write(OutputStream outputStream) throws IOException {
        boolean firstIt = true;
        for (Rule ruleS : ruleList) {

            String rule;

            if (firstIt) {
                rule = ruleS.toString();

                firstIt = false;
            } else {
                rule = " . " + System.getProperty("line.separator") + ruleS.toString();
            }
            outputStream.write(rule.getBytes());
        }
        outputStream.close();
    }

    public void write(FileWriter fileWriter) throws IOException {
        boolean write = true;
        for (Rule rule : ruleList) {
            if (write) {
                fileWriter.write(rule.toString());
                write = false;
            } else {
                fileWriter.write(" . " + System.getProperty("line.separator") + rule.toString());
            }
        }
        fileWriter.close();
    }

}
