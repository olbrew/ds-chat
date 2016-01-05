// Source: http://www.javacodegeeks.com/2011/02/xuggler-tutorial-frames-capture-video.html
// javac -cp "./lib/xuggle-xuggler-5.4.jar" ./src/com/javacodegeeks/xuggler/VideoThumbnailsExample.java 
// ant build; cd bin
// java -cp ".:../lib/jackson-core-asl-1.9.13.jar:../lib/slf4j-api-1.7.7.jar:../lib/xuggle-xuggler-5.4.jar" com.javacodegeeks.xuggler.VideoThumbnailsExample

package xuggler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.Global;

import avro.chat.proto.ChatClientServer;

public class VideoDecoder {
    // Time of last frame write
    private static long mLastPtsWrite = Global.NO_PTS;
    private static final String inputFilename = "./resources/videos/sender_1080x720_1mb.mp4";
    public static final double SECONDS_BETWEEN_FRAMES = 1.0 / 25.0;
    public static final long MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND
            * SECONDS_BETWEEN_FRAMES);
    public static ChatClientServer privateProxy;
    
    public void start(ChatClientServer proxy) {
        privateProxy = proxy;
        
        IMediaReader mediaReader = ToolFactory.makeReader(inputFilename);

        // stipulate that we want BufferedImages created in BGR 24bit color
        // space
        mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);

        mediaReader.addListener(new ImageSnapListener());

        // read out the contents of the media file and
        // dispatch events to the attached listener
        while (mediaReader.readPacket() == null)
            ;
    }

    private static class ImageSnapListener extends MediaListenerAdapter {

        public void onVideoPicture(IVideoPictureEvent event) {
            // if uninitialized, back date mLastPtsWrite to get the very first
            // frame
            if (mLastPtsWrite == Global.NO_PTS)
                mLastPtsWrite = event.getTimeStamp() - MICRO_SECONDS_BETWEEN_FRAMES;

            // if it's time to write the next frame
            if (event.getTimeStamp() - mLastPtsWrite >= MICRO_SECONDS_BETWEEN_FRAMES) {
                sendImageToOutputStream(event.getImage());

                // update last write time
                mLastPtsWrite += MICRO_SECONDS_BETWEEN_FRAMES;
            }

        }

        private void sendImageToOutputStream(BufferedImage image) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, "jpg", baos);
                baos.flush();
                ByteBuffer frame = ByteBuffer.wrap(baos.toByteArray());

                privateProxy.incomingFrame(frame);

                // Thread.sleep(40);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
