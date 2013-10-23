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
package org.apache.stanbol.enhancer.nlp.model.impl;

import java.lang.ref.SoftReference;
import java.util.Arrays;

import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A span selected in the given Text. This uses {@link SoftReference}s for
 * holding the {@link #getSpan()} text to allow the Garbage Collector to 
 * free up memory for large texts. In addition the span text is lazzy initialised
 * on the first call to {@link #getSpan()}.
 * 
 * @author Rupert Westenthaler
 *
 */
public abstract class SpanImpl extends AnnotatedImpl implements Span{
    
    
    private final static Logger log = LoggerFactory.getLogger(SpanImpl.class);

    protected final int[] span;
    /**
     * Lazzy initialised {@link SoftReference} to the text
     */
    private SoftReference<String> textReference = null;
    
    protected AnalysedTextImpl context;

    protected final SpanTypeEnum type;    

    /**
     * Allows to create a SpanImpl without the {@link #getContext()}. The
     * context MUST BE set by using {@link #setContext(AnalysedTextImpl)} before
     * using this span.
     * @param type
     * @param start
     * @param end
     */
    protected SpanImpl(SpanTypeEnum type, int start,int end) {
        assert type != null : "The parsed SpanType MUST NOT be NULL!";
        if(start < 0 || end < start){
            throw new IllegalArgumentException("Illegal span ["+start+','+end+']');
        }
        this.type = type;
        this.span = new int[]{start,end};

    }
//    protected SpanImpl(AnalysedTextImpl analysedText, SpanTypeEnum type, int start,int end) {
//        this(analysedText,type,null,start,end);
//    }
    protected SpanImpl(AnalysedTextImpl analysedText, SpanTypeEnum type, Span relativeTo,int start,int end) {
        this(type,
            relativeTo == null ? start : relativeTo.getStart()+start,
            relativeTo == null ? end : relativeTo.getStart()+end);
        setContext(analysedText);
        //check that Spans that are created relative to an other do not cross
        //the borders of that span
        if(relativeTo != null && relativeTo.getEnd() < getEnd()){
            throw new IllegalArgumentException("Illegal span ["+start+','+end
                + "] for "+type+" relative to "+relativeTo+" : Span of the "
                + " contained Token MUST NOT extend the others!");
        }
    }
    
    protected void setContext(AnalysedTextImpl analysedText){
        assert analysedText != null : "The parsed AnalysedText MUST NOT be NULL!";
        this.context = analysedText;
    }
        
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Span#getType()
     */
    @Override
    public SpanTypeEnum getType(){
        return type;
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Span#getStart()
     */
    @Override
    public int getStart(){
        return span[0];
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Span#getEnd()
     */
    @Override
    public int getEnd(){
        return span[1];
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Span#getText()
     */
    @Override
    public final AnalysedTextImpl getContext() {
        return context;
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Span#getSpan()
     */
    @Override
    public String getSpan(){
        String spanText = textReference == null ? null : textReference.get();
        if(spanText == null){
            spanText = getContext().getText().subSequence(span[0], span[1]).toString();
            textReference = new SoftReference<String>(spanText);
        }
        return spanText;
    }
    
    @Override
    public int hashCode() {
        //include the SpanTypeEnum in the hash
        return Arrays.hashCode(span);
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SpanImpl && getType() == ((Span)obj).getType() &&
                Arrays.equals(this.span, ((SpanImpl)obj).span); 
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s",type ,Arrays.toString(span));
    }
    
    @Override
    public int compareTo(Span o) {
        if(context != null && o.getContext() != null && 
                !context.equals(o.getContext())){
            log.warn("Comparing Spans with different Context. This is not an " +
            		"intended usage of this class as start|end|type parameters " +
            		"do not have a natural oder over different texts.");
            log.info("This will sort Spans based on start|end|type parameters "+
            		"regardless that the might be over different texts!");
            //TODO consider throwing an IllegalStateExcetion!
        }
        //Compare Integers ASC (used here three times)
        //    (x < y) ? -1 : ((x == y) ? 0 : 1);
        int start = (span[0] < o.getStart()) ? -1 : ((span[0] == o.getStart()) ? 0 : 1);
        if(start == 0){
            //sort end in DESC order
            int end = (span[1] < o.getEnd()) ? 1 : ((span[1] == o.getEnd()) ? 0 : -1);
            //if start AND end is the same compare based on the span type
            //Natural order of span types is defined by the Enum.ordinal()
            int o1 = getType().ordinal();
            int o2 = o.getType().ordinal();
            return end != 0 ? end :
                (o1 < o2) ? -1 : ((o1 == o2) ? 0 : 1);
        } else {
            return start;
        }
    }
    

}
