
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import shape.Car;
import shape.Light;

public class App extends Application {

    private static final double WINDOW_SIZE = 800;
    private static final double CENTER = WINDOW_SIZE / 2;
    private static final double CAR_WIDTH = 40;
    private static final double SAFETY_GAP = 40;
    private static final double SPEED = 0.1;
    private static final double STOPPING_DISTANCE = 80;

    private final List<Car> cars = new ArrayList<>();
    private final Map<KeyCode, Integer> lengthCars = new EnumMap<>(KeyCode.class);
    private final Map<KeyCode, Car> lastCarXandY = new EnumMap<>(KeyCode.class);
    private final Map<KeyCode, Car> prevCarXandY = new EnumMap<>(KeyCode.class);
    private final Color[] carColors = { Color.YELLOW, Color.PURPLE, Color.BLUE };
    private final List<Light> lights = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        Point2D North = new Point2D(CENTER, 0);
        Point2D South = new Point2D(CENTER, WINDOW_SIZE);
        Point2D East = new Point2D(WINDOW_SIZE, CENTER);
        Point2D West = new Point2D(0, CENTER);

        Map<KeyCode, Point2D> positions = Map.of(
                KeyCode.UP, new Point2D(South.getX(), South.getY()),
                KeyCode.DOWN, new Point2D(CENTER - CAR_WIDTH, -CAR_WIDTH),
                KeyCode.LEFT, new Point2D(East.getX(), CENTER - CAR_WIDTH),
                KeyCode.RIGHT, new Point2D(-CAR_WIDTH, CENTER));

        lights.addAll(List.of(new Light(CENTER - 80, CENTER - 80), new Light(CENTER + 40, CENTER - 80),
                new Light(CENTER + 40, CENTER + 40),
                new Light(CENTER - 80, CENTER + 40)));

        Pane pane = createRoadLines(North, South, East, West);
        pane.getChildren().addAll(lights);

