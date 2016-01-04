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
	private ServerSocket videoListenSocket;
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
		}

		// FrameAnimator animator = new FrameAnimator();
		while (true) {
			try {
				// Read the frames from the socket
				byte[] sizeAr = new byte[4];
				is.read(sizeAr);

				int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
				if (size > 0) {
					byte[] imageAr = new byte[size];
					is.read(imageAr);

					BufferedImage image = ImageIO
							.read(new ByteArrayInputStream(imageAr));

					long ms = System.currentTimeMillis();
					ImageIO.write(image, "jpg", new File(
							"../resources/images/receiver_" + ms + ".jpg"));

					// TODO display received frames instead of saving them
					// animator.setImage(image);
					// animator.repaint();
				}
			} catch (IOException e) {
				System.err.println(
						"client> Failed reading image / frame from the socket.");
				e.printStackTrace();
			}
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
