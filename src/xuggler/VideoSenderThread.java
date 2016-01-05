package xuggler;

import xuggler.VideoDecoder;
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

        System.out.println("client> The frames have been sent to the recipient!");
    }

    /***
     * Creates a thread if needed and starts it.
     */
    public void start() {
        if (t == null) {
            t = new Thread(this);
        }

        // TODO determine whether t needs to sleep so Receiver can
        // setup its socket properly (test for intermittent error)
        t.start();
    }
}
