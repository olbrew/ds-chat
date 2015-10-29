package avro.chat.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

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

		ChatClient cc = new ChatClient();
		cc.startLocalServer();
		
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(serverIP, port));
			
			Chat proxy = (Chat) SpecificRequestor.getClient(Chat.class, client);
			
			clientIP = InetAddress.getLocalHost().getHostAddress();
			System.out.println(clientIP);
			
			String response = proxy.register("Bob", clientIP, cc.clientPort);
			System.out.println(response);
			
			cc.localServer.join();
			client.close();
		} catch(InterruptedException e) {
			
		} catch(IOException e) {
			System.err.println("Error connecting to server ...");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
