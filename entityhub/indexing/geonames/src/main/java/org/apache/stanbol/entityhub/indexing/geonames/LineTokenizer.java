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
package org.apache.stanbol.entityhub.indexing.geonames;

import java.util.Iterator;
import java.util.StringTokenizer;

public class LineTokenizer implements Iterator<String>{
    private static final String DELIM ="\t";
    private final StringTokenizer t;
    private boolean prevElementWasNull = true;
    public LineTokenizer(String data){
        t = new StringTokenizer(data, DELIM, true);
    }
    @Override
   public boolean hasNext() {
        return t.hasMoreTokens();
    }

    @Override
    public String next() {
        if(!prevElementWasNull){
            t.nextElement();//dump the delim
        }
        if(!t.hasMoreElements()){
            //this indicated, that the current Element is
            // - the last Element
            // - and is null
            prevElementWasNull = true;
            return null;
        } else {
            String act = t.nextToken();
            if(DELIM.equals(act)){
                prevElementWasNull = true;
                return null;
            } else {
                prevElementWasNull = false;
                return act;
            }
        }
    }
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}