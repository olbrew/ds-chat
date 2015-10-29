package avro.chat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Hashtable;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import avro.chat.client.ChatClient;
import avro.chat.proto.Chat;
import avro.chat.proto.ChatClientServer;
import util.Address;

public class ChatServer implements Chat {
	private ChatRoom publicRoom;
	private Hashtable<String, Address> clients = new Hashtable<String, Address>();
	
	@Override
	public String register(String username, String clientIP, int clientPort) throws AvroRemoteException {
		if(clients.get(username) == null) {
			Address address = new Address(clientIP, clientPort);
			clients.put(username, address);
			System.out.println("Registered client with username: " + username);
			
			return "You're successfully registered with username: " + username;
		}
		return null;
	}
	
	@Override
	public boolean join(String roomName) {
		
		// get client
		String username = "";
				
				
		publicRoom.join(username);
		return true;
	}

	@Override
	public Void leave() throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void sendMessage(String username, String message) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args) {
		Server server = null;
		ChatServer cs = new ChatServer();
		
		try {
			server = new SaslSocketServer(new SpecificResponder(Chat.class, cs), new InetSocketAddress(10000));
		} catch(IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		server.start();
		
		/*
		Address clientAddress = cs.clients.get("Bob");
		String clientIP = clientAddress.getIp();
		
		Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(clientIP, clientAddress.getPort()));
		ChatClientServer proxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class, transceiver);
		CharSequence response = proxy.test("Bob");
		*/
		try {
			server.join();
		} catch(InterruptedException e) {}
	}
}
