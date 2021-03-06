package avro.chat.client;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

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
import xuggler.VideoImage;
import xuggler.VideoSenderThread;

public class ChatClient implements ChatClientServer, Runnable {
	/** Fields **/
	// Main server
	boolean disconnectedServer;
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
	String privateIP;
	int privatePort;
	Transceiver privateTransceiver;
	ChatClientServer privateProxy;

	// Video streaming related attributes
	boolean awaitingVideo = false;
	VideoSenderThread videoSender;
	// VideoReceiverThread videoReceiver;
	VideoImage player;

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
		try {
			if (privateProxy != null) {
				privateProxy.isAlive();
				return true;
			} else {
				return false;
			}
		} catch (AvroRemoteException e) {
			closeVideo();

			privateProxy = null;

			leave(false);
			String output = "client> The other user from this private room has gone offline.\n"
					+ "client> You have automatically been disconnected.";
			System.err.println(output);

			return false;
		}
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
	 * Processes incoming frame.
	 *
	 * @param size
	 *            The size of the frame.
	 * @param ByteBuffer
	 *            The frame itself in bytes.
	 *
	 * @throws AvroRemoteException
	 */
	@Override
	public Void incomingFrame(ByteBuffer frame) throws AvroRemoteException {
		try {
			if (player == null) {
				awaitingVideo = false;
				player = new VideoImage(privateProxy);
			}

			BufferedImage image = ImageIO.read(new ByteArrayInputStream(frame.array()));
			player.setImage(image);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/***
	 * Notifies the client that video stream has ended.
	 *
	 * @throws AvroRemoteException
	 */
	@Override
	public Void stopVideoStream() throws AvroRemoteException {
		closeVideo();
		privateProxy.sendRsvpPathTearMessage();

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
	 * @throws AvroRemoteException
	 */
	@Override
	public Void setupVideoStreaming(boolean privateProxy) throws AvroRemoteException {
		if (privateProxy) { // Sender
			sendRsvpPathMessage();

			videoSender = new VideoSenderThread(this.privateProxy);
			videoSender.start();
		} else { // Receiver
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
		privateIP = ((privateAddress.split(":"))[0]).substring(1);
		privatePort = Integer.parseInt((privateAddress.split(":"))[1]);

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
			privateProxy.leave(false);
		}

		closeVideo();

		try {
			privateProxy = null;
			privateTransceiver.close();
		} catch (IOException e) {
			// the other client is already offline
		}

		System.out.println(
				"client> You have left the private chat.\n" + "client> You can 'join' a new one now if you want.");

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
				disconnectedServer = false;
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

			clientTransceiver = new SaslSocketTransceiver(
					new InetSocketAddress(InetAddress.getByName(clientIP), clientPort));
			clientProxy = (ChatClientServer) SpecificRequestor.getClient(ChatClientServer.class, clientTransceiver);

			Thread t = new Thread(this);
			t.start();

			ShellFactory.createConsoleShell("client", "", new ClientUI(this)).commandLoop();

			t.interrupt();

			closeVideo();

			privateProxy = null;

			serverProxy.leave(username);
			serverTransceiver.close();
			clientTransceiver.close();
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
				disconnectedServer = true;
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
		if (!disconnectedServer) {
			try {
				serverProxy.isAlive();
			} catch (AvroRemoteException e) {
				reconnect(1);
			}
		}
	}

	/***
	 * Check if the private user is still alive every 5 seconds.
	 */
	private void checkPrivateUser() throws InterruptedException {
		try {
			getClientProxy().inPrivateRoom();
		} catch (AvroRemoteException e) {
			e.printStackTrace();
		}
	}

	/***
	 * Connects to the running click script and triggers a handler to send RSVP
	 * PATH msg.
	 */
	private void sendRsvpPathMessage() {
		try {
			Socket rsvpSocket = new Socket(clientIP, 10000);
			PrintWriter out = new PrintWriter(rsvpSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(rsvpSocket.getInputStream()));

			String arguments = "SRC " + clientIP + ", SRCPORT " + clientPort + ", DST " + privateIP + ", DSTPORT "
					+ privatePort;
			System.out.println(arguments);

			if (clientIP.equals(serverIP)) {
				out.println("write host2/rsvp_generator.send_path " + arguments);
			} else {
				out.println("write host1/rsvp_generator.send_path " + arguments);
			}

			System.out.println(in.readLine());
			in.close();
			out.close();
			rsvpSocket.close();

			Thread.sleep(1000);
		} catch (IOException e) {
			System.err.println("Failed to connect to click script on port 10000. Can't send RSVP PATH message.");
		} catch (InterruptedException e) {
		}
	}

	/***
	 * Connects to the running click script and triggers a handler to send RSVP
	 * PATH TEAR msg.
	 */
	@Override
	public Void sendRsvpPathTearMessage() {
		try {
			Socket rsvpSocket = new Socket(clientIP, 10000);
			PrintWriter out = new PrintWriter(rsvpSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(rsvpSocket.getInputStream()));

			String arguments = "SRC " + clientIP + ", SRCPORT " + clientPort + ", DST " + privateIP + ", DSTPORT "
					+ privatePort;
			System.out.println(arguments);

			System.out.println(in.readLine());
			in.close();
			out.close();
			rsvpSocket.close();

			Thread.sleep(1000);
		} catch (IOException e) {
			System.err
					.println("Failed to connect to click script on port 10000. Can't send RSVP PATH TEARDOWN message.");
		} catch (InterruptedException e) {
		}

		return null;
	}

	private void closeVideo() {
		if (videoSender != null) {
			videoSender.stop();
			videoSender = null;
		}

		if (player != null) {
			player.close();
			player = null;
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
				Thread.sleep(5000); // milliseconds
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
