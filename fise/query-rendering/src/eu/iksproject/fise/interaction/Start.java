package org.apache.stanbol.enhancer.interaction;

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

import org.apache.stanbol.enhancer.interaction.event.EventManager;
import org.apache.stanbol.enhancer.interaction.gui.Gui;
import org.apache.stanbol.enhancer.interaction.speech.NL2Sparql;
import org.apache.stanbol.enhancer.interaction.speech.SpeechServer;

public class Start {

    public static final boolean USE_TTS = false;
    static final String INITIAL_CLEREZZA_SERVER_HOST = "localhost";
    static final int INITIAL_CLEREZZA_SERVER_PORT = 8080;
    static final String INITIAL_CLEREZZA_SERVER_USERNAME = "admin";
    static final String INITIAL_CLEREZZA_SERVER_PASSWORD = "admin";

    public static void main (String[] args) {
        Gui gui = new Gui(INITIAL_CLEREZZA_SERVER_HOST,
                INITIAL_CLEREZZA_SERVER_PORT,
                INITIAL_CLEREZZA_SERVER_USERNAME,
                INITIAL_CLEREZZA_SERVER_PASSWORD);
        SpeechServer speechServer = new SpeechServer();
        NL2Sparql nl2sparql = new NL2Sparql();
        QueryManager queryManager = new QueryManager(INITIAL_CLEREZZA_SERVER_HOST,
                INITIAL_CLEREZZA_SERVER_PORT,
                INITIAL_CLEREZZA_SERVER_USERNAME,
                INITIAL_CLEREZZA_SERVER_PASSWORD);

        EventManager.addEventListener(gui);
        EventManager.addEventListener(speechServer);
        EventManager.addEventListener(nl2sparql);
        EventManager.addEventListener(queryManager);
    }

}
