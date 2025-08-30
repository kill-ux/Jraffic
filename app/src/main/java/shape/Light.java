package shape;


import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Light extends Rectangle {
    public static final double WIDTH = 40;
    public static final double HEIGHT = 40;

    public KeyCode direction;

    public Light(KeyCode direction) {
        super(0, 0, WIDTH, HEIGHT);
        this.direction = direction;
        setStroke(Color.RED);
    }
}
