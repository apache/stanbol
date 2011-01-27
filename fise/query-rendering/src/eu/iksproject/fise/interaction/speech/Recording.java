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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Recording {

    private AudioFormat audioFormat;
    private Boolean stopRecording = false;
    private Boolean recordingInProgress = false;

    public Recording () {
        this(16000, 16, 1, true, true);
    }

    public Recording (AudioFormat af) {
        this(af.getSampleRate(), af.getSampleSizeInBits(), af.getChannels(), !af.toString().toLowerCase().contains("unsigned"), af.isBigEndian());
    }

    public Recording (float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public boolean isRecording () {
        return recordingInProgress;
    }

    public void setAudioFormat (AudioFormat af) throws RecordingInProgressException {
        synchronized (recordingInProgress) {
            if (recordingInProgress) {
                throw new RecordingInProgressException ();
            }
            audioFormat = af;
        }
    }

    public void stopRecording () {
        synchronized (stopRecording) {
            stopRecording = true;
        }
    }

    public Future<byte[]> startRecording() throws RecordingInProgressException {
        synchronized (recordingInProgress) {
            if (recordingInProgress) {
                throw new RecordingInProgressException ();
            }
            else
                recordingInProgress = true;
        }
        synchronized (stopRecording) {
            stopRecording = false;
        }

        Callable<byte[]> runner = new Callable<byte[]>() {

            public byte[] call() {
                try {
                    int bufferSize = (int)audioFormat.getSampleRate() * audioFormat.getFrameSize();
                    byte buffer[] = new byte[bufferSize];

                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                    TargetDataLine line = (TargetDataLine)AudioSystem.getLine(info);
                    line.open(audioFormat);
                    line.start();

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    do {
                        int count = line.read(buffer, 0, buffer.length);
                        if (count > 0) {
                            out.write(buffer, 0, count);
                        }
                        synchronized (stopRecording) {
                            if (stopRecording)
                                break;
                        }
                    } while (!stopRecording);
                    out.flush();
                    out.close();
                    line.close();
                    return (out.toByteArray());
                } catch (IOException e) {
                    System.out.println("I/O problems: " + e);
                    return null;
                } catch (LineUnavailableException e) {
                    return null;
                } finally {
                    synchronized (recordingInProgress) {
                        recordingInProgress = false;
                    }
                }
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(runner);
    }

}
