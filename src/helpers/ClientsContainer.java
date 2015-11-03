package helpers;

import avro.chat.client.ChatClient;

public class ClientsContainer {

	
	public static void main(String[] args) {
		//for (Integer i = 0; i < Integer.parseInt(args[1]); ++i) {
		for (Integer i = 0; i < 3; ++i) {
			ChatClient cc = new ChatClient(); 
		}
	}
}
