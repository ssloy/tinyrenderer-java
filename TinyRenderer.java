import java.awt.Graphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Random;

import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import geometry.Matrix;

public class TinyRenderer extends JPanel {
    public static final int width = 800;
    public static final int height = 800;

    public static double vertices[] = null;         // Point cloud. The size equals to the number of vertices*3. E.g: in order to access to the y component of vertex index i, you should write vertices[i*3+1]
    public static int triangles[] = null;           // Collection of triangles. The size equals to the number of triangles*3. Each triplet references indices in the vertices[] array.
    public static BufferedImage framebuffer = null; // this image contains the rendered scene

    public static boolean in_triangle(int x0, int y0, int x1, int y1, int x2, int y2, int x, int y) {
        double[][] A = { { x0, x1, x2 }, { y0, y1, y2 }, { 1., 1., 1. } };
        double[][] b = { { x }, { y }, { 1. } };
        double[][] coord = Matrix.multiply(Matrix.inverse(A), b);
        return coord[0][0]>=0 && coord[1][0]>=0 && coord[2][0]>=0;
    }

    public void paint(Graphics g) {
        g.drawImage(framebuffer, 0, 0, this);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        { // crude WaveFront .obj file parsing: the goal is to fill vertices[] and triangles[] arrays.
            // TODO: create a proper class for the job
            File objFile = new File("obj/african_head/african_head.obj");
            FileReader fileReader = new FileReader(objFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;
            ArrayList<Float> ALvertices = new ArrayList<Float>();
            ArrayList<Integer> ALtriangles = new ArrayList<Integer>();
            while (true) {
                line = bufferedReader.readLine();
                if (null == line) {
                    break;
                }
                line = line.trim();
                String[] stringValues = line.split(" ");
                for (int i=0; i<stringValues.length; ++i ) {
                    stringValues[i] = stringValues[i].trim();
                }

                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                } else if (line.startsWith("v ")) {
                    for (int i=1; i<stringValues.length; ++i) {
                        if (stringValues[i].length()==0) continue;
                        ALvertices.add(Float.valueOf(stringValues[i]));
                    }
                }  else if (line.startsWith("f ")) {
                    for (int i=1; i<stringValues.length; ++i) {
                        if (stringValues[i].length()==0) continue;
                        String[] tmp = stringValues[i].split("/");
                        ALtriangles.add(Integer.valueOf(tmp[0])-1);
                    }
                }
            }
            vertices  =  ALvertices.stream().mapToDouble(Double::valueOf).toArray();
            triangles = ALtriangles.stream().mapToInt(i -> i).toArray();
        }

        framebuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random rand = new Random();

        for (int t=0; t<triangles.length/3; t++) { // iterate through all triangles
            int color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)).getRGB();
            int[] x = new int[3]; // triangle in screen coordinates
            int[] y = new int[3];
            for (int v=0; v<3; v++) {
                double xw = vertices[triangles[t*3+v]*3+0]; // world coordinates
                double yw = vertices[triangles[t*3+v]*3+1];
                x[v] = (int)( width*(xw+1.)/2.+.5); // world-to-screen transformation
                y[v] = (int)(height*(1.-yw)/2.+.5); // y is flipped to get a "natural" y orientation (origin in the bottom left corner)
            }

            int bbminx = width-1; // screen bounding box for the triangle to rasterize
            int bbminy = height-1;
            int bbmaxx = 0;
            int bbmaxy = 0;
            for (int v=0; v<3; v++) {
                bbminx = Math.max(0, Math.min(bbminx, x[v])); // note that the bounding box is clamped to the actual screen size
                bbminy = Math.max(0, Math.min(bbminy, y[v]));
                bbmaxx = Math.min(width-1,  Math.max(bbmaxx, x[v]));
                bbmaxy = Math.min(height-1, Math.max(bbmaxy, y[v]));
            }
            for (int px=bbminx; px<=bbmaxx; px++) {
                for (int py=bbminy; py<=bbmaxy; py++) {
//                  if (px<0 || py<0 || px>=width || py>=height) continue;
                    if (!in_triangle(x[0], y[0], x[1], y[1], x[2], y[2], px, py)) continue;
                    framebuffer.setRGB(px, py, color);
                }
            }
        }

        try {
            ImageIO.write(framebuffer, "png", new File("drop.png"));
        } catch (IOException ex) {}

        JFrame frame = new JFrame();
        frame.getContentPane().add(new TinyRenderer());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setVisible(true);
    }
}
