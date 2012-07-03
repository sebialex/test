import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

/**
 * @author Alexandru Stanisor
 *
 */
public class JobTrackerConnection implements Runnable {

	/*
     ***************************** PRIVATE FIELDS ******************************
     */	
	private Socket connectionSocket = null; 	
	private ZkConnector zkConnector = new ZkConnector();
	private InetSocketAddress zooKeeperAddress = null;
	
	
	/*
     ***************************** CONSTRUCTORS ******************************
     */
	public JobTrackerConnection(Socket connectionSocket, InetSocketAddress zooKeeperAddress) {
		this.connectionSocket = connectionSocket;
		this.zooKeeperAddress = zooKeeperAddress;
	}
	
	/*
     ***************************** PUBLIC METHODS ******************************
     */	
	@Override
	public void run() {
				       
		Payload receivedPacket = null;
        ObjectInputStream inFromClient = null;
   		ObjectOutputStream outToClient = null;
   		
   		
   		System.out.println("\nClient connected");   		
   		
		try {
			
			this.zkConnector.connect(Util.getAddress(this.zooKeeperAddress));			
			
			inFromClient = new ObjectInputStream(this.connectionSocket.getInputStream());
			outToClient = new ObjectOutputStream(this.connectionSocket.getOutputStream());
			
			while (( receivedPacket = (Payload) inFromClient.readObject()) != null) {
				
				Payload packetToClient = new Payload(Payload.OK);
				
				int type = receivedPacket.getType();				
				
				String jobPath = JobTracker.ZNODE_JOBS_PATH + "/" + receivedPacket.getJob();
				
				if (type == Payload.LEAVE) {
					break;
				}
				
				if (type == Payload.ADD_JOB) {																		
					ZKOpResponse response = this.zkConnector.create(jobPath, null, CreateMode.PERSISTENT);
					
					if (response.getCode() == Code.NODEEXISTS) {
						packetToClient.setJob(Payload.ALREADY_EXISTS);
					}					
					else {
						for (int i = 0; i < FileServer.DICTIONARY_PARTITIONS; i++) {
							String taskPath = jobPath + "/" + i;
							response = this.zkConnector.create(taskPath, null, CreateMode.PERSISTENT);
						}
						packetToClient.setJob(Payload.SUCCESS);
					}	
					
					packetToClient.setType(Payload.JOB_ADDED_RESULT);
														
				}				
				else if (type == Payload.REMOVE_JOB) {
					this.zkConnector.deleteRecursive(jobPath);
					Stat stat = this.zkConnector.exists(jobPath, null);
					
					packetToClient.setType(Payload.JOB_REMOVED_RESULT);
					if (stat == null) {						
						packetToClient.setJob(Payload.SUCCESS);
					}
					else {
						packetToClient.setJob(Payload.FAILED);
					}
				}
				else if (type == Payload.GET_JOB_STATUS) {
					ZKOpResponse response = this.zkConnector.getData(jobPath, null, null);
					String jobResult = null;
					
					packetToClient.setType(Payload.JOB_STATUS_RESULT);
					
					response = this.zkConnector.getChildren(jobPath, null);					
					List<String> tasks = response.getChildren();
					
					boolean childrenAreDone = true;
					
					
					if (tasks != null && tasks.size() == FileServer.DICTIONARY_PARTITIONS) {	
												
						for (String task : tasks) {
							String childPath = jobPath + "/" + task;
							response = this.zkConnector.getChildren(childPath, null);
							
							List<String> taskChildren = response.getChildren();
							
							if (taskChildren == null || taskChildren.isEmpty()) {
								childrenAreDone = false;								
								continue;
							}
							else {
								String resultPath = childPath + JobTracker.ZNODE_TASK_RESULT;
								response = this.zkConnector.getData(resultPath, null, null);
								
								if (response.getData() != null) {
									jobResult = response.getData();
								}
							}
						}
					}
					else {
						childrenAreDone = false;
					}
					
					if (!childrenAreDone && jobResult == null) {
						packetToClient.setJob(Payload.JOB_IN_PROGRESS);
					}
					else if (jobResult == null) {
						packetToClient.setJob(Payload.JOB_FAILED);
					}
					else {
						packetToClient.setJob(Payload.JOB_SUCCESS + jobResult);
					}					
				}
				
				outToClient.writeObject(packetToClient);
				outToClient.flush();
			}			 	
		} catch (Exception e) {
			//System.err.println("ERROR: " + e.getMessage());				
		}
		finally {
 			try {
 				if (inFromClient != null) {
 					inFromClient.close();
 				}
 				if (outToClient != null) {
 					outToClient.close();
 				}
 				if (this.connectionSocket != null) {
 					this.connectionSocket.close();
 				} 	   	 	
 				if (this.zkConnector != null) {
 					this.zkConnector.close();
 				}
			} catch (Exception e) {						
			}
		}
		System.out.println("Client disconnected");
		System.out.print("> ");
	}
	
	/*
     ***************************** PRIVATE METHODS ******************************
     */
	
}
