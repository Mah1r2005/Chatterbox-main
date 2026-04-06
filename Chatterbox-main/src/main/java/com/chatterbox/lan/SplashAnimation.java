package com.chatterbox.lan;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashAnimation {

    // Geometric constants for the isometric grid
    private static final double L = 65.0; // Length of each side
    private static final double DX = L * Math.cos(Math.toRadians(30)); // ~56.29
    private static final double DY = L * Math.sin(Math.toRadians(30)); // 32.5

    // Visual styling to match your schematic
    private static final double STROKE_WIDTH = 6.0;
    private static final double GAP = 12.0; // The distance pushing the pieces apart

    // Calculating the exact vector offsets to separate the pieces uniformly
    private static final double GAP_X = GAP * Math.cos(Math.toRadians(30));
    private static final double GAP_Y = GAP * Math.sin(Math.toRadians(30));

    /**
     * Plays the splash screen and triggers a callback when finished.
     * * @param stage The main application window
     * @param onFinish The action to trigger when the animation ends
     */
    public static void play(Stage stage, Runnable onFinish) {

        // --- 1. Construct the EXACT Open Polylines ---

        // LEFT PIECE (The open 'C' shape)
        double lcx = -GAP_X;
        double lcy = GAP_Y;
        Polyline leftPiece = createSharpLine(
                0 + lcx, 2 * DY + lcy,
                -DX + lcx, DY + lcy,
                -DX + lcx, -DY + lcy,
                0 + lcx, 0 + lcy
        );

        // RIGHT PIECE (The backward open 'C' shape)
        double rcx = GAP_X;
        double rcy = GAP_Y;
        Polyline rightPiece = createSharpLine(
                DX + rcx, -DY + rcy,
                0 + rcx, 0 + rcy,
                0 + rcx, 2 * DY + rcy,
                DX + rcx, DY + rcy
        );

        // TOP PIECE (The open chevron with one inner leg)
        double tcx = 0;
        double tcy = -GAP;
        Polyline topPiece = createSharpLine(
                0 + tcx, 0 + tcy,
                -DX + tcx, -DY + tcy,
                0 + tcx, -2 * DY + tcy,
                DX + tcx, -DY + tcy
        );

        Group logoGroup = new Group(leftPiece, rightPiece, topPiece);

        // --- 2. Construct the Text ---
        HBox textContainer = new HBox();
        textContainer.setAlignment(Pos.CENTER);
        textContainer.setOpacity(0);

        // Load your custom font (Note: using SplashScreenAnimation.class since we are in a static method)
        Font customFont = Font.loadFont(SplashAnimation.class.getResourceAsStream("/Image/Orbitron-Medium.ttf"), 36);

        if (customFont == null) {
            System.out.println("Warning: Custom font could not be loaded. Using fallback.");
            customFont = Font.font("Consolas", FontWeight.BOLD, 36);
        }

        String appName = "CHATTERBOX";
        for (char c : appName.toCharArray()) {
            Text letter = new Text(String.valueOf(c));
            letter.setFont(customFont);
            letter.setFill(Color.BLACK);
            textContainer.getChildren().add(letter);
        }

        // --- 3. Layout ---
        VBox root = new VBox(35, logoGroup, textContainer);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #FFFFFF;");

        Scene splashScene = new Scene(root, 800, 600);
        stage.setTitle("Chatterbox - Loading");
        stage.setScene(splashScene);
        stage.show();

        // --- 4. The Drawing Animations ---
        double totalLength = L * 3;

        Timeline drawLeft = createStrokeAnim(leftPiece, totalLength, Duration.millis(1200));
        Timeline drawRight = createStrokeAnim(rightPiece, totalLength, Duration.millis(1200));
        Timeline drawTop = createStrokeAnim(topPiece, totalLength, Duration.millis(1200));

        ParallelTransition drawLogo = new ParallelTransition(drawLeft, drawRight, drawTop);

        // --- 5. Text Reveal & Spacing ---
        FadeTransition textFade = new FadeTransition(Duration.millis(800), textContainer);
        textFade.setToValue(1.0);

        Timeline textTracking = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(textContainer.spacingProperty(), 0)),
                new KeyFrame(Duration.millis(800), new KeyValue(textContainer.spacingProperty(), 8, Interpolator.EASE_OUT))
        );
        ParallelTransition textReveal = new ParallelTransition(textFade, textTracking);

        // Assemble sequence
        SequentialTransition splashSequence = new SequentialTransition(
                new PauseTransition(Duration.millis(300)),
                drawLogo,
                textReveal,
                new PauseTransition(Duration.millis(500)) // A little buffer at the very end before swapping scenes
        );

        // --- 6. TRIGGER THE CALLBACK TO LOGIN ---
        splashSequence.setOnFinished(event -> {
            if (onFinish != null) {
                onFinish.run();
            }
        });

        splashSequence.play();
    }

    /**
     * Static helper to create the exact sharp, square-jointed lines from your image.
     */
    private static Polyline createSharpLine(double... points) {
        Polyline line = new Polyline(points);
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(STROKE_WIDTH);
        line.setFill(Color.TRANSPARENT);
        line.setStrokeLineCap(StrokeLineCap.BUTT);
        line.setStrokeLineJoin(StrokeLineJoin.MITER);
        return line;
    }

    /**
     * Static helper to set up the stroke-dash offset animation to make the polyline "draw" itself.
     */
    private static Timeline createStrokeAnim(Polyline line, double length, Duration duration) {
        line.getStrokeDashArray().setAll(length, length);
        line.setStrokeDashOffset(length);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(line.strokeDashOffsetProperty(), length)),
                new KeyFrame(duration, new KeyValue(line.strokeDashOffsetProperty(), 0, Interpolator.EASE_BOTH))
        );
        return timeline;
    }
}