package avro.chat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import avro.chat.proto.Chat;
import avro.chat.proto.ChatClientServer;

public class ChatServer implements Chat {
	private ChatRoom publicRoom = new ChatRoom();
	private Hashtable<String, ChatClientServer> clients = new Hashtable<String, ChatClientServer>();
	
	@Override
	/***
	 * Registers client's username to its local server proxy so we can communicate both ways.
	 * 
	 * @param username   The nickname of the client.
	 * @param clientIP   The IP address of the client.
	 * @param clientPort The port to which client's local server is bound to.
	 * 
	 * @return boolean Whether the client was successfully registered on the server. 
	 */
	public boolean register(String username, String clientIP, int clientPort) throws AvroRemoteException {
		if (clients.get(username) == null) {
			try {
				Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(clientIP, clientPort));
				System.out.println("transceiver.getRemoteName(): " + transceiver.getRemoteName());
				//TODO perhaps it's better to map client's username to Transceiver so we will be able to retrieve its remote address
				ChatClientServer proxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class, transceiver);
				clients.put(username, proxy);
				System.out.println("Registered client with username: " + username);
				
				return true;
			} catch (IOException e1) {
				System.err.println("Error: Couldn't connect back to the client.");
				e1.printStackTrace();
			}
		} else {
			System.err.println(username + " is already registered with the server.");
			return true;
		}
		
		return false;
	}
	
	@Override
	/***
	 * Gets the list of all client usernames that are connected to the server.
	 * 
	 * @return List<String> The list of usernames.
	 */
	public List<String> getClientList() throws AvroRemoteException {
		List<String> clientList = new ArrayList<String>();
		
		for (String client : clients.keySet()) {
			clientList.add(client);
		}
		
		return clientList;
	}
	
	@Override
	/***
	 * Allows a client to join a specific room.
	 * 
	 * @param username The nickname of the client.
	 * @param roomName The name of the room, either a public chat room or a private room.
	 * 
	 * @return boolean Whether or not the client has successfully joined the room.
	 */
	public boolean join(String username, String roomName) throws AvroRemoteException {
		if (roomName.equals("Public")) {
			//ChatClientServer proxy = clients.get(username);
			
			if (publicRoom.join(username)) {
				System.out.println(username + " has successfully joined the Public chat room.");
				return true;
			} else {
				System.err.println("ERROR: " + username + " is already in the public room.");
				return false;
			}
		}

		//TODO: join private room
		return false;
	}

	@Override
	/***
	 * Allows a client to leave the room.
	 * 
	 * @param username The nickname of the client.
	 */
	public Void leave(String username) throws AvroRemoteException {
		//TODO: determine whether the client is in the public or private room for correct recipients.
		
		publicRoom.leave(username);
		System.out.println(username + " has successfully left the Public chat room.");
		return null;
	}

	@Override
	/***
	 * Allows a client to send a message to the public room.
	 * 
	 * @param username The nickname of the client.
	 * @param message  The message to be delivered.
	 */
	public Void sendMessage(String username, String message) throws AvroRemoteException {
		//TODO check if username exists or retrieve the username from other means such as the transceiver
		
		publicRoom.sendMessage(username, message);
		return null;
	}
	
	
	public static void main(String[] args) {
		Server server = null;
		ChatServer cs = new ChatServer();
		
		int serverPort = 10010;
		
		if (args.length == 1) {
			serverPort = Integer.parseInt(args[0]);
		} else if (args.length > 1) {
			System.err.println("ERROR: Max. 1 arguments ([server port]) expected.");
		}
		
		try {
			server = new SaslSocketServer(new SpecificResponder(Chat.class, cs), new InetSocketAddress(serverPort));
		
			server.start();
			server.join();
		} catch(IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);		
		} catch(InterruptedException e) {}
	}
}
