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
		for (int x=x0; x<x1; x++) {
			double t = (x-x0)/(double)(x1-x0);
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
        line(41, 10, 273, 130, image, red);
        line(273, 130, 41, 10, image, green);
        line(41, 10, 130, 273, image, blue);
        line(130, 273, 41, 10, image, white);

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