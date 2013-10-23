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
package org.apache.stanbol.enhancer.nlp.json.valuetype.impl;

import java.util.EnumSet;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.json.JsonUtils;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParserRegistry;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializerRegistry;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.morpho.Case;
import org.apache.stanbol.enhancer.nlp.morpho.CaseTag;
import org.apache.stanbol.enhancer.nlp.morpho.Definitness;
import org.apache.stanbol.enhancer.nlp.morpho.Gender;
import org.apache.stanbol.enhancer.nlp.morpho.GenderTag;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.morpho.NumberFeature;
import org.apache.stanbol.enhancer.nlp.morpho.NumberTag;
import org.apache.stanbol.enhancer.nlp.morpho.Person;
import org.apache.stanbol.enhancer.nlp.morpho.Tense;
import org.apache.stanbol.enhancer.nlp.morpho.TenseTag;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMood;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMoodTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value={ValueTypeParser.class,ValueTypeSerializer.class})
@Property(name=ValueTypeParser.PROPERTY_TYPE, value=MorphoFeaturesSupport.TYPE_VALUE)
public class MorphoFeaturesSupport implements ValueTypeParser<MorphoFeatures>, ValueTypeSerializer<MorphoFeatures> {

    private final Logger log = LoggerFactory.getLogger(MorphoFeaturesSupport.class);
    
    public static final String TYPE_VALUE = "org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures";

    @Reference
    protected ValueTypeSerializerRegistry serializerRegistry;
    @Reference
    protected ValueTypeParserRegistry parserRegistry;
    
    protected ValueTypeSerializer<PosTag> getPosTagSerializer(){
        if(serializerRegistry == null){
            serializerRegistry = ValueTypeSerializerRegistry.getInstance();
        }
        return serializerRegistry.getSerializer(PosTag.class);
    }
    
    protected ValueTypeParser<PosTag> getPosTagParser(){
        if(parserRegistry == null){
            parserRegistry = ValueTypeParserRegistry.getInstance();
        }
        return parserRegistry.getParser(PosTag.class);
    }
    
    @Override
    public Class<MorphoFeatures> getType() {
        return MorphoFeatures.class;
    }

    @Override
    public ObjectNode serialize(ObjectMapper mapper, MorphoFeatures morpho){
        ObjectNode jMorpho = mapper.createObjectNode();
        jMorpho.put("lemma", morpho.getLemma());
        List<CaseTag> caseList = morpho.getCaseList();
        if(!caseList.isEmpty()){
            ArrayNode jCases = mapper.createArrayNode();
            for(CaseTag caseTag : caseList){
                ObjectNode jCase = mapper.createObjectNode();
                jCase.put("tag", caseTag.getTag());
                if(caseTag.getCase() != null){
                    jCase.put("type", caseTag.getCase().name());
                }
                jCases.add(jCase);
            }
            jMorpho.put("case", jCases);
        }
        List<Definitness> definitnesses = morpho.getDefinitnessList();
        if(!definitnesses.isEmpty()){
            if(definitnesses.size() == 1){
                jMorpho.put("definitness",definitnesses.get(0).name());
            } else {
                ArrayNode jDefinitnesses = mapper.createArrayNode();
                for(Definitness d : definitnesses){
                    jDefinitnesses.add(d.name());
                }
                jMorpho.put("definitness", jDefinitnesses);
            }
        }
        List<GenderTag> genderList = morpho.getGenderList();
        if(!genderList.isEmpty()){
            ArrayNode jGenders = mapper.createArrayNode();
            for(GenderTag genderTag : genderList){
                ObjectNode jGender = mapper.createObjectNode();
                jGender.put("tag", genderTag.getTag());
                if(genderTag.getGender() != null){
                    jGender.put("type", genderTag.getGender().name());
                }
                jGenders.add(jGender);
            }
            jMorpho.put("gender", jGenders);
        }
        List<NumberTag> numberList = morpho.getNumberList();
        if(!numberList.isEmpty()){
            ArrayNode jNumbers = mapper.createArrayNode();
            for(NumberTag numberTag : numberList){
                ObjectNode jNumber = mapper.createObjectNode();
                jNumber.put("tag", numberTag.getTag());
                if(numberTag.getNumber() != null){
                    jNumber.put("type", numberTag.getNumber().name());
                }
                jNumbers.add(jNumber);
            }
            jMorpho.put("number", jNumbers);
        }
        List<Person> persons = morpho.getPersonList();
        if(!persons.isEmpty()){
            if(persons.size() == 1){
                jMorpho.put("person",persons.get(0).name());
            } else {
                ArrayNode jPersons = mapper.createArrayNode();
                for(Person d : persons){
                    jPersons.add(d.name());
                }
                jMorpho.put("person", jPersons);
            }
        }
        List<PosTag> posList = morpho.getPosList();
        if(!posList.isEmpty()){
            ArrayNode jPosTags = mapper.createArrayNode();
            for(PosTag posTag : posList){
                jPosTags.add(getPosTagSerializer().serialize(mapper,posTag));
            }
            jMorpho.put("pos", jPosTags);
        }
        List<TenseTag> tenseList = morpho.getTenseList();
        if(!tenseList.isEmpty()){
            ArrayNode jTenses = mapper.createArrayNode();
            for(TenseTag tenseTag : tenseList){
                ObjectNode jTense = mapper.createObjectNode();
                jTense.put("tag", tenseTag.getTag());
                if(tenseTag.getTense() != null){
                    jTense.put("type", tenseTag.getTense().name());
                }
                jTenses.add(jTense);
            }
            jMorpho.put("tense", jTenses);
        }
        List<VerbMoodTag> verbMoodList = morpho.getVerbMoodList();
        if(!verbMoodList.isEmpty()){
            ArrayNode jMoods = mapper.createArrayNode();
            for(VerbMoodTag verbMoodTag : verbMoodList){
                ObjectNode jMood = mapper.createObjectNode();
                jMood.put("tag", verbMoodTag.getTag());
                if(verbMoodTag.getVerbForm() != null){
                    jMood.put("type", verbMoodTag.getVerbForm().name());
                }
                jMoods.add(jMood);
            }
            jMorpho.put("verb-mood", jMoods);
        }
        
        return jMorpho;
    }


