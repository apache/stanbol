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
package org.apache.stanbol.contenthub.servicesapi.ldpath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A collection class for the managed LDPath programs in the Contenthub. Operates on a {@link Map} keeping the
 * &lt;name,program> pairs.
 * 
 * @author anil.sinaci
 * 
 */
public class LDProgramCollection {

    private Map<String,String> nameProgramMap;

    /**
     * Creates an {@link LDProgramCollection} based on a provide {@link Map} keeping &lt;name,program> pairs.
     * 
     * @param nameProgramMap
     *            On which the {@link LDProgramCollection} will be initialized.
     */
    public LDProgramCollection(Map<String,String> nameProgramMap) {
        this.nameProgramMap = nameProgramMap;
    }

    /**
     * This method returns the list of LDPath programs stored in the scope of Contenthub.
     * 
     * @return {@link List} of {@link LDProgram}s.
     */
    public List<LDProgram> asList() {
        List<LDProgram> list = new ArrayList<LDProgram>();
        for (Map.Entry<String,String> entry : nameProgramMap.entrySet()) {
            list.add(new LDProgram(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    /**
     * This method returns the programs stored in the scope of Contenthub as a {@link Map}.
     * 
     * @return {@link Map} keeping the &lt;name,program> pairs.
     */
    public Map<String,String> asMap() {
        return this.nameProgramMap;
    }

    /**
     * This method returns the names of LDPath programs stored in the scope of Contenthub.
     * 
     * @return {@link List} of program names.
     */
    public List<String> getProgramNames() {
        return new ArrayList<String>(nameProgramMap.keySet());
    }

    /**
     * This method returns the LDPath programs themselves that are stored in the scope of Contenthub.
     * 
     * @return {@link List} of programs.
     */
    public List<String> getPrograms() {
        return new ArrayList<String>(nameProgramMap.values());
    }

}
