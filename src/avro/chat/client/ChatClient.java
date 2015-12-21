package avro.chat.client;

import java.io.IOException;
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

public class ChatClient implements ChatClientServer {
	/** Fields **/
	String username;

	String serverIP;
	int serverPort;

	static String clientIP = "127.0.0.1";
	int clientPort;
	Server localServer;

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
	 * Prints out the incoming message.
	 * 
	 * @param message
	 *            Content of the request message.
	 * 
	 * @return boolean Binary client answer to the request
	 * 
	 * @throws AvroRemoteException
	 */
	@Override
	public boolean sendRequest(String message) throws AvroRemoteException {
		Scanner reader = new Scanner(System.in);
		System.out.println(
				"Enter and than type 'y' to accept or 'n' to decline.");
		if (reader.hasNext("y")) {
			reader.close();
			return true;
		} else {
			reader.close();
			return false;
		}
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
	public boolean sendVideoRequest(String message, String file)
			throws AvroRemoteException {
		System.out.println(message);

		Scanner reader = new Scanner(System.in);
		System.out.println(
				"Enter and than type 'y' to accept or 'n' to decline.");
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
	public boolean connectToClient(String privateName, String privateIP) throws AvroRemoteException {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(
					new InetSocketAddress(InetAddress.getByName(clientIP),
							clientPort));

			ChatClientServer clientProxy = (ChatClientServer) SpecificRequestor
					.getClient(ChatClientServer.class, transceiver);

			if (clientProxy.register(username, clientIP, clientPort)) {
				System.out.println(
						"You are successfully registered to the server.");
			} else {
				System.out.println(
						"Something went wrong when registering with the client.");
				System.exit(1);
			}

			//ShellFactory.createConsoleShell("client", "", new ClientUI(chatProxy, username)).commandLoop();

			clientProxy.exit(username);
			transceiver.close();
			return true;
		} catch (IOException e) {
			System.err.println(
					"ERROR: Unknown remote host. Double check client-ip and client-port.");
			return false;
		}
	}

	/** Methods **/
	/***
	 * Starts a server for the client, so the client can also receive commands
	 * from other servers and clients.
	 */
	public void startLocalServer() {
		try {
			localServer = new SaslSocketServer(
					new SpecificResponder(ChatClientServer.class,
							new ChatClient()),
					new InetSocketAddress(clientPort));
			System.out.println("Starting client's local server on " + clientIP
					+ ":" + clientPort);
		} catch (IOException e) {
			System.err.println(
					"ERROR: Starting local server for client. Double check local-ip and local-port.");
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
			System.out.println(
					"2 arguments => username, client port. Rest uses defaults.");
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
			System.err.println(
					"ERROR: Invalid argument[s]. Try `ant ChatClient help` to see your options.");
			System.exit(1);
		}

		if (serverPort == clientPort) {
			if (serverIP.equals(clientIP)) {
				System.err.println(
						"ERROR: Server and client's local server's addresses must be different.");
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
			Transceiver transceiver = new SaslSocketTransceiver(
					new InetSocketAddress(InetAddress.getByName(serverIP),
							serverPort));

			Chat chatProxy = (Chat) SpecificRequestor.getClient(Chat.class,
					transceiver);

			if (chatProxy.register(username, clientIP, clientPort)) {
				System.out.println(
						"You are successfully registered to the server.");
			} else {
				System.out.println(
						"Something went wrong when registering with the server."
								+ " Maybe you've already registered.");
				System.exit(1);
			}

			ShellFactory.createConsoleShell("client", "",
					new ClientUI(chatProxy, username)).commandLoop();

			chatProxy.exit(username);
			transceiver.close();
		} catch (IOException e) {
			System.err.println(
					"ERROR: Unknown remote host. Double check server-ip and server-port.");
			System.exit(1);
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

	@Override
	public boolean register(String username, String clientIP, int clientPort)
			throws AvroRemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String sendMessage(String username, String message)
			throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void exit(String username) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}
}
