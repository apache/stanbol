/*
 * Copyright 2007 Marc Wick, geonames.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.geonames.wikipedia;

/**
 * @author marc
 *
 */
public class TextSummaryExtractor {

    /**
     * extract a text summary from a wikipedia article. The wikipedia markup is
     * removed. The length of the summary is equal or lower the length of the
     * parameter. The extractor tries to end the summary at a fullstop.
     *
     * @param pText
     * @param length
     * @return
     */
    public static String extractSummary(String pText, int length) {
        if (pText == null) {
            return null;
        }

        // remove all wikipedia markup (paragraphs are kept)
        //
        StringBuilder summary = new StringBuilder();
        int idx = 0;

        // loop over all characters in input string
        while (idx > -1 && (summary.length() < length || length == 0)
                && idx < pText.length()) {
            // get next chacter
            char c = pText.charAt(idx);

            if (c == '{') {
                // skip template and set idx to end of template
                idx = skipTemplate(pText, idx);
                continue;
            } else if (c == '<') {
                // is it a html comment
                if (pText.length() > idx + 1 && pText.charAt(idx + 1) == '!') {
                    // skip html comment
                    idx = skipHTMLComment(pText, idx);
                    continue;
                } else {
                    // html element starts here, skip it, set idx to end of html
                    // element
                    idx = skipHTMLElement(pText, idx);
                    continue;
                }
            } else if (c == '[') {

                // look ahead to see whether we have a link
                if (pText.charAt(idx + 1) == '[') {
                    // we have two square brackets "[[" (link)

                    // get the end of the double square bracket
                    int endOfLink = pText.indexOf("]]", idx);

                    // image link ?
                    int colon = pText.indexOf(":", idx);
                    if (colon > -1 && colon < endOfLink) {
                        // image link contains a caption which might contain
                        // a link within the link
                        idx = findEndOfLink(pText, idx);
                        continue;
                    }

                    int beginAnchor = pText.indexOf("|", idx);
                    if (beginAnchor > -1 && beginAnchor < endOfLink) {
                        idx = beginAnchor + 1;
                    } else {
                        idx = idx + 2;
                    }
                    continue;
                } else {
                    // next character is not a square brackets and thus a
                    // reference link to be removed
                    // get the end of the square bracket
                    int endOfLink = pText.indexOf("]", idx);
                    if (endOfLink > -1) {
                        idx = endOfLink + 1;
                        continue;
                    }
                }
            } else if (c == ']') {
                // look ahead
                if (idx + 1 < pText.length() && pText.charAt(idx + 1) == ']') {
                    idx = idx + 2;
                    continue;
                }
            }

            summary.append(c);
            idx++;
        }

        String textString = removeIndentAtBeginning(summary.toString());
        textString = removeWhiteSpace(
                textString.replaceAll("\r", " ").replaceAll("\n", " ")
                        .replaceAll("\t", " ")).trim();
        textString = removeBold(textString);
        textString = removeItalic(textString);

        // convert 'non breaking html spaces' into blanks. But preserve them
        // (don't remove white space)
        textString = textString.replaceAll("&nbsp;", " ");

        // find full stop near length of text
        // only look at first paragraph for summary
        int endOfTextIdx = textString.indexOf("==");
        if (endOfTextIdx == -1) {
            endOfTextIdx = length;
        }

        //
        if (endOfTextIdx < 20 || endOfTextIdx > length) {
            endOfTextIdx = textString.lastIndexOf(".");
            if (endOfTextIdx < 0.7 * length) {
                endOfTextIdx = textString.lastIndexOf(" ");
            }
        }

        // add elipsis if we have shortened the article
        if (endOfTextIdx > -1 && endOfTextIdx < textString.length()) {
            textString = textString.substring(0, endOfTextIdx) + " (...)";
        }

        // trim trailing spaces and return
        return textString.trim();
    }

