package com.chatterbox.lan.network;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class AudioCallManager {
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16_000f, 16, 1, true, false);
    private static final int BUFFER_SIZE = 2048;

    private final BlockingQueue<byte[]> playbackQueue = new LinkedBlockingQueue<>();

    private volatile boolean active;
    private volatile String peerUsername;
    private TargetDataLine microphoneLine;
    private SourceDataLine speakerLine;
    private Thread captureThread;
    private Thread playbackThread;

    public synchronized void startCall(String peerUsername, Consumer<byte[]> audioSender) throws LineUnavailableException {
        if (active) {
            return;
        }

        DataLine.Info microphoneInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);

        microphoneLine = (TargetDataLine) AudioSystem.getLine(microphoneInfo);
        speakerLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);

        microphoneLine.open(AUDIO_FORMAT);
        speakerLine.open(AUDIO_FORMAT);

        microphoneLine.start();
        speakerLine.start();

        this.peerUsername = peerUsername;
        this.active = true;

        captureThread = new Thread(() -> captureLoop(audioSender), "audio-call-capture");
        captureThread.setDaemon(true);
        captureThread.start();

        playbackThread = new Thread(this::playbackLoop, "audio-call-playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    public synchronized void stopCall() {
        active = false;
        playbackQueue.offer(new byte[0]);

        if (microphoneLine != null) {
            microphoneLine.stop();
            microphoneLine.close();
            microphoneLine = null;
        }
        if (speakerLine != null) {
            speakerLine.stop();
            speakerLine.close();
            speakerLine = null;
        }
        peerUsername = null;
    }

    public void receiveAudio(byte[] audioChunk) {
        if (!active || audioChunk == null || audioChunk.length == 0) {
            return;
        }
        playbackQueue.offer(audioChunk);
    }

    public boolean isActive() {
        return active;
    }

    public String getPeerUsername() {
        return peerUsername;
    }

    private void captureLoop(Consumer<byte[]> audioSender) {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (active && microphoneLine != null) {
            int bytesRead = microphoneLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                audioSender.accept(Arrays.copyOf(buffer, bytesRead));
            }
        }
    }

    private void playbackLoop() {
        while (active || !playbackQueue.isEmpty()) {
            try {
                byte[] audioChunk = playbackQueue.take();
                if (!active && audioChunk.length == 0) {
                    break;
                }
                if (speakerLine != null && audioChunk.length > 0) {
                    speakerLine.write(audioChunk, 0, audioChunk.length);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
