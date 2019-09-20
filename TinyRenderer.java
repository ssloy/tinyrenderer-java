import java.awt.Graphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.util.ArrayList;

import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TinyRenderer extends JPanel {
    public static final int width = 800;
    public static final int height = 800;
    
    public static double vertices[] = null; // Point cloud. The size equals to the number of vertices*3. E.g: in order to access to the y component of vertex index i, you should write vertices[i*3+1]
    public static int triangles[] = null;   // Collection of triangles. The size equals to the number of triangles*3. Each triplet references indices in the vertices[] array.

    public void line(int x0, int y0, int x1, int y1, BufferedImage image, int color) {
        if (x0==x1 && y0==y1) return;
        boolean steep = false;
        if (Math.abs(y1-y0) > Math.abs(x1-x0)) {
            steep = true;
            int tmp1 = x0, tmp2 = x1;
            x0 = y0;
            x1 = y1;
            y0 = tmp1;
            y1 = tmp2;
        }
        if (x0>x1) {
            int tmp1 = x0, tmp2 = y0;
            x0 = x1;
            x1 = tmp1;
            y0 = y1;
            y1 = tmp2;
        }
        int dx = x1-x0;
        int dy = y1-y0;
        int derror2 = 2*Math.abs(dy);
        int error2 = 0;
        int y = y0;
        for (int x=x0; x<x1; x++) {
            if (steep) {
                if (y>=0 && y<width && x>=0 && x<height) {
                    image.setRGB(y, x, color);
                }
            } else {
                if (x>=0 && x<width && y>=0 && y<height) {
                    image.setRGB(x, y, color);
                }
            }
            error2 += derror2;
            if (error2>dx) {
                y += (y1>y0?1:-1);
                error2 -= 2*dx;
            }
        }
    }

    public void paint(Graphics g) {
        int red   = new Color(255,   0,   0).getRGB();
        int green = new Color(  0, 255,   0).getRGB();
        int blue  = new Color(  0,   0, 255).getRGB();
        int white = new Color(255, 255, 255).getRGB();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int t=0; t<triangles.length/3; t++) { // iterate through all triangles
            for (int e=0; e<3; e++) { // iterate through 3 edges
                double x0 = vertices[triangles[t*3+e]*3+0];
                double y0 = vertices[triangles[t*3+e]*3+1];
                double x1 = vertices[triangles[t*3+(e+1)%3]*3+0];
                double y1 = vertices[triangles[t*3+(e+1)%3]*3+1];
                
                // The points live in [-1,1]^3. 
                // If we want to map an x from [-1,1] to [0,width], then (x+1)/2 belongs to in [0,1]. 
                // And then width*(x+1)/2 lives in the [0,width] range. +.5 is added for rounding and not truncation.
                int ix0 = (int)(width*(x0+1.)/2.+.5);
                int ix1 = (int)(width*(x1+1.)/2.+.5);
                int iy0 = (int)(height*(1.-y0)/2.+.5);
                int iy1 = (int)(height*(1.-y1)/2.+.5);
                line(ix0, iy0, ix1, iy1, image, green);
            }
        }

        try {
            ImageIO.write(image, "png", new File("drop.png"));
        } catch (IOException ex) {}
        g.drawImage(image, 0, 0, this);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        // crude WaveFront .obj file parsing: the goal is to fill vertices[] and triangles[] arrays.
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

        JFrame frame = new JFrame();
        frame.getContentPane().add(new TinyRenderer());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setVisible(true);
    }
}