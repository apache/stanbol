package org.apache.stanbol.entityhub.servicesapi.model;

import java.util.Iterator;

import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

public interface Symbol extends Sign{

    /**
     * The default state for new symbols if not defined otherwise
     */
    SymbolState DEFAULT_SYMBOL_STATE = SymbolState.proposed;
    /**
     * Enumeration that defines the different states of Symbols
     * @author Rupert Westenthaler
     *
     */
    enum SymbolState {
        /**
         * This symbol is marked as removed
         */
        removed(RdfResourceEnum.symbolStateRemoved.getUri()),
        /**
         * This symbol should no longer be moved. Usually there are one or more
         * new symbols that should be used instead of this one. See
         * {@link Symbol#getSuccessors()} for more information
         */
        depreciated(RdfResourceEnum.symbolStateDepreciated.getUri()),
        /**
         * Indicates usually a newly created {@link Symbol} that needs some kind
         * of confirmation.
         */
        proposed(RdfResourceEnum.symbolStateProposed.getUri()),
        /**
         * Symbols with that state are ready to be used.
         */
        active(RdfResourceEnum.symbolStateActive.getUri()),
        ;
        String uri;
        SymbolState(String uri){
            this.uri = uri;
        }
        public String getUri(){
            return uri;
        }
        @Override
        public String toString() {
            return uri;
        }
    };
    /**
     * The property to be used for the symbol label
     */
    String LABEL = RdfResourceEnum.label.getUri();
    /**
     * The label of this Symbol in the default language
     * @return the label
     */
    String getLabel();
    /**
     * Setter for the Label in the default Language
     * @param label
     */
    void setLabel(String label);
    /**
     * The preferred label of this Symbol in the given language or
     * <code>null</code> if no label for this language is defined
     * TODO: how to handle internationalisation.
     * @param lang the language
     * @return The preferred label of this Symbol in the given language or
     * <code>null</code> if no label for this language is defined
     */
    String getLabel(String lang);
    /**
     * Setter for a label of a specific language
     * @param label the label
     * @param language the language. <code>null</code> indicates to use no language tag
     */
    void setLabel(String label, String language);
    /**
     * The property to be used for the symbol description
     */
    String DESCRIPTION = RdfResourceEnum.description.getUri();
    /**
     * Getter for the descriptions of this symbol in the default language.
     * @return The descriptions or an empty collection.
     */
    Iterator<Text> getDescriptions();
    /**
     * Removes the description in the default language from the Symbol
     * @param description the description to remove
     */
    void removeDescription(String description);
    /**
     * Adds a description in the default language to the Symbol
     * @param description the description
     */
    void addDescription(String description);
    /**
     * Getter for the short description as defined for the parsed language.
     * @param lang The language. Parse <code>null</code> for values without language tags
     * @return The description or <code>null</code> if no description is defined
     * for the parsed language.
     */
    Iterator<Text> getDescriptions(String lang);
    /**
     * Removes the description in the parsed language from the Symbol
     * @param description the description to remove
     * @param language the language. <code>null</code> indicates to use no language tag
     */
    void removeDescription(String description, String language);
    /**
     * Adds a description in the parsed language to the Symbol
     * @param description the description
     * @param lanugage the language. <code>null</code> indicates to use no language tag
     */
    void addDescription(String description, String lanugage);

    /**
     * The property to be used for the symbol state
     */
    String STATE = RdfResourceEnum.hasSymbolState.getUri();
    /**
     * Getter for the state of this symbol
     * @return the state
     */
    SymbolState getState();
    /**
     * Setter for the state of the Symbol
     * @param state the new state
     * @throws IllegalArgumentException if the parsed state is <code>null</code>
     */
    void setState(SymbolState state) throws IllegalArgumentException;
    /**
     * The property used for linking to successors
     */
    String SUCCESSOR = RdfResourceEnum.successor.getUri();
    /**
     * Returns if this Symbols does have any successors
     * @return Returns <code>true</code> if successors are defined for this
     * symbol; otherwise <code>false</code>.
     */
    boolean isSuccessor();
    /**
     * Getter for the ID's of the symbols defined as successors of this one.
     * @return The id's of the symbols defined as successors of this one or an
     * empty list if there are no successors are defined.
     */
    Iterator<String> getSuccessors();
    /**
     * Adds the symbol with the parsed ID as a successor
     * @param successor the id of the successor
     */
    void addSuccessor(String successor);
    /**
     * Removes the symbol with the parsed ID as a successor
     * @param successor the id of the successor to remove
     */
    void removeSuccessor(String successor);
    /**
     * The property used for linking to predecessors
     */
    String PREDECESSOR = RdfResourceEnum.predecessor.getUri();
    /**
     * Returns if this Symbols does have any predecessors
     * @return Returns <code>true</code> if predecessors are defined for this
     * symbol; otherwise <code>false</code>.
     */
    boolean isPredecessors();
    /**
     * Getter for the ID's of the symbols defined as predecessors of this one.
     * @return The id's of the symbols defined as predecessors of this one or an
     * empty list if there are no predecessors are defined.
     */
    Iterator<String> getPredecessors();
    /**
     * Adds the symbol with the parsed ID as a predecessor
     * @param predecessor the id of the predecessors
     */
    void addPredecessor(String predecessor);
    /**
     * Removes the symbol with the parsed ID as a predecessor
     * @param predecessor the id of the predecessor to remove
     */
    void removePredecessor(String predecessor);

}
