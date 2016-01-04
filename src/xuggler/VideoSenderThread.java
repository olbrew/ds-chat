package xuggler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import com.javacodegeeks.xuggler.VideoThumbnailsExample;

public class VideoSenderThread implements Runnable {
	private Thread t;
	private InetAddress receiverIPAddress;
	private int receiverPort;
	private Socket videoSenderSocket;
	private OutputStream os;

	public VideoSenderThread(String ip, int port) {
		try {
			receiverIPAddress = InetAddress.getByName(ip);
			receiverPort = port;
		} catch (UnknownHostException e) {
			System.err.println("client> Unknown remote host address.");
		}
	}

	@Override
	public void run() {
		(new VideoThumbnailsExample()).start(os);
		System.out
				.println("client> The frames have been sent to the recipient!");
	}

	/***
	 * Creates a thread if needed and starts it.
	 */
	public void start() {
		if (t == null) {
			t = new Thread(this);
		}

		try {
			videoSenderSocket = new Socket(receiverIPAddress, receiverPort);
			os = videoSenderSocket.getOutputStream();

			// TODO determine whether t needs to sleep so Receiver can
			// setup its socket properly (test for intermittent error)
			t.start();
		} catch (IOException e) {
			System.err.println(
					"client> Failed creating private sender socket for video streaming.");
		}
	}
}
