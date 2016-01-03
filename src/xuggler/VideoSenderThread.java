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
        try {
            videoSenderSocket = new Socket(receiverIPAddress, receiverPort);
            OutputStream os = videoSenderSocket.getOutputStream();

            VideoThumbnailsExample framesExtractor = new VideoThumbnailsExample();
            framesExtractor.start(os);
            System.out.println("client> The frames have been sent to the recipient!");
        } catch (IOException e) {
            System.err.println("client> Failed creating private sender socket for video streaming.");
        }

        // TODO interrupted exception
    }

    /***
     * Creates a thread if needed and starts it.
     */
    public void start() {
        if (t == null) {
            t = new Thread(this);
        }

        // TODO determine whether t needs to sleep to allow Receiver to setup
        // its listen socket properly (test for intermittent error)
        t.start();
    }
}
