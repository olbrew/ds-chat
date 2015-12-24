package avro.chat.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import asg.cliche.ShellFactory;
import asg.cliche.client.ClientUI;
import avro.chat.proto.Chat;
import avro.chat.proto.ChatClientServer;

public class ChatClient implements ChatClientServer, Runnable {
	/** Fields **/
	String username;

	String serverIP;
	int serverPort;
	Transceiver chatTransceiver;
	Chat chatProxy;
	InetSocketAddress serverSocket;

	static String clientIP = "127.0.0.1";
	int clientPort;
	Server localServer;

	Transceiver privateTransceiver = null;
	ChatClientServer privateProxy = null;

	/** Proxy methods **/
	/***
	 * Simple method to test if the client is still alive.
	 *
	 * @return null Returns null if it it can answer and is thus still alive.
	 *
	 * @throws AvroRemoteException
	 */
	@Override
	public Void isAlive() throws AvroRemoteException {
		return null;
	}

	/***
	 * Simple method to test if the client is still alive.
	 *
	 * @return null Returns null if it it can answer and is thus still alive.
	 */
	@Override
	public boolean inPrivateRoom() {
		if (privateProxy != null) {
			try {
				privateProxy.isAlive();
				return true;
			} catch (AvroRemoteException e) {
				privateProxy = null;
				return false;
			}
		}
		return false;
	}

	/***
	 * Prints out the incoming message.
	 *
	 * @param message
	 *            Content of the incoming message.
	 *
	 * @throws AvroRemoteException
	 */
	@Override
	public Void incomingMessage(String message) throws AvroRemoteException {
		System.out.println(message);
		return null;
	}

	/***
	 * Allows a client to send a message to private room.
	 * 
	 * @param message
	 *            The message to be delivered.
	 *
	 * @throws AvroRemoteException
	 */
	@Override
	public Void sendMessage(String senderName, String message) throws AvroRemoteException {
		System.out.println(senderName + "> (Private): " + message);
		privateProxy.incomingMessage(senderName + "> (Private): " + message);

		return null;
	}

	/***
	 * Allows videostreaming to be intitiated between two clients.
	 *
	 * @param message
	 *            Content of the request message.
	 * @param file
	 *            The video file to be transmitted.
	 *
	 * @return boolean Binary client answer to the request
	 *
	 * @throws AvroRemoteException
	 */
	@Override
	public boolean sendVideoRequest(String file) throws AvroRemoteException {
		Scanner reader = new Scanner(System.in);
		System.out.println("Enter and then type 'y' to accept or 'n' to decline.");
		if (reader.hasNext("y")) {
			reader.close();
			return true;
		} else {
			reader.close();
			return false;
		}
	}

