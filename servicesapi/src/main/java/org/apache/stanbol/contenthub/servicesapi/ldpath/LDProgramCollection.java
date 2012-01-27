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
 * @author anil.sinaci
 * 
 */
public class LDProgramCollection {

    private Map<String,String> nameProgramMap;

    public LDProgramCollection(Map<String,String> nameProgramMap) {
        this.nameProgramMap = nameProgramMap;
    }

    public List<LDProgram> asList() {
        List<LDProgram> list = new ArrayList<LDProgram>();
        for(Map.Entry<String,String> entry : nameProgramMap.entrySet()) {
            list.add(new LDProgram(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    public Map<String,String> asMap() {
        return this.nameProgramMap;
    }
    
    public List<String> getProgramNames() {
        return new ArrayList<String>(nameProgramMap.keySet());
    }
    
    public List<String> getPrograms() {
        return new ArrayList<String>(nameProgramMap.values());
    }

}
