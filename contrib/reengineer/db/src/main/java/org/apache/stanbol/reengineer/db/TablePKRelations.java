package org.apache.stanbol.reengineer.db;

import java.util.ArrayList;

/**
 * 
 * Utility class for representing and managing in Semion primary and foreign keys of relatinal databases.
 * 
 * @author andrea.nuzzolese
 *
 */

public class TablePKRelations {

	String fkTableName;
	ArrayList<String> pkColumns;
	ArrayList<String> fkColumns;
	String pkTable;
	
	/**
	 * Create a new {@code TablePKRelations}
	 * 
	 * @param fkTableName
	 * @param fkColumns
	 * @param pkColumns
	 * @param pkTable
	 */
	public TablePKRelations(String fkTableName, ArrayList<String> fkColumns, ArrayList<String> pkColumns, String pkTable) {
		this.fkTableName = fkTableName;
		this.fkColumns = fkColumns;
		this.pkColumns = pkColumns;
		this.pkTable = pkTable;
	}
	
	
	public String getFkTableName() {
		return fkTableName;
	}

	public ArrayList<String> getFkColumns() {
		return fkColumns;
	}

	public ArrayList<String> getPkColumns() {
		return pkColumns;
	}

	public String getPkTable() {
		return pkTable;
	}

	public void setPkColumns(ArrayList<String> pkColumns) {
		this.pkColumns = pkColumns;
	}

	public void setFkColumns(ArrayList<String> fkColumns) {
		this.fkColumns = fkColumns;
	}
	
}
