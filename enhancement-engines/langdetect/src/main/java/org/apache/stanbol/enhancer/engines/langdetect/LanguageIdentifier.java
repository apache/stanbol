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
package org.apache.stanbol.enhancer.engines.langdetect;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

/**
 * Standalone version of the Language Identifier
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 * 
 */

public class LanguageIdentifier {
    
    public LanguageIdentifier() throws LangDetectException {
        DetectorFactory.clear();
        try {
            DetectorFactory.loadProfile(loadProfiles("profiles","profiles.cfg"));
        } catch (Exception e) {
            throw new LangDetectException(null, "Error in Initialization: "+e.getMessage());
        } 
    }
    /**
     * Load the profiles from the classpath
     * @param folder where the profiles are
     * @param configFile specifies which language profiles should be used
     * @return a list of profiles
     * @throws Exception
     */
    public List<String> loadProfiles(String folder, String configFile) throws Exception {
        List<String> profiles = new ArrayList<String>();
        java.util.Properties props = new java.util.Properties();
        props.load(getClass().getClassLoader().getResourceAsStream(configFile));
        String languages = props.getProperty("languages");
        if (languages == null) {
            throw new IOException("No languages defined");
        }
        for (String lang: languages.split(",")) {
            String profileFile = folder+"/"+lang;
            InputStream is = getClass().getClassLoader().getResourceAsStream(profileFile);
            try {
                String profile = IOUtils.toString(is, "UTF-8");
                if (profile != null && profile.length() > 0) {
                    profiles.add(profile);
                }
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return profiles;
    }
    
    public String getLanguage(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.detect();
    }
    
    public List<Language> getLanguages(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.getProbabilities();
    }

}
