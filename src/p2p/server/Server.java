package p2p.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import p2p.util.FileChunker;

public class Server {

	// The server will be listening on this port number
	private static final int sPort = 8000;
	static FileChunker chunker = null;
	static final Logger logger = Logger.getLogger(Server.class.getName());

	public static void main(String[] args) throws Exception {

		int noOfChunks = 5;

		if (args != null) {
			switch (args.length) {
			case 1:
				// Number of file chunks to be created.
				noOfChunks = Integer.parseInt(args[0]);
				break;
			default:
				noOfChunks = 10;
				logger.log(Level.INFO,
						"Not yet defined. Too many/ too few parameters.");
			}
		}

		logger.log(Level.INFO, "The server is starting.");
		chunker = FileChunker.getInstance(noOfChunks);
		ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;
		try {
			while (true) {
				new Handler(listener.accept(), clientNum).start();
				logger.log(Level.INFO, "Client " + clientNum + " is connected!");
				clientNum++;
			}
		} finally {
			listener.close();
		}

	}

	/**
	 * A handler thread class. Handlers are spawned from the listening loop and
	 * are responsible for dealing with a single client's requests.
	 */
	private static class Handler extends Thread {
		private String request; // message received from the client
		private String MESSAGE; // uppercase message send to the client
		private Socket connection;
		private ObjectInputStream in; // stream read from the socket
		private ObjectOutputStream out; // stream write to the socket
		private int no; // The index number of the client

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
		}

		public void run() {
			try {

				// initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				try {
					while (true) {
						// receive the message sent from the client
						request = (String) in.readObject();
						logger.log(Level.INFO, "Request : " + request
								+ " from connection " + connection.toString());
						sendFile(request);
					}
				} catch (ClassNotFoundException classnot) {
					System.err.println("Data received in unknown format");
				}
			} catch (IOException ioException) {
				System.out.println("Disconnect with Client " + no);
			} finally {
				// Close connections
				try {
					in.close();
					out.close();
					connection.close();
				} catch (IOException ioException) {
					System.out.println("Disconnect with Client " + no);
				}
			}
		}

		// send a message to the output stream
		public void sendFile(String msg) {
		
			String fileName = FileChunker.fileName + "." + msg;
			try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(fileName)))) {
				byte[] bytes = new byte[1024];
				int count;
				while ((count = bis.read(bytes)) > 0) {
					out.write(bytes, 0, bytes.length);
				}
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

}
