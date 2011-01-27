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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.stanbol.enhancer.interaction.Start;
import org.apache.stanbol.enhancer.interaction.event.Event;
import org.apache.stanbol.enhancer.interaction.event.EventListener;
import org.apache.stanbol.enhancer.interaction.event.EventManager;
import org.apache.stanbol.enhancer.interaction.event.PlaybackAudioEvent;
import org.apache.stanbol.enhancer.interaction.event.RecognizedSpeechEvent;
import org.apache.stanbol.enhancer.interaction.event.RecordedAudioEvent;
import org.apache.stanbol.enhancer.interaction.event.TtsEvent;

public class SpeechServer implements EventListener {

    private Asr asr;
    private Tts tts;

    public SpeechServer () {
        ExecutorService es = Executors.newFixedThreadPool(2);
        Callable<Asr> asrRunner = new Callable<Asr>() {

            public Asr call () {
                try {
                    return new Asr("resource/sphinx4_config.xml");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        Callable<Tts> ttsRunner = new Callable<Tts>() {

            public Tts call () {
                if (Start.USE_TTS) {
                    try {
                        return new Tts("localhost", 59125, "/Applications/MARYTTS/");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };

        Future<Asr> futureASR = es.submit(asrRunner);
        Future<Tts> futureTTS = es.submit(ttsRunner);

        try {
            asr = futureASR.get();
            tts = futureTTS.get();
        } catch (InterruptedException e) {
            //            e.printStackTrace();
        } catch (ExecutionException e) {
            //            e.printStackTrace();
        }
    }

    public void eventOccurred(Event e) {
        if (e instanceof RecordedAudioEvent) {
            RecordedAudioEvent rae = (RecordedAudioEvent)e;

            if (asr != null) {
                String recognizedSpeech = asr.recognizeSpeech(rae.getData());
                if (recognizedSpeech == null || recognizedSpeech.equals(""))
                    recognizedSpeech = null;

                System.out.println("RECOGNIZED SPEECH: '" + recognizedSpeech + "'");
                RecognizedSpeechEvent rse = new RecognizedSpeechEvent(recognizedSpeech);
                EventManager.eventOccurred(rse);
            }
            else {
                System.err.println("No ASR available!");
            }
        }
        else if (e instanceof TtsEvent) {
            TtsEvent te = (TtsEvent)e;

            if (tts != null) {
                byte[] audioData = tts.doTextToSpeech(te.getData());
                PlaybackAudioEvent pae = new PlaybackAudioEvent(audioData);
                EventManager.eventOccurred(pae);
            }
            else {
                System.err.println("No Mary TTS available!");
            }

        }
    }

}
