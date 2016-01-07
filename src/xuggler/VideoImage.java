/*******************************************************************************
 * Copyright (c) 2008, 2010 Xuggle Inc.  All rights reserved.
 *  
 * This file is part of Xuggle-Xuggler-Main.
 *
 * Xuggle-Xuggler-Main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xuggle-Xuggler-Main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Xuggle-Xuggler-Main.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package xuggler;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.avro.AvroRemoteException;

import avro.chat.proto.ChatClientServer;

/**
 * This class just displays a 2d graphic on a Swing window. It's only here so
 * the video playback demos look simpler. Please don't reuse this component;
 * why? Because I know next to nothing about Swing, and this is probably busted.
 * <p>
 * Of note though, is this class has NO XUGGLER dependencies.
 * </p>
 * 
 * @author aclarke
 *
 */
@SuppressWarnings("serial")
public class VideoImage extends JFrame {
    private final ImageComponent mOnscreenPicture;
    private ChatClientServer privateProxy;

    public VideoImage(ChatClientServer proxy) {
        super("Video stream.");
        privateProxy = proxy;
        mOnscreenPicture = new ImageComponent();
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().add(mOnscreenPicture);
        this.setVisible(true);
        this.pack();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                setVisible(false);
                dispose();
                
                try {
                    privateProxy.stopVideoStream();
                } catch (AvroRemoteException e1) {
                    // the other client is already offline
                }
            }
        });
    }

    public void setImage(final BufferedImage image) {
        mOnscreenPicture.setImage(image);
    }

    public class ImageComponent extends JComponent {
        private Image mImage;
        private Dimension mSize;

        public void setImage(Image image) {
            SwingUtilities.invokeLater(new ImageRunnable(image));
        }

        private class ImageRunnable implements Runnable {
            private final Image newImage;

            public ImageRunnable(Image newImage) {
                super();
                this.newImage = newImage;
            }

            public void run() {
                ImageComponent.this.mImage = newImage;
                final Dimension newSize = new Dimension(mImage.getWidth(null), mImage.getHeight(null));
                if (!newSize.equals(mSize)) {
                    ImageComponent.this.mSize = newSize;
                    VideoImage.this.setSize(mImage.getWidth(null), mImage.getHeight(null));
                    VideoImage.this.setVisible(true);
                }
                repaint();
            }
        }

        public ImageComponent() {
            mSize = new Dimension(0, 0);
            setSize(mSize);
        }

        public synchronized void paint(Graphics g) {
            if (mImage != null)
                g.drawImage(mImage, 0, 0, this);
        }
    }
}
