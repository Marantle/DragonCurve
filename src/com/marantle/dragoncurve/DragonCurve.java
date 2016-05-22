package com.marantle.dragoncurve;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

public class DragonCurve extends Application {
    private static final int splitAnimationDuration = 1500;
    int size = 1000;
    int os = size / 4;
    double currentSplit = 2;
    int pixels = 256;
    List<Circle> recs = new ArrayList<>();
    List<Circle> goUp = new ArrayList<>();
    List<Circle> goDown = new ArrayList<>();
    List<Circle> goRight = new ArrayList<>();
    List<Circle> goLeft = new ArrayList<>();
    private Timeline timeline = new Timeline();
    boolean vertical = false;
    int colorI = 0;
    Set<Point2D> points = new HashSet<>();
    Set<Integer> yPoint = new HashSet<>();
    List<PathTransition> transitions = new ArrayList<>();
    private boolean picDrawn = false;
    private List<Color> colors;

    @Override
    public void start(Stage primaryStage) {
        initColors();

        SVGPath svgImage = getSvgImage();
        Group pixelRoot = new Group();
        StackPane stackPane = new StackPane(svgImage);
        BorderPane rootPane = new BorderPane(stackPane);
        stackPane.getChildren().addAll(pixelRoot);
        svgImage.setRotate(180);
        svgImage.setRotationAxis(Rotate.X_AXIS);
        svgImage.setRotate(180);
        System.out.println("width: " + svgImage.getLayoutBounds().getWidth());
        System.out.println("height: " + svgImage.getLayoutBounds().getHeight());

        System.out.println(svgImage.getLayoutBounds());


        for (int i = 1; i < 11; i++) {
            createPainter(svgImage, i++);
        }

        try {
            Scene scene = new Scene(rootPane, 1000, 1000);
            // pixelRoot.setScaleX(2);
            //
            // pixelRoot.setScaleY(2);
            // pixelRoot.setTranslateX(500);
            // pixelRoot.setTranslateY(500);
            for (int y = 0; y < pixels; y++) {
                for (int x = 0; x < pixels; x++) {
                    Circle crcl = new Circle(1);
                    crcl.setFill(Color.BLACK);
                    crcl.relocate(x + os, y + os);

                    crcl.setId(crcl.getLayoutX() + "-" + crcl.getLayoutY());
                    pixelRoot.getChildren().add(crcl);
                    recs.add(crcl);
                }
            }
            // pixelRoot.getChildren().add(svgImage);

            // sort();
            Button butt = new Button("Start");

            butt.setOnAction(e -> action(butt));

            rootPane.setTop(butt);
            //stackPane.getChildren().add(butt);

            // pixelRoot.getChildren().add(butt2);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this method will call itself at the very end of the reset() method
     *
     * @param butt
     */
    private void action(Button butt) {
        butt.setDisable(true);
        // flip the split direction
        vertical = !vertical;

        // calculate the dimensions
        double minX = recs.stream().min((x1, x2) -> Double.compare(x1.getLayoutX(), x2.getLayoutX())).get()
                .getLayoutX();
        double maxX = recs.stream().max((x1, x2) -> Double.compare(x1.getLayoutX(), x2.getLayoutX())).get()
                .getLayoutX();
        double width = maxX - minX + 1;
        double minY = recs.stream().min((y1, y2) -> Double.compare(y1.getLayoutY(), y2.getLayoutY())).get()
                .getLayoutY();
        double maxY = recs.stream().max((y1, y2) -> Double.compare(y1.getLayoutY(), y2.getLayoutY())).get()
                .getLayoutY();
        double height = maxY - minY;

        System.out.println("minX: " + minX);
        System.out.println("maxX: " + maxX);
        System.out.println("width: " + width);
        System.out.println("minY: " + minY);
        System.out.println("maxY: " + maxY);
        System.out.println("height: " + height);
        int xx = 0, yy = 0;
        double prevX = Integer.MAX_VALUE;
        double prevY = Integer.MAX_VALUE;
        System.out.println("Vertical slide? " + vertical);
        double bp;

        // we must sort the pixels differently from left to right or from top to bottom depending on its orientation
        if (vertical) {
            sort();
            bp = pixels / currentSplit;
            System.out.println("bp is " + bp);
        } else {
            sort2();
            bp = pixels / currentSplit;
            System.out.println("bp is " + bp);
        }

        //if split is still possible, we can only handle even splits
        if (bp > 1) {
            calculateAndAnimateSplits(butt, xx, yy, prevX, prevY, bp);

        } else if (!picDrawn) {
            // can no longer split, draw the image
            moveToSvgLocations(butt);

        } else {
            // all is done, restart the whole thing
            reset(butt);
        }
    }

    private void calculateAndAnimateSplits(Button btnStart, int xx, int yy, double prevX, double prevY, double bp) {
        //how many slices we need to make, doubled each time
        currentSplit = currentSplit * 2;
        for (Circle rec : recs) {
            double y = rec.getLayoutY();
            double x = rec.getLayoutX();

            //send every other row/column one way, and the rest another way
            if (vertical) {
                if (prevY < y && prevY > 0) {
                    yy++;
                }
                prevY = y;
                if (yy / bp >= 2) {
                    yy = 0;
                }

                if (yy < bp/* && goLeft.size() < pixels*pixels/2 */) {
                    goLeft.add(rec);
                } else {
                    goRight.add(rec);
                }

            } else {
                if (prevX < x && prevX > 0) {
                    xx++;
                }
                prevX = x;
                if (xx / bp >= 2) {
                    xx = 0;
                }
                if (xx < bp /* && goDown.size() < pixels*pixels/2 */) {
                    goDown.add(rec);
                } else {
                    goUp.add(rec);
                }
            }

        }
        System.out.println("animations start!");
        long start = System.currentTimeMillis();
        double move = (bp / 2);
        if (colorI > 10)
            colorI = 0;
        initializeSplitAnimations(btnStart, start, move);
        System.err.println("timeline played");


        // clear the animations
        goRight.clear();
        goLeft.clear();
        goDown.clear();
        goUp.clear();
    }

    private void reset(Button butt) {
        timeline = new Timeline();
        ObservableList<KeyFrame> ls = FXCollections.observableArrayList();
        for (Circle rec : recs) {
            double x = Double.parseDouble(rec.getId().split("-")[0]);
            double y = Double.parseDouble(rec.getId().split("-")[1]);

            final KeyValue kvy = new KeyValue(rec.layoutYProperty(), y);
            final KeyValue kvx = new KeyValue(rec.layoutXProperty(), x);
            final KeyValue kvy2 = new KeyValue(rec.translateYProperty(), 0);
            final KeyValue kvx2 = new KeyValue(rec.translateXProperty(), 0);
            final KeyValue kvsw = new KeyValue(rec.fillProperty(), Color.BLACK);
            final KeyValue kvrad = new KeyValue(rec.radiusProperty(), 1);
            final KeyFrame kf = new KeyFrame(Duration.millis(5000), kvy, kvx, kvy2, kvx2, kvsw, kvrad);
            ls.add(kf);
        }
        timeline.getKeyFrames().setAll(ls);
        timeline.setOnFinished(f -> {
            Platform.runLater(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                action(butt);
            });
        });
        timeline.play();
        currentSplit = 2;
        butt.setDisable(false);
    }

    private void moveToSvgLocations(Button butt) {
        //if here, all the split animations possible have been played, draw the svg
        picDrawn = true;
        transitions.forEach(tr -> {
            tr.stop();
        });
        System.err.println("count of points: " + points.size());
        System.err.println("count of recs: " + recs.size());
        ObservableList<KeyFrame> ls = FXCollections.observableArrayList();
        List<Point2D> pnts = new ArrayList<Point2D>(points);
        int size = pnts.size();
        int pos = 0;
        timeline = new Timeline();
        long l = 0;

        //for each rec, map it to a point in the svg's path and move it there
        for (Circle rec : recs) {
            l++;
            Point2D point = pnts.get(pos);
            if (pos == size - 1)
                pos = 0;
            else
                pos++;

            final KeyValue kvy = new KeyValue(rec.layoutYProperty(), 0);
            final KeyValue kvx = new KeyValue(rec.layoutXProperty(), 0);
            final KeyValue kvy2 = new KeyValue(rec.translateYProperty(), point.getY());
            final KeyValue kvx2 = new KeyValue(rec.translateXProperty(), point.getX());
            final KeyValue kvsw = new KeyValue(rec.radiusProperty(), 1.5);
            final KeyFrame kf = new KeyFrame(Duration.millis(5000), kvy, kvx, kvy2, kvx2, kvsw);
            ls.add(kf);
        }
        timeline.getKeyFrames().setAll(ls);

        timeline.setOnFinished(f -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            action(butt);
        });
        timeline.play();
        butt.setDisable(false);
    }

