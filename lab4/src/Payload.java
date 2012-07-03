import java.io.Serializable;
import java.util.List;

/**
 * @author Alexandru Stanisor
 *
 */
public class Payload implements Serializable {
	/*
     ***************************** PUBLIC FIELDS *******************************
     */
	public static final int GET_JOB_STATUS = 0;
    
    public static final int LEAVE = 1;
    
    public static final int ADD_JOB = 2;
    
    public static final int REQUEST_TASK = 3;
    
    public static final int REMOVE_JOB = 4;
    
    public static final int OK = 5;
    
    public static final int JOB_STATUS_RESULT = 6;
    
    public static final int JOB_ADDED_RESULT = 7;
    
    public static final int JOB_REMOVED_RESULT = 8;
    
    public static final int REQUEST_PARTITION = 9;
    
    public static final int REQUEST_PARTITION_RESULT = 10;
    
    public static final int REQUEST_TASK_RESULT = 11;
    
    public static final String SUCCESS = "success";
    
    public static final String FAILED = "failed";
    
    public static final String ALREADY_EXISTS = "already exists";
    
    public static final String JOB_IN_PROGRESS = "job in progress";
    
    public static final String JOB_SUCCESS = "password was found: ";
    
    public static final String JOB_FAILED = "password was not found";
        
	/*
     ***************************** PRIVATE FIELDS ******************************
     */	
	
	private static final long serialVersionUID = 8443633990382329990L;
	
	private int type = -1;
	
	private String job = null;
	
	private int partition = -1;
	
	private List<String> partitionWords = null;
	
	/*
     ***************************** CONSTRUCTORS ******************************
     */
	public Payload(){
	}

	public Payload(int type) {
		this.type = type;
	}
		
	/*
     ***************************** PUBLIC METHODS ******************************
     */
	

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * @return the job
	 */
	public String getJob() {
		return job;
	}

	/**
	 * @param job the job to set
	 */
	public void setJob(String job) {
		this.job = job;
	}

	/**
	 * @return the partition
	 */
	public int getPartition() {
		return partition;
	}

	/**
	 * @param partition the partition to set
	 */
	public void setPartition(int partition) {
		this.partition = partition;
	}

	/**
	 * @return the partitionWords
	 */
	public List<String> getPartitionWords() {
		return partitionWords;
	}

	/**
	 * @param partitionWords the partitionWords to set
	 */
	public void setPartitionWords(List<String> partitionWords) {
		this.partitionWords = partitionWords;
	}

}
