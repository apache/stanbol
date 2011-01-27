package org.apache.stanbol.enhancer.interaction.speech;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import marytts.client.MaryClient;
import marytts.client.http.Address;
import marytts.server.Mary;

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

public class Tts {

    private MaryClient mary;

    public Tts (String serverHost, int serverPort, String maryTtsInstallDir) throws Exception {
        boolean startClient = false;
        //start server
        if (maryTtsInstallDir == null) {
            //            we assume that a server is already started elsewhere (<serverHost>:<serverPort>)
            startClient = true;
        } else {
            System.setProperty("mary.base", maryTtsInstallDir);
            Mary.main(new String[0]);
        }

        //start client and connect to Mary TTS server
        if (startClient) {
            mary = MaryClient.getMaryClient(new Address(serverHost, serverPort));
        } else {
            System.err.println("Could not start Mary TTS server!");
        }
    }

    public byte[] doTextToSpeech (String text) {
        if (mary == null) {
            System.err.println("No Mary Client started!");
            return new byte[0];
        }
        else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                mary.process(text, "TEXT", "AUDIO", "en-GB", "WAVE", "dfki-obadiah", baos);
            } catch (IOException e) {
                e.printStackTrace();
                return new byte[0];
            }
            return baos.toByteArray();
        }
    }

}
