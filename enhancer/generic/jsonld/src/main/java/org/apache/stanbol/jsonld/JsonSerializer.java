package org.apache.stanbol.jsonld;

import java.util.List;
import java.util.Map;


/**
 * Class to serialize a JSON object structure whereby the JSON structure is
 * defined by the basic data types Map and List.
 *
 * @author Fabian Christ
 */
public class JsonSerializer {

    public static String toString(Map<String, Object> jsonMap) {
        StringBuffer sb = new StringBuffer();

        appendJsonMap(jsonMap, sb, 0, 0);
        removeOddChars(sb, 0);

        return sb.toString();
    }

    public static String toString(Map<String, Object> jsonMap, int indent) {
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

    private static void appendJsonMap(Map<String, Object> jsonMap, StringBuffer sb, int indent, int level) {
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
        }
        else if (object instanceof Map<?,?>) {
            Map<String, Object> mapValue = (Map<String, Object>) object;
            appendJsonMap(mapValue, sb, indent, level);
        }
        else if (object instanceof List<?>) {
            List<Object> lstValue = (List<Object>) object;
            appendList(lstValue, sb, indent, level);
            sb.append(',');
            appendLinefeed(sb, indent);
        }
        else {
            sb.append(object.toString());
            sb.append(',');
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
            case '/':
                sb.append('\\');
                sb.append(ch);
                break;
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
                    sb.append("\\u" + str.substring(str.length() - 4));
                } else {
                    sb.append(ch);
                }
            }
        }
        sb.append('"');
    }

    private static void appendIndentation(StringBuffer sb, int indent, int level) {
        for (int i=0; i<(indent*level); i++) {
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

    private static void removeOddChars(StringBuffer sb, int indent) {
        sb.deleteCharAt(sb.length()-1);
        if (indent > 0) {
            sb.deleteCharAt(sb.length()-1);
        }
    }
}