    private void initializeSplitAnimations(Button btnStart, long start, double move) {
        timeline = new Timeline();
        ObservableList<KeyFrame> ls = FXCollections.observableArrayList();
        if (goLeft.size() > 0) {
            System.err.println("foreachLeft " + goLeft.size());
            for (Circle rec : goLeft) {
                rec.setFill(colors.get(colorI));
                final KeyValue kv = new KeyValue(rec.layoutXProperty(), rec.getLayoutX() - move);
                final KeyFrame kf = new KeyFrame(Duration.millis(splitAnimationDuration), kv);
                ls.add(kf);
            }
        }
        if (goRight.size() > 0) {
            System.err.println("foreachRight " + goRight.size());
            colorI++;
            for (Circle rec : goRight) {
                rec.setFill(colors.get(colorI));
                final KeyValue kv = new KeyValue(rec.layoutXProperty(), rec.getLayoutX() + move);
                final KeyFrame kf = new KeyFrame(Duration.millis(splitAnimationDuration), kv);

                ls.add(kf);
            }
        }
        if (goUp.size() > 0) {
            System.err.println("foreachUp" + goUp.size());
            for (Circle rec : goUp) {
                rec.setFill(colors.get(colorI));
                final KeyValue kv = new KeyValue(rec.layoutYProperty(), rec.getLayoutY() - move);
                final KeyFrame kf = new KeyFrame(Duration.millis(splitAnimationDuration), kv);
                ls.add(kf);
            }
        }
        System.err.println("foreachDown " + goDown.size());
        if (goDown.size() > 0) {
            colorI++;
            for (Circle rec : goDown) {
                rec.setFill(colors.get(colorI));
                final KeyValue kv = new KeyValue(rec.layoutYProperty(), rec.getLayoutY() + move);
                final KeyFrame kf = new KeyFrame(Duration.millis(splitAnimationDuration), kv);
                ls.add(kf);
            }
        }
        timeline.getKeyFrames().setAll(ls);
        timeline.setOnFinished(ae -> {
            for (Circle rec : recs) {
                rec.setFill(colors.get(colorI));
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("Animations end! took " + end / 1000 + " seconds.");
            timeline.getKeyFrames().clear();
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                action(btnStart);
            });
        });

