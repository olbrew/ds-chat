package avro.chat.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import avro.chat.proto.Chat;
import avro.chat.proto.ChatClientServer;

public class ChatServer implements Chat, Runnable {
    private ChatRoom publicRoom = new ChatRoom();
    private Hashtable<String, Transceiver> clients = new Hashtable<String, Transceiver>();
    private Hashtable<String, ChatClientServer> clientsServer = new Hashtable<String, ChatClientServer>();

    /***
     * Registers client's username to its local server proxy so we can
     * communicate both ways.
     *
     * @param username
     *            The nickname of the client.
     * @param clientIP
     *            The IP address of the client.
     * @param clientServerPort
     *            The port to which client's local server is bound to.
     *
     * @return boolean Whether the client was successfully registered on the
     *         server.
     *
     * @throws AvroRemoteException
     */
    @Override
    public boolean register(String username, String clientIP,
            int clientServerPort) throws AvroRemoteException {
        try {
            Transceiver transceiver = new SaslSocketTransceiver(
                    new InetSocketAddress(InetAddress.getByName(clientIP),
                            clientServerPort));
            ChatClientServer proxy = (ChatClientServer) SpecificRequestor
                    .getClient(ChatClientServer.class, transceiver);

            if (!clients.containsKey(username)) {
                clients.put(username, transceiver);
                clientsServer.put(username, proxy);
                System.out.println(
                        "Registered client with username: " + username);
                return true;
            } else {
                System.err.println(
                        username + " is already registered with the server.");
                return false;
            }
        } catch (IOException e1) {
            System.err.println("ERROR: Couldn't connect back to the client on: "
                    + clientIP + ":" + clientServerPort);
            return false;
        }
    }

    /***
     * Gets the list of all client usernames that are connected to the server.
     *
     * @return List<String> The list of usernames.
     *
     * @throws AvroRemoteException
     */
    @Override
    public ArrayList<String> getClientList() throws AvroRemoteException {
        ArrayList<String> clientList = new ArrayList<String>();
        clientList.addAll(clients.keySet());
        return clientList;
    }

    /***
     * Allows a client to join a specific room.
     *
     * @param username
     *            The nickname of the client.
     * @param roomName
     *            The name of the room, either a public chat room or a private
     *            room.
     *
     * @return boolean Whether or not the client has successfully joined the
     *         room.
     *
     * @throws AvroRemoteException
     */
    @Override
    public String join(String username, String roomName)
            throws AvroRemoteException {
        String output;
        if (username == roomName) {
            output = "You can just talk to yourself, "
                    + "you don't need our chat for that ;)";
            return output;
        }
        // public room
        if (roomName.equals("Public")) {
            if (publicRoom.join(username)) {
                output = username
                        + " has successfully joined the Public chat room.";
                System.out.println(output);
                return output;
            } else {
                output = username + " is already in the public room.";
                System.err.println(output);
                return output;
            }
            // private chat
        } else {
            String request = username
                    + " would like to start a private conversation with you.\n"
                    + "You will be disconnected from all your current chats if you accept.";
            if (clientsServer.get(roomName).sendRequest(request)) {
                if (clients.containsKey(roomName)) {
                    if (publicRoom.contains(username)) {
                        leave(username);
                    }
                    if (publicRoom.contains(roomName)) {
                        leave(roomName);
                    }
                    if (setupConnection(username, roomName)) {
                        output = roomName
                                + " did accept your invitation.\nYou can now chat privately.";
                        return output;
                    } else {
                        output = roomName + " did accept your invitation.\n"
                                + "But unfortunately something went wrong with setting up the connection.\n"
                                + "Please try again.";
                        return output;
                    }
                } else {
                    output = roomName + "has not accepted your invitation.";
                    return output;
                }
            } else {
                output = roomName + " is not connected to the server right now.";
                System.err.println(output);
                return output;
            }
        }
    }

