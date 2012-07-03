import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * @author Alexandru Stanisor
 *
 */
public class FileServerConnection implements Runnable {

	/*
     ***************************** PRIVATE FIELDS ******************************
     */	
	private Socket connectionSocket = null; 	
	
	private ZkConnector zkConnector = new ZkConnector();
	
	private List<List<String>> partitions = null;	
	
	/*
     ***************************** CONSTRUCTORS ******************************
     */
	public FileServerConnection(Socket connectionSocket, List<List<String>> partitions) {
		this.connectionSocket = connectionSocket;		
		this.partitions = partitions;
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
			inFromClient = new ObjectInputStream(this.connectionSocket.getInputStream());
			outToClient = new ObjectOutputStream(this.connectionSocket.getOutputStream());
			
			while (( receivedPacket = (Payload) inFromClient.readObject()) != null) {
				
				Payload packetToClient = new Payload(Payload.OK);
				
				int type = receivedPacket.getType();				
								
				if (type == Payload.LEAVE) {
					break;
				}
				
				if (type == Payload.REQUEST_PARTITION) {																																	
					packetToClient.setType(Payload.REQUEST_PARTITION_RESULT);
					packetToClient.setPartitionWords(this.partitions.get(receivedPacket.getPartition()));														
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
