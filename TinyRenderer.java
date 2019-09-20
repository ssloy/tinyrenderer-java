import java.awt.Graphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TinyRenderer extends JPanel {
    public static final int width = 300;
    public static final int height = 300;

    public void line(int x0, int y0, int x1, int y1, BufferedImage image, int color) {
        for (double t=0.; t<1.; t+=.01) {
            int x = (int)(x0*(1.-t) + x1*t);
            int y = (int)(y0*(1.-t) + y1*t);
            image.setRGB(x, y, color);
        }
    }

    public void paint(Graphics g) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int red   = new Color(255,   0,   0).getRGB();
        int green = new Color(  0, 255,   0).getRGB();
        int blue  = new Color(  0,   0, 255).getRGB();
        int white = new Color(255, 255, 255).getRGB();

        image.setRGB(30, 69, white);
        line(41, 10, 273, 180, image, green);

        try {
            ImageIO.write(image, "png", new File("drop.png"));
        } catch (IOException ex) {}
        g.drawImage(image, 0, 0, this);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new TinyRenderer());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setVisible(true);
    }
}