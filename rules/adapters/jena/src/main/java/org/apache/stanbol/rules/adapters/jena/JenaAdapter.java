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

package org.apache.stanbol.rules.adapters.jena;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.rules.adapters.AbstractRuleAdapter;
import org.apache.stanbol.rules.adapters.AdaptableAtom;
import org.apache.stanbol.rules.base.api.Adaptable;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdaptersFactory;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.base.api.util.AtomList;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;

/**
 * The adapter for Jena rules. <br/>
 * The output object of this adapter is a {@link com.hp.hpl.jena.reasoner.rulesys.Rule} instance.<br/>
 * For that reason the adaptTo method works only if the second argument is
 * <code>com.hp.hpl.jena.reasoner.rulesys.Rule.class</code>
 * 
 * @author anuzzolese
 * 
 */

@Component(immediate = true, metatype = true)
@Service(RuleAdapter.class)
public class JenaAdapter extends AbstractRuleAdapter {

    public static final String ARTIFACT = "org.apache.stanbol.rules.adapters.jena.atoms";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    RuleStore ruleStore;

    @Reference
    RuleAdaptersFactory ruleAdaptersFactory;

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRecipeTo(Recipe recipe, Class<T> type) throws RuleAtomCallExeption,
                                                               UnsupportedTypeForExportException,
                                                               UnavailableRuleObjectException {

        List<com.hp.hpl.jena.reasoner.rulesys.Rule> jenaRules = null;

        if (type == com.hp.hpl.jena.reasoner.rulesys.Rule.class) {

            RuleList ruleList = recipe.getRuleList();
            Iterator<Rule> ruleIterator = ruleList.iterator();

            jenaRules = new ArrayList<com.hp.hpl.jena.reasoner.rulesys.Rule>();
            for (int i = 0; ruleIterator.hasNext(); i++) {
                jenaRules.add((com.hp.hpl.jena.reasoner.rulesys.Rule) adaptRuleTo(ruleIterator.next(), type));
            }

        } else {
            throw new UnsupportedTypeForExportException(
                    "The Jena Adapter does not support the selected serialization : "
                            + type.getCanonicalName());
        }

        return (T) jenaRules;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRuleTo(Rule rule, Class<T> type) throws RuleAtomCallExeption,
                                                         UnsupportedTypeForExportException,
                                                         UnavailableRuleObjectException {

        if (type == com.hp.hpl.jena.reasoner.rulesys.Rule.class) {
            AtomList bodyAtomList = rule.getBody();
            AtomList headAtomList = rule.getHead();

            List<ClauseEntry> headClauseEntries = new ArrayList<ClauseEntry>();
            List<ClauseEntry> bodyClauseEntries = new ArrayList<ClauseEntry>();

            Iterator<RuleAtom> it = headAtomList.iterator();
            while (it.hasNext()) {
                RuleAtom atom = it.next();
                ClauseEntry clauseEntry = adaptRuleAtomTo(atom, com.hp.hpl.jena.reasoner.rulesys.Rule.class);

                if (clauseEntry instanceof HigherOrderClauseEntry) {
                    List<ClauseEntry> clauseEntries = ((HigherOrderClauseEntry) clauseEntry)
                            .getClauseEntries();

                    for (ClauseEntry ce : clauseEntries) {
                        headClauseEntries.add(ce);
                    }
                } else {
                    headClauseEntries.add(clauseEntry);
                }
            }

            it = bodyAtomList.iterator();
            while (it.hasNext()) {
                RuleAtom atom = it.next();
                ClauseEntry clauseEntry = adaptRuleAtomTo(atom, com.hp.hpl.jena.reasoner.rulesys.Rule.class);

                if (clauseEntry instanceof HigherOrderClauseEntry) {
                    List<ClauseEntry> clauseEntries = ((HigherOrderClauseEntry) clauseEntry)
                            .getClauseEntries();

                    for (ClauseEntry ce : clauseEntries) {
                        bodyClauseEntries.add(ce);
                    }
                } else {
                    bodyClauseEntries.add(clauseEntry);
                }
            }

            return (T) new com.hp.hpl.jena.reasoner.rulesys.Rule(rule.getRuleName(), headClauseEntries,
                    bodyClauseEntries);
        } else {
            throw new UnsupportedTypeForExportException("The adapter " + getClass()
                                                        + " does not support type : "
                                                        + type.getCanonicalName());
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRuleAtomTo(RuleAtom ruleAtom, Class<T> type) throws RuleAtomCallExeption,
                                                                     UnsupportedTypeForExportException,
                                                                     UnavailableRuleObjectException {

        if (type == com.hp.hpl.jena.reasoner.rulesys.Rule.class) {

            String className = ruleAtom.getClass().getSimpleName();

            String canonicalName = ARTIFACT + "." + className;

            try {
                // ClassLoader loader = Thread.currentThread().getContextClassLoader();
                // Class<AdaptableAtom> jenaAtomClass = (Class<AdaptableAtom>)loader.loadClass(canonicalName);
                Class<AdaptableAtom> jenaAtomClass = (Class<AdaptableAtom>) Class.forName(canonicalName);

                try {
                    AdaptableAtom jenaAtom = jenaAtomClass.newInstance();

                    jenaAtom.setRuleAdapter(this);

                    return (T) jenaAtom.adapt(ruleAtom);

                } catch (InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {
            throw new UnsupportedTypeForExportException("The adapter " + getClass()
                                                        + " does not support type : "
                                                        + type.getCanonicalName());
        }

        return null;

    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {

        log.info("in " + JenaAdapter.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Should be called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        log.info("The Jena Adapter for Stanbol Rules  is active", this);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + JenaAdapter.class + " deactivate with context " + context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> getExportClass() {
        return (Class<T>) com.hp.hpl.jena.reasoner.rulesys.Rule.class;
    }

    @Override
    public <T> boolean canAdaptTo(Adaptable adaptable, Class<T> type) {
        if (type == com.hp.hpl.jena.reasoner.rulesys.Rule.class) {
            return true;
        } else {
            return false;
        }
    }
}
