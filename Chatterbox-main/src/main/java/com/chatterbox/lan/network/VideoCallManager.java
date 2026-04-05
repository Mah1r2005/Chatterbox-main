package com.chatterbox.lan.network;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;

public class VideoCallManager {
    private static final int TARGET_WIDTH = 960;
    private static final int TARGET_HEIGHT = 540;
    private static final long FRAME_DELAY_MS = 250L;

    private volatile boolean active;
    private volatile String peerUsername;
    private volatile boolean internalClose;
    private Thread captureThread;
    private Robot robot;
    private Rectangle captureArea;
    private Stage stage;
    private ImageView remoteView;
    private ImageView localPreview;
    private Label statusLabel;

    public synchronized void startCall(String peerUsername, Consumer<byte[]> frameSender, Runnable onWindowClosed) throws AWTException {
        if (active) {
            return;
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.robot = new Robot();
        this.captureArea = new Rectangle(screenSize);
        this.peerUsername = peerUsername;
        this.active = true;

        createWindow(peerUsername, onWindowClosed);

        captureThread = new Thread(() -> captureLoop(frameSender), "video-call-capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public synchronized void stopCall() {
        active = false;
        peerUsername = null;
        robot = null;
        captureArea = null;

        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }

        Platform.runLater(() -> {
            if (stage != null) {
                internalClose = true;
                stage.close();
                stage = null;
                remoteView = null;
                localPreview = null;
                statusLabel = null;
                internalClose = false;
            }
        });
    }

    public void receiveFrame(byte[] imageBytes) {
        if (!active || imageBytes == null || imageBytes.length == 0) {
            return;
        }

        Image remoteImage = new Image(new ByteArrayInputStream(imageBytes));
        Platform.runLater(() -> {
            if (remoteView != null) {
                remoteView.setImage(remoteImage);
            }
            if (statusLabel != null) {
                statusLabel.setText("Live video with " + (peerUsername == null ? "peer" : peerUsername));
            }
        });
    }

    public boolean isActive() {
        return active;
    }

    public String getPeerUsername() {
        return peerUsername;
    }

    private void createWindow(String peerUsername, Runnable onWindowClosed) {
        Platform.runLater(() -> {
            Stage videoStage = new Stage();
            ImageView remoteVideoView = new ImageView();
            remoteVideoView.setPreserveRatio(true);
            remoteVideoView.setFitWidth(TARGET_WIDTH);
            remoteVideoView.setFitHeight(TARGET_HEIGHT);

            Label remotePlaceholder = new Label("Waiting for " + peerUsername + "'s video...");
            remotePlaceholder.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

            StackPane remotePane = new StackPane(remoteVideoView, remotePlaceholder);
            remotePane.setAlignment(Pos.CENTER);
            remotePane.setStyle("-fx-background-color: #0f172a;");

            remoteVideoView.imageProperty().addListener((obs, oldImage, newImage) ->
                    remotePlaceholder.setVisible(newImage == null));

            ImageView localVideoView = new ImageView();
            localVideoView.setPreserveRatio(true);
            localVideoView.setFitWidth(220);
            localVideoView.setFitHeight(124);

            Label status = new Label("Sharing your screen with " + peerUsername);
            status.setStyle("-fx-text-fill: deepskyblue; -fx-font-size: 14px; -fx-font-weight: bold;");

            VBox root = new VBox(10, status, remotePane, localVideoView);
            root.setPadding(new Insets(12));
            root.setStyle("-fx-background-color: #020617;");

            BorderPane.setAlignment(localVideoView, Pos.BOTTOM_RIGHT);
            StackPane localWrapper = new StackPane(localVideoView);
            localWrapper.setAlignment(Pos.BOTTOM_RIGHT);

            VBox sceneRoot = new VBox(10, status, remotePane, localWrapper);
            sceneRoot.setPadding(new Insets(12));
            sceneRoot.setStyle("-fx-background-color: #020617;");

            videoStage.setTitle("Video Call - " + peerUsername);
            videoStage.setScene(new Scene(sceneRoot, 980, 760));
            videoStage.setOnCloseRequest(event -> {
                if (!internalClose && onWindowClosed != null) {
                    onWindowClosed.run();
                }
            });
            videoStage.show();

            this.stage = videoStage;
            this.remoteView = remoteVideoView;
            this.localPreview = localVideoView;
            this.statusLabel = status;
        });
    }

    private void captureLoop(Consumer<byte[]> frameSender) {
        while (active && robot != null && captureArea != null) {
            try {
                BufferedImage frame = robot.createScreenCapture(captureArea);
                byte[] encodedFrame = encodeFrame(frame);
                if (encodedFrame.length == 0) {
                    Thread.sleep(FRAME_DELAY_MS);
                    continue;
                }

                frameSender.accept(encodedFrame);
                Image localImage = new Image(new ByteArrayInputStream(encodedFrame));
                Platform.runLater(() -> {
                    if (localPreview != null) {
                        localPreview.setImage(localImage);
                    }
                });
                Thread.sleep(FRAME_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                break;
            }
        }
    }

    private byte[] encodeFrame(BufferedImage source) throws IOException {
        BufferedImage resized = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.drawImage(source, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, null);
        graphics.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            return new byte[0];
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(0.45f);
            }
            writer.write(null, new IIOImage(resized, null, null), params);
        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }
}
