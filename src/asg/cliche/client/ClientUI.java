package asg.cliche.client;

import asg.cliche.Command;
import asg.cliche.Param;
import avro.chat.proto.Chat;
import avro.chat.proto.ChatClientServer;
import java.util.List;
import org.apache.avro.AvroRemoteException;

public class ClientUI {
	private String username;
	private Chat chatProxy = null;
	private ChatClientServer chatClientProxy = null;
	// TODO set ChatClientServer proxies for both clients once private room is
	// initialized, remove it once someone disconnects / crashes

	public ClientUI(Chat proxy, String user) {
		chatProxy = proxy;
		username = user;
	}

	public void updateChatProxy(Chat proxy) {
		chatProxy = proxy;
	}

	@Command(description = "Prints list of users connected to the server.")
	public void getClientList() {
		try {
			List<String> clients = chatProxy.getClientList();

			System.out.println("server> " + "Retrieving connected client list:");
			for (String client : clients) {
				System.out.println("server> " + client);
			}
		} catch (AvroRemoteException e) {
			System.err.println("server> Failed to receive answer from the server.");
		}
	}

	@Command(description = "Terminates connection with the public/private room.")
	public void leave() {
		// TODO determine if the client is already in a private room to use
		// correct proxy to leave the room
		try {
			if (chatProxy.leave(username)) {
				System.out.println("server> You have left the Public chat room.");
			} else {
				System.err.println("server> You couldn't leave the Public chat room, maybe you never joined it.");
			}
		} catch (AvroRemoteException e) {
			System.err.println("server> Failed to receive answer from the server.");
		}
	}

	@Command(description = "Initiates connection specified room or user.")
	public void join(
			@Param(name = "room", description = "'Public' for the public chat room or the name of the receiver you want to start a private conversation with.") String room) {
		// TODO determine if the client is already in a private room to use
		// correct proxy to join the (public) chatroom and consequently leave
		// the old one
		try {
			String output = chatProxy.join(username, room);
			System.out.println(output);
		} catch (AvroRemoteException e) {
			System.err.println("server> Failed to receive answer from the server.");
		}
	}

	@Command(description = "Sends message to the connected room, if any.")
	public void sendMessage(
			@Param(name = "message", description = "The message you would like to send.") String message) {
		// TODO determine if the client is already in a private room to use
		// correct proxy to send a message
		try {
			String output = chatProxy.sendMessage(username, message);
			System.out.println(output);
		} catch (AvroRemoteException e) {
			System.err.println("server> Failed to receive answer from the server.");
		}
	}

	@Command(description = "Tries to initiate videostreaming with help of RSVP.")
	public void video(@Param(name = "file", description = "The filename of the video to be transmitted.") String file) {
		// TODO determine if the client is already in a private room before
		// requesting videostreaming
		try {
			boolean isInPrivateRoom = false;

			if (isInPrivateRoom) {
				String request = username + " would like to start videostreaming from his end.";

				boolean response = chatClientProxy.sendVideoRequest(request, file);

				if (response) {
					// TODO trigger rsvp.send_path handler
					String output = "server> The client has accepted your videostreaming request, sending RSVP PATH message.";
					System.out.println(output);
				} else {
					String output = "server> The client declined your videostreaming request.";
					System.out.println(output);
				}
			} else {
				String output = "server> You are currently not part of any private rooms.\n"
						+ "server> Use `join username` before initiating videostream.";
				System.err.println(output);
			}
		} catch (AvroRemoteException e) {
			System.err.println("server> Failed to receive answer from the server.");
		}
	}

	@Command(description = "Accept private chat requests")
	public void accept(
			@Param(name = "chatPartner", description = "The person whose request you would like to accept.") String chatPartner) {
		try {
			if (chatProxy.setupConnection(username, chatPartner)) {
				System.out.println("server> Connection set up, you can chat privately now.");
			} else {
				System.err.println("server> Something went wrong with settin up the connection.\n"
						+ "server> Are you sure you got a chat request from" + chatPartner + "?\n"
						+ "server> Try again by reaccepting with 'accept'");
			}
		} catch (AvroRemoteException e) {
			System.err.println("server> Failed to receive answer from the server.");
		}
	}
}