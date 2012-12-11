package org.apache.stanbol.entityhub.indexing.geonames;

import java.util.Iterator;
import java.util.StringTokenizer;

public class LineTokenizer implements Iterator<String>{
    private static final String DELIM ="\t";
    private final StringTokenizer t;
    private boolean prevElementWasNull = true;
    public LineTokenizer(String data){
        t = new StringTokenizer(data, DELIM, true);
    }
    @Override
   public boolean hasNext() {
        return t.hasMoreTokens();
    }

    @Override
    public String next() {
        if(!prevElementWasNull){
            t.nextElement();//dump the delim
        }
        if(!t.hasMoreElements()){
            //this indicated, that the current Element is
            // - the last Element
            // - and is null
            prevElementWasNull = true;
            return null;
        } else {
            String act = t.nextToken();
            if(DELIM.equals(act)){
                prevElementWasNull = true;
                return null;
            } else {
                prevElementWasNull = false;
                return act;
            }
        }
    }
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}