    /***
     * Allows a client to leave the public chat room.
     *
     * @param username
     *            The nickname of the client.
     *
     * @throws AvroRemoteException
     */
    @Override
    public boolean leave(String userName) throws AvroRemoteException {
        // if the user is in a private room, the disconnection happens outside
        // the server
        if (publicRoom.contains(userName)) {
            publicRoom.leave(userName);

            if (!publicRoom.contains(userName)) {
                System.out.println(userName + " has left the Public chat room.");
                return true;
            } else {
                System.err.println(userName + " couldn't leave the Public chat room.");
                return false;
            }
        } else {
            return false;
        }
    }

    /***
     * Allows a client to exit the server.
     *
     * @param username
     *            The nickname of the client.
     *
     * @throws AvroRemoteException
     */
    @Override
    public Void exit(String userName) throws AvroRemoteException {
        leave(userName);
        clients.remove(userName);
        clientsServer.remove(userName);
        System.out.println(userName + " has exited the server.");
        return null;
    }

    /***
     * Allows a client to send a message to the public room.
     *
     * @param username
     *            The nickname of the client.
     * @param message
     *            The message to be delivered.
     *
     * @throws AvroRemoteException
     */
    @Override
    public String sendMessage(String userName, String message)
            throws AvroRemoteException {
        if (!publicRoom.contains(userName)) {
            String error = "You have not joined a chatroom yet.\n"
                    + "To join type: \"join 'Public'\" to join the public chatroom.\n"
                    + "Or \"join 'username'\" to start a private conversation with someone.";
            return error;
        } else {
            publicRoom.sendMessage(userName, message);

            // send the message to all other clients
            String output = userName + ": " + message;
            for (String client : clients.keySet()) {
                if (publicRoom.contains(client)) {
                    if (!client.equals(userName)) {
                        (clientsServer.get(client)).incomingMessage(output);
                    }
                }
            }
            return output;
        }
    }

    /***
     * Checks if all connected users are still alive. If not manually exits them
     * from the server.
     *
     * @throws AvroRemoteException
     */
    public void checkUsers() throws AvroRemoteException {
        Hashtable<String, Transceiver> clientsCopy = new Hashtable<String, Transceiver>(
                clients);

        for (String client : clientsCopy.keySet()) {
            try {
                clientsServer.get(client).isAlive();
            } catch (AvroRemoteException e) {
                System.out.println("Failed to reconnect to " + client
                        + ", dropping connection.");
                exit(client);
            }
        }
    }

    /***
     * Set up the connection between two clients for a private chat.
     *
     * @param client1
     *            The nickname of the first client.
     * @param client2
     *            The nickname of the second client.
     *
     * @return boolean Whether or not the connection was successfully made.
     *
     * @throws AvroRemoteException
     */
    private boolean setupConnection(String client1, String client2)
            throws AvroRemoteException {
        try {
            System.out.println("Setting up connections between " + client1
                    + " and " + client2);
            String client1IP = (clients.get(client1)).getRemoteName();
            String client2IP = (clients.get(client2)).getRemoteName();
            if (((clientsServer.get(client1)).connectToClient(client2,
                    client2IP))
                    && ((clientsServer.get(client2)).connectToClient(client1,
                            client1IP))) {
                System.out.println("Connection succesfully made between"
                        + client1 + " and " + client2);
                return true;
            } else {
                System.err.println(
                        "Something went wrong with setting up connections between "
                                + client1 + " and " + client2);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                checkUsers();
                Thread.sleep(1000); // milliseconds
            }
        } catch (InterruptedException e) {
            // This thread was interrupted, it needs to stop doing what it was
            // trying to do
            e.printStackTrace();
        } catch (AvroRemoteException e) {
            System.err.println(
                    "Server couldn't exit client after the client crashed.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = null;
        int serverPort = 10010;

        ChatServer cs = new ChatServer();

        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        } else if (args.length > 1) {
            System.err.println(
                    "ERROR: Max. 1 arguments ([server port]) expected.");
        }

        try {
            server = new SaslSocketServer(new SpecificResponder(Chat.class, cs),
                    new InetSocketAddress(serverPort));
            server.start();

            Thread t = new Thread(cs);
            t.start();
            t.join();

            server.join();
            server.close();
        } catch (IOException e) {
            System.err.println(
                    "ERROR: Starting server. Double check server-ip and server-port.");
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
