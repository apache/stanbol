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
package org.apache.stanbol.commons.stanboltools.datafileprovider.impl.tracking;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileListener;

/**
 * Internally used to manage {@link DataFileListener} and the state of
 * tracked DataFiles.<p>
 * Note that different {@link DataFileListener}s may have different {@link STATE}
 * for the same RDFTerm (e.g. if a new {@link DataFileListener} is registered
 * for a resource it will start with {@link STATE#UNKNOWN} while all the other
 * Listeners will be in the state of the resource (either {@link STATE#AVAILABLE}
 * or {@link STATE#UNAVAILABLE}). Only after the next tracking the newly added
 * {@link DataFileListener} will get fired and be updated to the current state
 * of the RDFTerm.<p>
 * This model will also allow to introduce an ERROR state that could be used
 * to manage that some {@link DataFileListener} where not able to consume a
 * current version of a data file.
 * @author Rupert Westenthaler
 *
 */
public final class TrackingState implements Iterable<Entry<DataFileListener,STATE>>{
    
    private final Map<DataFileListener,STATE> listenerStates = new HashMap<DataFileListener,STATE>();
    private final Set<STATE> states = EnumSet.noneOf(STATE.class);
    
    public boolean isListener(DataFileListener listener){
        synchronized (listenerStates) {
            return listenerStates.containsKey(listener);
        }
    }
    /**
     * Adds an new listener and sets its state to {@link STATE#UNKNOWN}. If the
     * listener is already present its current state will be changed to
     * {@link STATE#UNKNOWN}.
     * @param listener the listener to add
     */
    public void addListener(DataFileListener listener){
        updateListener(listener,STATE.UNKNOWN);
    }
    /**
     * Adds/Update the listener with the parsed state
     * @param listener the listener to add/update
     * @param state the new state of the listener
     * @return the previous state of the listener or <code>null</code> if added
     */
    public STATE updateListener(DataFileListener listener, STATE state) {
        synchronized (listenerStates) {
            state = listenerStates.put(listener, state);
            states.addAll(listenerStates.values());
            return state;
        }
    }
    @Override
    public Iterator<Entry<DataFileListener,STATE>> iterator() {
        Set<Entry<DataFileListener,STATE>> entryClone;
        synchronized (listenerStates) {
            entryClone = new HashSet<Entry<DataFileListener,STATE>>(listenerStates.entrySet());
        }
        return entryClone.iterator();
    }
    /**
     * Removes a listener
     * @param listener the listener
     * @return the state of the removed listener or <code>null</code> if the
     * listener was not known.
     */
    public STATE removeListener(DataFileListener listener){
        synchronized (listenerStates) {
            STATE state = listenerStates.remove(listener);
            if(state != null){
                states.addAll(listenerStates.values());
            }
            return state;
        }
    }
    /**
     * The number of listeners
     * @return the number of listener
     */
    public int size(){
        synchronized (listenerStates) {
            return listenerStates.size();
        }
    }
    /**
     * Returns the {@link STATE} if all listeners are in the same {@link STATE}
     * or otherwise <code>null</code>. This is intended to allow a fast check
     * if some processing is necessary on checking a specific resource. This
     * method assumes that in an high percentage of cases no processing is
     * necessary.
     * @return The common state for all {@link DataFileListener} or <code>null</code>
     * if no common state exists.
     */
    public STATE getTrackingState(){
        synchronized (listenerStates) {
            return states.size() == 1 ? states.iterator().next() : null;
        }
    }
    /**
     * Checks if there are no more listeners left
     * @return <code>true</code> if there are no listeners. Otherwise <code>false</code>
     */
    public boolean isEmpty() {
        synchronized (listenerStates) {
            return listenerStates.isEmpty();
        }
    }
}