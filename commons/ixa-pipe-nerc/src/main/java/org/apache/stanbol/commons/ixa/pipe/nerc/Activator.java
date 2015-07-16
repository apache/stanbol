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
package org.apache.stanbol.commons.ixa.pipe.nerc;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.model.ArtifactSerializer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import eus.ixa.ixa.pipe.nerc.dict.BrownCluster.BrownClusterSerializer;
import eus.ixa.ixa.pipe.nerc.dict.ClarkCluster.ClarkClusterSerializer;
import eus.ixa.ixa.pipe.nerc.dict.Dictionary.DictionarySerializer;
import eus.ixa.ixa.pipe.nerc.dict.LemmaResource.LemmaResourceSerializer;
import eus.ixa.ixa.pipe.nerc.dict.MFSResource.MFSResourceSerializer;
import eus.ixa.ixa.pipe.nerc.dict.POSModelResource.POSModelResourceSerializer;
import eus.ixa.ixa.pipe.nerc.dict.Word2VecCluster.Word2VecClusterSerializer;
import eus.ixa.ixa.pipe.nerc.features.BigramClassFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.BrownBigramFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.BrownTokenClassFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.BrownTokenFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.BrownTrigramFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.CharacterNgramFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.ClarkFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.DictionaryFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.FivegramClassFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.FourgramClassFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.MFSFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.MorphoFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.OutcomePriorFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.Prefix34FeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.Prev2MapFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.PreviousMapFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.PreviousMapTokenFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.SentenceFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.SuffixFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.SuperSenseFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.TokenClassFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.TokenFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.TrigramClassFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.WindowFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.Word2VecClusterFeatureGenerator;
import eus.ixa.ixa.pipe.nerc.features.WordShapeSuperSenseFeatureGenerator;

/**
 * This registers ixa extensions to OpenNLP as OSGI services so that OpenNLP
 * can also find them when running within an OSGI environment.
 *
 * OpenNLP expects extensions to be registered as OSGI services wiht the 
 * <code>opennlp</code> property set to the classname of the serice.
 *
 * This Bundle Activator manually registers the OpenNLP extensions provided
 * by the IXA NEC module as expected by OpenNLP
 *
 *
 *
 * @author Rupert Westenthaler
 *
 */
public class Activator implements BundleActivator{

    private final Set<ServiceRegistration<?>> registeredServices = new HashSet<ServiceRegistration<?>>();
    
    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String,Object> prop = new Hashtable<String,Object>();
        //(A) ArtifactSerializer
        //(1) Word2VecCluster
        //OpenNLP expects extensions to have their class name as value of the
        //"opennlp" property.
        prop.put("opennlp", Word2VecClusterSerializer.class.getName());
        registeredServices.add(context.registerService(
                ArtifactSerializer.class.getName(), 
                new Word2VecClusterSerializer(), 
                prop));
        //(2) ClarkClusterSerializer
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", ClarkClusterSerializer.class.getName());
        registeredServices.add(context.registerService(
                ArtifactSerializer.class.getName(), 
                new ClarkClusterSerializer(), 
                prop));
        //(3) ClarkClusterSerializer
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", BrownClusterSerializer.class.getName());
        registeredServices.add(context.registerService(
                ArtifactSerializer.class.getName(), 
                new BrownClusterSerializer(), 
                prop));
        //(4) POSModelResourceSerializer
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", POSModelResourceSerializer.class.getName());
        registeredServices.add(context.registerService(
                ArtifactSerializer.class.getName(), 
                new POSModelResourceSerializer(), 
                prop));
        //(5) MFSResourceSerializer
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", MFSResourceSerializer.class.getName());
        registeredServices.add(context.registerService(
                ArtifactSerializer.class.getName(), 
                new MFSResourceSerializer(), 
                prop));
        //(6) LemmaResourceSerializer
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", LemmaResourceSerializer.class.getName());
        registeredServices.add(context.registerService(
                ArtifactSerializer.class.getName(), 
                new LemmaResourceSerializer(), 
                prop));
        //(7) DictionarySerializer
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", DictionarySerializer.class.getName());
        registeredServices.add(context.registerService(
                ArtifactSerializer.class.getName(), 
                new DictionarySerializer(), 
                prop));
        
        //(B) AdaptiveFeatureGenerator
        //(1) BigramClassFeatureGenerator
        String[] services = new String[]{AdaptiveFeatureGenerator.class.getName()};
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", BigramClassFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new BigramClassFeatureGenerator(), 
                prop));
        //(2) BrownBigramFeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", BrownBigramFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new BrownBigramFeatureGenerator(), 
                prop));
        //(3) BrownTokenClassFeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", BrownTokenClassFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new BrownTokenClassFeatureGenerator(), 
                prop));
        //(4) BrownTokenFeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", BrownTokenFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new BrownTokenFeatureGenerator(), 
                prop));
        //(5) BrownTrigramFeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", BrownTrigramFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new BrownTrigramFeatureGenerator(), 
                prop));
        //(6) CharacterNgramFeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", CharacterNgramFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new CharacterNgramFeatureGenerator(), 
                prop));
        //(7) ClarkFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", ClarkFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new ClarkFeatureGenerator(), 
                prop));
        //(8) DictionaryFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", DictionaryFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new DictionaryFeatureGenerator(), 
                prop));
        //(9) FivegramClassFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", FivegramClassFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new FivegramClassFeatureGenerator(), 
                prop));
        //(10) FourgramClassFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", FourgramClassFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new FourgramClassFeatureGenerator(), 
                prop));
        //(11) MFSFeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", MFSFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new MFSFeatureGenerator(), 
                prop));
        //(12) MorphoFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", MorphoFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new MorphoFeatureGenerator(), 
                prop));
        //(13) OutcomePriorFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", OutcomePriorFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new OutcomePriorFeatureGenerator(), 
                prop));
        //(14) Prefix34FeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", Prefix34FeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new Prefix34FeatureGenerator(), 
                prop));
        //(15) Prev2MapFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", Prev2MapFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new Prev2MapFeatureGenerator(), 
                prop));
        //(16) PreviousMapFeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", PreviousMapFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new PreviousMapFeatureGenerator(), 
                prop));
        //(17) PreviousMapTokenFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", PreviousMapTokenFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new PreviousMapTokenFeatureGenerator(), 
                prop));
        //(18) SentenceFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", SentenceFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new SentenceFeatureGenerator(), 
                prop));
        //(19) SuffixFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", SuffixFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new SuffixFeatureGenerator(), 
                prop));
        //(20) SuperSenseFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", SuperSenseFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new SuperSenseFeatureGenerator(), 
                prop));
        //(21) TokenClassFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", TokenClassFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new TokenClassFeatureGenerator(), 
                prop));
        //(22) TokenFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", TokenFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new TokenFeatureGenerator(), 
                prop));
        //(23) TrigramClassFeatureGenerator
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", TrigramClassFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new TrigramClassFeatureGenerator(), 
                prop));
        //(24) WindowFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", WindowFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new WindowFeatureGenerator(), 
                prop));
        //(25) Word2VecClusterFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", Word2VecClusterFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new Word2VecClusterFeatureGenerator(), 
                prop));
        //(26) WordShapeSuperSenseFeatureGenerator 
        prop = new Hashtable<String,Object>();
        prop.put("opennlp", WordShapeSuperSenseFeatureGenerator.class.getName());
        registeredServices.add(context.registerService(services, 
                new WordShapeSuperSenseFeatureGenerator(), 
                prop));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for(ServiceRegistration<?> reg : registeredServices){
            reg.unregister();
        }
    }

}
