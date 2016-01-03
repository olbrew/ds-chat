package avro.chat.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import asg.cliche.ShellFactory;
import asg.cliche.client.ClientUI;
import avro.chat.proto.Chat;
import avro.chat.proto.ChatClientServer;
import xuggler.VideoReceiverThread;
import xuggler.VideoSenderThread;

public class ChatClient implements ChatClientServer, Runnable {
    /** Fields **/
    // Main server
    String serverIP;
    int serverPort;
    InetSocketAddress serverSocket;
    Transceiver serverTransceiver;
    Chat serverProxy;

    // Our local server
    String username;
    static String clientIP = "127.0.0.1";
    int clientPort;
    Server localServer;
    Transceiver clientTransceiver;
    ChatClientServer clientProxy;

    // Other client connected to us
    Transceiver privateTransceiver = null;
    ChatClientServer privateProxy = null;

    // Video streaming related attributes
    boolean awaitingVideo = false;
    VideoSenderThread videoSender;
    VideoReceiverThread videoReceiver;

    /** Getters **/
    public Chat getServerProxy() {
        return serverProxy;
    }

    public String getUsername() {
        return username;
    }

    public ChatClientServer getClientProxy() {
        return clientProxy;
    }

    /** Proxy methods **/
    /***
     * Simple method to test if the client received a video streaming request.
     *
     * @return boolean Whether or not a prior video request was received.
     *
     * @throws AvroRemoteException
     */
    @Override
    public boolean isAwaitingVideo() throws AvroRemoteException {
        return awaitingVideo;
    }

    /***
     * Simple method to test if the client is still alive.
     *
     * @return null Returns null if it it can answer and is thus still alive.
     *
     * @throws AvroRemoteException
     */
    @Override
    public Void isAlive() throws AvroRemoteException {
        return null;
    }

    /***
     * Simple method to test if the client is still alive.
     *
     * @return null Returns null if it it can answer and is thus still alive.
     * 
     * @throws AvroRemoteException
     */
    @Override
    public boolean inPrivateRoom() throws AvroRemoteException {
        if (privateProxy != null) {
            try {
                privateProxy.isAlive();
                return true;
            } catch (AvroRemoteException e) {
                privateProxy = null;
                return false;
            }
        }
        return false;
    }

    /***
     * Prints out the incoming message.
     *
     * @param message
     *            Content of the incoming message.
     *
     * @throws AvroRemoteException
     */
    @Override
    public Void incomingMessage(String message) throws AvroRemoteException {
        System.out.println(message);
        return null;
    }

    /***
     * Allows a client to send a message to private room.
     * 
     * @param message
     *            The message to be delivered.
     *
     * @throws AvroRemoteException
     */
    @Override
    public Void sendPrivateMessage(String message) throws AvroRemoteException {
        privateProxy.incomingMessage(message);
        return null;
    }

    /***
     * Requests video streaming.
     *
     * @param privateProxy
     *            Allows to switch between the proxies of both clients to
     *            prevent extra method.
     * 
     * @throws AvroRemoteException
     */
    @Override
    public Void setupVideoRequest(boolean privateProxy) throws AvroRemoteException {
        if (privateProxy) {
            this.privateProxy.setupVideoRequest(false);
            System.out.println("client> A video request has been sent to the other client.");
        } else {
            awaitingVideo = true;
        }

        return null;
    }

    /***
     * Initiates video streaming.
     *
     * @param privateProxy
     *            Allows to switch between the proxies of both clients to
     *            prevent extra method.
     * 
     * @throws AvroRemoteException
     */
    @Override
    public Void setupVideoStreaming(boolean privateProxy) throws AvroRemoteException {
        if (privateProxy) { // Sender
            try {
                String privateIP = privateTransceiver.getRemoteName().split(":")[0].substring(1);

                // TODO get listen port from program arguments
                videoSender = new VideoSenderThread(privateIP, 3333);
                videoSender.start();
                System.out.println("Sender thread started.");
            } catch (IOException e) {
                System.err.println("client> Failed getting remote name from private transceiver.");
            }
        } else { // Receiver
            // TODO get listen port from program arguments
            videoReceiver = new VideoReceiverThread(3333);
            videoReceiver.start();
            System.out.println("Receiver thread started.");

            // Create the sender thread
            this.privateProxy.setupVideoStreaming(true);
        }

        return null;
    }

