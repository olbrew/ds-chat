package avro.chat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Hashtable;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import avro.chat.proto.Chat;
import avro.chat.proto.ChatClientServer;
import util.Address;

public class ChatServer implements Chat {
	private ChatRoom publicRoom = new ChatRoom();
	private Hashtable<String, ChatClientServer> clients = new Hashtable<String, ChatClientServer>();
	
	@Override
	public String register(String username, String clientIP, int clientPort) throws AvroRemoteException {
		if (clients.get(username) == null) {
			//Address address = new Address(clientIP, clientPort);
			//clients.put(username, address);
			
			Transceiver transceiver;
			try {
				transceiver = new SaslSocketTransceiver(new InetSocketAddress(clientIP, clientPort));
				ChatClientServer proxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class, transceiver);
				
				clients.put(username, proxy);
				System.out.println("Registered client with username: " + username);
				
				return "You're successfully registered with username: " + username;
			} catch (IOException e1) {
				System.err.println("Error: Couldn't connect back to the client.");
				e1.printStackTrace();
			}
		}
		
		return null;
	}
	
	@Override
	public boolean join(String roomName) throws AvroRemoteException {
		if(roomName.equals("Public")) {
			CharSequence username = "Bob";
			//ChatClientServer proxy = clients.get(username);
			
			publicRoom.join(username);
			
			System.out.println("You have successfully joined the Public chat room.");
		}

		//TODO: join private room
		return true;
	}

	@Override
	public Void leave() throws AvroRemoteException {
		publicRoom.leave("Bob");
		System.out.println("You have successfully left the Public chat room.");
		return null;
	}

	@Override
	public Void sendMessage(String username, String message) throws AvroRemoteException {
		publicRoom.sendMessage(username, message);
		return null;
	}
	
	public static void main(String[] args) {
		Server server = null;
		ChatServer cs = new ChatServer();
		
		try {
			server = new SaslSocketServer(new SpecificResponder(Chat.class, cs), new InetSocketAddress(10000));
		
			server.start();
			server.join();
		} catch(IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);		
		} catch(InterruptedException e) {}
	}
}
