import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Alexandru Stanisor
 *
 */
public class Util {

	public final static String EXIT_STRING = "quit";	
	public final static String CONSOLE_INPUT_DELIMITER = " ";
	public static final String ADDRESS_PAIR_DELIMITER = ":";
	public static final String ADDRESS_PAIR_LIST_DELIMITER = ",";
	
	public static void waitToExit() {
		
		
		boolean isAlive = true;
		BufferedReader bufferRead = null;
		System.out.println("Type '" + EXIT_STRING + "' to shut down server");
		System.out.print("> ");
		while (isAlive) {			
			try {
			    bufferRead = new BufferedReader(new InputStreamReader(System.in));
			    String input = bufferRead.readLine();
		 
			    if (input == null || input.toLowerCase().indexOf(EXIT_STRING) != -1) {
			    	isAlive = false;
			    	System.out.println("Shutting down...");
			    }
			    else {
			    	System.out.print("> ");
			    }
			}
			catch(IOException e) {
				System.err.println("ERROR: InputStream error, shutting down...");
				isAlive = false;	
			}
		}
		
		if (bufferRead != null) {
			try {
				bufferRead.close();
			} catch (IOException e) {
			}
		}
	}
	
	public static String getStringArg(String [] args, int argIndex) {
		if (args == null || args.length < argIndex + 1) {
			System.err.println("ERROR: Invlid number of arguments!");
			System.exit(1);
		}
		
		String str = args[argIndex];
		if (str == null || str.isEmpty()) {
			System.err.println("ERROR: Invalid argument!");
			System.exit(1);
		}
		
		return str;
	}
	
	public static Integer getIntegerArg(String [] args, int argIndex) {
		
		if (args == null || args.length < argIndex + 1) {
			System.err.println("ERROR: Invlid number of arguments!");
			System.exit(1);
		}
				
		Integer int_ = null;
		try {
			int_ = Integer.valueOf(args[argIndex]);
		}
		catch (NumberFormatException ne) {
			System.err.println("ERROR: argument not an integer!");
			System.exit(1);
		}		

		return int_;	
	}
	
	public static boolean areEqual(InetSocketAddress a, InetSocketAddress b) {		
		if (a == null || b == null) {
			return false;
		}
		
		if (a.getHostName() == null ||	b.getHostName() == null) {
			return false;
		}
		
		if (a.getHostName().equals(b.getHostName()) && (a.getPort() == b.getPort())) {
			return true;
		}
		else {
			return false;
		}		
	}
		
	public static InetSocketAddress getServerLocation(String pair) {
		if (pair == null || pair.isEmpty()) {
			return null;
		}
		String[] fields = pair.split(ADDRESS_PAIR_DELIMITER);
		
		if (fields.length != 2) {
			return null;
		}				
		
		try {
			Integer port = Integer.valueOf(fields[1]);
			if (port == null) {
				return null;
			}
			
			return (new InetSocketAddress(fields[0], port));
		}
		catch (NumberFormatException nfe) {
			return null;
		}		
		catch (IllegalArgumentException iae) {
			return null;
		}
		catch (SecurityException se) {
			return null;
		}
	}
	
	public static String getAddress(InetSocketAddress address) {
		if (address == null) {
			return null;
		}
		
		return (address.getAddress().getHostAddress() + ADDRESS_PAIR_DELIMITER + address.getPort());
	}
	
	public static Collection<InetSocketAddress> getLocations(String locationList) {
		if (locationList == null || locationList.isEmpty()) {
			return null;
		}
		
		String[] locations = locationList.split(ADDRESS_PAIR_LIST_DELIMITER);
		Collection<InetSocketAddress> serverLocations = new ArrayList<InetSocketAddress>();		
		
		if (locations != null && locations.length <= 0) {
			for (String locationPair : locations) {
				InetSocketAddress serverLoc = Util.getServerLocation(locationPair);
				if (serverLoc != null) {
					serverLocations.add(serverLoc);
				}
			}
		}
				
		return serverLocations;		
	}
	
	public static String getHash(String word) {

        String hash = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
            hash = hashint.toString(16);
            while (hash.length() < 32) hash = "0" + hash;
        } catch (NoSuchAlgorithmException nsae) {
            // ignore
        }
        return hash;
    }
}
