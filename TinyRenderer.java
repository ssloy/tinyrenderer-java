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

    public static double[] vertices = null;         // Point cloud. The size equals to the number of vertices*3. E.g: in order to access to the y component of vertex index i, you should write vertices[i*3+1]
    public static int[] triangles = null;           // Collection of triangles. The size equals to the number of triangles*3. Each triplet references indices in the vertices[] array.
    public static BufferedImage framebuffer = null; // this image contains the rendered scene
    public static double[][] zbuffer = null;        // z-buffer array

    // compute the matrix product A*B
    public static double[][] matrix_product(double[][] A, double[][] B) {
        if (A.length==0 || A[0].length != B.length)
            throw new IllegalStateException("invalid dimensions");
        double[][] matrix = new double[A.length][B[0].length];
        for (int i=0; i<A.length; i++) {
            for (int j=0; j<B[0].length; j++) {
                double sum = 0;
                for (int k=0; k<A[i].length; k++)
                    sum += A[i][k]*B[k][j];
                matrix[i][j] = sum;
            }
        }
        return matrix;
    }

    // transpose the matrix
    public static double[][] matrix_transpose(double[][] matrix) {
        double[][] transpose = new double[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix[i].length; j++)
                transpose[j][i] = matrix[i][j];
        return transpose;
    }

    // invert the matrix; N.B. it works for 3x3 matrices only!
    public static double[][] matrix_inverse(double[][] m) {
        if (m[0].length != m.length || m.length != 3)
            throw new IllegalStateException("invalid dimensions");
        double[][] inverse = new double[m.length][m.length];
        double det = m[0][0]*(m[1][1]*m[2][2] - m[2][1]*m[1][2]) - m[0][1]*(m[1][0]*m[2][2] - m[1][2]*m[2][0]) + m[0][2]*(m[1][0]*m[2][1] - m[1][1]*m[2][0]);
        if (Math.abs(det)<1e-6)
            throw new IllegalStateException("non-invertible matrix");
        inverse[0][0] = (m[1][1]*m[2][2] - m[2][1]*m[1][2])/det;
        inverse[0][1] = (m[0][2]*m[2][1] - m[0][1]*m[2][2])/det;
        inverse[0][2] = (m[0][1]*m[1][2] - m[0][2]*m[1][1])/det;
        inverse[1][0] = (m[1][2]*m[2][0] - m[1][0]*m[2][2])/det;
        inverse[1][1] = (m[0][0]*m[2][2] - m[0][2]*m[2][0])/det;
        inverse[1][2] = (m[1][0]*m[0][2] - m[0][0]*m[1][2])/det;
        inverse[2][0] = (m[1][0]*m[2][1] - m[2][0]*m[1][1])/det;
        inverse[2][1] = (m[2][0]*m[0][1] - m[0][0]*m[2][1])/det;
        inverse[2][2] = (m[0][0]*m[1][1] - m[1][0]*m[0][1])/det;
        return inverse;
    }

    // dot product between two vectors; N.B. works for dimension 3 vectors only
    public static double dot_product(double[] v1, double[] v2) {
        if (v1.length != v2.length || v1.length != 3)
            throw new IllegalStateException("invalid dimensions");
        return v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
    }

    // cross product between two vectors
    public static double[] cross_product(double[] v1, double[] v2) {
        if (v1.length != v2.length || v1.length != 3)
            throw new IllegalStateException("invalid dimensions");
        double[] cross = new double[3];
        cross[0] = v1[1]*v2[2] - v1[2]*v2[1];
        cross[1] = v1[2]*v2[0] - v1[0]*v2[2];
        cross[2] = v1[0]*v2[1] - v1[1]*v2[0];
        return cross;
    }

    // given a triangle, return its normal
    public static double [] triangle_normal(double[] x, double[] y, double[] z) {
        if (x.length != y.length || x.length != z.length || x.length != 3)
            throw new IllegalStateException("invalid dimensions");
        double[] edge_a = {x[1] - x[0], y[1] - y[0], z[1] - z[0]};
        double[] edge_b = {x[2] - x[0], y[2] - y[0], z[2] - z[0]};
        double[] cross = cross_product(edge_a, edge_b);
        double norm = Math.sqrt(cross[0]*cross[0] + cross[1]*cross[1] + cross[2]*cross[2]);
        if (norm<1e-6)
            throw new IllegalStateException("degenerate triangle");
        cross[0] /= norm;
        cross[1] /= norm;
        cross[2] /= norm;
        return cross;
    }

    // given a triangle [(x0,y0), (x1,y1), (x2,y2)], compute barycentric coordinates of the point (x,y) w.r.t the triangle
    public static double[] barycentric_coords(int x0, int y0, int x1, int y1, int x2, int y2, int x, int y) {
        double[][] A = { { x0, x1, x2 }, { y0, y1, y2 }, { 1., 1., 1. } };
        double[][] b = { { x }, { y }, { 1. } };
        return matrix_transpose(matrix_product(matrix_inverse(A), b))[0];
    }

    // well duh
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
        { // initialize the z-buffer with min possible values
            zbuffer = new double[width][height];
            for (int i=0; i<width; i++) {
                for (int j=0; j<height; j++) {
                    zbuffer[i][j] = -1.;
                }
            }
        }

        for (int t=0; t<triangles.length/3; t++) { // iterate through all triangles
            double[] xw = new double[3]; // triangle in world coordinates
            double[] yw = new double[3];
            double[] zw = new double[3];
            int[] x = new int[3]; // triangle in screen coordinates
            int[] y = new int[3];
            for (int v=0; v<3; v++) {
                xw[v] = vertices[triangles[t*3+v]*3+0]; // world coordinates
                yw[v] = vertices[triangles[t*3+v]*3+1];
                zw[v] = vertices[triangles[t*3+v]*3+2];
                x[v] = (int)( width*(xw[v]+1.)/2.+.5); // world-to-screen transformation
                y[v] = (int)(height*(1.-yw[v])/2.+.5); // y is flipped to get a "natural" y orientation (origin in the bottom left corner)
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
            try { // non-ivertible matrix (can happen if a triangle is degenerate)
                for (int px=bbminx; px<=bbmaxx; px++) { // rasterize the bounding box
                    for (int py=bbminy; py<=bbmaxy; py++) {
                        double[] coord = barycentric_coords(x[0], y[0], x[1], y[1], x[2], y[2], px, py);
                        if (coord[0]<0. || coord[1]<0. || coord[2]<0.) continue; // discard the point outside the triangle
                        double pz = coord[0]*zw[0] + coord[1]*zw[1] + coord[2]*zw[2]; // compute the depth of the fragment
                        if (zbuffer[px][py]>pz) continue; // discard the fragment if it lies behind the z-buffer
                        zbuffer[px][py] = pz;
                        double[] normal = triangle_normal(xw, yw, zw);
                        int intensity = (int)Math.min(255, Math.max(0, 255*dot_product(normal, new double[]{0., 0., 1.}))); // triangle intensity is the (clamped) cosine of the angle between the triangle normal and the view direction
                        int color = new Color(intensity, intensity, intensity).getRGB();
                        framebuffer.setRGB(px, py, color);
                    }
                }
            } catch (IllegalStateException ex) {}
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

