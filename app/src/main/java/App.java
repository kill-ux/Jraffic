
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
    private long lastLightChangeTime = 0;
    private long greenDuration = 3_000_000_000L; // default 3 seconds in nanoseconds

    private final List<Car> cars = new ArrayList<>();
    private final Map<KeyCode, List<Car>> lengthCars = new EnumMap<>(KeyCode.class);
    private final Map<KeyCode, Car> lastCarXandY = new EnumMap<>(KeyCode.class);
    private final Map<KeyCode, Car> prevCarXandY = new EnumMap<>(KeyCode.class);
    private final Color[] carColors = { Color.YELLOW, Color.PURPLE, Color.BLUE };
    private final List<Light> lights = new ArrayList<>();

    private Light lastGreen;

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
        lengthCars.putAll(Map.of(
                KeyCode.UP, new ArrayList<>(),
                KeyCode.DOWN, new ArrayList<>(),
                KeyCode.LEFT, new ArrayList<>(),
                KeyCode.RIGHT, new ArrayList<>()

        ));

        lastGreen = lights.get(0);

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

    private int Index(KeyCode key) {
        return switch (key) {
            case DOWN -> 0;
            case LEFT -> 1;
            case UP -> 2;
            default -> 3;
        };
    }

    private AnimationTimer createAnimationTimer(Pane pane) {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                fireLights(now);
                updateCars(pane);
            }
        };
    }

    private void fireLights(long now) {
        // If still within current green duration, do nothing
        if (now - lastLightChangeTime < greenDuration) {
            return;
        }
        // Find the road with the max queue
        Optional<Entry<KeyCode, List<Car>>> maxEntry = lengthCars.entrySet()
                .stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()));

        if (maxEntry.isPresent()) {
            // Switch lights
            lastGreen.setStroke(Color.RED);
            Light nextGreen = lights.get(Index(maxEntry.get().getKey()));
            nextGreen.setStroke(Color.GREEN);
            lastGreen = nextGreen;
            // Adjust duration proportional to number of cars
            int queueSize = maxEntry.get().getValue().size();
            // e.g. base 3 seconds + 1 second per car
            greenDuration = (1 + queueSize) * 1_000_000_000L;
            // Reset timer
            lastLightChangeTime = now;
        }
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
            removeFromLine(car);
        }
    }

    private void removeFromLine(Car car) {
        Point2D center1 = new Point2D(CENTER, CENTER);
        Point2D center2 = new Point2D(car.getX() + car.getWidth() / 2, car.getY() + car.getWidth() / 2);
        double dis = center1.distance(center2);
        if (dis < 63.245) {
            List<Car> list = lengthCars.get(car.direction);
            list.remove(car);
            // System.out.println(a);
        }
    }

    private void moveCar(Car car) {
        switch (car.direction) {
            case UP -> {
                Light light = lights.get(2);
                boolean isRedLight = light.getStroke().equals(Color.RED);
                boolean isNearIntersection = car.getY() <= light.getY();
                boolean hasPassed = car.getPassed();

                // Car can move if: already passed OR (not red light OR not near intersection)
                if (hasPassed || !isRedLight || !isNearIntersection) {
                    if (hasPassed
                            || isSafeDistance(car, lengthCars.get(car.direction), car.getX(), car.getY() - SPEED)) {
                        car.setY(car.getY() - SPEED);
                        if (isNearIntersection && !hasPassed) {
                            car.setPassed();
                        }
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
                boolean hasPassed = car.getPassed();

                if (hasPassed || !isRedLight || !isNearIntersection) {
                    if (hasPassed
                            || isSafeDistance(car, lengthCars.get(car.direction), car.getX(), car.getY() + SPEED)) {
                        car.setY(car.getY() + SPEED);
                        if (isNearIntersection && !hasPassed) {
                            car.setPassed();
                        }
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
                boolean hasPassed = car.getPassed();

                if (hasPassed || !isRedLight || !isNearIntersection) {
                    if (hasPassed
                            || isSafeDistance(car, lengthCars.get(car.direction), car.getX() - SPEED, car.getY())) {
                        car.setX(car.getX() - SPEED);
                        if (isNearIntersection && !hasPassed) {
                            car.setPassed();
                        }
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
                boolean hasPassed = car.getPassed();

                if (hasPassed || !isRedLight || !isNearIntersection) {
                    if (hasPassed
                            || isSafeDistance(car, lengthCars.get(car.direction), car.getX() + SPEED, car.getY())) {
                        car.setX(car.getX() + SPEED);
                        if (isNearIntersection && !hasPassed) {
                            car.setPassed();
                        }
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
        if (lengthCars.get(direction).size() < capacity / 2) {
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
        List<Car> list = lengthCars.get(direction);
        list.add(car);
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
        // else if (car.getFill().equals(Color.BLUE) && car.getY() <= CENTER) {
        // lengthCars.merge(car.direction, -1, Integer::sum);
        // }
    }

    private void handleDownDirection(Car car) {
        if (car.getFill().equals(Color.YELLOW) && car.getY() >= CENTER - CAR_WIDTH) {
            changeDirection(car, KeyCode.LEFT);
        } else if (car.getFill().equals(Color.PURPLE) && car.getY() >= CENTER) {
            changeDirection(car, KeyCode.RIGHT);
        }
        // else if (car.getFill().equals(Color.BLUE) && car.getY() >= CENTER -
        // CAR_WIDTH) {
        // lengthCars.merge(car.direction, -1, Integer::sum);
        // }
    }

    private void handleLeftDirection(Car car) {
        if (car.getFill().equals(Color.YELLOW) && car.getX() <= CENTER) {
            changeDirection(car, KeyCode.UP);
        } else if (car.getFill().equals(Color.PURPLE) && car.getX() <= CENTER - CAR_WIDTH) {
            changeDirection(car, KeyCode.DOWN);
        }
        // else if (car.getFill().equals(Color.BLUE) && car.getY() <= CENTER) {
        // lengthCars.merge(car.direction, -1, Integer::sum);
        // }
    }

    private void handleRightDirection(Car car) {
        if (car.getFill().equals(Color.YELLOW) && car.getX() >= CENTER - CAR_WIDTH) {
            changeDirection(car, KeyCode.DOWN);
        } else if (car.getFill().equals(Color.PURPLE) && car.getX() >= CENTER) {
            changeDirection(car, KeyCode.UP);
        }
        // else if (car.getFill().equals(Color.BLUE) && car.getX() >= CENTER -
        // CAR_WIDTH) {
        // lengthCars.merge(car.direction, -1, Integer::sum);
        // }
    }

    private void changeDirection(Car car, KeyCode newDirection) {
        // lengthCars.merge(car.direction, -1, Integer::sum);
        // lengthCars.merge(newDirection, 1, Integer::sum);
        car.setDirection(newDirection);
    }

    // Helper method
    private boolean isSafeDistance(Car currentCar, List<Car> cars, double x,double y) {
        for (Car other : cars) {
            if (other == currentCar)
                continue;

            switch (currentCar.direction) {
                case UP -> {
                    if (Math.abs(x - other.getX()) < Car.WIDTH / 2) {
                        // Check if other car is ahead (has smaller Y) and too close
                        if (other.getY() < y && y - other.getY() < SAFETY_GAP + Car.HEIGHT) {
                            return false;
                        }
                    }
                }
                case DOWN -> {
                    if (Math.abs(x - other.getX()) < Car.WIDTH / 2) {
                        // Check if other car is ahead (has larger Y) and too close
                        if (other.getY() > y && other.getY() - y < SAFETY_GAP + Car.HEIGHT) {
                            return false;
                        }
                    }
                }
                case LEFT -> {
                    if (Math.abs(y - other.getY()) < Car.HEIGHT / 2) {
                        // Check if other car is ahead (has smaller X) and too close
                        if (other.getX() < x && x - other.getX() < SAFETY_GAP + Car.WIDTH) {
                            System.out.println("ffffffffffffffffff");
                            return false;
                        }
                    }
                }
                case RIGHT -> {
                    if (Math.abs(y - other.getY()) < Car.HEIGHT / 2) {
                        // Check if other car is ahead (has larger X) and too close
                        if (other.getX() > x && other.getX() - x < SAFETY_GAP + Car.WIDTH) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {
        launch();
    }
}
