package org.apache.stanbol.enhancer.nlp.model.impl;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.InstanceofPredicate;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Token;


/**
 * A Span that contains other spans
 * @author Rupert Westenthaler
 *
 */
public abstract class SectionImpl extends SpanImpl implements Section {

    /**
     * Allows to create a SectionImpl without setting the AnalysedText context.
     * {@link #setContext(AnalysedTextImpl)} needs to be called before using
     * this instance.<p>
     * NOTE: this constructor is needed to instantiate {@link AnalysedTextImpl}.
     * @param type the type. MUST NOT be <code>null</code> nor {@link SpanTypeEnum#Token}
     * @param start
     * @param end
     */
    public SectionImpl(SpanTypeEnum type,int start,int end) {
        super(type,start,end);
        assert type != SpanTypeEnum.Token : "The SpanType 'Token' is NOT a Section - can not cover other spans!";
    }
//    public SectionImpl(AnalysedTextImpl at, SpanTypeEnum type,int start,int end) {
//        this(at,type,null,start,end);
//    }
    public SectionImpl(AnalysedTextImpl at, SpanTypeEnum type,Span relativeTo, int start,int end) {
        super(at,type,relativeTo,start,end);
        assert type != SpanTypeEnum.Token : "The SpanType 'Token' is NOT a Section - can not cover other spans!";
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Span> getEnclosed(final Set<SpanTypeEnum> types) {
        return IteratorUtils.filteredIterator(getIterator(), 
            new Predicate() {
                @Override
                public boolean evaluate(Object span) {
                    return types.contains(((Span)span).getType());
                }
            });
    }
    /**
     * Iterator that does not throw {@link ConcurrentModificationException} but
     * considers modifications to the underlying set by using the
     * {@link NavigableMap#higherKey(Object)} method for iterating over the
     * Elements!<p>
     * This allows to add new {@link Span}s to the {@link Section} while
     * iterating (e.g. add {@link Token}s and/or {@link Chunk}s while iterating
     * over the {@link Sentence}s of an {@link AnalysedText})
     * @return the iterator
     */
    protected Iterator<Span> getIterator(){
        //the end of this section
        final Span end = new SubSetHelperSpan(getEnd());
        return new Iterator<Span>() {
            
            boolean init = false;
            boolean removed = true;
            private Span span = SectionImpl.this;
            
            @Override
            public boolean hasNext() {
                return getNext() != null;
            }
            
            private Span getNext(){
                Span next = context.spans.higherKey(span);
                return next == null || next.compareTo(end) >= 0 ? null : next;
            }
            
            @Override
            public Span next() {
                init = true;
                span = getNext();
                removed = false;
                if(span == null){
                    throw new NoSuchElementException();
                }
                return span;
            }

            @Override
            public void remove() {
                if(!init){
                    throw new IllegalStateException("remove can not be called before the first call to next");
                }
                if(removed){
                    throw new IllegalStateException("the current Span was already removed!");
                }
                context.spans.remove(span);
                removed = true;
            }
            
        };
    }
    
    /**
     * Adds a Token <b>relative</b> to the current Span. Negative values for start and
     * end are allowed (e.g. to add a Token that starts some characters before
     * this one.<p>
     * Users that want to use <b>absolute</b> indexes need to use
     * <code><pre>
     *     Span span; //any type of Span (Token, Chunk, Sentence ...)
     *     span.getContext().addToken(absoluteStart, absoluteEnd)
     * </pre></code>
     * @param start the start relative to this Span
     * @param end the end relative to this span
     * @return the created and added token
     */
    public Token addToken(int start,int end){
        return register(new TokenImpl(context, this,start, end));
    }
    /**
     * Registers the parsed - newly created token - with the {@link #getContext()}.
     * If the parsed {@link Span} already exists (an other Span instance with the
     * same values for {@link Span#getType()}, {@link Span#getStart()} and 
     * {@link Span#getEnd()}) than the already present instance is returned
     * instead of the parsed one. In case the parsed Token does not already
     * exist the parsed instance is registered with the context and
     * returned.<p>
     * Typical usage:<pre><code>
     *     public add{something}(int start, int end){
     *         return register(new {somthing}Impl(context, this,start,end));
     *     }
     * </code></pre>
     * {something} ... the Span type (Token, Chunk, Sentence ...)<p>
     * @param span the Span instance to register
     * @return the parsed or an already existing instance
     */
    protected <T extends Span> T register(T span){
        //check if this token already exists
        @SuppressWarnings("unchecked")
        T current = (T)context.spans.get(span);
        //NOTE: type safety is ensured by the SpanTypeEnum in combination with the
        //      Compareable implementation of SpanImpl.
        if(current == null){ //add the new one
            context.spans.put(span, span);
            return span;
        } else { //else return the already contained token
            return current;
        }
    }

    public Iterator<Token> getTokens(){
        return filter(Token.class);
    }
    /**
     * Internal helper to generate correctly generic typed {@link Iterator}s for
     * filtered {@link Span} types
     * @param interfaze the Span interface e.g. {@link Token}
     * @param clazz the actual Span implementation e.g. {@link TokenImpl}
     * @return the {@link Iterator} of type {interface} iterating over 
     * {implementation} instances (e.g. 
     * <code>{@link Iterator}&lt;{@link Token}&gt;</code> returning 
     * <code>{@link TokenImpl}</code> instances on calls to {@link Iterator#next()}
     */
    @SuppressWarnings("unchecked")
    protected <T extends Span> Iterator<T> filter(
        final Class<T> clazz){
        return IteratorUtils.filteredIterator(
            getIterator(),
            new InstanceofPredicate(clazz));
    }
    
    /**
     * Internal helper class used for building {@link SortedSet#subSet(Object, Object)}.
     * 
     * @author Rupert Westenthaler
     *
     */
    class SubSetHelperSpan extends SpanImpl implements Span{
        /**
         * Create the start constraint for {@link SortedSet#subSet(Object, Object)}
         * @param start
         * @param end
         */
        protected SubSetHelperSpan(int start,int end){
            super(SpanTypeEnum.Text, //lowest pos type
                start,end);
            setContext(SectionImpl.this.context);
        }
        /**
         * Creates the end constraint for {@link SortedSet#subSet(Object, Object)}
         * @param pos
         */
        protected SubSetHelperSpan(int pos){
            super(SpanTypeEnum.Token, //highest pos type,
                pos,Integer.MAX_VALUE);
            setContext(SectionImpl.this.context);
        }
    }

}
