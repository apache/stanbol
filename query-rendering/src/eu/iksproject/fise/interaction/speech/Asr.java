package org.apache.stanbol.enhancer.interaction.speech;

/*
 * Copyright 2010
 * German Research Center for Artificial Intelligence (DFKI)
 * Department of Intelligent User Interfaces
 * Germany
 *
 *     http://www.dfki.de/web/forschung/iui
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 *     Sebastian Germesin
 *     Massimo Romanelli
 *     Tilman Becker
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class Asr {

    private Recognizer recognizer;
    private JSGFGrammar jsgfGrammarManager;
    private StreamDataSource inputStreamDataSource;

    public Asr (String configFilename) throws IOException, PropertyException, InstantiationException {

        URL url = new File(configFilename).toURI().toURL();
        ConfigurationManager cm = new ConfigurationManager(url);

        // retrive the recognizer, jsgfGrammar and the microphone from
        // the configuration file.

        recognizer = (Recognizer) cm.lookup("recognizer");
        jsgfGrammarManager = (JSGFGrammar) cm.lookup("jsgfGrammar");
        inputStreamDataSource = (StreamDataSource) cm.lookup("inputStreamDataSource");

        System.out.print(" Loading recognizer ...");
        recognizer.allocate();

        dumpSampleSentences();
    }

    protected String recognizeSpeech (byte[] audioData) {
        InputStream input = new ByteArrayInputStream(audioData);
        inputStreamDataSource.setInputStream(input, ByteArrayInputStream.class.getName());

        Result result = recognizer.recognize();
        if (result == null)
            return null;
        else
            return result.getBestFinalResultNoFiller();
    }

    protected void dumpSampleSentences() {
        System.out.println("Speak one of: \n");
        jsgfGrammarManager.dumpRandomSentences(200);
    }

    protected void deallocate () {
        recognizer.deallocate();
    }

}
