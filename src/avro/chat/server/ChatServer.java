package avro.chat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
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

public class ChatServer implements Chat {
	private ChatRoom publicRoom = new ChatRoom();
	private Hashtable<String, Transceiver> clients = new Hashtable<String, Transceiver>();
	private Hashtable<String, ChatClientServer> clientsServer = new Hashtable<String, ChatClientServer>();

	@Override
	/***
	 * Registers client's username to its local server proxy so we can
	 * communicate both ways.
	 * 
	 * @param username
	 *            The nickname of the client.
	 * @param clientIP
	 *            The IP address of the client.
	 * @param clientServerPort
	 *            The port to which client's local server is bound to.
	 * 
	 * @return boolean Whether the client was successfully registered on the
	 *         server.
	 */
	public boolean register(String username, String clientIP, int clientServerPort) throws AvroRemoteException {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(
					new InetSocketAddress(InetAddress.getByName(clientIP), clientServerPort));
			ChatClientServer proxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class,
					transceiver);

			if (!clients.containsKey(username)) {
				clients.put(username, transceiver);
				clientsServer.put(username, proxy);
				System.out.println("Registered client with username: " + username);
				return true;
			} else {
				System.err.println(username + " is already registered with the server.");
				return false;
			}
		} catch (IOException e1) {
			System.err.println("Error: Couldn't connect back to the client.");
			e1.printStackTrace();
			return false;
		}
	}

	@Override
	/***
	 * Gets the list of all client usernames that are connected to the server.
	 * 
	 * @return List<String> The list of usernames.
	 */
	public ArrayList<String> getClientList() throws AvroRemoteException {
		ArrayList<String> clientList = new ArrayList<String>();
		clientList.addAll(clients.keySet());
		return clientList;
	}

	@Override
	/***
	 * Allows a client to join a specific room.
	 * 
	 * @param username
	 *            The nickname of the client.
	 * @param roomName
	 *            The name of the room, either a public chat room or a private
	 *            room.
	 * 
	 * @return boolean Whether or not the client has successfully joined the
	 *         room.
	 */
	public boolean join(String username, String roomName) throws AvroRemoteException {
		if (roomName.equals("Public")) {
			if (publicRoom.join(username)) {
				System.out.println(username + " has successfully joined the Public chat room.");
				return true;
			} else {
				System.err.println("ERROR: " + username + " is already in the public room.");
				return false;
			}
		}
		// TODO: join private room
		return false;
	}

	@Override
	/***
	 * Allows a client to leave the room.
	 * 
	 * @param username
	 *            The nickname of the client.
	 */
	public boolean leave(String userName) throws AvroRemoteException {
		// TODO: determine whether the client is in the public or private room
		// for correct recipients.

		publicRoom.leave(userName);
		if (!publicRoom.contains(userName)) {
			System.out.println(userName + " has left the Public chat room.");
			return true;
		} else {
			System.err.println(userName + " couldn't leave the Public chat room.");
			return false;
		}
	}

	@Override
	/***
	 * Allows a client to exit the server.
	 * 
	 * @param username
	 *            The nickname of the client.
	 */
	public Void exit(String userName) throws AvroRemoteException {
		leave(userName);
		clients.remove(userName);
		clientsServer.remove(userName);
		System.out.println(userName + " has exited the server.");
		return null;
	}

	@Override
	/***
	 * Allows a client to send a message to the public room.
	 * 
	 * @param username
	 *            The nickname of the client.
	 * @param message
	 *            The message to be delivered.
	 */
	public String sendMessage(String username, String message) throws AvroRemoteException {
		if (publicRoom.contains(username)) {
			String error = "You have not joined the public chatroom yet. " + "To join type: `join 'Public'`";
			return error;
		} else {
			publicRoom.sendMessage(username, message);
			return message;
		}
	}

	public void checkUsers() throws IOException {
		for (String client : clientsServer.keySet()) {
			if (!(clientsServer.get(client)).isAlive()) {
				exit(client);
			}
		}
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

			long lastCall = 0;
			while (true) {
				if (System.currentTimeMillis() - lastCall > 1000) {
					lastCall = System.currentTimeMillis();
					cs.checkUsers();
				}
			}
			// server.join();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
