package avro.chat.server;

import java.util.ArrayList;
import java.util.Hashtable;

public class ChatRoom {
    private ArrayList<String> clients = new ArrayList<String>();
    private ArrayList<Hashtable<String, String>> messages = new ArrayList<Hashtable<String, String>>();

    /***
     * Connects the user to the public room if he is not connected yet.
     *
     * @param username
     *            The nickname of the user.
     *
     * @return boolean Whether the user was added to the room or not.
     */
    public boolean join(String username) {
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
     * @param username
     *            The nickname of the user.
     *
     * @return boolean Whether the room contains the user.
     */
    public boolean contains(String username) {
	if (clients.contains(username)) {
	    return true;
	} else {
	    return false;
	}
    }

    /***
     * Disconnects the user from the public room.
     *
     * @param username
     *            The nickname of the user.
     */
    public void leave(String username) {
	clients.remove(username);
    }

    /***
     * Logs a message of the user. Must be used after join(username).
     *
     * @param username
     *            The nickname of the user.
     * @param message
     *            The message of the user.
     */
    public void sendMessage(String username, String message) {
	Hashtable<String, String> userMessage = new Hashtable<String, String>();

	userMessage.put(username, message);
	messages.add(userMessage);

	System.out.println(username + ": " + message);
    }
}