    /***
     * Registers with another client.
     *
     * @param username
     *            Name of the client to register to.
     * @param privateAddress
     *            Address of the client to register to.
     *
     * @return boolean Whether or not the connection was successfully made.
     *
     * @throws AvroRemoteException
     */
    @Override
    public boolean register(String username, String privateAddress) throws AvroRemoteException {
        String privateIP = ((privateAddress.split(":"))[0]).substring(1);
        int privatePort = Integer.parseInt((privateAddress.split(":"))[1]);

        try {
            privateTransceiver = new SaslSocketTransceiver(
                    new InetSocketAddress(InetAddress.getByName(privateIP), privatePort));

            privateProxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class, privateTransceiver);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /***
     * Leave a private chat and call on the other participant to automatically
     * do the same.
     *
     * @param closeOtherProxy
     *            Whether or not your participants chat must also be closed.
     *            Necessary to avoid an endless recursion loop.
     *
     * @throws AvroRemoteException
     */
    @Override
    public Void leave(boolean closeOtherProxy) throws AvroRemoteException {
        if (closeOtherProxy) {
            privateProxy.incomingMessage("client> " + username + " has left the private chat.\n"
                    + "client> You will automatically be disconnected.");
            privateProxy.leave(false);
        }
        try {
            privateTransceiver.close();
            privateProxy = null;
            System.out.println(
                    "client> You have left the private chat.\n" + "client> You can 'join' a new one now if you want.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Methods **/
    /***
     * Starts a server for the client, so the client can also receive commands
     * from other servers and clients.
     * 
     * This emulates push based (server side) communication.
     */
    private void startLocalServer() {
        try {
            localServer = new SaslSocketServer(new SpecificResponder(ChatClientServer.class, new ChatClient()),
                    new InetSocketAddress(clientPort));
            System.out.println("Starting client's local server on " + clientIP + ":" + clientPort);
        } catch (IOException e) {
            System.err.println("ERROR: Starting local server for client. Double check local-ip and local-port.");
            if (localServer != null) {
                localServer.close();
            }
            System.exit(1);
        }
        localServer.start();
    }

    /***
     * Reads in the command line arguments to configure the client, or uses
     * defaults if none are given.
     *
     * @param args
     *            The given command line arguments. Execute client with 'help'
     *            as argument to see the options.
     */
    private void configure(String[] args) {
        // default values, override with command-line arguments

        // parse command line arguments
        if (args.length == 0) {
            username = "Bob";
            serverIP = "127.0.0.1";
            serverPort = 10010;
            clientPort = 11000;
        } else if (args.length == 1) {
            System.out.println("Usage: ant ChatClient [args]");
            System.out.println("----------------------------");
            System.out.println(
                    "0 arguments use our defaults: username = Bob, server ip-address = 127.0.0.1, server port = 10010, client ip-address = 127.0.0.1, client port = 11000");
            System.out.println("2 arguments => username, client port. Rest uses defaults.");
            System.out.println(
                    "4 arguments => username, server ip-address, server port, client port. Rest uses defaults.");
            System.out.println(
                    "5 arguments => username, server ip-address, server port, client ip-address, client port.");
            System.exit(0);
        } else if (args.length == 2) {
            username = args[0];
            serverIP = "127.0.0.1";
            serverPort = 10010;
            clientPort = Integer.parseInt(args[1]);
        } else if (args.length == 4) {
            username = args[0];
            serverIP = args[1];
            serverPort = Integer.parseInt(args[2]);
            clientPort = Integer.parseInt(args[3]);
        } else if (args.length == 5) {
            username = args[0];
            serverIP = args[1];
            serverPort = Integer.parseInt(args[2]);
            clientIP = args[3];
            clientPort = Integer.parseInt(args[4]);
        } else {
            System.err.println("ERROR: Invalid argument[s]. Try `ant ChatClient help` to see your options.");
            System.exit(1);
        }

        if (serverPort == clientPort) {
            if (serverIP.equals(clientIP)) {
                System.err.println("ERROR: Server and client's local server's addresses must be different.");
                System.exit(1);
            }
        }
    }

    /***
     * Registers with the server, start the Cliche CLI and keep the connection
     * open until the client exits or the server is down for more than 60s.
     */
    private void connectToServer() {
        try {
            serverSocket = new InetSocketAddress(InetAddress.getByName(serverIP), serverPort);
            serverTransceiver = new SaslSocketTransceiver(serverSocket);

            serverProxy = (Chat) SpecificRequestor.getClient(Chat.class, serverTransceiver);

            if (serverProxy.register(username, clientIP, clientPort)) {
                System.out.println("You are successfully registered to the server.");
            } else {
                System.out.println(
                        "Something went wrong when registering with the server." + " Maybe you've already registered.");
                if (serverTransceiver != null) {
                    serverTransceiver.close();
                }
                if (localServer != null) {
                    localServer.close();
                }
                System.exit(1);
            }

            Thread t = new Thread(this);
            t.start();

            clientTransceiver = new SaslSocketTransceiver(
                    new InetSocketAddress(InetAddress.getByName(clientIP), clientPort));
            clientProxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class, clientTransceiver);

            ShellFactory.createConsoleShell("client", "", new ClientUI(this)).commandLoop();

            t.interrupt();
            serverProxy.leave(username);
            clientTransceiver.close();
            serverTransceiver.close();
        } catch (IOException e) {
            System.err.println("client> Something went wrong when communicating with the server.");
            System.exit(1);
        }
    }

    /***
     * Tries to ping the server up to n times in increasing intervals (multiples
     * of 5). Give up after 75 seconds.
     *
     * @param n
     *            Number of recent failed attempts to reconnect to server.
     */
    private void reconnect(int n) {
        try {
            if (n == 6) {
                System.err.println("client> Failed to reconnect to server after " + n + " attempts.\n"
                        + "client> Closing transceiver, you may try to connect to the server manually later.");

                serverTransceiver.close();
                return;
            } else {
                System.err.println("client> Cannot access the server, trying to reconnect in " + n * 5 + " seconds.");

                Thread.sleep(n * 5000); // milliseconds

                serverSocket = new InetSocketAddress(InetAddress.getByName(serverIP), serverPort);
                serverTransceiver = new SaslSocketTransceiver(serverSocket);
                serverProxy = (Chat) SpecificRequestor.getClient(Chat.class, serverTransceiver);

                serverProxy.isAlive();
                serverProxy.register(username, clientIP, clientPort);

                System.out.println("Server is accessible again.");
                return;
            }
        } catch (ConnectException e) {
            reconnect(++n); // server is still offline
        } catch (AvroRemoteException e) {
            reconnect(++n); // server is still offline
        } catch (InterruptedException e) {
            e.printStackTrace(); // thread interrupted
        } catch (IOException e) {
            e.printStackTrace(); // transceiver close
        }
    }

    /***
     * Check if the server is still alive every 5 seconds.
     */
    private void checkServer() throws InterruptedException {
        try {
            serverProxy.isAlive();
            Thread.sleep(5000); // milliseconds
        } catch (AvroRemoteException e) {
            reconnect(1);
        }
    }

    /***
     * Check if the private user is still alive every 5 seconds.
     */
    private void checkPrivateUser() throws InterruptedException {
        try {
            if (inPrivateRoom()) {
                privateProxy.isAlive();
                Thread.sleep(5000); // milliseconds
            }
        } catch (AvroRemoteException e) {
            try {
                leave(false);
                String output = "client> The other user from this private room has gone offline.\n"
                        + "client> You have been automatically disconnected.";
                System.err.println(output);
            } catch (AvroRemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    /***
     * This thread runs a polling function which checks if the server and the
     * chat partner, if you're in a private room, is still alive.
     */
    @Override
    public void run() {
        try {
            while (true) {
                checkServer();
                checkPrivateUser();
            }
        } catch (InterruptedException e) {
            // This thread was interrupted, it needs to stop doing what it was
            // trying to do
        }
    }

    /***
     * Main method for the client.
     * 
     * Configures and starts a chatClient. Then connect to the server.
     * 
     * @param args
     *            CLI arguments which are passed to the configure function.
     */
    public static void main(String[] args) {
        try {
            ChatClient chatClient = new ChatClient();

            chatClient.configure(args);
            chatClient.startLocalServer();
            chatClient.connectToServer();
            chatClient.localServer.close();
        } catch (Exception e) {
            // Client was abruptly terminated (for instance by pressing CTRL + D
            // in terminal after cliche interface was constructed).
            System.exit(1);
        }
    }
}
