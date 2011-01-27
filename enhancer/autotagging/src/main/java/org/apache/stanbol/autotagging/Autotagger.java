package org.apache.stanbol.autotagging;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.search.similar.MoreLikeThisQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.apache.stanbol.autotagging.jena.ModelIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Engine that uses a Lucene index of DBpedia entities (types and abstracts) to
 * suggest the top 3 entities that are semantically related to the text content
 * to annotate.
 *
 * @author ogrisel
 */
public class Autotagger {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public final String typeFieldName = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final String lookupFieldName = "http://www.w3.org/2000/01/rdf-schema#label";

    private String[] likeFieldNames = {
            "http://www.w3.org/2000/01/rdf-schema#label",
            "http://dbpedia.org/property/abstract" };

    private String idField = ModelIndexer.URI_FIELD;

    private int maxSuggestions = 3;

    private float lookupBoost = 2f;

    private float contextBoost = 1f;

    private Analyzer analyzer = getDefaultAnalyzer();

    private String typePrefix = "http://dbpedia.org/ontology/";

    private boolean strictLookup = true;

    private final Directory directory;

    public Autotagger(Directory directory) {
        this.directory = directory;
    }

    public static Analyzer getDefaultAnalyzer() {
        return new StandardAnalyzer(Version.LUCENE_30);
    }

    public Analyzer getAnalyzer(boolean withShingles) {
        if (withShingles) {
            return new ShingleAnalyzerWrapper(analyzer);
        } else {
            return analyzer;
        }
    }

    public Autotagger withFieldNames(String[] fieldNames) {
        this.likeFieldNames = fieldNames;
        return this;
    }

    public Autotagger withIdFieldName(String idField) {
        this.idField = idField;
        return this;
    }

    public Autotagger withMaxSuggestions(int maxSuggestions) {
        this.maxSuggestions = maxSuggestions;
        return this;
    }

    public Autotagger withAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public Autotagger withLookupBoost(float lookupBoost) {
        this.lookupBoost = lookupBoost;
        return this;
    }

    public Autotagger withContextBoost(float contextBoost) {
        this.contextBoost = contextBoost;
        return this;
    }

    public Autotagger withTypePrefix(String typePrefix) {
        this.typePrefix = typePrefix;
        return this;
    }

    public Autotagger withStrictNameLookup(boolean strictLookup) {
        this.strictLookup = strictLookup;
        return this;
    }

    /**
     * Suggest entities that are textually similar to the given text.
     *
     * @param text
     * @return entities info that best match the text
     * @throws CorruptIndexException
     * @throws IOException
     */
    public List<TagInfo> suggest(String text) throws CorruptIndexException,
            IOException {
        return suggest(text, null);
    }

    /**
     * Suggest entities that are textually similar to the given text. If the
     * text is short enough, a fuzzy name lookup is performed instead. Further
     * restrict the results to match the field values given in the fieldFilter
     *
     * @param text the textual content used for similarity search
     * @param fieldFilters
     * @return entities info that best match the text
     * @throws CorruptIndexException
     * @throws IOException
     */
    public List<TagInfo> suggest(String text,
            Map<String, List<String>> fieldFilters)
            throws CorruptIndexException, IOException {

        // count tokens using the analyzer
        TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
        int tokens = 0;
        while (tokenStream.incrementToken()) {
            tokens++;
        }
        if (tokens > 3) {
            // this is a context based suggestion
            return suggest(null, text, fieldFilters);
        } else {
            // this is a name lookup
            return suggest(text, null, fieldFilters);
        }
    }

