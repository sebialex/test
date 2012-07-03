import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.KeeperException.Code;

import org.apache.zookeeper.CreateMode;

/**
 * @author Alexandru Stanisor
 *
 */
public class LeaderElector implements Watcher, Runnable {

	private CountDownLatch responseSignal = new CountDownLatch(1);	
	
	private WatchedEvent event = null;	
	
	private InetSocketAddress zkAddress = null;
	
	private InetSocketAddress myAddress = null;
	
	private ZkConnector zKConnector = null; 
	
	private boolean isAlive = false;		
	
	private String nodePath = null;	
	
	public LeaderElector(InetSocketAddress zkAddress, InetSocketAddress myAddress, String nodePath) {
		this.zkAddress = zkAddress;
		this.myAddress = myAddress;
		this.nodePath = nodePath;
	}
	
	@Override
	public void run() {
		
		this.isAlive = true;
					
		while (this.isAlive) {
			try {
				this.connectToZooKeeper();
			
				System.out.println("Connected to Zookeeper.");
				
				while (this.isAlive) {
					String data = Util.getAddress(this.myAddress);					
					ZKOpResponse response = this.zKConnector.create(this.nodePath, data, CreateMode.EPHEMERAL);
					
					if (response.getCode() == Code.NODEEXISTS) {
						System.out.println("Leader already exists. Wait as backup.");						
					}
					else if (response.getCode() == Code.OK) {
						System.out.println("I am the leader.");
					}		
					this.zKConnector.exists(this.nodePath, this);
					this.responseSignal.await();					
					this.responseSignal = new CountDownLatch(1);
				}
				
			}
			catch (Exception e) {			
				try {
					this.zKConnector.close();
				} catch (InterruptedException e1) {					
				}
			}													
		}
		
	}
	
	@Override
	public void process(WatchedEvent event) {		
		this.event = event;  
		if (event.getType() == Event.EventType.NodeDeleted) {
			this.responseSignal.countDown();	   
		}		  	
		else if (event.getState() == Event.KeeperState.Disconnected) {
			this.responseSignal.countDown();	   
		}
	}

	/**
	 * @return the event
	 */
	public WatchedEvent getEvent() {
		return event;
	}

	public void kill() {
		this.isAlive = false;	
		try {
			this.zKConnector.close();
		} catch (InterruptedException e) {					
		}
	}
	
	/**
	 * Getter of the isAlive flag
	 * @return	the isAlive flag
	 */
	public boolean isAlive() {
		return this.isAlive;
	}
	
	private boolean connectToZooKeeper() {
		boolean connected = false;
		
		try {
			this.zKConnector = new ZkConnector();			
			this.zKConnector.connect(Util.getAddress(this.zkAddress));
			connected = true;
		} catch (IOException e) {
			connected = false;			
		} catch (InterruptedException e) {	
			connected = false;			
		}		

		if (connected = false) {
			try {
				Thread.sleep(5000);
				System.out.println("Sleeping for 5000 ms");
			} catch (InterruptedException e) {							
			}
		}		
		return connected;
	}
}
