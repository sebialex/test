import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

/**
 * @author Alexandru Stanisor
 *
 */
public abstract class AbstractServer implements Runnable {
	
	/*
     ***************************** PROTECTED FIELDS *******************************
     */
	
	protected InetSocketAddress myAddress = null;
	
	protected int listeningPort = -1;
	
	protected ServerSocket listeningSocket = null; 
	
	protected boolean isAlive = false;	
		
	/*
     ***************************** PRIVATE FIELDS ******************************
     */	
	private boolean boundFlag = false;
		
	/*
     ***************************** PUBLIC METHODS ******************************
     */
	/**
	 * Sets listeningPort 
	 * @param listeningPort
	 */
	public void setPort(int listeningPort) {
		this.listeningPort = listeningPort;
		try {
			this.myAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), listeningPort);
		} catch (UnknownHostException e) {			
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets listeningPort 
	 * @param listeningPort
	 */
	public void setPort(Integer listeningPort) {
		if (listeningPort != null) {
			this.listeningPort = listeningPort.intValue();
			
			try {
				this.myAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), listeningPort);
			} catch (UnknownHostException e) {			
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Tries to bind the listening port of this server
	 * @return	true if port was bound, false if it failed
	 */
	public boolean bindPort() {
		if (this.boundFlag) {
			System.err.println("ERROR: port " + this.listeningPort + "  already bound");			
			return false;
		}
		
		try {
			this.listeningSocket = new ServerSocket(this.listeningPort);
			this.boundFlag = true;
			System.out.println("Listening on port " + this.listeningPort);			
		} catch (IOException e) {
			System.err.println("Failed to bind port " + this.listeningPort);
			return false;
		}
		return true;
	}
	
	/**
	 * Unbinds the listening port of this server
	 */
	public void unbindPort() {
		if (this.listeningSocket != null) {
			try {
				this.listeningSocket.close();
				this.listeningSocket = null;
				this.boundFlag = false;
			} catch (IOException e) {					
			}
		}
	}
	
	/**
	 * Sets the isAlive flag to false and unbinds the listening port, effectively closing the server
	 */
	public void kill() {
		this.isAlive = false;
		this.unbindPort();
	}
	
	/**
	 * Getter of the isAlive flag
	 * @return	the isAlive flag
	 */
	public boolean isAlive() {
		return this.isAlive;
	}
}
