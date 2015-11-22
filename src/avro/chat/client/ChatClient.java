package avro.chat.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import avro.chat.proto.Chat;
import avro.chat.server.ChatServer;

public class ChatClient {
	/** Fields **/
	static Integer ports = 0;
	Server localServer;
	static String clientIP;
	int clientPort = 11000;

	/** Methods **/
	public void startLocalServer() {
		try {
			clientPort += ports;
			System.out.println("Starting client's local server on port: " + clientPort);
			localServer = new SaslSocketServer(new SpecificResponder(Chat.class, new ChatServer()), new InetSocketAddress(clientPort));
			ports++;
		} catch(IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		localServer.start();
	}
	
	public static void main(String[] args) {
		String serverIP = "localhost";
		int port = 10000;

		if (args.length == 1) {
			serverIP = "localhost";
			port = Integer.parseInt(args[0]);
		} else if (args.length == 2) {
			serverIP = args[0];
			port = Integer.parseInt(args[1]);
		} else if (args.length > 2) {
			System.err.println("ERROR: Max. 2 arguments ([ip-address,] port) exepected.");
		}

		ChatClient chatClient = new ChatClient();
		chatClient.startLocalServer();
		
		try {
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(InetAddress.getByName(serverIP), port));
			
			Chat chatProxy = (Chat) SpecificRequestor.getClient(Chat.class, transceiver);
			
			clientIP = InetAddress.getLocalHost().getHostAddress();
			System.out.println("Client's ip: " + clientIP);
			
			boolean response = chatProxy.register("Bob", clientIP, chatClient.clientPort);
			System.out.println("The client has successfully registered to the server: " + response);

			List<String> clients = chatProxy.getClientList();
			System.out.println("Retrieving connected client list:");
			for (String client : clients) {
				System.out.println(client);
			}
			
			chatProxy.join("Bob", "Public");
			chatProxy.sendMessage("Bob", "Hello World!");
			chatProxy.leave("Bob");
			
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
