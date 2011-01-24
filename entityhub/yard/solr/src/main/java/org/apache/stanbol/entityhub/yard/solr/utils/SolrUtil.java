package org.apache.stanbol.entityhub.yard.solr.utils;

import java.util.regex.Pattern;


public class SolrUtil {


    private static final String LUCENE_ESCAPE_CHARS = "[\\\\+\\-\\!\\(\\)\\:\\^\\]\\{\\}\\~\\*\\?]";
    private static final Pattern LUCENE_PATTERN = Pattern.compile(LUCENE_ESCAPE_CHARS);
    private static final String REPLACEMENT_STRING = "\\\\$0";


    /**
     * Escapes all special chars in an string (field name or constraint) to be
     * used in an SolrQuery.
     * @param string the string to be escaped
     * @return the escaped string
     */
    public static String escapeSolrSpecialChars(String string) {
        return string != null?LUCENE_PATTERN.matcher(string).replaceAll(REPLACEMENT_STRING):null;
    }

}
