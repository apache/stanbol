package org.apache.stanbol.enhancer.engines.opencalais.impl;

import org.apache.clerezza.rdf.core.Resource;

/**
 * This class stores the values extracted from the Calais entity data.
 * 
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 *
 */
public class CalaisEntityOccurrence {
  public Resource id;
  public Resource type;
  public Resource name;
  public Integer offset;
  public Integer length;
  public Resource exact;
  public String context;
  public Double relevance = -1.0;

  
  public CalaisEntityOccurrence() {}
  
  public String getTypeName() {
    if (type != null) {
      String tName = type.toString();
      int i = tName.lastIndexOf('/');
      if (i != -1) {
        return tName.substring(i+1);
      }
    }
    return null;
  }
  
  public String toString() {
  	return String.format("[id=%s, name=%s, exact=%s, type=%s, offset=%d, length=%d, context=\"%s\"]",
  			id,name,exact,type,offset,length,context);
  }
}