	/***
	 * Registers with another client.
	 *
	 * @param clientIP
	 *            IP address of the client to connect to.
	 *
	 * @return boolean Whether or not the connection was successfully made.
	 *
	 * @throws AvroRemoteException
	 */
	@Override
	public boolean register(String username, String privateAddress) throws AvroRemoteException {
		String privateIP = ((privateAddress.split(":"))[0]).substring(1);
		int privatePort = Integer.parseInt((privateAddress.split(":"))[1]);

		try {
			privateTransceiver = new SaslSocketTransceiver(
					new InetSocketAddress(InetAddress.getByName(privateIP), privatePort));

			privateProxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class, privateTransceiver);

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public Void leave(boolean closeOtherProxy) throws AvroRemoteException {
		if (closeOtherProxy) {
			privateProxy.leave(false);
			privateProxy = null;
		}
		try {
			privateTransceiver.close();
			System.out.println("client> You have left the private chat.\n"
					+ "client> You can 'join' a new one now if you want.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Methods **/
	/***
	 * Starts a server for the client, so the client can also receive commands
	 * from other servers and clients.
	 */
	public void startLocalServer() {
		try {
			localServer = new SaslSocketServer(new SpecificResponder(ChatClientServer.class, new ChatClient()),
					new InetSocketAddress(clientPort));
			System.out.println("Starting client's local server on " + clientIP + ":" + clientPort);
		} catch (IOException e) {
			System.err.println("ERROR: Starting local server for client. Double check local-ip and local-port.");
			System.exit(1);
		}
		localServer.start();
	}

	/***
	 * Reads in the command line arguments to configure the client, or uses
	 * defaults if none are given.
	 *
	 * @param args
	 *            The given command line arguments.
	 */
	public void configure(String[] args) {
		// default values, override with command-line arguments

		// parse command line arguments
		if (args.length == 0) {
			username = "Bob";
			serverIP = "127.0.0.1";
			serverPort = 10010;
			clientPort = 11000;
		} else if (args.length == 1) {
			System.out.println("Usage: ant ChatClient [args]");
			System.out.println("----------------------------");
			System.out.println(
					"0 arguments use our defaults: username = Bob, server ip-address = 127.0.0.1, server port = 10010, client ip-address = 127.0.0.1, client port = 11000");
			System.out.println("2 arguments => username, client port. Rest uses defaults.");
			System.out.println(
					"4 arguments => username, server ip-address, server port, client port. Rest uses defaults.");
			System.out.println(
					"5 arguments => username, server ip-address, server port, client ip-address, client port.");
			System.exit(0);
		} else if (args.length == 2) {
			username = args[0];
			serverIP = "127.0.0.1";
			serverPort = 10010;
			clientPort = Integer.parseInt(args[1]);
		} else if (args.length == 4) {
			username = args[0];
			serverIP = args[1];
			serverPort = Integer.parseInt(args[2]);
			clientPort = Integer.parseInt(args[3]);
		} else if (args.length == 5) {
			username = args[0];
			serverIP = args[1];
			serverPort = Integer.parseInt(args[2]);
			clientIP = args[3];
			clientPort = Integer.parseInt(args[4]);
		} else {
			System.err.println("ERROR: Invalid argument[s]. Try `ant ChatClient help` to see your options.");
			System.exit(1);
		}

		if (serverPort == clientPort) {
			if (serverIP.equals(clientIP)) {
				System.err.println("ERROR: Server and client's local server's addresses must be different.");
				System.exit(1);
			}
		}
	}

	/***
	 * Registers with the server, start the Cliche CLI and keep the connection
	 * open until the client exits or the server is down for more than 60s.
	 */
	public void connectToServer() {
		try {
			serverSocket = new InetSocketAddress(InetAddress.getByName(serverIP), serverPort);
			chatTransceiver = new SaslSocketTransceiver(serverSocket);

			chatProxy = (Chat) SpecificRequestor.getClient(Chat.class, chatTransceiver);

			if (chatProxy.register(username, clientIP, clientPort)) {
				System.out.println("You are successfully registered to the server.");
			} else {
				System.out.println(
						"Something went wrong when registering with the server." + " Maybe you've already registered.");
				System.exit(1);
			}

			Thread t = new Thread(this);
			t.start();

			Transceiver clientTransceiver = new SaslSocketTransceiver(
					new InetSocketAddress(InetAddress.getByName(clientIP), clientPort));
			ChatClientServer clientProxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class,
					clientTransceiver);

			ShellFactory.createConsoleShell("client", "", new ClientUI(username, clientProxy, chatProxy)).commandLoop();

			t.interrupt();
			chatProxy.leave(username);
			chatTransceiver.close();
		} catch (IOException e) {
			System.err.println("client> Something went wrong when communicating with the server.");
			System.exit(1);
		}
	}

	/***
	 * Tries to ping the server up to n times in increasing intervals (multiples
	 * of 5). Gives up after 75 seconds.
	 *
	 * @param n
	 *            Number of recent failed attempts to reconnect to server.
	 */
	private void reconnect(int n) {
		try {
			if (n == 6) {
				System.err.println("client> Failed to reconnect to server after " + n + " attempts.\n"
						+ "client> Closing transceiver, you may try to connect to the server manually later.");

				chatTransceiver.close();
				return;
			} else {
				System.err.println("client> Cannot access the server, trying to reconnect in " + n * 5 + " seconds.");

				Thread.sleep(n * 5000); // milliseconds
				chatTransceiver.close();
				chatTransceiver = new SaslSocketTransceiver(serverSocket);

				chatProxy = (Chat) SpecificRequestor.getClient(Chat.class, chatTransceiver);

				chatProxy.isAlive();
				chatProxy.register(username, clientIP, clientPort);
				System.out.println("Server is accessible again.");
				return;
			}
		} catch (ConnectException e) {
			reconnect(++n); // server is still offline
		} catch (AvroRemoteException e) {
			reconnect(++n); // server is still offline
		} catch (InterruptedException e) {
			e.printStackTrace(); // thread interrupted
		} catch (IOException e) {
			e.printStackTrace(); // transceiver close
		}
	}

	private void checkServer() throws InterruptedException {
		try {
			chatProxy.isAlive();
			Thread.sleep(5000); // milliseconds
		} catch (AvroRemoteException e) {
			reconnect(1);
		}
	}

	private void checkPrivateUser() throws InterruptedException {
		try {
			if (inPrivateRoom()) {
				privateProxy.isAlive();
				Thread.sleep(5000); // milliseconds
			}
		} catch (AvroRemoteException e) {
			try {
				leave(false);
				String output = "client> The other user from this private room has gone offline.\n"
						+ "client> You have been automatically disconnected.";
				System.err.println(output);
			} catch (AvroRemoteException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				checkServer();
				checkPrivateUser();
			}
		}  catch (InterruptedException e) {
			// This thread was interrupted, it needs to stop doing what it was
			// trying to do
		} 
	}

	public static void main(String[] args) {
		try {
			ChatClient chatClient = new ChatClient();

			chatClient.configure(args);
			chatClient.startLocalServer();
			chatClient.connectToServer();
			chatClient.localServer.close();
		} catch (Exception e) {
			// Client was abruptly terminated (for instance by pressing CTRL + D
			// in terminal after cliche interface was constructed).
			System.exit(1);
		}
	}
}
