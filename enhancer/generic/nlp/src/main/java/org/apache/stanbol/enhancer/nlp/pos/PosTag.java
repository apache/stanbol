package org.apache.stanbol.enhancer.nlp.pos;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.tag.Tag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * An POS (part-of-speech) tag typically assigned by an POS-Tagger (an
 * NLP component) to a {@link Token} by using the {@link POS#POS_ANNOTATION}<p>
 * The only required field is {@link #getTag()} - the string tag assigned by
 * the POS Tagger.<p>
 * PosTags can be mapped to a {@link LexicalCategory} and be part of an
 * {@link TagSet}. NOTE that the {@link TagSet} is set by
 * the {@link TagSet#addTag(PosTag)} method.<p>
 */
public class PosTag extends Tag<PosTag>{
    
    /**
     * The {@link LexicalCategory LexicalCategories} applying to this PosTag
     */
    private final Set<LexicalCategory> category;
    /**
     * The mapped {@link Pos} tags. Empty if none are mapped
     */
    private final Set<Pos> pos;
    /**
     * NOTE: NULL if {@link #pos} is empty!
     */
    private final Set<Pos> posHierarchy;
//    /**
//     * Creates a new POS tag for the parsed tag. The created Tag is not
//     * assigned to any {@link LexicalCategory}.<p> This constructor can be used
//     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
//     * (e.g. that is not defined by the configured {@link TagSet}).<p>
//     * @param tag the Tag
//     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
//     * or empty.
//     */
//    public PosTag(String tag){
//        this(tag,(LexicalCategory)null);
//    }
    /**
     * Creates a PosTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param category the lexical categor(ies) mapped to the tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PosTag(String tag,LexicalCategory...category){
        super(tag);
        this.category = EnumSet.noneOf(LexicalCategory.class);
        if(category != null){
            this.category.addAll(Arrays.asList(category));
        }
        this.pos = Collections.emptySet();
        this.posHierarchy = Collections.emptySet();
    }
    /**
     * Creates a PosTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param pos a concrete {@link Pos} mapped to the string
     * @param furtherPos allows to add additional {@link Pos} mappings
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PosTag(String tag,Pos pos,Pos...furtherPos){
        this(tag, null,pos,furtherPos);
    }
    
    public PosTag(String tag,LexicalCategory category, Pos pos,Pos...furtherPos){
        super(tag);
        if(pos != null){
            if(furtherPos == null || furtherPos.length < 1){
                this.pos = Collections.singleton(pos);
                this.posHierarchy = pos.hierarchy();
                if(category == null){
                    this.category = pos.categories();
                } else {
                    this.category = EnumSet.of(category);
                    this.category.addAll(pos.categories());
                }
            } else { // in case of multiple Pos Tags
                this.pos = EnumSet.of(pos,furtherPos);
                //we need to collect categories
                this.category = category == null ? 
                        EnumSet.noneOf(LexicalCategory.class) :
                            EnumSet.of(category);
                //and the union over the pos parents
                this.posHierarchy = EnumSet.noneOf(Pos.class);
                for(Pos p : this.pos){
                    this.posHierarchy.addAll(p.hierarchy());
                    this.category.addAll(p.categories());
                }
            }
        } else {
            if(furtherPos != null && furtherPos.length > 0){
                throw new IllegalArgumentException("furtherPos parameter MUST BE NULL "
                    + "or empty if the pos parameter is NULL!");
            }
            this.category = category == null ? 
                    Collections.EMPTY_SET : Collections.singleton(category);
            this.pos = Collections.emptySet();
            this.posHierarchy = Collections.emptySet();
        }
    }
    /**
     * The {@link LexicalCategory LexicalCategories} of this tag
     * @return the {@link LexicalCategory LexicalCategories} or an
     * empty {@link Set} if the string {@link #getTag() tag} is 
     * not mapped.
     */
    public Set<LexicalCategory> getCategories(){
       return category; 
    }
    
    /**
     * Checks if this {@link PosTag} is mapped to the parsed
     * {@link LexicalCategory}
     * @param category the category
     * @return <code>true</code> if this PosTag is mapped to
     * the parsed category.
     */
    public boolean hasCategory(LexicalCategory category){
        return this.category.contains(category);
    }
    
    /**
     * Checks if the {@link PosTag} is of the parsed {@link Pos}
     * tag. This also considers the transitive hierarchy of
     * the {@link Pos} enum.
     * @param pos the {@link Pos} to check
     * @return <code>true</code> if this PosTag is mapped to
     * the parsed {@link Pos}.
     */
    public boolean hasPos(Pos pos){
        return this.pos.isEmpty() ? false : 
            posHierarchy.contains(pos);
    }
    /**
     * Returns <code>true</code> if this PosTag is mapped to a
     * {@link LexicalCategory} or a {@link Pos} type as defined
     * by the <a herf="">Olia</a> Ontology
     * @return
     */
    public boolean isMapped() {
        return !category.isEmpty();
    }
    
    /**
     * Getter for the {@link Pos} mapped to this PosTag
     * @return the mapped {@link Pos} mapped to the string
     * string {@link #getTag() tag} or an empty set of not
     * mapped. This are the directly mapped {@link Pos} types
     * and does not include the parent Pos types.
     */
    public Set<Pos> getPos() {
        return pos;
    }
    
    public Set<Pos> getPosHierarchy(){
        return posHierarchy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("pos: ");
        sb.append(tag);
        if(pos != null || !category.isEmpty()){
            sb.append('(');
            if(!pos.isEmpty()){
                if(pos.size() == 1){
                    sb.append(pos.iterator().next());//.name());
                } else {
                    sb.append(pos);
                }
                sb.append('|');
            }
            if(category.size() == 1){
                sb.append(category.iterator().next());//.name());
            } else {
                sb.append(category);
            }
            sb.append(')');
        }
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode() + category.hashCode() + pos.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof PosTag &&
                category.equals(((PosTag)obj).category) &&
                pos.equals(((PosTag)obj).pos);
    }
}
