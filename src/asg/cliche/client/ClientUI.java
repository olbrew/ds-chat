package asg.cliche.client;

import asg.cliche.Command;
import asg.cliche.Param;
import avro.chat.proto.Chat;

import java.util.List;

import org.apache.avro.AvroRemoteException;

public class ClientUI {
	private String username;
	private Chat chatProxy = null;

	public ClientUI(Chat proxy, String user) {
		chatProxy = proxy;
		username = user;
	}

	@Command(description = "Prints list of users connected to the server.")
	public void getClientList() throws AvroRemoteException {
		List<String> clients = chatProxy.getClientList();

		System.out.println("Retrieving connected client list:");
		for (String client : clients) {
			System.out.println(client);
		}
	}

	@Command(description = "Initiates connection with the specified room.")
	public void join(
			@Param(name = "room", description = "'Public' for the public chat room or the name of the receiver you want to start a private conversation with.") String room)
					throws AvroRemoteException {
		String output = chatProxy.join(username, room);
		System.out.println(output);
	}

	@Command(description = "Terminates connection with the public/private room.")
	public void leave() throws AvroRemoteException {
		if (chatProxy.leave(username)) {
			System.out.println("You have left the Public chat room.");
		} else {
			System.err.println("You couldn't leave the Public chat room, maybe you never joined it.");
		}
	}

	@Command(description = "Sends message to the connected room, if any.")
	public void sendMessage(
			@Param(name = "message", description = "The message you would like to send.") String message)
					throws AvroRemoteException {
		String output = chatProxy.sendMessage(username, message);
		System.out.println(output);
	}

}