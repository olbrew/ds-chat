package avro.chat.server;

import java.util.ArrayList;
import java.util.Hashtable;

public class ChatRoom {
	private ArrayList<String> clients;
	private ArrayList<Hashtable<String, String>> messages;
	
	public boolean join(String username) {
		if(!clients.contains(username)) {
			clients.add(username);
			return true;
		} else {
			System.err.println("ERROR: You are already in the public room.");
			return false;
		}
	}
	
	public void leave(String username) {
		clients.remove(username);
	}
	
	public void sendMessage(String username, String message) {
		Hashtable<String, String> userMessage = new Hashtable<String, String>();
		userMessage.put(username, message);
		messages.add(userMessage);
	}
}
