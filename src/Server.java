import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

	static {
		// Programmatic configuration
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] (%2$s) %5$s %6$s%n");
	}

	// The server will be listening on this port number
	private static int sPort = 8000;

	public static String formatSpecifier = "%05d";
	static final Logger log = Logger.getLogger(Server.class.getSimpleName());
	private static int noOfChunks = 10;
	public static final int sizeOfChunks = 100 * 1024;// 100KB chunks.
	public static final String fileName = "43394921.txt";
	private static final String fileContent = "\nThis content is just a repetitive boring one line that has been written to make this line look longer and the file size to grow really faster. If you are still reading this, Just stop.\nLine:";
	private static ConcurrentHashMap<String, Long> listOfChunks = null;

	private static ConcurrentHashMap<String, Long> pendingListOfFiles = null;

	public static void main(String[] args) throws Exception {

		switch (args.length) {
		case 2:
			noOfChunks = Integer.parseInt(args[1]);
		case 1:
			sPort = Integer.parseInt(args[0]);
			break;
		default:
			sPort = 8000;
			noOfChunks = 10;
		}

		log.info(String.format("Input: \nServer port \t: %d,\nnoOfChunks \t: %d", sPort, noOfChunks));

		initFile(noOfChunks);

		pendingListOfFiles = listOfChunks;

		try {

			ServerSocket listener = new ServerSocket(sPort);
			int clientNum = 1;
			try {
				while (true) {
					new Handler(listener.accept(), clientNum).start();
					log.info("Client " + clientNum + " is connected!");
					clientNum++;
				}
			} finally {
				listener.close();
			}
		} catch (BindException be) {
			log.severe("Address already in use : " + sPort + " Exiting.");
		}
	}

	private static AtomicInteger sharedVariable = new AtomicInteger(0);

	/**
	 * A handler thread class. Handlers are spawned from the listening loop and
	 * are responsible for dealing with a single client's requests.
	 */
	private static class Handler extends Thread {
		private String request; // message received from the client
		private Socket connection;
		private int peerId; // The index number of the client

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.peerId = no;
		}

		@Override
		public void run() {
			try {

				// initialize Input and Output streams
				try (ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
						ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
						DataOutputStream d = new DataOutputStream(out)) {
					// Keep the connection open with the peer.
					while (true) {
						// receive the message sent from the client
						request = ((String) in.readObject()).trim();

						if (request.startsWith(Peer.Constants.getNextChunk)) {
							int size = pendingListOfFiles.keySet().size();
							int temp = sharedVariable.incrementAndGet();
							int next = temp % size;
							String resourceName = fileName + "." + String.format(formatSpecifier, next);
							String response = resourceName + Peer.Constants.seperator + pendingListOfFiles.get(resourceName);
							log.info("Sending getNextChunk : " + response + " next : " + next + " size : " + size);
							out.writeObject(response);
							out.flush();
						} else if (request.startsWith(Peer.Constants.getTotalChunks)) {
							out.writeObject(listOfChunks.size());
							out.flush();
						} else if (request.startsWith(fileName)) {
							String fileName = request;
							File f = new File(Peer.Constants.ServerDir + fileName);
							Files.copy(f.toPath(), d);
							d.flush();
							log.info("Sent " + fileName + " from server to peer " + peerId);
						}
					}
				}
			} catch (EOFException eof) {
				log.log(Level.SEVERE, "Peer disconnected - " + peerId);
			} catch (IOException ioException) {
				ioException.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				// Close connections
				try {
					connection.close();
				} catch (IOException ioException) {
					System.out.println("Disconnect with Client " + peerId);
				}
			}
		}
	}

	private static void initFile(int noOfChunks) {
		// Initialization code to read the files.
		try {
			File f = createFile(noOfChunks);
			listOfChunks = new ConcurrentHashMap<String, Long>();
			splitFile(f, noOfChunks);
		} catch (SecurityException | IOException e) {
			log.log(Level.SEVERE, e.getMessage());
		}

	}

	private static File createFile(int noOfChunks) throws SecurityException, IOException {
		File f = new File(Peer.Constants.ServerDir + fileName);
		if (!f.exists() || f.length() == 0L) {
			f.getParentFile().mkdirs();
			f.createNewFile();
			FileWriter fileWritter = new FileWriter(f, true);
			BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
			long timestamp = System.currentTimeMillis();
			long localVar = 0L;
			StringBuffer sb;
			while (f.length() < noOfChunks * 100 * 1024) {
				sb = new StringBuffer();
				for (int i = 0; i < 1000; i++) {
					sb.append(fileContent + localVar++ + "\n");
				}
				fileWritter.append(sb);
				bufferWritter.flush();
			}
			bufferWritter.close();
			log.info("Created File of size : " + f.length() + "bytes in " + (System.currentTimeMillis() - timestamp) + " milliseconds");
		}
		return f;
	}

	/*
	 * File name.txt will be split into name.txt.001, name.txt.002, name.txt.003
	 * so on up to name.txt.noOfChunks
	 */
	private static void splitFile(File f, int noOfChunks) throws FileNotFoundException, IOException {
		if (f.length() > 0L) {

			int partCtr = 0;
			byte[] buffer = new byte[sizeOfChunks];
			try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))) {
				int tmp = 0;
				while ((tmp = bis.read(buffer)) > 0) {
					File newFile = new File(f.getParent(), f.getName() + "." + String.format(formatSpecifier, partCtr++));
					try (FileOutputStream out = new FileOutputStream(newFile)) {
						out.write(buffer, 0, tmp);
						listOfChunks.put(newFile.getName(), newFile.length());
						log.info("Created File : " + newFile.getName() + " size : " + newFile.length() + "bytes");
					}
				}
			}
		}
	}
}
