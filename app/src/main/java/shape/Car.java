package shape;

import java.awt.geom.Point2D;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Car extends Rectangle {

    public static double WIDTH = 40;
    public static double HEIGTH = 40;
    public KeyCode direction;
    public boolean changed;

    public Car(Color color, KeyCode direction) {
        super(0, 0, WIDTH, HEIGTH);
        this.direction = direction;
        setFill(color);
    }

    public double distance(double x, double y) {
        return Point2D.distance(this.getX(), this.getY(), x, y);
    }

    public void setDirection(KeyCode direction) {
        this.direction = direction;
        this.setChanged(true);
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