        timeline.play();
    }

    private void initColors() {
        colors = new ArrayList<>();
        colors.add(Color.DARKBLUE);
        colors.add(Color.DARKCYAN);
        colors.add(Color.DARKGOLDENROD);
        colors.add(Color.DARKGRAY);
        colors.add(Color.DARKOLIVEGREEN);
        colors.add(Color.DARKVIOLET);
        colors.add(Color.DARKORANGE);
        colors.add(Color.DARKSLATEBLUE);
        colors.add(Color.DARKORCHID);
        colors.add(Color.DARKRED);
        colors.add(Color.DARKBLUE);
        colors.add(Color.DARKCYAN);
        colors.add(Color.DARKGOLDENROD);
        colors.add(Color.DARKGRAY);
        colors.add(Color.DARKOLIVEGREEN);
        colors.add(Color.DARKVIOLET);
        colors.add(Color.DARKORANGE);
        colors.add(Color.DARKSLATEBLUE);
        colors.add(Color.DARKORCHID);
        colors.add(Color.DARKRED);
    }

    private void createPainter(SVGPath svg, int time) {
        Circle c1 = new Circle(1);
        c1.setVisible(false);
        c1.translateXProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Point2D p = new Point2D((int) c1.getTranslateX(), (int) c1.getTranslateY());
                if (!points.contains(p))
                    points.add(p);
            }
        });
        c1.translateYProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Point2D p = new Point2D((int) c1.getTranslateX(), (int) c1.getTranslateY());
                if (!points.contains(p))
                    points.add(p);
            }
        });
        PathTransition pthTrs1 = new PathTransition();
        pthTrs1.setDuration(Duration.seconds(time));
        pthTrs1.setPath(svg);
        pthTrs1.setNode(c1);
        pthTrs1.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
        pthTrs1.setCycleCount(Timeline.INDEFINITE);
        pthTrs1.setAutoReverse(true);

        pthTrs1.play();
        transitions.add(pthTrs1);
    }

    private void sort() {
        Comparator<Circle> comparator = Comparator.comparing(Circle::getLayoutY).thenComparing(Circle::getLayoutX);
        recs = recs.stream().sorted(comparator).collect(Collectors.toList());
    }

    private void sort2() {
        Comparator<Circle> comparator = Comparator.comparing(Circle::getLayoutX).thenComparing(Circle::getLayoutY);
        recs = recs.stream().sorted(comparator).collect(Collectors.toList());
    }

    private SVGPath getSvgImage() {
        SVGPath svg = new SVGPath();
        List<String> paths = new ArrayList<>();
        paths.add("M448.947,218.475c-0.922-1.168-23.055-28.933-61-56.81c-50.705-37.253-105.877-56.944-159.551-56.944" +
                "c-53.672,0-108.844,19.691-159.551,56.944c-37.944,27.876-60.077,55.642-61,56.81L0,228.397l7.846,9.923" +
                "c0.923,1.168,23.056,28.934,61,56.811c50.707,37.252,105.879,56.943,159.551,56.943c53.673,0,108.845-19.691,159.55-56.943" +
                "c37.945-27.877,60.078-55.643,61-56.811l7.848-9.923L448.947,218.475z M228.396,315.039c-47.774,0-86.642-38.867-86.642-86.642" +
                "c0-7.485,0.954-14.751,2.747-21.684l-19.781-3.329c-1.938,8.025-2.966,16.401-2.966,25.013c0,30.86,13.182,58.696,34.204,78.187" +
                "c-27.061-9.996-50.072-24.023-67.439-36.709c-21.516-15.715-37.641-31.609-46.834-41.478c9.197-9.872,25.32-25.764,46.834-41.478" +
                "c17.367-12.686,40.379-26.713,67.439-36.71l13.27,14.958c15.498-14.512,36.312-23.412,59.168-23.412" +
                "c47.774,0,86.641,38.867,86.641,86.642C315.037,276.172,276.17,315.039,228.396,315.039z M368.273,269.875" +
                "c-17.369,12.686-40.379,26.713-67.439,36.709c21.021-19.49,34.203-47.326,34.203-78.188s-13.182-58.697-34.203-78.188" +
                "c27.061,9.997,50.07,24.024,67.439,36.71c21.516,15.715,37.641,31.609,46.834,41.477" +
                "C405.91,238.269,389.787,254.162,368.273,269.875z");
        paths.add("M173.261,211.555c-1.626,5.329-2.507,10.982-2.507,16.843c0,31.834,25.807,57.642,57.642,57.642" +
                "c31.834,0,57.641-25.807,57.641-57.642s-25.807-57.642-57.641-57.642c-15.506,0-29.571,6.134-39.932,16.094l28.432,32.048" +
                "L173.261,211.555z");

        StringBuilder sb = new StringBuilder("");
        paths.forEach(p -> {
            sb.append(p);
            sb.append(" ");
        });
        // System.out.println(sb.toString());
        svg.setContent(sb.toString());
        // svg.setScaleX(0.01);
        // svg.setScaleY(0.01);
        svg.setStroke(Color.TRANSPARENT);
        svg.setFill(Color.TRANSPARENT);
        return svg;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
