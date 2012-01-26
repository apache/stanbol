package org.apache.stanbol.contenthub.servicesapi.search.related;

public interface RelatedKeyword {

    String getKeyword();
    
    double getScore();
    
    String getSource();
    
    /*
     * To enumerate the source for a related keyword 
     */
    public enum Source {
        
        UNKNOWN("Unknown"),
        
        WORDNET("Wordnet"),
        
        ONTOLOGY("Ontology");
        
        private final String name;

        private Source(String n) {
            this.name = n;
        }

        @Override
        public final String toString() {
            return this.name;
        }        
    }
    
}
