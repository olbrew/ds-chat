package asg.cliche.client;

import asg.cliche.Command;
import asg.cliche.Param;
import avro.chat.proto.Chat;
import avro.chat.proto.ChatClientServer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.CallFuture;

public class ClientUI {
	private String username;
	private Chat chatProxy = null;
	private Chat.Callback chatCallbackProxy = null;
	private ChatClientServer chatClientProxy = null;
	// TODO set ChatClientServer proxies for both clients once private room is
	// initialized, remove it once someone disconnects / crashes

	public ClientUI(Chat proxy, Chat.Callback callbackProxy, String user) {
		chatProxy = proxy;
		chatCallbackProxy = callbackProxy;
		username = user;
	}

	public void updateChatProxy(Chat proxy) {
		chatProxy = proxy;
	}
	
	@Command(description = "Prints list of users connected to the server.")
	public void getClientList() {
		try {
			List<String> clients = chatProxy.getClientList();

			System.out.println("Retrieving connected client list:");
			for (String client : clients) {
				System.out.println(client);
			}
		} catch (AvroRemoteException e) {
			System.out.println("Failed to receive answer from the server.");
		}
	}

	@Command(description = "Initiates connection specified room or user.")
	public void join(
			@Param(name = "room", description = "'Public' for the public chat room or the name of the receiver you want to start a private conversation with.") String room) {
		// TODO determine if the client is already in a private room to use
		// correct proxy to join the (public) chatroom and consequently leave
		// the old one

		try {
			CallFuture<String> future = new CallFuture<String>();
			chatCallbackProxy.join(username, room, future);
			System.out.println(future.get());
		} catch (AvroRemoteException e) {
			System.out.println("Failed to receive answer from the server.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Command(description = "Terminates connection with the public/private room.")
	public void leave() {
		// TODO determine if the client is already in a private room to use
		// correct proxy to leave the room
		try {
			if (chatProxy.leave(username)) {
				System.out.println("You have left the Public chat room.");
			} else {
				System.err.println("You couldn't leave the Public chat room, maybe you never joined it.");
			}
		} catch (AvroRemoteException e) {
			System.out.println("Failed to receive answer from the server.");
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
			System.out.println("Failed to receive answer from the server.");
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
					String output = "The client has accepted your videostreaming request, sending RSVP PATH message.";
					System.out.println(output);
				} else {
					String output = "The client declined your videostreaming request.";
					System.out.println(output);
				}
			} else {
				String output = "You are currently not part of any private rooms."
						+ " Use `join username` before initiating videostream.";
				System.err.println(output);
			}
		} catch (AvroRemoteException e) {
			System.out.println("Failed to receive answer from the server.");
		}
	}

	@Command(description = "Accept requests")
	public boolean accept() {
		return true;
	}
}