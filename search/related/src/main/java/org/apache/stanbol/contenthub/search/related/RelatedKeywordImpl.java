package org.apache.stanbol.contenthub.search.related;

import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;

public class RelatedKeywordImpl implements RelatedKeyword {
    
    private String keyword;
    private double score;
    private String source;

    public RelatedKeywordImpl(String keyword, double score) {
        this.keyword = keyword;
        this.score = score;
        this.source = RelatedKeyword.Source.UNKNOWN.toString();
    }
    
    public RelatedKeywordImpl(String keyword, double score, String source) {
        this.keyword = keyword;
        this.score = score;
        this.source = source;
    }
    
    public RelatedKeywordImpl(String keyword, double score, RelatedKeyword.Source source) {
        this.keyword = keyword;
        this.score = score;
        this.source = source.toString();
    }

    @Override
    public String getKeyword() {
        return this.keyword;
    }

    @Override
    public double getScore() {
        return this.score;
    }

    @Override
    public String getSource() {
        return this.source;
    }
}
