package shape;

import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class Car extends Rectangle {

    public static double WIDTH = 40;
    public static double HEIGTH = 40;
    public KeyCode direction ;

    public Car(Color color,KeyCode direction) {
        super(0, 0, WIDTH, HEIGTH);
        this.direction = direction;
        setFill(color);
    }

    public double distance(double x, double y) {
        return Point2D.distance(this.getX(), this.getY(), x, y);  
    }
}
