package org.apache.stanbol.enhancer.engines.opennlp.impl;

public class NameOccurrence {

    public final String name;

    public final Integer start;

    public final Integer end;

    public final String context;

    public final Double confidence;

    public NameOccurrence(String name, Integer start, Integer end,
            String context, Double confidence) {
        this.start = start;
        this.end = end;
        this.name = name;
        this.context = context;
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return String.format(
                "[name='%s', start='%d', end='%d', confidence='%f', context='%s']",
                name, start, end, confidence, context);
    }

}
