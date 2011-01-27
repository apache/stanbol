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

import org.apache.stanbol.enhancer.interaction.event.Event;
import org.apache.stanbol.enhancer.interaction.event.EventListener;
import org.apache.stanbol.enhancer.interaction.event.EventManager;
import org.apache.stanbol.enhancer.interaction.event.NotUnderstoodEvent;
import org.apache.stanbol.enhancer.interaction.event.QueryEvent;
import org.apache.stanbol.enhancer.interaction.event.RecognizedSpeechEvent;
import org.apache.stanbol.enhancer.interaction.event.SparqlEvent;

public class NL2Sparql implements EventListener {

    /*

SELECT ?tag ?content
WHERE {
   ?content <http://rdfs.org/sioc/ns#related_to> ?tag .
   ?tag <http://www.w3.org/2000/01/rdf-schema#label> ?label
   FILTER REGEX(?label,".*Jimi Hendrix.*") .
}
     */

    private String nl2Sparql (String text) {
        //extract last two words!
        String person = text.replaceFirst(".*? (\\w+ \\w+)\\W*$", "$1");

        if (person.equals("") || person.equals(text)) {
            // DEBUG //
            return "TODO: not a 'correct' sentence";
        }
        else {
            return "SELECT ?content ?tag\n" +
            "WHERE {\n" +
            "   ?content <http://rdfs.org/sioc/ns#related_to> ?tag .\n" +
            "   ?tag <http://www.w3.org/2000/01/rdf-schema#label> ?label\n" +
            "   FILTER REGEX(?label,\".*" + person + ".*\") .\n" +
            "}";
        }
    }

    private String sparql2Text (String sparqlQuery) {
        /*if (sparqlQuery.matches("SELECT ?content ?tag ?type\n" +
                "WHERE {\n" +
                "   ?content <http://rdfs.org/sioc/ns#related_to> ?tag.\n" +
                "   ?tag <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type.\n" +
                "   ?tag <http://www.w3.org/2000/01/rdf-schema#label> ?label\n" +
                "   REGEX(?label,\"[a-zA-Z ]?\")\n" +
                "}")) {
            String person = sparqlQuery.replaceAll("SELECT ?content ?tag ?type\n" +
                "WHERE {\n" +
                "   ?content <http://rdfs.org/sioc/ns#related_to> ?tag.\n" +
                "   ?tag <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type.\n" +
                "   ?tag <http://www.w3.org/2000/01/rdf-schema#label> ?label\n" +
                "   REGEX\\(?label,\"([a-zA-Z ]?)\"\\)\n" +
                "}", "$1");
            return "Show me information regarding " + person + "!";
        }*/
        return "TODO: SPARQL2TEXT";
    }

    public void eventOccurred(Event e) {
        if (e instanceof RecognizedSpeechEvent) {
            RecognizedSpeechEvent rse = (RecognizedSpeechEvent)e;

            String speechText = rse.getData();

            if (speechText == null) {
                EventManager.eventOccurred(new NotUnderstoodEvent());
            }
            else {
                String sparqlQuery = nl2Sparql(speechText);

                QueryEvent qe = new QueryEvent(sparqlQuery, speechText);

                EventManager.eventOccurred(qe);
            }
        }
        else if (e instanceof SparqlEvent) {
            SparqlEvent se = (SparqlEvent)e;

            String sparqlQuery = se.getData();

            String speechText = sparql2Text(sparqlQuery);

            QueryEvent qe = new QueryEvent(sparqlQuery, speechText);

            EventManager.eventOccurred(qe);
        }
    }

}
