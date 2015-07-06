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
package org.apache.stanbol.entityhub.indexing.core.impl;

import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.PROCESS_DURATION;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.SOURCE_DURATION;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.SOURCE_STARTED;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.STORE_DURATION;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.slf4j.Logger;

public class FinishedEntityDaemon extends IndexingDaemon<Representation,Object> {

    private static final int DEFAULT_MAJOR_INTERVAL = 100000;
    /**
     * For now use an logger as output!
     */
    private Logger out;

    private int major;
    private int minor;
    private double sourceDurationAll;
    private double processDurationAll;
    private double storeDurationAll;
    private double durationAll;
    private double sourceDurationMajor;
    private double processDurationMajor;
    private double storeDurationMajor;
    private double durationMajor;
    private double durationMinor;
    private long start;
    private long startMajor;
    private long startMinor;
    
    private double timeAll;
    private double timeMajor;
    private double timeMinor;
    
    private long count;
    private long countedAll;
    private long countedMajor;
    private long countedMinor;
    /**
     * Allows to write finished ids to a file. one ID per line
     */
    private final BufferedWriter idWriter;
    /**
     * The charset used for the {@link #idWriter}
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    
    public FinishedEntityDaemon(String name, BlockingQueue<QueueItem<Representation>> consume,
                                int majorInterval, Logger out, OutputStream idOut) {
        super(name, IndexerConstants.SEQUENCE_NUMBER_FINISHED_DAEMON,
            consume, null, null);
        this.out = out;
        if(majorInterval > 0){
            this.major = majorInterval;
        } else {
            this.major = DEFAULT_MAJOR_INTERVAL;
        }
        this.minor = major/10;
        if(idOut != null){
            this.idWriter = new BufferedWriter(new OutputStreamWriter(idOut, UTF8));
        } else {
            this.idWriter = null;
        }
    }

    @Override
    public void run() {
        count = 0; //Elements indexed
        //Elements with valid statistics
        countedAll = 0; 
        countedMajor = 0;
        countedMinor = 0;
        long current = System.currentTimeMillis();
        while(!isQueueFinished()){
            QueueItem<Representation> item = consume();
            if(item != null){
                if(idWriter != null && item.getItem() != null){
                    String id = item.getItem().getId();
                    try {
                        if(count != 0){
                            idWriter.newLine();
                        }
                        idWriter.write(id);
                    } catch (Exception e){
                        log.error("Exception while logging ID of indexed Entity '"+id+"'!",e);
                    }
                }
                current = System.currentTimeMillis();
                if(count == 0){
                    start = System.currentTimeMillis(); //default for the start!
                }
                if(startMajor == 0){
                    startMajor = current;
                }
                if(startMinor == 0){
                    startMinor = current;
                }
                count++;
                try {
                    long startSource = ((Long)item.getProperty(SOURCE_STARTED)).longValue();
                    if(count < minor){ //for the first few item
                        //try to get the correct start time for the indexing!
                        if(startSource < start){
                            start = startSource;
                        }
                    }
                    float sourceDuration = ((Float)item.getProperty(SOURCE_DURATION)).floatValue();
                    float processDuration = ((Float)item.getProperty(PROCESS_DURATION)).floatValue();
                    float storeDuration = ((Float)item.getProperty(STORE_DURATION)).floatValue();
                    sourceDurationAll+=sourceDuration;
                    sourceDurationMajor+=sourceDuration;
                    processDurationAll+=processDuration;
                    processDurationMajor+=processDuration;
                    storeDurationAll+=storeDuration;
                    storeDurationMajor+=storeDuration;
                    double duration = sourceDuration+processDuration+storeDuration;
                    durationAll+=duration;
                    durationMajor+=duration;
                    durationMinor+=duration;
                    
                    long time = current-startSource;
                    timeAll+=time;
                    timeMajor+=time;
                    timeMinor+=time;
                    countedAll++;
                    countedMajor++;
                    countedMinor++;
                }catch(Exception e){
                    //ignore NullpointerExceptions that will be thrown on missing
                    //metadata!
                }
                if(count%major == 0){
                    printMajor(current);
                    sourceDurationMajor = 0;
                    processDurationMajor = 0;
                    storeDurationMajor = 0;
                    durationMajor = 0;
                    timeMajor = 0;
                    countedMajor = 0;
                    startMajor = 0;
                    //reset also minor
                    durationMinor = 0;
                    timeMinor = 0;
                    countedMinor = 0;
                    startMinor = 0;
                } else if(count%minor == 0){
                    printMinor(current);
                    durationMinor = 0;
                    timeMinor = 0;
                    countedMinor = 0;
                    startMinor = 0;
                }
                
            }
        }
        printSummary(current);
        IOUtils.closeQuietly(idWriter);
        setFinished();
    }

    private void printMinor(long current) {
        long interval = current-start;
        long intervalMinor = current-startMinor;
//        double itemDurationAll = countedAll>0?durationAll/countedAll:-1;
        double itemDurationMinor = countedMinor>0?durationMinor/countedMinor:-1;
//        double itemTimeAll = countedAll>0?timeAll/countedAll:-1;
        double itemTimeMinor = countedMinor>0?timeMinor/countedMinor:-1;
        out.info(String.format("    - %d items in %dsec (last %d in %dsec | %7.3fms/item | %7.3fms in queue)",
            count,(int)interval/1000,minor,(int)intervalMinor/1000,itemDurationMinor,itemTimeMinor));
    }

    private void printMajor(long current) {
        long interval = current-start;
        long intervalMajor = current-startMajor;
        double itemDurationAll = countedAll>0?durationAll/countedAll:-1;
        double itemDurationMajor = countedMajor>0?durationMinor/countedMajor:-1;
        double itemTimeAll = countedAll>0?timeAll/countedAll:-1;
        double itemTimeMajor = countedMajor>0?timeMajor/countedMajor:-1;
        
        double itemSourceDurationAll = countedAll>0? sourceDurationAll/countedAll:-1;
        double itemProcessingDurationAll = countedAll>0? processDurationAll/countedAll:-1;
        double itemStoreDurationAll = countedAll>0? storeDurationAll/countedAll:-1;
        
        double itemSourceDurationMajor = countedMajor>0? sourceDurationMajor/countedMajor:-1;
        double itemProcessingDurationMajor = countedMajor>0? processDurationMajor/countedMajor:-1;
        double itemStoreDurationMajor = countedMajor>0? storeDurationMajor/countedMajor:-1;
        out.info(String.format("+ %d items in %dsec (%7.3fms/item): processing: %7.3fms/item | queue: %7.3fms",
            count,(int)interval/1000,(float)interval/count,itemDurationAll,itemTimeAll));
        out.info(String.format("  last %d items in %dsec (%7.3fms/item): processing %7.3fms/item | queue: %7.3fms",
            major,(int)intervalMajor/1000,(float)intervalMajor/major,itemDurationMajor,itemTimeMajor));
        out.info(String.format("  - source   : all: %7.3fms/item | current: %7.3fms/item",
            itemSourceDurationAll,itemSourceDurationMajor));
        out.info(String.format("  - processing: all: %7.3fms | current: %7.3fms/item",
            itemProcessingDurationAll,itemProcessingDurationMajor));
        out.info(String.format("  - store     : all: %7.3fms | current: %7.3fms/item",
            itemStoreDurationAll,itemStoreDurationMajor));
    }
    private void printSummary(long current){
        long interval = current-start;
        double itemDurationAll = countedAll>0?durationAll/countedAll:-1;
        double itemTimeAll = countedAll>0?timeAll/countedAll:-1;
        double itemSourceDurationAll = countedAll>0? sourceDurationAll/countedAll:-1;
        double itemProcessingDurationAll = countedAll>0? processDurationAll/countedAll:-1;
        double itemStoreDurationAll = countedAll>0? sourceDurationAll/countedAll:-1;
        out.info(String.format("Indexed %d items in %dsec (%7.3fms/item): processing: %7.3fms/item | queue: %7.3fms",
            count,(int)interval/1000,(float)interval/count,itemDurationAll,itemTimeAll));
        out.info(String.format("  - source   : %7.3fms/item",
            itemSourceDurationAll));
        out.info(String.format("  - processing: %7.3fms/item",
            itemProcessingDurationAll));
        out.info(String.format("  - store     : %7.3fms/item",
            itemStoreDurationAll));
        
    }

}
