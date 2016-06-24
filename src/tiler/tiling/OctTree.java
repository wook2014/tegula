package tiler.tiling;

import javafx.geometry.Point3D;
/**
 * Computes an OctTree for 3d-points. Returns true if a given point is added to the tree.
 * Created by Ruediger on 2016.06.23.
 */
public class OctTree {
    private double eps = 0.01;
    public Node root = new Node(1, 1, 1); //Root node of the tree.


    private class Node {
        double x, y, z;
        Node ppp, mpp, pmp, ppm, mmp, mpm, pmm, mmm; // Eight nodes for each direction in space

        Node (double x, double y, double z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private double distance (Point3D a, double x, double y, double z){ // Todo: Implement distance function for hyperbolic case
        double dist = a.distance(x,y,z);
        return dist;
    }

    public boolean insert (double x, double y, double z){   //Returns true if (x,y,z) is added to the tree structure.
        Node h = root;
        Point3D point = new Point3D(x,y,z);

        while (h != null){
            if (point.distance(h.x,h.y,h.z) > eps) {
                if (x >= h.x && y >= h.y && z >= h.z) {
                    if (h.ppp == null) {
                        h.ppp = new Node(x, y, z);
                        h = null;
                    } else h = h.ppp;
                } else if (x < h.x && y >= h.y && z >= h.z) {
                    if (h.mpp == null) {
                        h.mpp = new Node(x, y, z);
                        h = null;
                    } else h = h.mpp;
                } else if (x >= h.x && y < h.y && z >= h.z) {
                    if (h.pmp == null) {
                        h.pmp = new Node(x, y, z);
                        h = null;
                    } else h = h.pmp;
                } else if (x >= h.x && y >= h.y && z < h.z) {
                    if (h.ppm == null) {
                        h.ppm = new Node(x, y, z);
                        h = null;
                    } else h = h.ppm;
                } else if (x < h.x && y < h.y && z >= h.z) {
                    if (h.mmp == null) {
                        h.mmp = new Node(x, y, z);
                        h = null;
                    } else h = h.mmp;
                } else if (x < h.x && y >= h.y && z < h.z) {
                    if (h.mpm == null) {

                        h.mpm = new Node(x, y, z);
                        h = null;
                    } else h = h.mpm;
                } else if (x >= h.x && y < h.y && z < h.z) {
                    if (h.pmm == null) {
                        h.pmm = new Node(x, y, z);
                        h = null;
                    } else h = h.pmm;
                } else if (x < h.x && y < h.y && z < h.z) {
                    if (h.mmm == null) {
                        h.mmm = new Node(x, y, z);
                        h = null;
                    } else h = h.mmm;
                }
            }
            else {return false;}
        }
        return true;
    }
}
