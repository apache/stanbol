package org.apache.stanbol.enhancer.nlp.model.tag;

import org.apache.stanbol.enhancer.nlp.pos.PosTag;


public abstract class Tag<T extends Tag<T>> { //lol ??!! is that how to define T

    
    protected final String tag;
    private TagSet<T> annotationModel;

    /**
     * Creates a PosTag for the given String
     * @param tag the tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public Tag(String tag){
        if(tag == null || tag.isEmpty()){
            throw new IllegalArgumentException("The tag MUST NOT be NULL!");
        }
        this.tag = tag;
    }
    
    public final String getTag() {
        return tag;
    }
    /**
     * @return the annotationModel
     */
    public final TagSet<T> getAnnotationModel() {
        return annotationModel;
    }
    /**
     * Used by the {@link TagSet} class to assign itself to an PosTag
     * that is {@link TagSet#addTag(PosTag) added}.
     * @param annotationModel the annotationModel to set
     */
    protected final void setAnnotationModel(TagSet<T> annotationModel) {
        this.annotationModel = annotationModel;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s ", getClass().getSimpleName(), tag);
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Tag && tag.equals(((Tag<?>)obj).tag)){
            return (annotationModel == null && ((Tag<?>)obj).annotationModel == null) ||
                    (annotationModel != null && annotationModel.equals(((Tag<?>)obj).annotationModel));
        } else {
            return false;
        }
    }
}