        AnimationTimer timer = createAnimationTimer(pane);
        setupScene(stage, pane, timer, positions);
    }

    private Pane createRoadLines(Point2D North, Point2D South, Point2D East, Point2D West) {
        Pane pane = new Pane();
        Line[] lines = {
                new Line(West.getX(), West.getY(), East.getX(), East.getY()),
                new Line(West.getX(), West.getY() - CAR_WIDTH, East.getX(), East.getY() - CAR_WIDTH),
                new Line(West.getX(), West.getY() + CAR_WIDTH, East.getX(), East.getY() + CAR_WIDTH),
                new Line(North.getX(), North.getY(), South.getX(), South.getY()),
                new Line(North.getX() + CAR_WIDTH, North.getY(), South.getX() + CAR_WIDTH, South.getY()),
                new Line(North.getX() - CAR_WIDTH, North.getY(), South.getX() - CAR_WIDTH, South.getY()),
        };

        for (Line line : lines) {
            line.setStroke(Color.GRAY);
            pane.getChildren().add(line);
        }

        return pane;
    }

    private AnimationTimer createAnimationTimer(Pane pane) {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateCars(pane);
            }
        };
    }

    private void updateCars(Pane pane) {
        Iterator<Car> iterator = cars.iterator();
        while (iterator.hasNext()) {
            Car car = iterator.next();

            if (!car.changed) {
                rotateCar(car);
            }

            moveCar(car);

            if (isCarOutOfBounds(car)) {
                removeCar(pane, iterator, car);
            }
        }
    }

    private void moveCar(Car car) {
        switch (car.direction) {
            case UP -> {
                Light light = lights.get(2);
                boolean isRedLight = light.getStroke().equals(Color.RED);
                boolean isNearIntersection = car.getY() <= light.getY();

                if (!(isRedLight && isNearIntersection)) {
                    if (isSafeDistance(car, prevCarXandY.get(car.direction))) {
                        car.setY(car.getY() - SPEED);
                    }
                    prevCarXandY.put(KeyCode.UP, car);
                } else {
                    prevCarXandY.put(KeyCode.UP, car);
                }
            }
            case DOWN -> {
                Light light = lights.get(0);
                boolean isRedLight = light.getStroke().equals(Color.RED);
                boolean isNearIntersection = car.getY() >= light.getY();

                if (!(isRedLight && isNearIntersection)) {
                    if (isSafeDistance(car, prevCarXandY.get(car.direction))) {
                        car.setY(car.getY() + SPEED);
                    }
                    prevCarXandY.put(KeyCode.DOWN, car);
                } else {
                    prevCarXandY.put(KeyCode.DOWN, car);
                }
            }
            case LEFT -> {

                Light light = lights.get(1);
                boolean isRedLight = light.getStroke().equals(Color.RED);
                boolean isNearIntersection = car.getX() <= light.getX();

                if (!(isRedLight && isNearIntersection)) {
                    if (isSafeDistance(car, prevCarXandY.get(car.direction))) {
                        car.setX(car.getX() - SPEED);
                    }
                    prevCarXandY.put(KeyCode.LEFT, car);
                } else {
                    prevCarXandY.put(KeyCode.LEFT, car);
                }

            }
            case RIGHT -> {
                Light light = lights.get(3);
                boolean isRedLight = light.getStroke().equals(Color.RED);
                boolean isNearIntersection = car.getX() >= light.getX();

                if (!(isRedLight && isNearIntersection)) {
                    if (isSafeDistance(car, prevCarXandY.get(car.direction))) {
                        car.setX(car.getX() + SPEED);
                    }
                    prevCarXandY.put(KeyCode.RIGHT, car);
                } else {
                    prevCarXandY.put(KeyCode.RIGHT, car);
                }
            }
        }
    }

    private boolean isCarOutOfBounds(Car car) {
        return car.getX() > WINDOW_SIZE || car.getY() > WINDOW_SIZE ||
                car.getX() < -CAR_WIDTH - 1 || car.getY() < -CAR_WIDTH - 1;
    }

    private void removeCar(Pane pane, Iterator<Car> iterator, Car car) {
        iterator.remove();
        pane.getChildren().remove(car);
        lengthCars.merge(car.direction, -1, Integer::sum);
    }

    private void setupScene(Stage stage, Pane pane, AnimationTimer timer, Map<KeyCode, Point2D> positions) {
        Scene scene = new Scene(pane, WINDOW_SIZE, WINDOW_SIZE, Color.BLACK);
        setupKeyHandlers(scene, pane, positions);

        stage.setTitle("Traffic Simulation");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        timer.start();
    }

    private void setupKeyHandlers(Scene scene, Pane pane, Map<KeyCode, Point2D> positions) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode key = event.getCode();
            if (positions.containsKey(key)) {
                addCarIfPossible(pane, key, positions.get(key));
            }
        });
    }

    private void addCarIfPossible(Pane pane, KeyCode direction, Point2D position) {
        double capacity = Math.floor(WINDOW_SIZE / (CAR_WIDTH + SAFETY_GAP));
        if (lengthCars.getOrDefault(direction, 0) < capacity) {
            Car lastCar = lastCarXandY.get(direction);

            if (lastCar == null || lastCar.distance(position.getX(), position.getY()) >= SAFETY_GAP + CAR_WIDTH) {
                createAndAddCar(pane, direction, position);
            }
        }
    }

    private void createAndAddCar(Pane pane, KeyCode direction, Point2D position) {
        Car car = new Car(getRandomColor(), direction);
        car.setX(position.getX());
        car.setY(position.getY());

        pane.getChildren().add(car);
        cars.add(car);
        lengthCars.merge(direction, 1, Integer::sum);
        lastCarXandY.put(direction, car);
    }

    private Color getRandomColor() {
        return carColors[ThreadLocalRandom.current().nextInt(carColors.length)];
    }

    private void rotateCar(Car car) {
        switch (car.direction) {
            case UP -> handleUpDirection(car);
            case DOWN -> handleDownDirection(car);
            case LEFT -> handleLeftDirection(car);
            case RIGHT -> handleRightDirection(car);
        }
    }

    private void handleUpDirection(Car car) {
        if (car.getFill().equals(Color.YELLOW) && car.getY() <= CENTER) {
            changeDirection(car, KeyCode.RIGHT);
        } else if (car.getFill().equals(Color.PURPLE) && car.getY() <= CENTER - CAR_WIDTH) {
            changeDirection(car, KeyCode.LEFT);
        }
    }

    private void handleDownDirection(Car car) {
        if (car.getFill().equals(Color.YELLOW) && car.getY() >= CENTER - CAR_WIDTH) {
            changeDirection(car, KeyCode.LEFT);
        } else if (car.getFill().equals(Color.PURPLE) && car.getY() >= CENTER) {
            changeDirection(car, KeyCode.RIGHT);
        }
    }

    private void handleLeftDirection(Car car) {
        if (car.getFill().equals(Color.YELLOW) && car.getX() <= CENTER) {
            changeDirection(car, KeyCode.UP);
        } else if (car.getFill().equals(Color.PURPLE) && car.getX() <= CENTER - CAR_WIDTH) {
            changeDirection(car, KeyCode.DOWN);
        }
    }

    private void handleRightDirection(Car car) {
        if (car.getFill().equals(Color.YELLOW) && car.getX() >= CENTER - CAR_WIDTH) {
            changeDirection(car, KeyCode.DOWN);
        } else if (car.getFill().equals(Color.PURPLE) && car.getX() >= CENTER) {
            changeDirection(car, KeyCode.UP);
        }
    }

    private void changeDirection(Car car, KeyCode newDirection) {
        lengthCars.merge(car.direction, -1, Integer::sum);
        lengthCars.merge(newDirection, 1, Integer::sum);
        car.setDirection(newDirection);
    }

    // Helper method
    private boolean isSafeDistance(Car currentCar, Car previousCar) {
        if (previousCar == null || previousCar.equals(currentCar)) {
            return true;
        }
        System.out.println(previousCar.distance(currentCar.getX(), currentCar.getY()));
        return previousCar.distance(currentCar.getX(), currentCar.getY()) >= SAFETY_GAP + CAR_WIDTH;
    }

    public static void main(String[] args) {
        launch();
    }
}