package avro.chat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Hashtable;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;

import avro.chat.proto.Chat;

public class ChatServer implements Chat {
	//private Hashtable<CharSequence, String> clients = new Hashtable<CharSequence, String>();
	
	@Override
	public CharSequence register(CharSequence username) throws AvroRemoteException {
		//if(clients.get(username) == null) {
			//clients.put(username, address);
			System.out.println("Registered client with username: " + username);
			
			return "You're successfully registered with username: " + username;
		//}
	}
	
	public static void main(String[] args) {
		Server server = null;
		
		try {
			server = new SaslSocketServer(new SpecificResponder(Chat.class, new ChatServer()), new InetSocketAddress(10000));
		} catch(IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		server.start();
		try {
			server.join();
		} catch(InterruptedException e) {}
	}
}
