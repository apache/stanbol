package org.apache.stanbol.enhancer.sace.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

import org.apache.stanbol.enhancer.sace.util.ImageAnnotation;
import org.apache.stanbol.enhancer.sace.util.TextAnnotation;


public class SacePanelImage extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -7607274145586243340L;

    private SaceGUI saceGUI;

    private Image image;
    private String imageFilename;
    private JLabel img;

    private Rectangle currentRect = null;
    private List<Rectangle> regions;

    public SacePanelImage(SaceGUI gui) {
        super();
        saceGUI = gui;

        init();

    }

    public void clear() {
        imageFilename = "";
        img.setText("<drop image here>");
        img.setIcon(null);
    }

    private Rectangle translateRect(Rectangle r) {
        Rectangle newRect = new Rectangle();
        if (r != null) {
            if (r.width < 0) {
                newRect.x = r.x + r.width;
                newRect.width = -r.width;
            } else {
                newRect.x = r.x;
                newRect.width = r.width;
            }
            if (r.height < 0) {
                newRect.y = r.y + r.height;
                newRect.height = -r.height;
            } else {
                newRect.y = r.y;
                newRect.height = r.height;
            }
            return newRect;
        }
        return r;
    }

    private void init() {
        currentRect = null;
        this.regions = new LinkedList<Rectangle>();
        img = new JLabel("<drop image here>") {
            /**
             *
             */
            private static final long serialVersionUID = 9085980562151354916L;

            @Override
            public void paint(Graphics g) {
                super.paint(g);

                for (Rectangle r : regions) {
                    g.setColor(Color.white);
                    g.drawRect(r.x, r.y, r.width, r.height);
                    g.drawRect(r.x + 1, r.y + 1, r.width, r.height);
                }
                if (currentRect != null) {
                    Rectangle newRect = translateRect(currentRect);
                    if (newRect != null) {
                        if (doesIntersect(newRect)) {
                            g.setColor(Color.red);
                            g.drawRect(newRect.x, newRect.y, newRect.width,
                                    newRect.height);
                            g.drawRect(newRect.x + 1, newRect.y + 1,
                                    newRect.width, newRect.height);
                        } else {
                            g.setColor(Color.orange);
                            g.drawRect(newRect.x, newRect.y, newRect.width,
                                    newRect.height);
                            g.drawRect(newRect.x + 1, newRect.y + 1,
                                    newRect.width, newRect.height);
                        }
                    }
                }
            }
        };

        setTransferHandler(new FileTransferHandler());
        img.setTransferHandler(new AnnotationTransferHandler());

        img.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {

            }

            public void mouseEntered(MouseEvent e) {
                currentRect = null;
                SacePanelImage.this.repaint();
            }

            public void mouseExited(MouseEvent e) {
                currentRect = null;
                SacePanelImage.this.repaint();
            }

            public void mousePressed(MouseEvent e) {
                currentRect = new Rectangle(e.getX(), e.getY(), 0, 0);
                SacePanelImage.this.repaint();
            }

            public void mouseReleased(MouseEvent e) {
                Rectangle newRect = translateRect(currentRect);
                if (newRect != null) {
                    if (!doesIntersect(newRect))
                        regions.add(newRect);
                    currentRect = null;
                    SacePanelImage.this.repaint();
                }
            }

        });

        img.addMouseMotionListener(new MouseMotionListener() {

            public void mouseDragged(MouseEvent e) {
                if (currentRect != null) {
                    currentRect.height = e.getY() - currentRect.y;
                    currentRect.width = e.getX() - currentRect.x;

                    SacePanelImage.this.repaint();
                }
            }

            public void mouseMoved(MouseEvent e) {
            }

        });

        setLayout(new GridBagLayout());

        add(img);

        // DEBUG//
        setImage("./src/main/resources/germany.jpg");
    }

    private boolean doesIntersect(Rectangle rect) {
        if (rect != null) {
            for (Rectangle r : regions) {
                if (r.intersects(rect)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Rectangle getRectangleToPoint(Point p) {
        if (p != null)
            for (Rectangle r : regions) {
                if (r.contains(p))
                    return r;
            }
        return null;
    }

    public void setImage(String filename) {
        image = Toolkit.getDefaultToolkit().getImage(filename);
        ImageIcon ii = new ImageIcon(image);
        if (ii.getImageLoadStatus() == MediaTracker.COMPLETE
                || ii.getImageLoadStatus() == MediaTracker.LOADING) {
            imageFilename = filename;
            img.setText("");
            img.setIcon(ii);
        } else {
            img.setText("<drop image here>");
        }
    }

    class AnnotationTransferHandler extends TransferHandler {

        /**
         *
         */
        private static final long serialVersionUID = -4564763585453555835L;

        @Override
        public boolean importData(JComponent comp, Transferable t) {

            try {
                if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    Object o = t.getTransferData(DataFlavor.stringFlavor);

                    if (o instanceof TextAnnotation) {
                        Rectangle r = getRectangleToPoint(comp
                                .getMousePosition());

                        if (r != null) {
                            ImageAnnotation ia = new ImageAnnotation();
                            ia.setAnnotation(((TextAnnotation) o));
                            ia.setRegion(r);
                            ia.setFilename(imageFilename);
                            saceGUI.submitImageAnnotationToStanbolEnhancer(ia);
                        }
                    }
                    return true;
                }
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            return true;
        }

    }

    class FileTransferHandler extends TransferHandler {

        /**
         *
         */
        private static final long serialVersionUID = -4564763585453555835L;

        @Override
        public boolean importData(JComponent comp, Transferable t) {

            try {
                if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    List fileList = (java.util.List) t
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    Object o = fileList.get(0);
                    if (o instanceof File) {
                        File f = (File) o;

                        if (f.exists() && f.isFile()) {
                            String mimeType = getMimeType(f);

                            if (mimeType.contains("image")) {
                                setImage(f.getAbsolutePath());
                                return true;
                            }
                        }
                    }
                }
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        private String getMimeType(File f) throws java.io.IOException,
                MalformedURLException {
            String type = null;
            URL u = f.toURI().toURL();
            URLConnection uc = null;
            uc = u.openConnection();
            type = uc.getContentType();
            return type;
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            return true;
        }

    }

}
