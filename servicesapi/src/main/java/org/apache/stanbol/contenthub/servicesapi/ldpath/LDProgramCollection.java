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
