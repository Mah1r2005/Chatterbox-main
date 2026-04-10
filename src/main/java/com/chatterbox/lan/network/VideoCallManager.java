package com.chatterbox.lan.network;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class VideoCallManager {
    private static final int VIDEO_PORT = 50005;
    private static final int MAX_UDP_SIZE = 63_000;
    private static final int FRAME_INTERVAL_MS = 70;
    private static final int LOCAL_PREVIEW_WIDTH = 200;
    private static final int LOCAL_PREVIEW_HEIGHT = 150;
    private static final int BIND_RETRY_COUNT = 6;

    private volatile int boundReceivePort = -1;
    private final BlockingQueue<byte[]> incomingFrames = new ArrayBlockingQueue<>(120);

    private volatile boolean active;
    private volatile String peerUsername;
    private volatile String peerIp = "127.0.0.1";

    private Webcam webcam;
    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;

    private Thread captureThread;
    private Thread receiveThread;
    private Thread renderThread;

    private Stage window;
    private ImageView remoteDisplay;
    private ImageView localPreview;


    public synchronized void startCall(String peerUsername, Consumer<byte[]> frameSender, Runnable onFatalError) throws java.awt.AWTException {
        if (active) {
            return;
        }

        Webcam selected = Webcam.getDefault();
        if (selected == null) {
            throw new java.awt.AWTException("No webcam detected");
        }

        this.peerUsername = peerUsername;
        this.webcam = selected;
        this.webcam.setViewSize(new Dimension(320, 240));

        try {
            this.sendSocket = new DatagramSocket();
            this.receiveSocket = bindReceiveSocketWithFallback(VIDEO_PORT, BIND_RETRY_COUNT);
            this.boundReceivePort = this.receiveSocket.getLocalPort();
        } catch (IOException e) {
            cleanupSockets();
            throw new java.awt.AWTException("Unable to open UDP receive socket: " + e.getMessage());
        }

        this.active = true;
        setupUI();

        captureThread = new Thread(() -> captureLoop(frameSender, onFatalError), "video-call-capture");
        captureThread.setDaemon(true);
        captureThread.start();

        receiveThread = new Thread(() -> receiveLoop(onFatalError), "video-call-receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();

        renderThread = new Thread(this::renderLoop, "video-call-render");
        renderThread.setDaemon(true);
        renderThread.start();
    }


    public void receiveFrame(byte[] imageBytes) {
        if (!active || imageBytes == null || imageBytes.length == 0) {
            return;
        }
        incomingFrames.offer(imageBytes);
    }

    public boolean isActive() {
        return active;
    }

    public String getPeerUsername() {
        return peerUsername;
    }
    private DatagramSocket bindReceiveSocketWithFallback(int preferredPort, int retryCount) throws IOException {
        IOException last = null;

        try {
            return new DatagramSocket(preferredPort);
        } catch (IOException e) {
            last = e;
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                return new DatagramSocket(0); // OS picks a free port
            } catch (IOException e) {
                last = e;
            }
        }

        throw last != null ? last : new IOException("No UDP port available");
    }

    public synchronized void stopCall() {
        active = false;
        incomingFrames.clear();
        incomingFrames.offer(new byte[0]);

        if (webcam != null) {
            try {
                webcam.close();
            } catch (Exception ignored) {
            }
            webcam = null;
        }

        cleanupSockets();

        peerUsername = null;
        peerIp = "127.0.0.1";
        boundReceivePort = -1;

        Platform.runLater(() -> {
            if (window != null) {
                window.close();
                window = null;
            }
            remoteDisplay = null;
            localPreview = null;
        });
    }

    private void captureLoop(Consumer<byte[]> frameSender, Runnable onFatalError) {
        try {
            webcam.open();
            while (active) {
                BufferedImage frame = webcam.getImage();
                if (frame != null) {
                    byte[] jpeg = compressFrame(frame);
                    if (jpeg.length < MAX_UDP_SIZE) {
                        // Keep existing messenger event pipeline.
                        frameSender.accept(jpeg);
                        sendUdpFrame(jpeg);

                        Image localImage = new Image(new ByteArrayInputStream(jpeg));
                        Platform.runLater(() -> {
                            if (localPreview != null) {
                                localPreview.setImage(localImage);
                            }
                        });
                    }
                }
                Thread.sleep(FRAME_INTERVAL_MS);
            }
        } catch (Exception e) {
            triggerFatal(onFatalError);
        }
    }

    private void receiveLoop(Runnable onFatalError) {
        byte[] buffer = new byte[MAX_UDP_SIZE];
        while (active) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                receiveSocket.receive(packet);
                byte[] frameBytes = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, frameBytes, 0, packet.getLength());
                incomingFrames.offer(frameBytes);
            } catch (IOException e) {
                if (active) {
                    triggerFatal(onFatalError);
                }
                return;
            }
        }
    }

    private void renderLoop() {
        while (active || !incomingFrames.isEmpty()) {
            try {
                byte[] frame = incomingFrames.take();
                if (!active && frame.length == 0) {
                    break;
                }
                if (frame.length == 0) {
                    continue;
                }
                ByteArrayInputStream input = new ByteArrayInputStream(frame);
                Image image = new Image(input);
                Platform.runLater(() -> {
                    if (remoteDisplay != null) {
                        remoteDisplay.setImage(image);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void sendUdpFrame(byte[] jpeg) {
        if (!active || sendSocket == null) {
            return;
        }
        try {
            InetAddress destination = InetAddress.getByName(peerIp);
            DatagramPacket packet = new DatagramPacket(jpeg, jpeg.length, destination, VIDEO_PORT);
            sendSocket.send(packet);
        } catch (IOException ignored) {
            // Non-fatal: controller event
        }
    }

    private byte[] compressFrame(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.40f);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private void setupUI() {
        Platform.runLater(() -> {
            window = new Stage();

            remoteDisplay = new ImageView();
            remoteDisplay.setFitWidth(640);
            remoteDisplay.setFitHeight(480);
            remoteDisplay.setPreserveRatio(true);

            localPreview = new ImageView();
            localPreview.setFitWidth(LOCAL_PREVIEW_WIDTH);
            localPreview.setFitHeight(LOCAL_PREVIEW_HEIGHT);
            localPreview.setPreserveRatio(true);
            localPreview.setTranslateX(210);
            localPreview.setTranslateY(150);

            StackPane root = new StackPane(remoteDisplay, localPreview);
            root.setStyle("-fx-background-color: #111111;");

            window.setScene(new Scene(root, 640, 480));
            window.setTitle("Video Call");
            window.setOnCloseRequest(e -> stopCall());
            window.show();
        });
    }

    private void triggerFatal(Runnable onFatalError) {
        if (!active) {
            return;
        }
        stopCall();
        if (onFatalError != null) {
            Platform.runLater(onFatalError);
        }
    }

    private void cleanupSockets() {
        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
        sendSocket = null;
        receiveSocket = null;
    }
}