package xuggler;

import xuggler.VideoDecoder;

import org.apache.avro.AvroRemoteException;

import avro.chat.proto.ChatClientServer;

public class VideoSenderThread implements Runnable {
    private Thread t;
    ChatClientServer privateProxy;

    public VideoSenderThread(ChatClientServer proxy) {
        privateProxy = proxy;
    }

    @Override
    public void run() {
        (new VideoDecoder()).start(privateProxy);
        
        try {
			privateProxy.stopVideoStream();
		} catch (AvroRemoteException e) {
			System.err.println("VideoSenderThread: bad private proxy");
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
        t.interrupt();
    }
}
