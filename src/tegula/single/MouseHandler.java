/*
 * MouseHandler.java Copyright (C) 2019. Daniel H. Huson
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

package tegula.single;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;
import jloda.util.Basic;
import tegula.core.dsymbols.Geometry;
import tegula.tiling.EuclideanTiling;
import tegula.tiling.HyperbolicTiling;
import tegula.tiling.SphericalTiling;

/**
 * mouse handler
 * Daniel Huson 3/2016
 */
public class MouseHandler {
    private double originalMouseDownX;
    private double originalMouseDownY;
    private double mouseDownX;
    private double mouseDownY;
    private long mouseDownTime;

    private final Animator animator;

    private long lastScroll = 0;
    private Thread thread = null;
    private final ObjectProperty<EventHandler<? super ScrollEvent>> onScrollEnded = new SimpleObjectProperty<>();

    /**
     * constructor
     *
     */
    public MouseHandler(final SingleTilingPane singleTilingPane) {
        animator = new Animator(singleTilingPane);

        singleTilingPane.setOnMousePressed((me) -> {
            originalMouseDownX = mouseDownX = me.getSceneX();
            originalMouseDownY = mouseDownY = me.getSceneY();
            mouseDownTime = System.currentTimeMillis();
        });
        singleTilingPane.setOnMouseDragged((me) -> {
            double mouseDeltaX = me.getSceneX() - mouseDownX;
            double mouseDeltaY = me.getSceneY() - mouseDownY;

            if (me.isPrimaryButtonDown()) {
                if (singleTilingPane.getTiling() instanceof SphericalTiling) {
                    final Point2D delta = new Point2D(me.getSceneX() - mouseDownX, me.getSceneY() - mouseDownY);
                    //noinspection SuspiciousNameCombination
                    final Point3D dragOrthogonalAxis = new Point3D(delta.getY(), -delta.getX(), 0);
                    final Rotate rotate = new Rotate(0.25 * delta.magnitude(), dragOrthogonalAxis);
                    singleTilingPane.setWorldRotate(rotate.createConcatenation(singleTilingPane.getWorldRotate()));
                    mouseDownX = me.getSceneX();
                    mouseDownY = me.getSceneY();
                } else if (singleTilingPane.getTiling() instanceof HyperbolicTiling) {
                    final HyperbolicTiling tiling = (HyperbolicTiling) singleTilingPane.getTiling();

                    double modifierFactor = 1;
                    double dx = mouseDeltaX * modifierFactor;
                    double dy = mouseDeltaY * modifierFactor;

                    if (dx != 0 || dy != 0) {
                        singleTilingPane.translateTiling(dx, dy);

                        // Checks whether (dx,dy) has been modified.
                        if (tiling.directionChanged()) {
                            // Modify mouse position in hyperbolic case.
                            mouseDownX = me.getSceneX() - tiling.getTransVector().getX();
                            mouseDownY = me.getSceneY() - tiling.getTransVector().getY();
                        } else {
                            mouseDownX = me.getSceneX();
                            mouseDownY = me.getSceneY();
                        }
                    }
                } else if (singleTilingPane.getTiling() instanceof EuclideanTiling) {
                    double modifierFactor = 1;
                    double dx = mouseDeltaX * modifierFactor;
                    double dy = mouseDeltaY * modifierFactor;

                    if (dx != 0 || dy != 0) {
                        singleTilingPane.translateTiling(dx, dy);

                        mouseDownX = me.getSceneX();
                        mouseDownY = me.getSceneY();
                    }
                }
            }
        });
        singleTilingPane.setOnMouseReleased((me) -> {
            if (me.isShiftDown()) {
                if (singleTilingPane.geometryProperty().getValue() == Geometry.Spherical) {
                    final Point2D delta = new Point2D(me.getSceneX() - originalMouseDownX, me.getSceneY() - originalMouseDownY);
                    final Point3D dragOrthogonalAxis = new Point3D(delta.getY(), -delta.getX(), 0);
                    final double angle = 0.25 * delta.magnitude();
                    animator.set(dragOrthogonalAxis, angle, System.currentTimeMillis() - mouseDownTime);
                    if (angle != 0)
                        animator.play();
                    else
                        animator.stop();
                } else { // slide
                    double mouseDeltaX = me.getSceneX() - originalMouseDownX;
                    double mouseDeltaY = me.getSceneY() - originalMouseDownY;

                    double modifierFactor = 1;
                    double dx = mouseDeltaX * modifierFactor;
                    double dy = mouseDeltaY * modifierFactor;

                    animator.set(dx, dy, System.currentTimeMillis() - mouseDownTime);

                    if (dx != 0 || dy != 0) {
                        animator.play();
                    } else
                        animator.stop();
                }
            }
        });

        singleTilingPane.setOnMouseClicked((me) -> {
            if (me.getClickCount() == 2) {
                animator.stop();
            }
        });

        singleTilingPane.setOnScroll(me -> {
                    if (me.getDeltaY() != 0) {
                        double factor = (me.getDeltaY() > 0 ? 1.1 : 0.9);
                        singleTilingPane.getWorldScale().setX(factor * singleTilingPane.getWorldScale().getX());
                        singleTilingPane.getWorldScale().setY(factor * singleTilingPane.getWorldScale().getY());
                        singleTilingPane.setEuclideanWidth((singleTilingPane.getEuclideanWidth()) / factor);
                        singleTilingPane.setEuclideanHeight((singleTilingPane.getEuclideanHeight()) / factor);
                    }
                    if (onScrollEnded.get() != null) {
                        lastScroll = System.currentTimeMillis();
                        if (thread == null) {
                            thread = new Thread(() -> {
                                while (System.currentTimeMillis() - lastScroll < 100) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        Basic.caught(e);
                                    }
                                }
                                Platform.runLater(() ->
                                {
                                    if (onScrollEnded.get() != null)
                                        onScrollEnded.get().handle(me);
                                    thread = null;
                                });
                            });
                            thread.setDaemon(true);
                            thread.start();
                        }
                    }
                }
        );

        onScrollEnded.set(me -> {
            if (singleTilingPane.getGeometry() == Geometry.Euclidean)
                singleTilingPane.update();
        });

        singleTilingPane.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case SPACE:
                    if (animator.isPlaying())
                        animator.pause();
                    else if (animator.isPaused())
                        animator.play();
                    break;
                case LEFT:
                    if (singleTilingPane.getTilingStyle().getBandWidth() - 1 > 1)
                        singleTilingPane.getTilingStyle().setBandWidth(singleTilingPane.getTilingStyle().getBandWidth() - 1);
                    break;
                case RIGHT:
                    singleTilingPane.getTilingStyle().setBandWidth(singleTilingPane.getTilingStyle().getBandWidth() + 1);
                    break;
                case DOWN: {
                    singleTilingPane.decreaseTiling();
                    break;
                }
                case UP: {
                    singleTilingPane.increaseTiling();
                    break;
                }
                case EQUALS:
                case PLUS:
                    break;
                case UNDERSCORE:
                case MINUS:
                    break;
            }
        });
    }

    public Animator getAnimator() {
        return animator;
    }
}