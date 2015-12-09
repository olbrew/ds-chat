package avro.chat.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import asg.cliche.Command;
import asg.cliche.ShellFactory;
import asg.cliche.client.ClientUI;
import avro.chat.proto.Chat;
import avro.chat.server.ChatServer;

public class ChatClient {
	/** Fields **/
	Server localServer;
	static String clientIP;
	int clientPort = 11000;

	/** Methods **/
	public void startLocalServer() {
		try {
			System.out.println("Starting client's local server on port: " + clientPort);
			localServer = new SaslSocketServer(new SpecificResponder(Chat.class, new ChatServer()), new InetSocketAddress(clientPort));
		} catch(IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		localServer.start();
	}
	
	public static void main(String[] args) {
		//TODO provide client's ip address as argument so the server will be able to pass correct addresses for private rooms
		String username = "Bob";
		String serverIP = "localhost";
		int serverPort = 10010;
		
		ChatClient chatClient = new ChatClient();
		
		if (args.length == 3) {
			username = args[0];
			serverPort = Integer.parseInt(args[1]);
			chatClient.clientPort = Integer.parseInt(args[2]);
		} else if (args.length == 4) {
			username = args[0];
			serverIP = args[1];
			serverPort = Integer.parseInt(args[2]);
			chatClient.clientPort = Integer.parseInt(args[3]);
		} else if (args.length < 3 || args.length > 4) {
			System.err.println("ERROR: Min. 2 and max. 3 arguments (username, [server ip-address,] server port, client port) expected.");
		}
		
		chatClient.startLocalServer();
		try {
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(InetAddress.getByName(serverIP), serverPort));
			
			Chat chatProxy = (Chat) SpecificRequestor.getClient(Chat.class, transceiver);
			
			clientIP = InetAddress.getLocalHost().getHostAddress();
			System.out.println("Client's ip: " + clientIP);
			
			boolean response = chatProxy.register(username, clientIP, chatClient.clientPort);
			System.out.println("The client has successfully registered to the server: " + response);

			ShellFactory.createConsoleShell("client", "", new ClientUI(chatProxy, username)).commandLoop();

			chatClient.localServer.join();
			transceiver.close();
		} catch(InterruptedException e) {
			
		} catch(IOException e) {
			System.err.println("Error connecting to server ...");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
