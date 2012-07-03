import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.IllegalBlockingModeException;

/**
 * @author Alexandru Stanisor
 *
 * Abstract class which can be used by clients to connect to servers
 *
 */
public abstract class AbstractClient implements Runnable {
 
	/*
     ***************************** PROTECTED FIELDS *******************************
     */
	
	protected InetSocketAddress address = null;
	
	protected Socket connectionSocket = null;
	
	protected ObjectInputStream inFromServer = null;
	
	protected ObjectOutputStream outToServer = null;
	
	protected boolean isAlive = false;		
	
	/*
     ***************************** PRIVATE FIELDS ******************************
     */		
	private boolean connectedFlag = false;
		
	/*
     ***************************** PUBLIC METHODS ******************************
     */
	
	/**
	 * Sets the server hostName and port
	 * @param hostName the hostName to set
	 * @param port the port to set
	 */
	public void setServer(String hostName, Integer port) {		
		this.address = new InetSocketAddress(hostName, port);
	}
	
	public void setServer(InetSocketAddress addressAndPort) {
		this.address = addressAndPort;
	}
	
	public InetSocketAddress getServer() {
		return this.address;
	}
	
	public void kill() {
		this.isAlive = false;		
	}
	
	/**
	 * Connects to the server given by the hostName and port of this object. If currently connected to
	 * another server an Exception will be thrown. Disconnect first before using this method.
	 * @throws Exception on any problem encountered
	 */
	public void connect() throws IOException, IllegalBlockingModeException, IllegalArgumentException  {
		if (this.connectedFlag) {
			throw new IOException("ERROR: client already connected");			
		}		
									
		this.connectionSocket = new Socket();
		this.connectionSocket.connect(this.address);
		this.outToServer = new ObjectOutputStream(this.connectionSocket.getOutputStream());
		this.inFromServer = new ObjectInputStream(this.connectionSocket.getInputStream());
		this.connectedFlag = true;				
	}
		
	
	/**
	 * Disconnects from the currently connected server.
	 */
	public void disconnectFromServer() {
		if (!this.connectedFlag) {
			return;
		}
				
    	try {    		
			this.outToServer.writeObject(new Payload(Payload.LEAVE));
			this.outToServer.flush();
					
			if (this.inFromServer != null) {
				inFromServer.close();
			}
			if (this.outToServer != null) {
				this.outToServer.close();
			}
			if (this.connectionSocket != null) {
				this.connectionSocket.close();
			} 	   	 				 	   	 				
		} catch (Exception e) {							
		}
		finally {			
			this.inFromServer = null;
			this.outToServer = null;
			this.connectionSocket = null;
		}
		
		this.connectedFlag = false;
	}
	
	/**
	 * Performs a request to the currently connected server with the given BrokerPacket
	 * @param packetToServer the packet containing the request
	 * @return returns a response BrokerPacket, could be null
	 * @throws Exception on any problem
	 */
	public Payload sendAndReceive(Payload packetToServer) throws Exception {		
		this.outToServer.writeObject(packetToServer);
    	this.outToServer.flush();    	    				    
    	return ((Payload)this.inFromServer.readObject());				
	}

	public void send(Payload packetToServer) throws Exception {		
		this.outToServer.writeObject(packetToServer);
    	this.outToServer.flush();    	    				        			
	}
	
	public Payload receive() throws Exception {  	    				    
    	return ((Payload)this.inFromServer.readObject());				
	}
	
	/**
	 * @return the connectedFlag
	 */
	public boolean isConnected() {
		return this.connectedFlag;
	}
}
