import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

import org.apache.zookeeper.KeeperException.Code;



/**
 * @author Alexandru Stanisor
 *
 */
public class ClientDriver extends AbstractClient {
	/*
     ***************************** PUBLIC FIELDS *******************************
     */
	
	/*
     ***************************** PRIVATE FIELDS ******************************
     */	
	private InetSocketAddress zooKeeperAddress = null;
	
	private ZkConnector zKConnector = new ZkConnector(); 
	
	/*
     ***************************** CONSTRUCTORS ******************************
     */

	public ClientDriver() {			
	}
		
	public ClientDriver(String lookUpHostName, Integer lookUpPort){		
		this.zooKeeperAddress = new InetSocketAddress(lookUpHostName, lookUpPort);
	}
	
	public ClientDriver(InetSocketAddress zooKeeperAddress){		
		this.zooKeeperAddress = zooKeeperAddress;		
	}
	
	/*
     ***************************** PUBLIC METHODS ******************************
     */
	
	/**
	 * Main method
	 * @param args
	 */
	public static void main(String [] args) {		
		String hostName = Util.getStringArg(args, 0);
		Integer port = Util.getIntegerArg(args, 1);		
		ClientDriver client = new ClientDriver(hostName, port);
	    client.run();  			    
	}
	
	@Override
	public void run() {
		System.out.println("Enter command or quit for exit:");	
		
		super.isAlive = true;
		String tempInput = null;
		String input = null;
		
		while (super.isAlive) {	
			try {
				if (this.getJobTracker()) {
					this.connect();					
					System.out.print("> ");
					BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
					
					while (super.isAlive) {			
						if (tempInput == null) {
							input = bufferRead.readLine();							
						}
						else {
							input = tempInput;
							tempInput = null;
						}											
						this.runFunction(input);
					}
					bufferRead.close();
				}
				else {
					super.isAlive = false;
					System.err.println("ERROR: could not aquire JobTracker server");
				}
				
			} catch (Exception e) {				
				tempInput = input;
			} finally { 
				super.disconnectFromServer();
			}
		}				
	}
	
	/*
     ***************************** PRIVATE METHODS ******************************
     */
	
	private boolean getJobTracker() {
		
		boolean result = false;
		
		if (super.isConnected()) {
			result = true;
		}
		else {
			try {
				this.zKConnector = new ZkConnector();				
				this.zKConnector.connect(Util.getAddress(this.zooKeeperAddress));				
				ZKOpResponse response = this.zKConnector.getData(JobTracker.ZNODE_JOB_TRACKER_PATH, null, null);
				
				if (response != null) {
					
					if (response.getCode() != Code.OK) {
						return false;
					}
				
					String address = new String (response.getData());
				
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
	
	/**
	 * This method holds the logic for the exchange client. It is executed every time a new
	 * console input is typed
	 * 
	 * @param input			the string received from System.in
	 * @throws Exception	thrown on any problem encountered
	 */
	private void runFunction(String input) throws Exception {		
	    
	    Payload packetToServer = new Payload();
	    if (input == null || input.indexOf(Util.EXIT_STRING) != -1) {
	    	super.isAlive = false;
	    	
	    	// send bye packet to broker
	    	packetToServer.setType(Payload.LEAVE);
	    	super.outToServer.writeObject(packetToServer);
	    	super.outToServer.flush();
	    	System.out.println("ClientDriver shutting down...");
	    	return;
	    }
	    	    	    
    	// parse console input
    	String [] inputs = input.split(Util.CONSOLE_INPUT_DELIMITER);
    	
    	if (inputs == null || inputs.length > 2 || inputs.length <= 1) {
    		System.err.println("ERROR: Invalid arguments!\n> ");
    		return;
    	}
    	
    	String command = inputs[0];
    	String arg1 = inputs[1];
    	    	    	   		    		    		    
		if ("add".equals(command)) {
			packetToServer.setType(Payload.ADD_JOB);
			packetToServer.setJob(arg1);
		}
		else if ("remove".equals(command)) {
			packetToServer.setType(Payload.REMOVE_JOB);
			packetToServer.setJob(arg1);
		}
		else if ("status".equals(command)) {
			packetToServer.setType(Payload.GET_JOB_STATUS);
			packetToServer.setJob(arg1);
		}
		else if ("md5".equals(command)) {
			System.out.println(Util.getHash(arg1));	
			return;
		}
		else {
			System.err.println("ERROR: Invalid command!\n> ");
    		return;
		}    		    		
    	    	
    	// send broker the request			    	    	
    	super.outToServer.writeObject(packetToServer);
    	super.outToServer.flush();
    	
    	// wait for the response			    	
    	Payload packetFromServer = (Payload)super.inFromServer.readObject();			    	
    	String result = packetFromServer.getJob();
    	
    	if (packetFromServer.getType() == Payload.JOB_STATUS_RESULT) {    		    	
    		System.out.println(result);
    	}
    	else if (packetFromServer.getType() == Payload.JOB_ADDED_RESULT) {
    		System.out.println(result);
    	}    
    	else if (packetFromServer.getType() == Payload.JOB_REMOVED_RESULT) {
    		System.out.println(result);
    	}    
    	else {
    		System.out.println("ERROR: Invalid reply!");
    	}	    		    				    				   
    	System.out.print("> ");	    
	}	
}
