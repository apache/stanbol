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
package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import org.slf4j.Logger;

public class Statistic {
    
    private final String name;
    int count = 0;
    int closedCount = 0;
    boolean started = false;
    long start;
    long duration;
    long max = -1;
    long min = Long.MAX_VALUE;
    private final int numPrint;
    private final Logger log;
    
    public Statistic(String name){
        this(name,-1,null);
    }
    public Statistic(String name, int numPrint, Logger log){
        this.name = name;
        this.numPrint = numPrint;
        if(numPrint > 0){
            this.log = log;
        } else {
            this.log = null;
        }
    }
    public double getDuration(){
        return this.duration/1000000.0;
    }
    public void begin(){
        count++;
        started = true;
        start = System.nanoTime();
    }
    public void cancel(){
        count--;
        started = false;
    }
    public void complete(){
        if(started){
            long end = System.nanoTime();
            closedCount++;
            long dif = (end - start);
            duration = duration + dif;
            if(dif > max) {
                max = dif;
            } else if(dif < min){
                min = dif;
            }
            started = false;
        } //else close without start ... ignore
        if(log != null && numPrint > 0){
            if(count % numPrint == 0){
                printStatistics(log);
            }
        }
    }

    public String getStatistics(){
        int count = this.count;
        int closedCount = this.closedCount;
        double duration = this.duration/1000000.0;
        double max = this.max/1000000.0;
        double min = this.min/1000000.0; 
        StringBuilder sb = new StringBuilder(name).append(": ");
        sb.append(duration).append("ms [");
        sb.append("count: ").append(count).append(" | ");
        sb.append("time: ").append(duration/(double)closedCount).append("ms (max:");
        sb.append(max).append(", min:").append(min).append(")]");
        return sb.toString();
    }
    
    public void printStatistics(Logger log){
        log.info("  - {}", getStatistics());
        
    }
    
}
