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

package org.apache.stanbol.rules.adapters.swrl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SWRL adapter.<br/>
 * This class allows to adapt Stanbol recipes to {@link SWRLRule} objects.<br/>
 * The class {@link SWRLRule} is provided by the OWL API.
 * 
 * @author anuzzolese
 * 
 */

@Component(immediate = true)
@Service(RuleAdapter.class)
public class SWRLAdapter extends AbstractRuleAdapter {

    public static final String ARTIFACT = "org.apache.stanbol.rules.adapters.swrl.atoms";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    RuleStore ruleStore;

    @Reference
    RuleAdaptersFactory ruleAdaptersFactory;
    
    private ComponentContext componentContext;

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRecipeTo(Recipe recipe, Class<T> type) throws RuleAtomCallExeption,
                                                               UnsupportedTypeForExportException,
                                                               UnavailableRuleObjectException {

        List<SWRLRule> swrlRules = null;

        if (type == SWRLRule.class) {

            RuleList ruleList = recipe.getRuleList();

            swrlRules = new ArrayList<SWRLRule>();

            if (ruleList != null && !ruleList.isEmpty()) {

                Iterator<Rule> ruleIterator = ruleList.iterator();

                while (ruleIterator.hasNext()) {
                    Rule rule = ruleIterator.next();
                    swrlRules.add(adaptRuleTo(rule, SWRLRule.class));
                }
            }

        } else {
            throw new UnsupportedTypeForExportException(
                    "The SPARQL Export Provider does not support the selected serialization : "
                            + type.getCanonicalName());
        }

        return (T) swrlRules;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRuleTo(Rule rule, Class<T> type) throws RuleAtomCallExeption,
                                                         UnsupportedTypeForExportException,
                                                         UnavailableRuleObjectException {

        if (type == SWRLRule.class) {
            OWLDataFactory factory = OWLManager.getOWLDataFactory();

            Set<SWRLAtom> bodyAtoms = new HashSet<SWRLAtom>();
            Set<SWRLAtom> headAtoms = new HashSet<SWRLAtom>();
            for (RuleAtom atom : rule.getBody()) {
                bodyAtoms.add((SWRLAtom) adaptRuleAtomTo(atom, SWRLRule.class));
            }
            for (RuleAtom atom : rule.getHead()) {
                headAtoms.add((SWRLAtom) adaptRuleAtomTo(atom, SWRLRule.class));
            }

            return (T) factory.getSWRLRule(bodyAtoms, headAtoms);
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

        if (type == SWRLRule.class) {

            String className = ruleAtom.getClass().getSimpleName();

            String canonicalName = ARTIFACT + "." + className;

            try {
                
                Class<AdaptableAtom> swrlAtomClass = (Class<AdaptableAtom>) Class.forName(canonicalName);

                try {
                    AdaptableAtom swrlAtom = swrlAtomClass.newInstance();

                    swrlAtom.setRuleAdapter(this);

                    return (T) swrlAtom.adapt(ruleAtom);

                } catch (InstantiationException e) {
                    log.error(e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                }

            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            } catch (SecurityException e) {
                log.error(e.getMessage(), e);
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

        log.info("in " + SWRLAdapter.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        
        componentContext = context;
        
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Should be called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        log.info("SWRL Adapter for Stanbol Rules is active", this);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + SWRLAdapter.class + " deactivate with context " + context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> getExportClass() {
        return (Class<T>) SWRLRule.class;
    }

    @Override
    public <T> boolean canAdaptTo(Adaptable adaptable, Class<T> type) {
        if(type == SWRLRule.class){
            return true;
        }
        else{
            return false;
        }
    }
}
