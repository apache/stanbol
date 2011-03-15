package org.apache.stanbol.reengineer.base.util;

/**
 * The ReenginnerType class allows to declare the type of data source that a concrete SemionReengineer
 * is able to manage. The type is represented as an {@code int}
 * <br>
 * <br>
 * 
 * Valid values are:
 * <ul>
 * <li> 0 - Relational Databases
 * <li> 1 - XML
 * <li> 2 - iCalendar
 * <li> 3 - RSS
 * </ul>
 * 
 * 
 * @author andrea.nuzzolese
 *
 */

public class ReengineerType {
	
	/**
	 * 0 - Relational Databases
	 */
	public static int RDB = 0;
	
	/**
	 * 1 - XML
	 */	
	public static int XML = 1;
	
	/**
	 * 2 - iCalendar
	 */
	public static int I_CALENDAR = 2;
	
	/**
	 * 3 - RSS
	 */
	public static int RSS = 3;
	
	
	/**
	 * Static method that enables to know the the type of a data source supported by the reengineer in a human-readable string
	 * format
	 * 
	 * @param type {@code int}
	 * @return the string representing the data source type supported by the reengineer
	 */
	public static String getTypeString(int type){
		String typeString = null;
		switch(type){
		case 0:
			typeString = "rdbms";
			break;
		case 1:
			typeString = "xml";
			break;
		case 2:
			typeString = "v-calendar";
			break;
		case 3:
			typeString = "rss";
			break;
		}
		return typeString;
	}
	
	
	/**
	 * Static method that enables to know the the type of a data source supported by the reengineer in a human-readable string
	 * format
	 * 
	 * @param type {@code int}
	 * @return the string representing the data source type supported by the reengineer
	 */
	public static int getType(String typeString) throws UnsupportedReengineerException {
		int type = -1;
		if(typeString.equals("rdbms"))
			type = 0;
		else if(typeString.equals("xml"))
			type = 1;
		else if(typeString.equals("v-calendar"))
			type = 3;
		else if(typeString.equals("rss"))
			type = 3;
		else throw new UnsupportedReengineerException(typeString);
		
		return type;
	}
}
