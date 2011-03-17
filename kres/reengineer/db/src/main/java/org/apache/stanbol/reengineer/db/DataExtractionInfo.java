package org.apache.stanbol.reengineer.db;

public class DataExtractionInfo {

	String table;
	int jobDone;
	int jobDonePerc;
	int partialJobDone;
	int partialJobDonePerc;
	
	public DataExtractionInfo() {
		jobDone = 0;
		jobDonePerc = 0;
		partialJobDone = 0;
		partialJobDonePerc = 0;
		table = "";
	}
	
	public synchronized String getTable() {
		return table;
	}
	
	public synchronized void setTable(String table) {
		this.table = table;
	}
	
	public synchronized int getJobDone() {
		return jobDone;
	}
	
	public synchronized void setJobDone(int jobDone) {
		this.jobDone = jobDone;
	}
	
	public synchronized void addJobDone(int jobDone) {
		this.jobDone += jobDone;
	}
	
	public synchronized int getJobDonePerc() {
		return jobDonePerc;
	}
	
	public synchronized void setJobDonePerc(int jobDonePerc) {
		this.jobDonePerc = jobDonePerc;
	}
	
	public synchronized void addJobDonePerc(int jobDonePerc) {
		this.jobDonePerc += jobDonePerc;
	}
	
	public synchronized int getPartialJobDone() {
		return partialJobDone;
	}
	
	public synchronized void setPartialJobDone(int partialJobDone) {
		this.partialJobDone = partialJobDone;
	}
	
	public synchronized void addPartialJobDone(int partialJobDone) {
		this.partialJobDone += partialJobDone;
	}
	
	public synchronized int getPartialJobDonePerc() {
		return partialJobDonePerc;
	}
	
	public synchronized void setPartialJobDonePerc(int partialJobDonePerc) {
		this.partialJobDonePerc = partialJobDonePerc;
	}
	
	public synchronized void addPartialJobDonePerc(int partialJobDonePerc) {
		this.partialJobDonePerc += partialJobDonePerc;
	}
}
