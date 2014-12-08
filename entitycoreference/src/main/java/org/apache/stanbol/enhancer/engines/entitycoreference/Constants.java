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
    public final static String PLACE_ADJECTIVALS_FOLDER = DATA_FOLDER + "/place_adjectivals";

    private Constants() {}
}
