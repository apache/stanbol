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
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Playback {

    public AudioFormat audioFormat;

    private Boolean stopPlayback = false;
    private Boolean playbackInProgress = false;

    public Playback () {
        this(16000, 16, 1, true, true);
    }

    public Playback (AudioFormat af) {
        this(af.getSampleRate(), af.getSampleSizeInBits(), af.getChannels(), !af.toString().toLowerCase().contains("unsigned"), af.isBigEndian());
    }

    public Playback (float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public AudioFormat getAudioFormat () {
        return audioFormat;
    }

    public boolean isPlaybacking () {
        return playbackInProgress;
    }

    public void setAudioFormat (AudioFormat af) throws PlaybackInProgressException {
        synchronized (playbackInProgress) {
            if (playbackInProgress) {
                throw new PlaybackInProgressException ();
            }
            audioFormat = af;
        }
    }

    public void stopPlayback () {
        synchronized (stopPlayback) {
            stopPlayback = true;
        }
    }

    public void playAudio (final byte[] audioData, boolean asynchronous) throws PlaybackInProgressException {
        synchronized (playbackInProgress) {
            if (playbackInProgress) {
                throw new PlaybackInProgressException ();
            }
            else
                playbackInProgress = true;
        }
        synchronized (stopPlayback) {
            stopPlayback = false;
        }

        Runnable runner = new Runnable() {
            int bufferSize = (int) audioFormat.getSampleRate() * audioFormat.getFrameSize();
            byte buffer[] = new byte[bufferSize];

            public void run() {
                try {
                    InputStream input = new ByteArrayInputStream(audioData);
                    final AudioInputStream ais = new AudioInputStream(input, audioFormat, audioData.length / audioFormat.getFrameSize());
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                    final SourceDataLine line = (SourceDataLine)
                    AudioSystem.getLine(info);
                    line.open(audioFormat);
                    line.start();
                    int count;
                    while (!stopPlayback && (count = ais.read(buffer, 0, buffer.length)) != -1) {
                        if (count > 0) {
                            line.write(buffer, 0, count);
                        }
                        synchronized (stopPlayback) {
                            if (stopPlayback)
                                break;
                        }
                    }
                    line.drain();
                    line.close();
                } catch (IOException e) {
                    System.err.println("I/O problems: " + e);
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                } finally {
                    synchronized (playbackInProgress) {
                        playbackInProgress = false;
                    }
                }
            }
        };

        if (asynchronous) {
            ExecutorService es = Executors.newSingleThreadExecutor();
            es.execute(runner);
        }
        else {
            runner.run();
        }
    }

}
