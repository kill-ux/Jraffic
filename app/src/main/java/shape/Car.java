package shape;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Car extends Rectangle {

    public static final double WIDTH = 40;
    public static final double HEIGHT = 40;

    public KeyCode direction;
    public boolean changed;
    public boolean passed;

    public Car(Color color, KeyCode direction) {
        super(0, 0, WIDTH, HEIGHT);
        this.direction = direction;
        this.changed = false;
        this.passed = false;
        setFill(color);
    }

    public double distance(double x, double y) {
        double dx = this.getX() - x;
        double dy = this.getY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void setDirection(KeyCode direction) {
        this.direction = direction;
        this.changed = true;
    }

    public boolean getPassed() {
        return this.passed;
    }
    
    public void setPassed(){
        this.passed = true;
    }

    public boolean hasChangedDirection() {
        return this.changed;
    }
}