    @Override
    public MorphoFeatures parse(ObjectNode jMorpho, AnalysedText at) {
        JsonNode jLemma = jMorpho.path("lemma");
        if(!jLemma.isTextual()){
            throw new IllegalStateException("Field 'lemma' MUST provide a String value (parsed JSON: "
                +jMorpho);
        }
        MorphoFeatures morpho = new MorphoFeatures(jLemma.asText());
        JsonNode node = jMorpho.path("case");
        if(node.isArray()){
            ArrayNode jCases = (ArrayNode)node;
            for(int i=0;i<jCases.size();i++){
                JsonNode member = jCases.get(i);
                if(member.isObject()){
                    ObjectNode jCase = (ObjectNode)member;
                    JsonNode tag = jCase.path("tag");
                    if(tag.isTextual()){
                        EnumSet<Case> type = JsonUtils.parseEnum(jCase, "type", Case.class);
                        if(type.isEmpty()){
                            morpho.addCase(new CaseTag(tag.getTextValue()));
                        } else {
                            morpho.addCase(new CaseTag(tag.getTextValue(),type.iterator().next()));
                        }
                    } else {
                        log.warn("Unable to parse CaseTag becuase 'tag' value is "
                            + "missing or is not a String (json: "+jCase.toString()+")");
                    }
                } else {
                    log.warn("Unable to parse CaseTag from "+member.toString());
                }
            }
        } else if(!node.isMissingNode()) {
            log.warn("Unable to parse CaseTags (Json Array expected as value for field 'case' but was "
                + node);
        }
        if(jMorpho.has("definitness")){
            for(Definitness d : JsonUtils.parseEnum(jMorpho, "definitness", Definitness.class)){
                morpho.addDefinitness(d);
            }
        }
        node = jMorpho.path("gender");
        if(node.isArray()){
            ArrayNode jGenders = (ArrayNode)node;
            for(int i=0;i<jGenders.size();i++){
                JsonNode member = jGenders.get(i);
                if(member.isObject()){
                    ObjectNode jGender = (ObjectNode)member;
                    JsonNode tag = jGender.path("tag");
                    if(tag.isTextual()){
                        EnumSet<Gender> type = JsonUtils.parseEnum(jGender, "type", Gender.class);
                        if(type.isEmpty()){
                            morpho.addGender(new GenderTag(tag.getTextValue()));
                        } else {
                            morpho.addGender(new GenderTag(tag.getTextValue(),type.iterator().next()));
                        }
                    } else {
                        log.warn("Unable to parse GenderTag becuase 'tag' value is "
                                + "missing or is not a String (json: "+jGender.toString()+")");
                    }
                } else {
                    log.warn("Unable to parse GenderTag from "+member.toString());
                }
            }
        } else if(!node.isMissingNode()) {
            log.warn("Unable to parse GenderTag (Json Array expected as value for field 'case' but was "
                    + node);
        }
        
        node = jMorpho.path("number");
        if(node.isArray()){
            ArrayNode jNumbers = (ArrayNode)node;
            for(int i=0;i<jNumbers.size();i++){
                JsonNode member = jNumbers.get(i);
                if(member.isObject()){
                    ObjectNode jNumber = (ObjectNode)member;
                    JsonNode tag = jNumber.path("tag");
                    if(tag.isTextual()){
                        EnumSet<NumberFeature> type = JsonUtils.parseEnum(jNumber, "type", NumberFeature.class);
                        if(type.isEmpty()){
                            morpho.addNumber(new NumberTag(tag.getTextValue()));
                        } else {
                            morpho.addNumber(new NumberTag(tag.getTextValue(),type.iterator().next()));
                        }
                    } else {
                        log.warn("Unable to parse NumberTag becuase 'tag' value is "
                                + "missing or is not a String (json: "+jNumber.toString()+")");
                    }
                } else {
                    log.warn("Unable to parse NumberTag from "+member.toString());
                }
            }
        } else if(!node.isMissingNode()) {
            log.warn("Unable to parse NumberTag (Json Array expected as value for field 'case' but was "
                    + node);
        }
        
        if(jMorpho.has("person")){
            for(Person p : JsonUtils.parseEnum(jMorpho, "person", Person.class)){
                morpho.addPerson(p);
            }
        }

        node = jMorpho.path("pos");
        if(node.isArray()){
            ArrayNode jPosTags = (ArrayNode)node;
            for(int i=0;i<jPosTags.size();i++){
                JsonNode member = jPosTags.get(i);
                if(member.isObject()){
                    ObjectNode jPosTag = (ObjectNode)member;
                    morpho.addPos(getPosTagParser().parse(jPosTag, at));
                } else {
                    log.warn("Unable to parse PosTag from "+member.toString());
                }
            }
        } else if(!node.isMissingNode()){
            log.warn("Unable to parse PosTag (Json Array expected as value for field 'case' but was "
                    + node);
        }
        
        node = jMorpho.path("tense");
        if(node.isArray()){
            ArrayNode jTenses = (ArrayNode)node;
            for(int i=0;i<jTenses.size();i++){
                JsonNode member = jTenses.get(i);
                if(member.isObject()){
                    ObjectNode jTense = (ObjectNode)member;
                    JsonNode tag = jTense.path("tag");
                    if(tag.isTextual()){
                        EnumSet<Tense> type = JsonUtils.parseEnum(jTense, "type", Tense.class);
                        if(type.isEmpty()){
                            morpho.addTense(new TenseTag(tag.getTextValue()));
                        } else {
                            morpho.addTense(new TenseTag(tag.getTextValue(),type.iterator().next()));
                        }
                    } else {
                        log.warn("Unable to parse TenseTag becuase 'tag' value is "
                                + "missing or is not a String (json: "+jTense.toString()+")");
                    }
                } else {
                    log.warn("Unable to parse TenseTag from "+member.toString());
                }
            }
        } else if(!node.isMissingNode()) {
            log.warn("Unable to parse TenseTag (Json Array expected as value for field 'case' but was "
                    + node);
        }
        
        node = jMorpho.path("verb-mood");
        if(node.isArray()){
            ArrayNode jVerbMoods = (ArrayNode)node;
            for(int i=0;i<jVerbMoods.size();i++){
                JsonNode member = jVerbMoods.get(i);
                if(member.isObject()){
                    ObjectNode jVerbMood = (ObjectNode)member;
                    JsonNode tag = jVerbMood.path("tag");
                    if(tag.isTextual()){
                        EnumSet<VerbMood> type = JsonUtils.parseEnum(jVerbMood, "type", VerbMood.class);
                        if(type.isEmpty()){
                            morpho.addVerbForm(new VerbMoodTag(tag.getTextValue()));
                        } else {
                            morpho.addVerbForm(new VerbMoodTag(tag.getTextValue(),type.iterator().next()));
                        }
                    } else {
                        log.warn("Unable to parse VerbMoodTag becuase 'tag' value is "
                                + "missing or is not a String (json: "+jVerbMood.toString()+")");
                    }
                } else {
                    log.warn("Unable to parse VerbMoodTag from "+member.toString());
                }
            }
        } else if(!node.isMissingNode()) {
            log.warn("Unable to parse VerbMoodTag (Json Array expected as value for field 'case' but was "
                    + node);
        }
        
        return morpho;
    }

}
