import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Peer {

	static {
		// Programmatic configuration
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] (%2$s) %5$s %6$s%n");
	}

	static final String myId = (new SimpleDateFormat("mmssSSS")).format(new Date());
	static final String Name = Peer.class.getSimpleName() + myId;
	private static String summaryFile = "Summary.txt";
	private static String peerDirPrefix = "Files/" + Name + "/";
	static final Logger log = Logger.getLogger(Name);
	private static int peerPort = -1;
	private static String serverHost = "localHost";
	private static int sPort = 8000;
	private static int neighPort = -1;

	class Constants {
		static final String servergetList = "getList";
		static final String getSummary = "getSummary";
		static final String ServerDir = "Files/Server/";
		static final String getTotalChunks = "getTotalChunks";
		static final String getNextChunk = "getNextChunk";
		static final String seperator = "#";
	}

	private static int totalChunks = -1;
	private static AtomicInteger totalChunksCtr = new AtomicInteger();

	// main method
	public static void main(String args[]) throws Exception {

		switch (args.length) {
		case 3:
			neighPort = Integer.parseInt(args[2]);
		case 2:
			peerPort = Integer.parseInt(args[1]);
		case 1:
			sPort = Integer.parseInt(args[0]);
			break;
		default:
			sPort = 8000;
			peerPort = neighPort = 9999;
			System.err.println("Cannot boot with out port information. Please enter serverPort, Peer listening port & Neighbor port. Exiting.");
			// return;
		}

		log.info(String.format("Input: \nServer listening port \t: %d,\nPeer listening Port\t: %d, \nNeighbour port\t\t: %d", sPort, peerPort, neighPort));

		// Get few chunks from the server.
		getChunksFromServer(2);

		Thread u = new UploadThread(peerPort);
		u.start();

		Thread d = new DownloadThread(neighPort);
		d.start();
	}

	static boolean onlyOnce = true;

	private static void getChunksFromServer(int noOfChunks) throws Exception {

		log.info("Requesting connect to " + serverHost + " at port " + sPort);
		try (Socket requestSocket = new Socket(serverHost, sPort);
				ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());) {

			log.info("Connected to " + serverHost + " at port " + sPort);

			if (onlyOnce) {
				onlyOnce = !onlyOnce;
				// Get how many total chunks are present on the server. We need
				// to download these many number of chunks from server/peers.
				out.writeObject(Constants.getTotalChunks);
				out.flush();
				totalChunks = Integer.parseInt(String.valueOf(in.readObject()));
				totalChunksCtr.set(totalChunks);
			}
			HashMap<String, Long> chunkNames = new HashMap<String, Long>();
			// Download random chunks from the server. Pass peer id to help
			// server differentiate.
			while (noOfChunks > 0) {

				// Get the list of files residing on the server.
				out.writeObject(Constants.getNextChunk + Constants.seperator + Name);
				out.flush();

				String str = (String) in.readObject();
				String[] info = str.split(Constants.seperator);
				String chunkName = info[0];
				long size = Integer.parseInt(info[1]);

				if (chunkName != null && size > 0) {
					// Request for the above received chunk.
					log.info("Requesting chunk : " + chunkName);
					out.writeObject(chunkName);
					out.flush();

					// receive file
					long timeStamp = System.currentTimeMillis();
					File f = new File(peerDirPrefix + chunkName);
					f.getParentFile().mkdirs();
					try (OutputStream fos = new FileOutputStream(f)) {
						byte[] bytes = new byte[Server.sizeOfChunks];

						int count;
						int totalBytes = 0;
						while ((count = in.read(bytes)) > 0) {
							totalBytes += count;
							log.finest("Writing " + count + " total : " + totalBytes + " size : " + Server.sizeOfChunks);
							fos.write(bytes, 0, count);
							if (totalBytes == size)
								break;
						}
						fos.flush();
						log.info("Created chunk : " + chunkName + " in " + (System.currentTimeMillis() - timeStamp));
						chunkNames.put(chunkName, size);
					}
				}
				noOfChunks--;
			}
			log.info(Name + " Received " + noOfChunks + " from " + serverHost);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(peerDirPrefix + summaryFile, true))) {
				for (String chunkName : chunkNames.keySet()) {
					String entry = chunkName + Constants.seperator + chunkNames.get(chunkName);
					writer.write(entry);
					writer.newLine();
					totalChunksCtr.decrementAndGet();
				}
			}
		} catch (ConnectException ce) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} catch (UnknownHostException uhe) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

	}

	synchronized private static void appendToSummary(String chunkName, Long size) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(peerDirPrefix + summaryFile, true))) {
			totalChunksCtr.decrementAndGet();
			writer.write(chunkName + Constants.seperator + size);
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static HashMap<String, Long> summaryAsMap() {
		// Read the summary file.
		File f = new File(peerDirPrefix + summaryFile);
		if (f.exists()) {
			HashMap<String, Long> list = new HashMap<String, Long>();
			try {
				try (FileInputStream fis = new FileInputStream(f); BufferedReader br = new BufferedReader(new InputStreamReader(fis));) {
					String line = null;
					while ((line = br.readLine()) != null) {
						if (line.contains(Constants.seperator)) {
							String[] arr = line.split(Constants.seperator);
							list.put(arr[0], Long.parseLong(arr[1]));
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}
		return null;
	}

	private static class DownloadThread extends Thread {

		int neighPort;

		public DownloadThread(int neighPort) {
			this.neighPort = neighPort;
		}

		@Override
		public void run() {
			Socket neighSocket = null;
			try {
				// Try to make a connections to neighPort, If the connection
				// fails, sleep for 5 seconds and try again.
				boolean isConnected = false;
				while (!isConnected) {
					try {
						neighSocket = new Socket(serverHost, neighPort);
						isConnected = neighSocket.isConnected();
					} catch (Exception e) {
						try {
							if (neighSocket == null || !neighSocket.isConnected()) {
								long sleepTime = 5000;
								log.info("Sleeping for " + (sleepTime / 1000) + " seconds as the neighbor has not yet booted.");
								Thread.sleep(sleepTime);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}

				int consecutiveFailureCount = 0;
				// Able to connect to server, now find what files the neighbor
				// has and download them.
				try (ObjectOutputStream out = new ObjectOutputStream(neighSocket.getOutputStream()); ObjectInputStream in = new ObjectInputStream(neighSocket.getInputStream());) {
					while (true) {

						log.info("Requesting summary of " + neighSocket.toString() + " totalChunksCtr " + totalChunksCtr.get());
						out.writeObject(Constants.getSummary);
						out.flush();
						HashMap<String, Long> neighList = (HashMap<String, Long>) in.readObject();
						HashMap<String, Long> diffList = getDiffOfSummary(neighList);

						if (diffList.size() == 0) {
							if (consecutiveFailureCount == 5)
								getChunksFromServer(5);
							else {
								consecutiveFailureCount++;
								Thread.sleep(1000);
								continue;
							}
						}
						consecutiveFailureCount = 0;
						log.info("Received summary. Found new chunks: " + diffList);
						// Iteratively request and receive all the files.
						for (String chunkName : diffList.keySet()) {

							// Request for a chunk from the above difference of
							// summary list.
							log.info("Requesting chunk : " + chunkName);
							out.writeObject(chunkName);
							out.flush();

							// Receive the actual file
							long timeStamp = System.currentTimeMillis();
							File f = new File(peerDirPrefix + chunkName);
							f.getParentFile().mkdirs();
							try (OutputStream fos = new FileOutputStream(f)) {
								byte[] bytes = new byte[Server.sizeOfChunks];
								int count;
								int totalBytes = 0;
								while ((count = in.read(bytes)) > 0) {
									totalBytes += count;
									log.finest("Writing " + count + " total : " + totalBytes + " size : " + Server.sizeOfChunks);
									fos.write(bytes, 0, count);
									if (totalBytes == diffList.get(chunkName))
										break;
								}
								fos.flush();
								log.info("Created chunk : " + chunkName + " in " + (System.currentTimeMillis() - timeStamp));
								appendToSummary(chunkName, diffList.get(chunkName));
							}
						}

						// Check if the system downloaded all the chunks. If
						// yes, merge the files and stop download thread. Else,
						// sleep for 1 second and start downloading again.

						if (totalChunksCtr.get() == 0) {
							// System converged. Merge all the files to recreate
							// the original file.
							OutputStream fos = new FileOutputStream(peerDirPrefix + Server.fileName);
							for (int i = 0; i < totalChunks; i++) {
								String fname = peerDirPrefix + Server.fileName + "." + String.format(Server.formatSpecifier, i);
								log.info("Looking for file " + fname);
								File f = new File(fname);
								Files.copy(f.toPath(), fos);
							}
							fos.flush();
							log.info("Downloaded File!!");
							fos.close();
							break;
						} else {
							Thread.sleep(100);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (neighSocket != null && neighSocket.isConnected())
					try {
						neighSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}

		private HashMap<String, Long> getDiffOfSummary(HashMap<String, Long> neighList) {
			HashMap<String, Long> diff = (HashMap<String, Long>) neighList.clone();
			if (neighList != null && neighList.size() > 0) {
				HashMap<String, Long> primary = summaryAsMap();
				for (String key : primary.keySet()) {
					diff.remove(key);
				}
			}
			return diff;
		}
	}

	private static class UploadThread extends Thread {

		int myPort = -1;

		public UploadThread(int myPort) {
			this.myPort = myPort;
		}

		@Override
		public void run() {

			// We need only one connection for this socket. This peer uploads
			// data to only one specific neighbor.
			ServerSocket listener = null;
			try {
				// Code blocks here. Wait until another peer listens to you.
				listener = new ServerSocket(myPort);
				log.info("Ready for uploading to peer.");
				while (true) {
					Socket con = listener.accept();
					try (ObjectInputStream in = new ObjectInputStream(con.getInputStream());
							ObjectOutputStream out = new ObjectOutputStream(con.getOutputStream());
							DataOutputStream d = new DataOutputStream(out)) {
						while (true) {
							String request = (String) in.readObject();
							if (request.equals(Constants.getSummary)) {
								log.info("Sending summary to " + con.toString());
								out.writeObject(summaryAsMap());
							} else if (request.startsWith(Server.fileName)) {
								File f = new File(peerDirPrefix + request);
								Files.copy(f.toPath(), d);
								d.flush();
								log.info("Sent " + f.getName() + " to peer " + con.toString());
							}
						}
					} catch (IOException e) {
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (listener != null && !listener.isClosed())
					try {
						listener.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
			}
		}
	}
}
