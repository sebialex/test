import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.io.IOException;

public class ZkConnector implements Watcher {

    // ZooKeeper Object
    private ZooKeeper zooKeeper = null;

    // To block any operation until ZooKeeper is connected. It's initialized
    // with count 1, that is, ZooKeeper connect state.
    private CountDownLatch connectedSignal = new CountDownLatch(1);
    
    // ACL, set to Completely Open
    protected static final List<ACL> acl = Ids.OPEN_ACL_UNSAFE;

    private static final boolean DEBUG_PRINT = false;
    
    /**
     * Connects to ZooKeeper servers specified by hosts.
     */
    public void connect(String hosts) throws IOException, InterruptedException {

    	this.printLine("ZkConnector: trying to connect to=" + hosts);
        this.zooKeeper = new ZooKeeper(
                hosts, // ZooKeeper service hosts
                5000,  // Session timeout in milliseconds
                this); // watcher - see process method for callbacks
        this.connectedSignal.await();
    }

    /**
     * Closes connection with ZooKeeper
     */
    public void close() throws InterruptedException {
    	this.zooKeeper.close();
    }

    /**
     * @return the zooKeeper
     */
    public ZooKeeper getZooKeeper() {
        // Verify ZooKeeper's validity
        if (null == this.zooKeeper || !this.zooKeeper.getState().equals(States.CONNECTED)) {
	        throw new IllegalStateException ("ZooKeeper is not connected.");
        }
        return this.zooKeeper;
    }

    protected Stat exists(String path, Watcher watch) {              
        try {
        	Stat stat = this.zooKeeper.exists(path, watch);        	
        	return stat;
        } catch(Exception e) {
        	return null;
        }      
    }

    protected ZKOpResponse create(String path, String data, CreateMode mode) {
            	
    	Code code = null;
    	String newPath = null;
    	
        try {
            byte[] byteData = null;
            if(data != null) {
                byteData = data.getBytes();
            }
            newPath = this.zooKeeper.create(path, byteData, acl, mode);
            code = KeeperException.Code.OK;
            
        } catch(KeeperException e) {
        	code =  e.code();
        } catch(Exception e) {
        	code = KeeperException.Code.SYSTEMERROR;
        	e.printStackTrace();
        }
        
        return new ZKOpResponse(code, newPath);
    }
    
    protected void delete(String path, int version) {
    	
        try {           
            this.zooKeeper.delete(path, version);            
        } catch(KeeperException e) {
        	//code =  e.code();
        }
    	catch(Exception e) {
        	//code = KeeperException.Code.SYSTEMERROR;
        }
        
    }
    
    protected void deleteRecursive(String path) {    	    	    	
    	this.printLine("deleteRecursive(): path:" + path);
    	try {           
        	List<String> children = this.zooKeeper.getChildren(path, null);    
        	
        	if (children != null && !children.isEmpty()) {
        		for (String child : children) {        			
        			this.deleteRecursive(path + "/" + child);
        		}
        	}
        	
        	this.delete(path, -1);        	
        } catch(KeeperException e) {        	
        }
    	catch(Exception e) {        	
        }        
    }

    protected ZKOpResponse getChildren(String path, Watcher watch) {
    	
    	Code code = null;
    	List<String> children = null;
    	
    	try {
         	children = this.zooKeeper.getChildren(path, watch);        	         	             	         	
        } catch(KeeperException e) {
        	code =  e.code();
        } catch(Exception e) {
        	code = KeeperException.Code.SYSTEMERROR;
        }
        
        code = KeeperException.Code.OK;
        
        return new ZKOpResponse(code, children);
    }
    
    protected ZKOpResponse getData(String path, Watcher watcher, Stat stat) {
    	
    	Code code = null;
    	byte[] data = null;
    	
    	code = KeeperException.Code.OK;
    	
    	try {    		
    		data = this.zooKeeper.getData(path, watcher, stat);
    		this.printLine("ZkConnector: getData(): path:" + path + ", data:" + data);
        } catch(KeeperException e) {
        	code =  e.code();
        } catch(Exception e) {
        	code = KeeperException.Code.SYSTEMERROR;
        }
                
        String stringData = null;
        if (data != null) {
        	stringData = new String(data);
        }
        
        return new ZKOpResponse(stringData, code);
    }
    
    public void process(WatchedEvent event) {
        // release lock if ZooKeeper is connected.
        if (event.getState() == KeeperState.SyncConnected) {
        	this.connectedSignal.countDown();
        }
    }
    
    private void printLine(String toPrint) {
    	if (DEBUG_PRINT) {
    		System.out.println(toPrint);
    	}    	
	}
}

