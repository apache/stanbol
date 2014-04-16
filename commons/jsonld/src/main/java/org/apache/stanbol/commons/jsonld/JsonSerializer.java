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
package org.apache.stanbol.commons.jsonld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

/**
 * Class to serialize a JSON object structure whereby the JSON structure is defined by the basic data types
 * Map and List.
 * 
 * @author Fabian Christ
 */
public final class JsonSerializer {
   
    /**
     * Restrict instantiation
     */
    private JsonSerializer() {}

    public static String toString(Map<String,Object> jsonMap) {
        StringBuffer sb = new StringBuffer();

        appendJsonMap(jsonMap, sb, 0, 0);
        removeOddChars(sb, 0);

        return sb.toString();
    }

    public static String toString(Map<String,Object> jsonMap, int indent) {
        StringBuffer sb = new StringBuffer();

        appendJsonMap(jsonMap, sb, indent, 0);
        removeOddChars(sb, indent);

        return sb.toString();
    }

    public static String toString(List<Object> jsonArray) {
        StringBuffer sb = new StringBuffer();

        appendList(jsonArray, sb, 0, 0);

        return sb.toString();
    }

    public static String toString(List<Object> jsonArray, int indent) {
        StringBuffer sb = new StringBuffer();

        appendList(jsonArray, sb, indent, 0);

        return sb.toString();
    }

    private static void appendJsonMap(Map<String,Object> jsonMap, StringBuffer sb, int indent, int level) {
        sb.append('{');
        level = increaseIndentationLevel(sb, indent, level);
        for (String key : jsonMap.keySet()) {
            appendIndentation(sb, indent, level);
            appendQuoted(key, sb);
            if (indent == 0) {
                sb.append(':');
            } else {
                sb.append(": ");
            }
            appendValueOf(jsonMap.get(key), sb, indent, level);
        }
        removeOddChars(sb, indent);
        level = decreaseIndentationLevel(sb, indent, level);
        appendIndentation(sb, indent, level);
        sb.append('}').append(',');
        appendLinefeed(sb, indent);
    }

    @SuppressWarnings("unchecked")
    private static void appendValueOf(Object object, StringBuffer sb, int indent, int level) {
        if (object == null) {
            return;
        }

        if (object instanceof String) {
            String strValue = (String) object;
            appendQuoted(strValue, sb);
            sb.append(',');
            appendLinefeed(sb, indent);
        } else if (object instanceof Map<?,?>) {
            Map<String,Object> mapValue = (Map<String,Object>) object;
            appendJsonMap(mapValue, sb, indent, level);
        } else if (object instanceof List<?>) {
            List<Object> lstValue = (List<Object>) object;
            if (lstValue.size() == 1) {
                // if the list contains only 1 element, we can serialize it as a single value
                appendValueOf(lstValue.get(0), sb, indent, level);
            }
            else {
                // the list has more or no elements
                appendList(lstValue, sb, indent, level);
                sb.append(',');
                appendLinefeed(sb, indent);
            }
        } else if (object instanceof JSONArray) {
            JSONArray ja = (JSONArray) object;
            List<Object> jsonList = new ArrayList<Object>();
            try {
                for (int i = 0; i < ja.length(); i++) {
                    jsonList.add(ja.get(i));
                }
            } catch (JSONException e) {
                // ignore
            }
            appendValueOf(jsonList, sb, indent, level);
        } else {
            sb.append(object.toString());
            sb.append(',');
            appendLinefeed(sb, indent);
        }
    }

    private static void appendList(List<Object> jsonArray, StringBuffer sb, int indent, int level) {
        sb.append('[');
        level = increaseIndentationLevel(sb, indent, level);
        for (Object object : jsonArray) {
            appendIndentation(sb, indent, level);
            appendValueOf(object, sb, indent, level);
        }
        removeOddChars(sb, indent);
        level = decreaseIndentationLevel(sb, indent, level);
        appendIndentation(sb, indent, level);
        sb.append(']');
    }

    private static void appendQuoted(String string, StringBuffer sb) {
        sb.append('"');
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            switch (ch) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(ch);
                    break;
//                case '/':
//                    sb.append('\\');
//                    sb.append(ch);
//                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (ch < ' ') {
                        String str = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(str.substring(str.length() - 4));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        sb.append('"');
    }

    private static void appendIndentation(StringBuffer sb, int indent, int level) {
        for (int i = 0; i < (indent * level); i++) {
            sb.append(' ');
        }
    }

    private static int decreaseIndentationLevel(StringBuffer sb, int indent, int level) {
        if (indent > 0) {
            appendLinefeed(sb, indent);
            level--;
        }
        return level;
    }

    private static int increaseIndentationLevel(StringBuffer sb, int indent, int level) {
        if (indent > 0) {
            appendLinefeed(sb, indent);
            level++;
        }
        return level;
    }

    private static void appendLinefeed(StringBuffer sb, int indent) {
        if (indent > 0) {
            sb.append('\n');
        }
    }

    /**
     * During the serialization there are added ',' and line breaks '\n' by default that need to be deleted
     * when not needed, e.g. at the end of a list.
     * 
     * @param sb
     * @param indent
     */
    private static void removeOddChars(StringBuffer sb, int indent) {
        if (sb.length() > 2) {
            sb.deleteCharAt(sb.length() - 1);
            if (indent > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
        }
    }
}
