import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexandru Stanisor
 *
 */
public class FileServer extends AbstractServer {

	public static final String ZNODE_FILE_SERVER_PATH = "/file_server";
	
	public static final int DICTIONARY_SIZE = 265744;
	
	public static final int DICTIONARY_PARTITIONS = 136;
	
	public static final int DICTIONARY_PARTITION_SIZE = 1954;
	
	public static final String DICTIONARY_FILENAME = "./dictionary/lowercase.rand";
		
	private LeaderElector leaderElector = null;
	
	private InetSocketAddress zooKeeperAddress = null;
	
	private List<List<String>> partitions = new ArrayList<List<String>>();
		
	
	
	public static void main(String[] args) {
		
		String zkHost = Util.getStringArg(args, 0);
		Integer zkPort = Util.getIntegerArg(args, 1);
		Integer listeningPort = Util.getIntegerArg(args, 2);
		
		FileServer fileServer = new FileServer();		
		fileServer.setPort(listeningPort);	
		fileServer.setZooKeeperAddress(zkHost, zkPort);
		
		if (!fileServer.bindPort()) {
			System.exit(1);
		}
						
		new Thread(fileServer).start();
			
		Util.waitToExit();					
		fileServer.kill();
		
		System.out.println("FileServer successfully closed");
				
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
	
	@Override
	public void run() {
		
		for (int i = 0; i < DICTIONARY_PARTITIONS; i++) {
			List<String> partition = new ArrayList<String>();
			this.partitions.add(i, partition);
		}
		
		if (!this.loadDictionaryFile()) {
			return;
		}
		
		super.isAlive = true;
		
		this.leaderElector = 
				new LeaderElector(this.zooKeeperAddress, this.myAddress , FileServer.ZNODE_FILE_SERVER_PATH);		
		new Thread(this.leaderElector).start();
		
		while (super.isAlive) {
			try {											
				Socket connectionSocket = super.listeningSocket.accept();				
				FileServerConnection connection = new FileServerConnection(connectionSocket, this.partitions);				
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
	}
	
	private boolean loadDictionaryFile() {
		
		try {
			 		  		
		    File file = new File(DICTIONARY_FILENAME);
			BufferedReader in = new BufferedReader(new FileReader(file));

		    for (int i = 0; i < DICTIONARY_PARTITIONS; i++) {
		    	List<String> partition = this.partitions.get(i);		    	
		    	boolean stop = false;		    	
		    	for (int j = 0; j < DICTIONARY_PARTITION_SIZE; j++) {
		    		String word = in.readLine();
		    		if (word == null) {
		    			stop = true;
		    			break;
		    		}
		    		else {		    			
		    			partition.add(j, word);
		    		}
		    	}
		    	if (stop) {
		    		break;
		    	}
			}
		    
		    System.out.println("Done loading dictionary.");
		    in.close();
		} catch (IOException e) {
			System.err.println("Failed to parse file: " + DICTIONARY_FILENAME);
			return false;
		}
				
		return true;
	}
}
