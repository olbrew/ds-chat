package asg.cliche.client;

import asg.cliche.Command;
import asg.cliche.Param;
import avro.chat.client.ChatClient;
import java.util.List;
import org.apache.avro.AvroRemoteException;

public class ClientUI {
    ChatClient client;

    public ClientUI(ChatClient cl) {
        // private proxy is always null, since this is a copy of original object
        client = cl;
    }

    @Command(description = "Prints list of users connected to the server.")
    public void getClientList() {
        try {
            List<String> clients = client.getServerProxy().getClientList();

            System.out.println("server> " + "Connected clients:");
            for (String client : clients) {
                System.out.println("server> " + client);
            }
        } catch (AvroRemoteException e) {
            System.err.println("server> Failed to receive answer from the server.");
        }
    }

    @Command(description = "Terminates connection with the public/private room.")
    public void leave() {
        try {
            if (client.getClientProxy().inPrivateRoom()) {
                client.getClientProxy().sendPrivateMessage("client> " + client.getUsername()
                        + " has left the private chat.\n" + "client> You will automatically be disconnected.");
                client.getClientProxy().leave(true);
            } else {
                if (client.getServerProxy().leave(client.getUsername())) {
                    System.out.println("server> You have left the Public chat room.");
                } else {
                    System.err.println("server> You couldn't leave the Public chat room, maybe you never joined it.");
                }
            }
        } catch (AvroRemoteException e) {
            System.err.println("server> Failed to receive answer from the server.");
        }
    }

    @Command(description = "Initiates connection specified room or user.")
    public void join(
            @Param(name = "room", description = "'Public' for the public chat room or the name of the receiver you want to start a private conversation with.") String room) {
        try {
            if (client.getClientProxy().inPrivateRoom()) {
                String output = "client> You've to 'leave' the private room before joining another room.";
                System.out.println(output);
            } else {
                String output = client.getServerProxy().join(client.getUsername(), room);
                System.out.println(output);
            }
        } catch (AvroRemoteException e) {
            System.err.println("server> Failed to receive answer from the server.");
        }
    }

    @Command(description = "Sends message to the connected room, if any.")
    public void sendMessage(
            @Param(name = "message", description = "The message you would like to send.") String message) {
        try {
            if (client.getClientProxy().inPrivateRoom()) {
                String output = client.getUsername() + "> (Private): " + message;
                client.getClientProxy().sendPrivateMessage(output);
                System.out.println(output);
            } else {
                String output = client.getServerProxy().sendMessage(client.getUsername(), message);
                System.out.println(output);
            }
        } catch (AvroRemoteException e) {
            System.err.println("server> Failed to receive answer from the server.");
        }
    }

    @Command(description = "Tries to initiate video streaming with the help of RSVP.")
    public void video() {
        try {
            if (client.getClientProxy().inPrivateRoom()) {
                String request = "client> " + client.getUsername() + " would like to start video streaming.\n"
                        + "client> Use 'accept' to initiate the video streaming.";
                client.getClientProxy().sendPrivateMessage(request);
                client.getClientProxy().setupVideoRequest(true);
            } else {
                String output = "server> You are currently not connected to any private room.\n"
                        + "server> Use `join username` before initiating video streaming.";
                System.err.println(output);
            }
        } catch (AvroRemoteException e) {
            System.err.println("client> Something went wrong using client proxy when requesting video streaming.");
        }
    }

    @Command(description = "Accept video requests.")
    public void accept() {
        try {
            if (client.getClientProxy().inPrivateRoom()) {
                if (client.getClientProxy().isAwaitingVideo()) {
                    // TODO trigger rsvp.send_path handler
                    String output = "The client has accepted your video streaming request.";
                    client.getClientProxy().sendPrivateMessage(output);
                    
                    // set up videostream: sender -> receiver
                    client.getClientProxy().setupVideoStreaming(false);
                    
                    // set up videostream: receiver -> sender
                    client.getClientProxy().setupVideoStreaming(true);
                } else {
                    String output = "client> You don't have any video streaming requests yet.\n"
                            + "client> You can request to send your own video by using 'video'.";
                    System.out.println(output);
                }
            } else {
                String output = "client> You're not in a private room yet.\n"
                        + "client> Connect to one by using 'join username'.";
                System.out.println(output);
            }
        } catch (AvroRemoteException e) {
            System.err.println("client> Something went wrong with the client proxy when accepting video streaming.");
        }
    }

    @Command(description = "Accept private chat requests.")
    public void accept(
            @Param(name = "chatPartner", description = "The person whose request you would like to accept.") String chatPartner) {
        try {
            if (client.getClientProxy().inPrivateRoom()) {
                String output = "client> You've to 'leave' the private room before joining another room.";
                System.out.println(output);
            } else if (client.getServerProxy().setupConnection(chatPartner, client.getUsername())) {
                System.out.println("server> Connection set up, you can chat privately now.");
            } else {
                System.err.println("server> Something went wrong with setting up the connection.\n"
                        + "server> Are you sure you got a chat request from " + chatPartner + "?\n"
                        + "server> Check if " + chatPartner + " is still online by typing 'gcl'.\n"
                        + "server> If so try again by reaccepting with 'accept'");
            }
        } catch (AvroRemoteException e) {
            System.err.println("server> Failed to receive answer from the server.");
        }
    }
}