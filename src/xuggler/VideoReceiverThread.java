package xuggler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

public class VideoReceiverThread implements Runnable {
	private Thread t;
	private int listenPort;
	private ServerSocket videoListenSocket = null;
	private Socket videoReceiverSocket;
	private InputStream is;

	public VideoReceiverThread(int port) {
		listenPort = port;
	}

	@Override
	public void run() {
		try {
			videoListenSocket = new ServerSocket(listenPort);
			videoReceiverSocket = videoListenSocket.accept();
			is = videoReceiverSocket.getInputStream();
		} catch (IOException e1) {
			System.err.println(
					"client> Failed creating private receiver socket for video streaming.");
			close();
			return;
		}

		try {
			while (true) {
				// Read the frames from the socket
				byte[] sizeAr = new byte[4];
				is.read(sizeAr);

				int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
				if (size > 0) {
					Thread.sleep(40);

					byte[] imageAr = new byte[size];
					is.read(imageAr);

					BufferedImage image = ImageIO
							.read(new ByteArrayInputStream(imageAr));

					long ms = System.currentTimeMillis();
					ImageIO.write(image, "jpg", new File(
							"./resources/images/receiver_" + ms + ".jpg"));

					// TODO display received frames instead of saving them
				}
			}
		} catch (IOException e1) {
			System.err.println(
					"client> Failed reading from private receiver socket for video streaming.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		close();
	}

	public void close() {
		try {
			if (is != null) {
				is.close();
			}

			if (videoReceiverSocket != null) {
				videoReceiverSocket.close();
			}

			if (videoListenSocket != null) {
				videoListenSocket.close();
			}

			System.out.println("client> Receiver's video sockets closed.");
		} catch (IOException e) {
			System.err.println(
					"client> Failed closing receiver's video sockets.");
		}
	}

	/***
	 * Creates a thread if needed and starts it.
	 */
	public void start() {
		if (t == null) {
			t = new Thread(this);
		}

		t.start();
	}
}