    /**
     * skips templates in wikipedia markup. Templates are enclosed within braces
     * {}. There might be nested templates within an other template.
     *
     * @param pText
     *            : the wikipedia text with templates
     * @param pIdx
     *            , pos in text to start with, MUST be a {
     * @return the idx into the text where the template ends, or the last
     *         character in the text if it does not properly end.
     */
    static int skipTemplate(String pText, int pIdx) {
        // make sure we start with opening braces
        if (pText.charAt(pIdx) != '{') {
            return pIdx;
        }

        // counter for the braces we have opened, braces might be recursive
        // we use an iterative implementation, since it is a tiny little bit
        // faster
        int numOpenings = 1;
        // start with the next character
        int idx = pIdx + 1;
        // loop over the text starting from the next character till the end of
        // the template or the end of the text
        while (numOpenings > 0 && pText.length() > idx) {
            if (pText.charAt(idx) == '{') {
                numOpenings++;
            } else if (pText.charAt(idx) == '}') {
                numOpenings--;
            }
            idx++;
        }
        return idx;
    }

    /**
     * @param pText
     * @param pIdx
     *            , pos in text to start with, MUST be a {
     * @return
     */
    static int skipHTMLElement(String pText, int pIdx) {
        if (pText.charAt(pIdx) != '<') {
            return pIdx;
        }

        int numOpenings = 1;
        int idx = pIdx + 1;
        while (numOpenings > 0 && pText.length() > idx) {
            if (pText.charAt(idx) == '<') {
                numOpenings++;
            } else if (pText.charAt(idx) == '>') {
                numOpenings--;
            }
            idx++;
        }
        return idx;
    }

/**
     * @param pText
     * @param pIdx,
     *            pos in text to start with, MUST be a '<'
     * @return
     */
    static int skipHTMLComment(String pText, int pIdx) {
        if (pText.charAt(pIdx) != '<' && pText.charAt(pIdx + 1) != '!') {
            return pIdx;
        }

        int idx = pIdx;
        while ((idx = pText.indexOf('-', idx)) > -1) {
            if (pText.length() < idx + 2) {
                return pText.length();
            }
            if (pText.charAt(idx) == '-' && pText.charAt(idx + 1) == '-'
                    && pText.charAt(idx + 2) == '>') {
                return idx + 3;
            }
            idx++;
        }
        return idx;
    }

    private static String removeIndentAtBeginning(String pText) {
        pText = pText.trim();
        if (pText.startsWith(":")) {
            int lineFeed = pText.indexOf("\n");
            if (lineFeed > -1) {
                pText = pText.substring(lineFeed + 1);
            } else {
                // we may already have removed the linefeed
                // check for italics
                if (pText.startsWith(":''")) {
                    int italic = pText.indexOf("''", 3);
                    if (italic > -1) {
                        pText = pText.substring(italic + 2);
                    }
                }
            }
        }
        return pText;
    }

    private static int findEndOfLink(String pText, int pIdx) {
        int end = pText.indexOf("]]", pIdx);
        if (end == -1) {
            return pIdx;
        }

        int idx = pIdx;
        int openingIdx = pText.indexOf("[[", idx + 2);
        while (openingIdx > -1 && openingIdx < end) {
            idx = end;
            end = pText.indexOf("]]", end + 2);
            openingIdx = pText.indexOf("[[", idx);
        }
        if (end != -1) {
            idx = end;
        }
        return idx;
    }

    /**
     * Removes sequences of whitespace and keeps only one whitespace character.
     */
    public static String removeWhiteSpace(String pString) {
        StringBuilder buf = new StringBuilder();
        char[] chars = pString.toCharArray();
        int counter = 0;
        for (char c : chars) {
            if (c == ' ') {
                if (counter == 0) {
                    buf.append(' ');
                }
                counter++;
            } else {
                buf.append(c);
                counter = 0;
            }
        }
        return buf.toString();
    }

    public static String removeBold(String pString) {
        return pString.replaceAll("'''", "");
    }

    public static String removeItalic(String pString) {
        return pString.replaceAll("''", "");
    }

}
