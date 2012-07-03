import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * @author Alexandru Stanisor
 *
 */
public class Worker extends AbstractClient implements Watcher {

	private InetSocketAddress zooKeeperAddress = null;
	
	private ZkConnector zkConnector = new ZkConnector(); 
	
	private List<List<String>> partitions = new ArrayList<List<String>>();
	
	private CountDownLatch responseSignal = new CountDownLatch(1);	
	
	private int threads = 1; 
	
	public static final String ZNODE_WORKER_POOL_PATH = "/worker_pool"; 
	
	public static final String ZNODE_WORKER_PATH = "/worker"; 
	
	private static final boolean DEBUG_PRINT = false;
	
	public Worker() {		
	}
	
	public Worker(int threads) {
		this.threads = threads;
	}
		
	public Worker(String lookUpHostName, Integer lookUpPort, Integer threads){		
		this.zooKeeperAddress = new InetSocketAddress(lookUpHostName, lookUpPort);
		this.threads = threads;
	}
	
	public Worker(InetSocketAddress zooKeeperAddress){		
		this.zooKeeperAddress = zooKeeperAddress;		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String hostName = Util.getStringArg(args, 0);
		Integer port = Util.getIntegerArg(args, 1);
		Integer threads = new Integer(1);
		
		if (args.length == 3) {
			threads = Util.getIntegerArg(args, 2);
		}
				
		Worker worker = new Worker(hostName, port, threads);
		
		new Thread(worker).start();
		
		Util.waitToExit();					
		worker.kill();
		
		System.out.println("Worker successfully closed");
				
		System.exit(0);			    

	}
	
	@Override
	public void kill(){
		super.kill();		
		try {
			this.responseSignal.countDown();
			this.zkConnector.close();
			super.disconnectFromServer();
		} catch (Exception e) {								
		}
	}
	
	@Override
	public void process(WatchedEvent event) {		
		 
		if (event.getType() == Event.EventType.NodeChildrenChanged) {
			this.responseSignal.countDown();	   
		}		  			
	}
	
	@Override
	public void run() {
					
		for (int i = 0; i < FileServer.DICTIONARY_PARTITIONS; i++) {
			List<String> partition = new ArrayList<String>();
			this.partitions.add(i, partition);
		}
		
		super.isAlive = true;
				
		while (super.isAlive) {	
			try {
				if (this.getFileServer()) {
					this.connect();					
					this.zkConnector.connect(Util.getAddress(this.zooKeeperAddress));
					this.zkConnector.create(Worker.ZNODE_WORKER_POOL_PATH, null, CreateMode.PERSISTENT);
					String workerPath = Worker.ZNODE_WORKER_POOL_PATH + Worker.ZNODE_WORKER_PATH;
					this.zkConnector.create(workerPath, null, CreateMode.EPHEMERAL_SEQUENTIAL);
					
					while (super.isAlive) {			
						System.out.println("Worker running...");			
						
						List<Payload> tasks = this.getTask(this.threads);
						
						if (tasks.isEmpty()) {
							this.zkConnector.getChildren(JobTracker.ZNODE_JOBS_PATH, this);								
							this.zkConnector.getChildren(Worker.ZNODE_WORKER_POOL_PATH, this.getNewWatcher());	
							System.out.println("Waiting...");	
							this.responseSignal = new CountDownLatch(1);
							this.responseSignal.await();
							Thread.sleep(5000);							
						}
						else {
							Date start = new Date();
							for (Payload task : tasks) {							
								String jobHash = task.getJob();
								if (jobHash == null) {
									continue;
								}
								List<String> words = this.getPartition(task.getPartition());
								
								String result = null;
								
								if (words != null) {
									for (String word : words) {									
										if (jobHash.equals(Util.getHash(word))) {
											result = word;											
										}										
									}
								}
								
								String path = JobTracker.ZNODE_JOBS_PATH + "/" + task.getJob() + "/" + task.getPartition() + JobTracker.ZNODE_TASK_RESULT;														
								ZKOpResponse response = this.zkConnector.create(path, result, CreateMode.PERSISTENT);	
								this.printLine("zNodeCreated code:" + response.getCode().toString());
																							
							}	
							Date end = new Date();
							long diff = end.getTime() - start.getTime();
							System.out.println("Processed " + tasks.size() + " in " + diff + " ms");	
						}
					}
					
				}
				else {
					super.isAlive = false;
					System.err.println("ERROR: could not aquire JobTracker server");
					System.exit(1);
				}
				
			} catch (Exception e) {		
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {					
				}
			} finally { 
				super.disconnectFromServer();
				if (this.zkConnector != null) {
					try {
						this.zkConnector.close();
					} catch (InterruptedException e) {											
					}
				}
			}
		}				
	}
	
