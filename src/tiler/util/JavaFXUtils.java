/*
 *  Copyright (C) 2016 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tiler.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;

/**
 * some Java FX untilities
 * Created by huson on 4/19/16.
 */
public class JavaFXUtils {
    /**
     * copy a group meshes and shapes
     *
     * @param group
     * @return copy of group
     */
    public static Group copyFundamentalDomain(Group group) {
        final Group result = new Group();

        for (Node node : group.getChildren()) {
            if (node instanceof MeshView) {
                MeshView src = (MeshView) node;
                MeshView target = new MeshView(((MeshView) node).getMesh());
                target.setMaterial(src.getMaterial());
                result.getChildren().add(target);
            } else if (node instanceof Polyline) {
                Polyline src = (Polyline) node;
                Polyline target = new Polyline();
                target.getPoints().addAll(src.getPoints());
                target.setFill(src.getFill());
                target.setStroke(src.getStroke());
                target.setStrokeLineCap(src.getStrokeLineCap());
                result.getChildren().add(target);
            } else if (node instanceof Sphere) {
                Sphere src = (Sphere) node;
                Sphere target = new Sphere(src.getRadius());
                target.setMaterial(src.getMaterial());
                target.getTransforms().addAll(src.getTransforms());
                result.getChildren().add(target);
            } else if (node instanceof Text) {
                Text src = (Text) node;
                Text target = new Text(src.getText());
                target.setFont(src.getFont());
                target.setFill(src.getFill());
                target.getTransforms().addAll(src.getTransforms());
                result.getChildren().add(target);
            }
        }
        return result;
    }
}