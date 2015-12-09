package avro.chat.server;

import java.util.ArrayList;
import java.util.Hashtable;

public class ChatRoom {
	private ArrayList<CharSequence> clients = new ArrayList<CharSequence>();
	private ArrayList<Hashtable<CharSequence, CharSequence>> messages = new ArrayList<Hashtable<CharSequence, CharSequence>>();
	
	/***
	 * Connects the user to the public room if he is not connected yet.
	 * 
	 * @param username The nickname of the user.
	 * 
	 * @return boolean Whether the user was added to the room or not.
	 */
	public boolean join(CharSequence username) {
		if (!clients.contains(username)) {
			clients.add(username);
			return true;
		} else {
			return false;
		}
	}
	
	/***
	 * Check if the user is in the chatroom.
	 * 
	 * @param username The nickname of the user.
	 * 
	 * @return boolean Whether the room contains the user.
	 */
	public boolean contains(CharSequence username) {
		if (!clients.contains(username)) {
			return true;
		} else {
			return false;
		}
	}
	
	/***
	 * Disconnects the user from the public room.
	 * 
	 * @param username The nickname of the user.
	 */
	public void leave(CharSequence username) {
		clients.remove(username);
	}
	
	/***
	 * Logs a message of the user.
	 * 
	 * @param username The nickname of the user.
	 * @param message  The message of the user.
	 */
	public void sendMessage(CharSequence username, CharSequence message) {
		Hashtable<CharSequence, CharSequence> userMessage = new Hashtable<CharSequence, CharSequence>();
		
		userMessage.put(username, message);
		messages.add(userMessage);
		
		System.out.println(username + ": " + message);
	}
}