	private Watcher getNewWatcher() {
		
		return new Watcher() { // Anonymous Watcher
            @Override
            public void process(WatchedEvent event) {
                this.process(event);        
            } };
	}
	
	private List<Payload> getTask(int wantedNum) {
		
		int currentNum = 0;
		String path = JobTracker.ZNODE_JOBS_PATH;		
		ZKOpResponse response = this.zkConnector.getChildren(path, null);
		
		List<String> jobs = response.getChildren();
		
		List<Payload> tasksToReturn = new ArrayList<Payload>();
		
		if (jobs == null || jobs.isEmpty()) {
			return tasksToReturn;			
		}

		for (String job : jobs) {
			String jobPath = path + "/" + job;
			this.printLine("jobPath:" + jobPath);
			
			response = this.zkConnector.getChildren(jobPath, null);
			List<String> tasks = response.getChildren();
											
			if (tasks != null && tasks.size() >= FileServer.DICTIONARY_PARTITIONS) {
				
				for (String task : tasks) {
					String taskPath = jobPath + "/" + task;
											
					response = this.zkConnector.getChildren(taskPath, null);
					List<String> tasksChildren = response.getChildren();
					
					if (tasksChildren == null || tasksChildren.isEmpty()) {
						String inProgressPath = taskPath + JobTracker.ZNODE_TASK_IN_PROGRESS;
						this.printLine("inProgressPath:" + inProgressPath);
						response = this.zkConnector.create(inProgressPath, null, CreateMode.EPHEMERAL);
						
						if (response.getCode() == Code.OK) {								
							try {
								Payload newTask = new Payload();
								newTask.setJob(job);
								int partitionIndex = Integer.valueOf(task);
								newTask.setPartition(partitionIndex);
								tasksToReturn.add(newTask);
								this.printLine("taskPath added:" + taskPath);
								currentNum++;
							}
							catch (NumberFormatException e) {									
							}												
						}	
					}
					
					if (currentNum >= wantedNum)  {
						break;
					}
				}
									
			}
						
			if (currentNum >= wantedNum)  {
				break;
			}
		}
		
		return tasksToReturn;
	}
	
	/**
	 * This method holds the logic for the exchange client. It is executed every time a new
	 * console input is typed
	 * 
	 * @param input			the string received from System.in
	 * @throws Exception	thrown on any problem encountered
	 */
	private List<String> getPartition(int partition) throws Exception {		
	    
		List<String> partitionWords = null ; 		
				
		Payload packetToServer = new Payload();
	    packetToServer.setType(Payload.REQUEST_PARTITION);
	    packetToServer.setPartition(partition);
	    
    	// send broker the request			    	    	
    	super.outToServer.writeObject(packetToServer);
    	super.outToServer.flush();
    	
    	// wait for the response			    	
    	Payload packetFromServer = (Payload)super.inFromServer.readObject();			    	
    	    	
    	if (packetFromServer.getType() == Payload.REQUEST_PARTITION_RESULT) {    		    	
    		partitionWords = packetFromServer.getPartitionWords();
    		if (partitionWords != null) {
    			this.partitions.add(partition, partitionWords);
    		}
    		return partitionWords;
    	}
    	else {
    		System.out.println("ERROR: Invalid reply!");
    		System.out.print("> ");
    		return null;
    	}	    		    				    				   
    		    
	}	
	
	private boolean getFileServer() {
		
		boolean result = false;
		
		if (super.isConnected()) {
			result = true;
		}
		else {
			try {
				this.zkConnector = new ZkConnector();				
				this.zkConnector.connect(Util.getAddress(this.zooKeeperAddress));				
				ZKOpResponse response = this.zkConnector.getData(FileServer.ZNODE_FILE_SERVER_PATH, null, null);
				
				if (response != null) {
					
					this.printLine(response.printString());				
					String address = new String (response.getData());
					this.printLine("FileServer address:" + address);					
					if (address != null) {
						super.setServer(Util.getServerLocation(address));								
						result = true;
					}
				}				
			} catch (IOException e) {
				result = false;				
			} catch (InterruptedException e) {	
				result = false;				
			}		
		}
		
		return result;
	}
	
	private void printLine(String toPrint) {
    	if (DEBUG_PRINT) {
    		System.out.println(toPrint);
    	}    	
	}

}
