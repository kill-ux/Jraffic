package shape;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Light extends Rectangle {
    public static final double WIDTH = 40;
    public static final double HEIGHT = 40;
    

    public Light(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
        setStroke(Color.RED);
    }
}
