package p2p.peer;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import p2p.util.FileChunker;

public class Client {
	Socket requestSocket; // socket connect to the server
	ObjectOutputStream out; // stream write to the socket
	ObjectInputStream in; // stream read from the socket
	String request; // message send to the server

	void run() {
		try {
			// create a socket to connect to the server
			requestSocket = new Socket("localhost", 8000);
			System.out.println("Connected to localhost in port 8000");
			// initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());

			// get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(System.in));
			while (true) {
				System.out.print("Hello, please input a sentence: ");
				// read a sentence from the standard input
				request = bufferedReader.readLine();
				// Send the sentence to the server
				sendMessage(request);
				// Receive the upperCase sentence from the server

				try (OutputStream output = new FileOutputStream(
						FileChunker.fileName + "." + request + ".client")) {
					int bytesRead;
					byte[] buffer = new byte[1024];
					while ((bytesRead = in.read(buffer)) != -1) {
						output.write(buffer, 0, bytesRead);
					}
					output.flush();
				}
			}
		} catch (ConnectException e) {
			System.err
					.println("Connection refused. You need to initiate a server first.");
		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			// Close connections
			try {
				in.close();
				out.close();
				requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	// send a message to the output stream
	void sendMessage(String msg) {
		try {
			// stream write the message
			out.writeObject(msg);
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	// main method
	public static void main(String args[]) {
		Client client = new Client();
		client.run();
	}

}
