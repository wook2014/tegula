package tiler.tiling;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import sun.misc.FDBigInteger;
import tiler.core.dsymbols.FDomain;
import tiler.core.dsymbols.Geometry;

/** Tools for calculation
 * Created by Ruediger on 2017.05.22.
 */
public class Tools {

    /**
     * Distance of points a,b.
     * In hyperbolic case: Hyperbolic distance between normalized points on z^2=x^2+y^2+1).
     * In spherical and Euclidean case: Euclidean distance between points.
     * @param f
     * @param a
     * @param b
     * @return distance
     */
    public static double distance(FDomain f, Point3D a, Point3D b){
        if (f.getGeometry() == Geometry.Hyperbolic){
            double scalar = a.getZ()*b.getZ() - a.getX()*b.getX() - a.getY()*b.getY();
            return Math.log(Math.abs(scalar + Math.sqrt(Math.abs(scalar * scalar - 1))));
        }
        else{
            return a.distance(b);
        }
    }


    /**
     * Calculates midpoint between points a and b on 3d-models
     * @param geometry
     * @param a
     * @param b
     * @return midpoint between a and b
     */
    public static Point3D midpoint3D(Geometry geometry, Point3D a, Point3D b){
        if (geometry == Geometry.Euclidean) {
            return a.midpoint(b);
        }
        else if (geometry == Geometry.Spherical) {
            return (a.midpoint(b)).normalize().multiply(100);
        }
        else {
            // TODO Refactoren: Namen etc.
            // die Distanzen werden hier mometan nur uber den Z-Wert berechnet

            Point3D point1 = a.multiply(0.01);
            Point3D point2 = b.multiply(0.01);

            Point3D xAxis = new Point3D(1, 0, 0);
            Point3D ursprung = new Point3D(0, 0 ,1);

            double rotAngle = xAxis.angle(point1.getX(), point1.getY(), 0);

            Point3D rotAxis = null;
            if (point1.getY() >= 0)
                rotAxis = new Point3D(0,0,-1);
            else
                rotAxis = new Point3D(0,0,1);

            Rotate rotateToX = new Rotate(rotAngle, rotAxis);
            Rotate rotateToOr = new Rotate(-rotAngle, rotAxis);

            double dist = Math.log(Math.abs(point1.getZ() + Math.sqrt(Math.abs(point1.getZ() * point1.getZ() - 1))));

            Affine translate1 = new Affine(Math.cosh(-dist), 0 , Math.sinh(-dist), 0, 0, 1, 0, 0, Math.sinh(-dist), 0, Math.cosh(-dist), 0);
            Affine translate1inv = new Affine(Math.cosh(dist), 0 , Math.sinh(dist), 0, 0, 1, 0, 0, Math.sinh(dist), 0, Math.cosh(dist), 0);

            Point3D p2moved = translate1.transform(rotateToX.transform(point2));

            rotAngle = xAxis.angle(p2moved.getX(), p2moved.getY(), 0);
            if (p2moved.getY() >= 0)
                rotAxis = new Point3D(0,0,-1);
            else
                rotAxis = new Point3D(0,0,1);

            Rotate rotat2 = new Rotate(-rotAngle, rotAxis);

            dist = 0.5 * Math.log(Math.abs(p2moved.getZ() + Math.sqrt(Math.abs(p2moved.getZ() * p2moved.getZ() - 1))));

            Affine translate2 = new Affine(Math.cosh(dist), 0 , Math.sinh(dist), 0, 0, 1, 0, 0, Math.sinh(dist), 0, Math.cosh(dist), 0);

            return rotateToOr.transform(translate1inv.transform(rotat2.transform(translate2.transform(ursprung)))).multiply(100);
        }
    }



    /**
     * Calculate hyperbolic translation along vector (dx,dy)
     * @param dx
     * @param dy
     * @return transform
     */
    public static Transform hyperbolicTranslation(double dx, double dy){
        Rotate rotateForward, rotateBackward; //Rotations to x-axis and back
        Affine translateX;
        final Point3D X_Axis = new Point3D(1,0,0);
        double d = Math.sqrt(dx*dx+dy*dy);  // Length of translation
        final Point3D vec = new Point3D(dx,dy,0);

        double rotAngle = vec.angle(X_Axis); //Rotation angle between direction of translation and x-axis
        Point3D rotAxis = new Point3D(0,0,1);  // Rotation axis

        if (dy <= 0){ rotAxis = new Point3D(0,0,-1); }

        rotateForward = new Rotate(rotAngle, rotAxis);
        rotateBackward = new Rotate(-rotAngle, rotAxis);

        translateX = new Affine(Math.cosh(d), 0 , Math.sinh(d), 0, 0, 1, 0, 0, Math.sinh(d), 0, Math.cosh(d), 0); // Translation along x-axis

        return rotateForward.createConcatenation(translateX).createConcatenation(rotateBackward); // Hyperbolic translation
    }


    /**
     * map 2D point (unit model) to 3D point (scaled with 100), depending on set geometry
     *
     * @param apt
     * @return 3D point
     */
    public static Point3D map2Dto3D(Geometry geometry, Point2D apt) {
        switch (geometry) {
            default:
            case Euclidean: {
                return new Point3D(100 * apt.getX(), 100 * apt.getY(), 0);
            }
            case Spherical: {
                final double d = apt.getX() * apt.getX() + apt.getY() * apt.getY();
                return new Point3D(100 * (2 * apt.getX() / (1 + d)), 100 * (2 * apt.getY() / (1 + d)), 100 * ((d - 1) / (d + 1)));
            }
            case Hyperbolic: {
                final double d = apt.getX() * apt.getX() + apt.getY() * apt.getY();
                if (d < 1)
                    return new Point3D(100 * (2 * apt.getX() / (1 - d)), 100 * (2 * apt.getY() / (1 - d)), 100 * ((1 + d) / (1 - d)));
                else
                    return new Point3D(0, 0, 0);
            }
        }
    }


    /**
     * Euclidean case: Scaling by 0.01 and drop coordinate z = 0.
     * Spherical case: Calculates inverse of stereographic projection. Maps from sphere with radius 100 to Euclidean plane in unit scale.
     * Hyperbolic case: Maps a point on hyperboloid model (scaled with factor 100) to Poincare disk model (open unit disk).
     * @param bpt
     * @return
     */

    public static Point2D map3Dto2D(Geometry geometry, Point3D bpt){
        bpt = bpt.multiply(0.01); //scale by 0.01
        switch (geometry) {
            default:
            case Euclidean: {
                return new Point2D(bpt.getX(), bpt.getY());
            }
            case Spherical: { // Inverse of stereographic projection
                double d = (1+bpt.getZ())/(1-bpt.getZ());
                return new Point2D((bpt.getX()*(d+1)/2), (bpt.getY()*(d+1)/2));
            }
            case Hyperbolic: { // Transforms hyperboloid model to Poincare disk model
                return new Point2D(bpt.getX()/(1+bpt.getZ()), bpt.getY()/(1+bpt.getZ()));
            }
        }
    }
}