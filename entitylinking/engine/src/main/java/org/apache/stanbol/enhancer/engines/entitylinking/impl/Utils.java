package org.apache.stanbol.enhancer.engines.entitylinking.impl;

public final class Utils {

    private Utils(){}
    
    public static boolean hasAlphaNumericChar(String label){
        if (label == null) {
            return false;
        }
        int sz = label.length();
        for (int i = 0; i < sz; i++) {
            if (Character.isLetterOrDigit(label.codePointAt(i))) {
                return true;
            }
        }
        return false;
    }
}
