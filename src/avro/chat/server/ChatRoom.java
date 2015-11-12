package avro.chat.server;

import java.util.ArrayList;
import java.util.Hashtable;

public class ChatRoom {
	private ArrayList<CharSequence> clients = new ArrayList<CharSequence>();
	private ArrayList<Hashtable<CharSequence, CharSequence>> messages = new ArrayList<Hashtable<CharSequence, CharSequence>>();
	
	public boolean join(CharSequence username) {
		if(!clients.contains(username)) {
			clients.add(username);
			return true;
		} else {
			System.err.println("ERROR: You are already in the public room.");
			return false;
		}
	}
	
	public void leave(CharSequence username) {
		clients.remove(username);
	}
	
	public void sendMessage(CharSequence username, CharSequence message) {
		Hashtable<CharSequence, CharSequence> userMessage = new Hashtable<CharSequence, CharSequence>();
		userMessage.put(username, message);
		messages.add(userMessage);
		
		System.out.println(username + ": " + message);
	}
}
