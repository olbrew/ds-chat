package xuggler;

import xuggler.VideoDecoder;

import org.apache.avro.AvroRemoteException;

import avro.chat.proto.ChatClientServer;

public class VideoSenderThread implements Runnable {
    private Thread t;
    ChatClientServer privateProxy;
    VideoDecoder decoder;
    
    public VideoSenderThread(ChatClientServer proxy) {
        privateProxy = proxy;
        decoder = new VideoDecoder();
        decoder.updateProxy(proxy);
    }

    @Override
    public void run() {
        decoder.start();
        
        try {
			privateProxy.stopVideoStream();
			System.out.println("client> The video stream has ended.");
		} catch (AvroRemoteException e) {
		    // other client is already offline
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
    
    /***
     * Interrupts the thread.
     */
    public void stop() {
        decoder.updateProxy(null);
        //t.interrupt();
    }
}
