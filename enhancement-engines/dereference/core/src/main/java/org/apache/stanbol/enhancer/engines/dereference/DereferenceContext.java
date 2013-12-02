package org.apache.stanbol.enhancer.engines.dereference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;

public class DereferenceContext {

    /**
     * The {@link OfflineMode} status
     */
    protected final boolean offlineMode;
    /** 
     * Read-only set with languages that need to be dereferenced.
     */
    private Set<String> languages = new HashSet<String>();
    
    /**
     * Create a new DereferenceContext.
     * @param offlineMode the {@link OfflineMode} state
     */
    protected DereferenceContext(boolean offlineMode){
        this.offlineMode = offlineMode;
    }

    /**
     * If the {@link OfflineMode} is active
     * @return the offline mode status
     */
    public boolean isOfflineMode() {
        return offlineMode;
    }
    /**
     * Setter for the languages of literals that should be dereferenced
     * @param languages the ContentLanguages
     */
    protected void setLanguages(Set<String> languages) {
        if(languages == null){
            this.languages = Collections.emptySet();
        } else {
            this.languages = Collections.unmodifiableSet(languages);
        }
    }
    /**
     * Getter for the languages that should be dereferenced. If 
     * empty all languages should be included.
     * @return the languages for literals that should be dereferenced.
     */
    public Set<String> getLanguages() {
        return languages;
    }
}
