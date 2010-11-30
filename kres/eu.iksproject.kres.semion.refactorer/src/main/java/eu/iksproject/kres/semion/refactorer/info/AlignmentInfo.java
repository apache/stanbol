package eu.iksproject.kres.semion.refactorer.info;

/**
 * The AlignmentInfo is an utility class for monitoring the progress of a refactoring process performed by one of the concrete
 * SemionRefactorers.
 * @author andrea.nuzzolese
 *
 */

public class AlignmentInfo {

	private int algimentJobDone;
	private int algimentJobDonePerc;
	private String operation;
	
	private int totalJobRequest;
	private int scale;
	
	/**
	 * Create a new {@code AlignmentInfo}. The {@code scale} identifies the reference scale in order to calculate the progress percentage,
	 * while the {@code totalJobRequest} is the total amount of work required to complete the refactoring task.
	 * 
	 * @param scale {@code int}
	 * @param totalJobRequest {@code int}
	 */
	public AlignmentInfo(int scale, int totalJobRequest) {
		this.scale = scale;
		this.totalJobRequest = totalJobRequest;
		algimentJobDone = 0;
		algimentJobDonePerc = 0;
	}
	
	/**
	 * Sets the job done by the refactoring task to the value passed as input.
	 * 
	 * @param algimentJobDone {@code int}
	 */
	public void setJobDone(int algimentJobDone) {
		this.algimentJobDone = algimentJobDone;
		this.algimentJobDonePerc = (this.algimentJobDone*scale)/totalJobRequest;
		
	}
	
	/**
	 * Increments the job done by the value passed as input.
	 * 
	 * @param plusJobDone {@code int}
	 */
	public void addJobDone(int plusJobDone){
		this.setJobDone(algimentJobDone+plusJobDone);
	}
	
	/**
	 * Sets the operation description that is currently performed by the refactoring task.
	 * 
	 * @param operation {@link String}
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	/**
	 * Gets the operation description that is currently performed by the refactoring task.
	 * 
	 * @return the operation name - {@link String}.
	 */
	public String getOperation() {
		return operation;
	}
	
	/**
	 * Gets the total job done during by a refactoring task as {@code int}.
	 * 
	 * @return the total done during by a refactoring task.
	 */
	public int getAlgimentJobDone() {
		return algimentJobDone;
	}
	
	/**
	 * Gets the total job done during by a refactoring task in percentage value respect to the scale setted.
	 * 
	 * @return the total job done during by a refactoring task in percentage value respect to the scale setted.
	 */
	public int getAlgimentJobDonePerc() {
		return algimentJobDonePerc;
	}
	
}
