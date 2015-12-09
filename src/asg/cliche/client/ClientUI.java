package asg.cliche.client;

import asg.cliche.Command;
import avro.chat.proto.Chat;

import java.util.List;

import org.apache.avro.AvroRemoteException;

public class ClientUI {
	private Chat chatProxy = null;
			
	public ClientUI(Chat proxy) {
		chatProxy = proxy;
	}
	
    @Command
    public void getClientList() throws AvroRemoteException {
    	List<String> clients = chatProxy.getClientList();
    	
    	System.out.println("Retrieving connected client list:");
		for (String client : clients) {
			System.out.println(client);
		}
    }
    
    @Command
    //TODO remove username argument, otherwise one could impersonate others
    public boolean join(String user, String room) throws AvroRemoteException {
        return chatProxy.join(user, room);
    }
    
    @Command
    //TODO remove username argument, otherwise one could impersonate others
    public void leave(String user) throws AvroRemoteException {
        chatProxy.leave(user);
    }
    
    @Command
    //TODO remove username argument, otherwise one could impersonate others
    public void sendMessage(String user, String message) throws AvroRemoteException {
        chatProxy.sendMessage(user, message);
    }
}