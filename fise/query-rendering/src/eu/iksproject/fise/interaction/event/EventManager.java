package org.apache.stanbol.enhancer.interaction.event;

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

import java.util.HashSet;
import java.util.Set;

public class EventManager {

    private static final Set<EventListener> listeners = new HashSet<EventListener>();

    public static void eventOccurred (Event e) {
        synchronized (listeners) {
            for (EventListener el : listeners) {
                System.out.println("Send event " + e.getClass().getName() + " to " + el.getClass().getName());
                el.eventOccurred(e);
            }
        }
    }

    public static void addEventListener (EventListener el) {
        synchronized (listeners) {
            listeners.add(el);
        }
    }

    public static void removeEventListener (EventListener el) {
        synchronized (listeners) {
            listeners.remove(el);
        }
    }

}
