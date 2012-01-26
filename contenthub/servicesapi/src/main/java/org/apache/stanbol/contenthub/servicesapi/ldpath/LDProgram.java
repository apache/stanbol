package org.apache.stanbol.contenthub.servicesapi.ldpath;

/**
 * @author anil.sinaci
 *
 */
public class LDProgram {

    private String name;
    private String ldPathProgram;

    public LDProgram(String name, String ldPathProgram) {
        this.name = name;
        this.ldPathProgram = ldPathProgram;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getLdPathProgram() {
        return ldPathProgram;
    }
    public void setLdPathProgram(String ldPathProgram) {
        this.ldPathProgram = ldPathProgram;
    }
    
}