    /**
     * Suggest entities that are fuzzy matching the given name and/or textually
     * similar to the given context. Further restrict the results to match the
     * field values given in the fieldFilter
     *
     * @param text the textual content used for similarity search
     * @param fieldFilters
     *
     * @return entities info that best match the text
     * @throws CorruptIndexException
     * @throws IOException
     */
    public List<TagInfo> suggest(String name, String context,
            Map<String, List<String>> fieldFilters)
            throws CorruptIndexException, IOException {

        if ((name == null || name.length() == 0)
                && (context == null || context.length() == 0)) {
            throw new IllegalArgumentException(
                    "name and context value cannot be both null or empty");
        }

        List<TagInfo> suggestions = new ArrayList<TagInfo>(maxSuggestions);
        IndexReader reader = IndexReader.open(directory, true);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery query = new BooleanQuery();
        try {

            // fuzzy name lookup
            if (name != null) {
                TokenStream ts = analyzer.tokenStream(lookupFieldName,
                        new StringReader(name));
                TermAttribute termAtt = ts.addAttribute(TermAttribute.class);
                while (ts.incrementToken()) {
                    FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(
                            lookupFieldName, termAtt.term()), 0.8f);
                    // TODO: divide boost by number of terms
                    fuzzyQuery.setBoost(lookupBoost);
                    query.add(fuzzyQuery,
                            strictLookup ? BooleanClause.Occur.MUST
                                    : BooleanClause.Occur.SHOULD);
                }
            }

            // similarity context search
            if (context != null) {
                // TODO: use FuzzyLikeThisQuery instead?
                // TODO: re-enable shingles once we can get rid of the "-"
                // shingles
                MoreLikeThisQuery mltQuery = new MoreLikeThisQuery(context,
                        likeFieldNames, getAnalyzer(false));
                mltQuery.setPercentTermsToMatch(0.15f);
                mltQuery.setMaxQueryTerms(20);
                mltQuery.setMinTermFrequency(1);
                mltQuery.setMinDocFreq(1);
                mltQuery.setBoost(contextBoost);
                query.add(mltQuery, BooleanClause.Occur.SHOULD);
            }

            // additional exact match filters
            if (fieldFilters != null) {
                for (Map.Entry<String, List<String>> fieldFilter : fieldFilters.entrySet()) {
                    for (String value : fieldFilter.getValue()) {
                        TermQuery tq = new TermQuery(new Term(
                                fieldFilter.getKey(), value));
                        // should not influence ranking, just filtering
                        tq.setBoost(0.0f);
                        query.add(tq, BooleanClause.Occur.MUST);
                    }
                }
            }
            TopDocs hits = searcher.search(query, maxSuggestions);
            ScoreDoc[] scoreDocs = hits.scoreDocs;
            for (int i = 0; i < Math.min(maxSuggestions, hits.totalHits); i++) {
                double confidence = scoreDocs[i].score;
                if (confidence == 0.0) {
                    // this might happen with BooleanClause.Occur.SHOULD queries
                    continue;
                }
                Document d = searcher.doc(scoreDocs[i].doc);
                String id = d.get(idField);
                log.debug(String.format("entity '%s' matches with score %f",
                        id, confidence));

                // assuming we are using DBPedia, we are extracting the label
                // from the entity URI to avoid loading the lucene index with
                // a stored label field
                String label = URLDecoder.decode(id, "UTF-8");
                label = label.substring(
                        "http://dbpedia.org/resource/".length(), label.length());
                label = label.replace("_", " ");
                TagInfo tag = new TagInfo(id, label,
                        d.getValues(typeFieldName), confidence);
                suggestions.add(tag);
            }
        } finally {
            reader.close();
            searcher.close();
        }
        return suggestions;
    }

    /**
     * Suggest entities that are fuzzy matching the given text (if short) or
     * textually similar to the text (if long). Further restrict the results to
     * match the type given either as full URI or DBpedia class name.
     *
     * @param text
     * @param type
     * @return ranked entities info that best match
     * @throws CorruptIndexException
     * @throws IOException
     */
    public List<TagInfo> suggestForType(String text, String type)
            throws CorruptIndexException, IOException {
        Map<String, List<String>> fieldFilters = new HashMap<String, List<String>>();
        if (!type.startsWith("http://")) {
            type = typePrefix + type;
        }
        fieldFilters.put(typeFieldName, Arrays.asList(type));
        return suggest(text, fieldFilters);
    }

    /**
     * Suggest entities that are fuzzy matching the given name and/or textually
     * similar to the given context. Further restrict the results to match the
     * type given either as full URI or DBpedia class name.
     *
     * @param name
     * @param context
     * @param type
     * @return ranked entities info that best match
     * @throws CorruptIndexException
     * @throws IOException
     */
    public List<TagInfo> suggestForType(String name, String context, String type)
            throws CorruptIndexException, IOException {
        Map<String, List<String>> fieldFilters = new HashMap<String, List<String>>();
        if (type != null) {
            if (!type.startsWith("http://")) {
                type = typePrefix + type;
            }
            fieldFilters.put(typeFieldName, Arrays.asList(type));
        }
        return suggest(name, context, fieldFilters);
    }


    public String[] mostImportantTerms(String text) throws CorruptIndexException, IOException {
        IndexReader reader = IndexReader.open(directory, true);
        MoreLikeThis mlt = new MoreLikeThis(reader);
        mlt.setFieldNames(likeFieldNames);
        mlt.setAnalyzer(analyzer);
        mlt.setMaxQueryTerms(maxSuggestions);
        return mlt.retrieveInterestingTerms(new StringReader(text));
    }
}
