package com.github.dialogos.plugin.remote.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WebSocketInputStream extends InputStream {
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private byte[] currentBuffer = null;
    private int currentBufferPosition = 0;
    private volatile boolean recording = false;

    public void startRecording() {
        System.out.println("Started WebSocket Recording");
        recording = true;
    }

    public boolean isRecording() {
        return recording;
    }

    public void stopRecording() {
        recording = false;
        synchronized (audioQueue) {
            audioQueue.offer(new byte[0]);
        }
    }

    public void addAudioData(byte[] audioData) {
        if (recording && audioData != null && audioData.length > 0) {
            System.out.println("Adding Audio to queue");
            System.out.flush();
            audioQueue.offer(audioData);
        }
    }

    @Override
    public int read() throws IOException {
        if (currentBuffer == null || currentBufferPosition >= currentBuffer.length) {
            try {
                currentBuffer = audioQueue.take();
                currentBufferPosition = 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread interrupted while reading audio data.", e);
            }

            if (currentBuffer.length == 0) {
                return -1;
            }
        }
        System.out.println("Reading one Byte from Queue");
        System.out.flush();
        return currentBuffer[currentBufferPosition++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = 0;

        while (bytesRead < len) {
            if (currentBuffer == null || currentBufferPosition >= currentBuffer.length) {
                try {
                    currentBuffer = audioQueue.take();
                    currentBufferPosition = 0;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Thread interrupted while reading audio data.", e);
                }

                if (currentBuffer.length == 0) {
                    return bytesRead == 0 ? -1 : bytesRead;
                }
            }

            int bytesToCopy = Math.min(len - bytesRead, currentBuffer.length - currentBufferPosition);
            System.arraycopy(currentBuffer, currentBufferPosition, b, off + bytesRead, bytesToCopy);
            currentBufferPosition += bytesToCopy;
            bytesRead += bytesToCopy;
        }
        System.out.println("Read " + bytesRead + " Bytes from Queue");
        System.out.flush();
        return bytesRead;
    }
}
