package org.apache.stanbol.jsonld;

import java.util.Comparator;

/**
 * A comparator for JSON-LD maps to ensure the order of certain key elements
 * like '#', '@', 'a' in JSON-LD output.
 *
 * @author Fabian Christ
 */
public class JsonComparator implements Comparator<Object> {

    @Override
    public int compare(Object arg0, Object arg1) {
        int value;
        if (arg0.equals(arg1)) {
            value = 0;
        } else if (arg0.equals("#")) {
            value = -1;
        } else if (arg1.equals("#")) {
            value = 1;
        } else if (arg0.equals("@")) {
            value = -1;
        } else if (arg1.equals("@")) {
            value = 1;
        } else if (arg0.equals("a")) {
            value = -1;
        } else if (arg1.equals("a")) {
            value = 1;
        } else if (arg0.equals("#base")) {
            value = -1;
        } else if (arg1.equals("#base")) {
            value = 1;
        } else if (arg0.equals("#vocab")) {
            value = -1;
        } else if (arg1.equals("#vocab")) {
            value = 1;
        } else {
            value = String.valueOf(arg0).toLowerCase().compareTo(String.valueOf(arg1).toLowerCase());
        }

        return value;
    }

}
