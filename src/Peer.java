import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class Peer {

	static final String myId = (new SimpleDateFormat("mmssSSS")).format(new Date());
	static final String Name = Peer.class.getSimpleName() + myId;
	static final Logger log = Logger.getLogger(Name);
	private static int peerPort = -1;
	private static String serverHost = "localHost";
	private static int sPort = 8000;
	private static int neighPort = -1;

	// main method
	public static void main(String args[]) throws Exception {

		// initialize();
		log.info(String.format("Input: \nServer listening port \t: %d,\nPeer listening Port\t: %d, \nNeighbour port\t\t: %d", sPort, peerPort, neighPort));

		// Get few chunks from the server. 2 chunks at the least.
		Peer client = new Peer();
		client.getChunksFromServer(2);
		//client.startUploadThread();
		
	}

	void getChunksFromServer(int noOfChunks) throws Exception {

		log.info("Requesting connect to " + serverHost + " at port " + sPort);
		try (Socket requestSocket = new Socket(serverHost, sPort);
				ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());) {

			log.info("Connected to " + serverHost + " at port " + sPort);

			// Get the list of files residing on the server.
			String request = "getList";
			out.writeObject(request);
			out.flush();
			ConcurrentHashMap<String, Long> fileList = null;

			try {
				fileList = (ConcurrentHashMap<String, Long>) in.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			}

			log.info("Received list size" + fileList.size());

			String[] chunkNames = getRandomKeysInMap(fileList, noOfChunks);

			for (String chunkName : chunkNames) {
				Thread.sleep(10000);
				// Request for a random chunk from the above received list.
				log.info("Requesting chunk : " + chunkName + " at " + Name);
				out.writeObject(chunkName);
				out.flush();

				// receive file
				long timeStamp = System.currentTimeMillis();
				String fileName = chunkName + "." + Name;
				try (OutputStream fos = new FileOutputStream(fileName)) {
					byte[] bytes = new byte[Server.sizeOfChunks];

					int count;
					int totalBytes = 0;
					while ((count = in.read(bytes)) > 0) {
						totalBytes += count;
						log.finest("Writing " + count + " total : " + totalBytes + " size : " + Server.sizeOfChunks);
						fos.write(bytes, 0, count);
						if (totalBytes == fileList.get(chunkName))
							break;
					}
					fos.flush();
					log.info("Created chunk : " + fileName + " in " + (System.currentTimeMillis() - timeStamp));
				}
			}
			log.info(Name + " Received " + noOfChunks + " from " + serverHost);
		} catch (ConnectException ce) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} catch (UnknownHostException uhe) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

	}

	private static void initialize() {
		Scanner sc = new Scanner(System.in);
		log.info("Peer booting up. Enter details:");

		while (!(1024 < peerPort && peerPort <= 65535)) {
			log.info("Please enter a valid peer listening port :");
			try {
				peerPort = Integer.parseInt(sc.nextLine());
			} catch (Exception e) {
			}
		}
		try {
			log.info("Please enter a valid server listening port : [Enter to default]");
			sPort = Integer.parseInt(sc.nextLine());
		} catch (Exception e) {
			sPort = 8000;
		}
		while (!(1024 < neighPort && neighPort <= 65535)) {
			log.info("Please enter a valid peer neigbour listening port :");
			try {
				neighPort = Integer.parseInt(sc.nextLine());
			} catch (Exception e) {
			}
		}

		try {
			sc.close();
		} catch (Exception e) {
		}

	}

	private String[] getRandomKeysInMap(ConcurrentHashMap<String, Long> map, int count) {
		if (map != null) {
			// Put all the keys of the map in to a list. We have to calculate
			// random indices of this list.
			ArrayList<String> keysList = new ArrayList<String>(map.keySet());
			String[] indexes = new String[count];
			for (int i = 0; i < indexes.length; i++) {
				int randIndex = ThreadLocalRandom.current().nextInt(0, keysList.size());
				indexes[i] = keysList.get(randIndex);
				keysList.remove(randIndex);
			}
			return indexes;
		}
		return null;
	}

}
