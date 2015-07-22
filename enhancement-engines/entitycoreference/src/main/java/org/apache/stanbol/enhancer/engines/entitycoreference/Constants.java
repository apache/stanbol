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
package org.apache.stanbol.enhancer.engines.entitycoreference;

/**
 * Constants used engine wide.
 * 
 * @author Cristian Petroaca
 * 
 */
public final class Constants {
	/**
	 * The main config folder of the engine
	 */
	public final static String CONFIG_FOLDER = "/config";

	/**
	 * The main data folder
	 */
	public final static String DATA_FOLDER = "/data";

	/**
	 * The path to the pos config folder.
	 */
	public final static String POS_CONFIG_FOLDER = CONFIG_FOLDER + "/pos";

	/**
	 * The path to the place adjectivals folder.
	 */
	public final static String PLACE_ADJECTIVALS_FOLDER = DATA_FOLDER
			+ "/place_adjectivals";

	public static final int MAX_DISTANCE_DEFAULT_VALUE = 1;
	
    public static final int MAX_DISTANCE_NO_CONSTRAINT = -1;
    
	public final static String DEFAULT_SPATIAL_ATTR_FOR_PERSON = "http://dbpedia.org/ontology/birthPlace,"
			+ "http://dbpedia.org/ontology/region,http://dbpedia.org/ontology/nationality,http://dbpedia.org/ontology/country";

	public final static String DEFAULT_SPATIAL_ATTR_FOR_ORGANIZATION = "http://dbpedia.org/ontology/foundationPlace,"
			+ "http://dbpedia.org/ontology/locationCity,http://dbpedia.org/ontology/location,http://dbpedia.org/ontology/hometown";
	
	public final static String DEFAULT_SPATIAL_ATTR_FOR_PLACE = "http://dbpedia.org/ontology/country,"
			+ "http://dbpedia.org/ontology/subdivisionName,http://dbpedia.org/ontology/location";
	
	public final static String DEFAULT_ENTITY_CLASSES_TO_EXCLUDE = "http://dbpedia.org/ontology/Person,"
			+ "http://dbpedia.org/class/yago/LivingThing100004258,http://dbpedia.org/class/yago/PhysicalEntity100001930,"
			+ "http://dbpedia.org/class/yago/Abstraction100002137,http://dbpedia.org/class/yago/Organism100004475,"
			+ "http://dbpedia.org/class/yago/Location100027167,http://schema.org/Place,http://dbpedia.org/class/yago/Object100002684,"
			+ "http://dbpedia.org/class/yago/YagoGeoEntity,http://www.w3.org/2002/07/owl#Thing,"
			+ "http://dbpedia.org/class/yago/YagoPermanentlyLocatedEntity";
	
	public final static String DEFAULT_ORG_ATTR_FOR_PERSON = "http://dbpedia.org/ontology/occupation,"
			+ "http://dbpedia.org/ontology/associatedBand,http://dbpedia.org/ontology/employer";
	
	private Constants() {
	}
}
