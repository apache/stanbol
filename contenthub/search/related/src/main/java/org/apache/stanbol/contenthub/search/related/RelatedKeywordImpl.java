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
