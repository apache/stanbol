package org.apache.stanbol.launchpad;

import static org.apache.sling.launchpad.base.shared.SharedConstants.SLING_HOME;

import java.io.File;

public class Main {

    public static final String DEFAULT_STANBOL_HOME = "stanbol";
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        String home = System.getProperties().getProperty(SLING_HOME);
        if(home == null){
            home = new File(DEFAULT_STANBOL_HOME).getAbsolutePath();
            System.setProperty(SLING_HOME, home);
        } //else do not override user configured values
        //now use the standard Apache Sling launcher to do the job
        org.apache.sling.launchpad.app.Main.main(args);
    }

}
