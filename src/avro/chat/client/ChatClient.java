package avro.chat.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

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
	Server localServer;
	static String clientIP = "127.0.0.1";
	int clientPort = 11000;

	String username;
	String serverIP;
	int serverPort;

	/** Proxy methods **/
	@Override
	public boolean isAlive() throws AvroRemoteException {
		return true;
	}
	
	@Override
	public Void incomingMessage(String message) throws AvroRemoteException {
		System.out.println(message);
		return null;
	}
	
	/** Methods **/
	public void startLocalServer() {
		try {
			localServer = new SaslSocketServer(new SpecificResponder(ChatClientServer.class, new ChatClient()),
					new InetSocketAddress(clientPort));
			System.out.println("Starting client's local server on port: " + clientPort);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		localServer.start();
	}

	public void configure(String[] args) {
		// default values, override with command-line arguments
		username = "Bob";
		serverIP = "127.0.0.1";
		serverPort = 10010;

		// parse command line arguments
		if (args.length == 4) {
			username = args[0];
			serverIP = args[1];
			serverPort = Integer.parseInt(args[2]);
			this.clientPort = Integer.parseInt(args[3]);
		} else if (args.length == 5) {
			username = args[0];
			serverIP = args[1];
			serverPort = Integer.parseInt(args[2]);
			clientIP = args[3];
			this.clientPort = Integer.parseInt(args[4]);
		} else if (args.length < 4 || args.length > 5) {
			System.err.println(
					"ERROR: Min. 4 and max. 5 arguments (username, server ip-address, server port, [client ip-address,] client port) expected.");
		}
	}

	public void connectToServer() {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(
					new InetSocketAddress(InetAddress.getByName(serverIP), serverPort));
			Chat chatProxy = (Chat) SpecificRequestor.getClient(Chat.class, transceiver);
			System.out.println("Client's ip: " + clientIP);

			if (chatProxy.register(username, clientIP, clientPort)) {
				System.out.println("You are successfully registered to the server.");
			} else {
				System.out.println(
						"Something went wrong when registering with the server." + "Maybe you've already registered.");
			}

			ShellFactory.createConsoleShell("client", "", new ClientUI(chatProxy, username)).commandLoop();

			chatProxy.exit(username);
			transceiver.close();
		} catch (IOException e) {
			System.err.println("Error connecting to server ...");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		ChatClient chatClient = new ChatClient();

		chatClient.configure(args);
		chatClient.startLocalServer();
		chatClient.connectToServer();
	}
}
