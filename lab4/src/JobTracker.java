import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;

/**
 * @author Alexandru Stanisor
 *
 */
public class JobTracker extends AbstractServer {

	public static final String ZNODE_JOB_TRACKER_PATH = "/job_tracker";
	public static final String ZNODE_JOBS_PATH = "/jobs";
	public static final String ZNODE_TASK_RESULT = "/result";
	public static final String ZNODE_TASK_IN_PROGRESS = "/in_progress";
		
	private ZkConnector zkConnector = new ZkConnector(); 
	private LeaderElector leaderElector = null;
	private InetSocketAddress zooKeeperAddress = null;
		
	public static void main(String[] args) {
		
		String zkHost = Util.getStringArg(args, 0);
		Integer zkPort = Util.getIntegerArg(args, 1);
		Integer listeningPort = Util.getIntegerArg(args, 2);			
		
		JobTracker jobTracker = new JobTracker();		
		jobTracker.setPort(listeningPort);	
		jobTracker.setZooKeeperAddress(zkHost, zkPort);
		
		if (!jobTracker.bindPort()) {
			System.exit(1);
		}
						
		new Thread(jobTracker).start();
			
		Util.waitToExit();					
		jobTracker.kill();			
				
		System.exit(0);	

	}

	/**
	 * @return the zooKeeperAddress
	 */
	public InetSocketAddress getZooKeeperAddress() {
		return zooKeeperAddress;
	}


	/**
	 * @param zooKeeperAddress the zooKeeperAddress to set
	 */
	public void setZooKeeperAddress(InetSocketAddress zooKeeperAddress) {
		this.zooKeeperAddress = zooKeeperAddress;
	}
	
	/**
	 * @param zooKeeperAddress the zooKeeperAddress to set
	 */
	public void setZooKeeperAddress(String zkHost, int zkPort) {
		this.zooKeeperAddress = new InetSocketAddress(zkHost, zkPort);				
	}
	
	@Override
	public void kill() {
		super.kill();
		
		if (this.leaderElector != null) {
			this.leaderElector.kill();
		}
	}
	
	public void checkJobs() {
		try {
			
			String path = JobTracker.ZNODE_JOBS_PATH;
			this.zkConnector.connect(Util.getAddress(this.zooKeeperAddress));
			this.zkConnector.create(path, null, CreateMode.PERSISTENT);
									
			ZKOpResponse response = this.zkConnector.getChildren(path, null);
			
			List<String> jobs = response.getChildren();
			
			if (jobs == null || jobs.isEmpty()) {
				return;			
			}
			

			for (String job : jobs) {
				String jobPath = path + "/" + job;
								
				response = this.zkConnector.getChildren(jobPath, null);
				List<String> tasks = response.getChildren();
												
				if (tasks != null && tasks.size() < FileServer.DICTIONARY_PARTITIONS) {
					
					Collections.sort(tasks);
					
					for (int i = tasks.size(); i < FileServer.DICTIONARY_PARTITIONS; i++) {
						String taskPath = jobPath + "/" + i;
						this.zkConnector.create(taskPath, null, CreateMode.PERSISTENT);
					}						
				}
			}
			
		} catch (IOException e1) {			
		} catch (InterruptedException e1) {			
		}
	}
	
	@Override
	public void run() {

		this.leaderElector = new LeaderElector(this.zooKeeperAddress, this.myAddress , JobTracker.ZNODE_JOB_TRACKER_PATH);		
		new Thread(this.leaderElector).start();
		
		this.checkJobs();
		
		super.isAlive = true;
		
		
		while (super.isAlive) {
			try {											
				Socket connectionSocket = super.listeningSocket.accept();				
				JobTrackerConnection connection = new JobTrackerConnection(connectionSocket, this.zooKeeperAddress);				
				new Thread(connection).start();				
			} catch (Exception e) {					
				if (super.isAlive) {
					//System.err.println("ERROR: " + e.getMessage());
				}					
			}					          
		}
		
		try {			
			if (super.listeningSocket != null) {
				super.listeningSocket.close();
			} 	   	 				 	   	 				
		} catch (Exception e) {						
		}
		
		System.out.println("JobTracker shutdown.");
		
	}
}
