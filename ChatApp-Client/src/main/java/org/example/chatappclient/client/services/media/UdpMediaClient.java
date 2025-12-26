package org.example.chatappclient.client.services.media;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ✅ UDP Media Client - ENHANCED với Video Streaming 2-way
 */
public class UdpMediaClient {
    private final String serverIP;
    private final int serverPort;
    private final boolean isVideo;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    private final AtomicBoolean muted;
    private final AtomicBoolean speakerEnabled;
    private final AtomicBoolean videoEnabled;

    // ✅ Video frame callback
    private VideoFrameCallback onVideoFrameReceived;

    private static final int AUDIO_BUFFER_SIZE = 1024;
    private static final int SPEAKER_BUFFER_SIZE = 2048;
    private static final int MAX_PACKET_SIZE = 65507;
    private static final float SAMPLE_RATE = 16000.0f;

    // ✅ Video settings
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 480;
    private static final float VIDEO_QUALITY = 0.5f; // JPEG compression quality

    public UdpMediaClient(String serverIP, int serverPort, boolean isVideo) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.isVideo = isVideo;
        this.executor = Executors.newFixedThreadPool(isVideo ? 5 : 3);
        this.running = new AtomicBoolean(false);
        this.muted = new AtomicBoolean(false);
        this.speakerEnabled = new AtomicBoolean(true);
        this.videoEnabled = new AtomicBoolean(isVideo);
        setupAudioFormat();
    }

    // ==================== SETUP ====================

    private void setupAudioFormat() {
        audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                16,
                1,
                2,
                SAMPLE_RATE,
                false
        );
    }

    private boolean initializeMicrophone() {
        try {
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(micInfo)) {
                System.err.println("❌ Microphone không được hỗ trợ");
                return false;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
            microphone.open(audioFormat, AUDIO_BUFFER_SIZE);
            microphone.start();

            System.out.println("✅ Microphone initialized");
            return true;
        } catch (LineUnavailableException e) {
            System.err.println("❌ Không thể mở microphone: " + e.getMessage());
            return false;
        }
    }

    private boolean initializeSpeakers() {
        try {
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(speakerInfo)) {
                System.err.println("❌ Loa không được hỗ trợ");
                return false;
            }

            speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speakers.open(audioFormat, SPEAKER_BUFFER_SIZE);
            speakers.start();

            System.out.println("✅ Speakers initialized");
            return true;
        } catch (LineUnavailableException e) {
            System.err.println("❌ Không thể mở loa: " + e.getMessage());
            return false;
        }
    }

    // ==================== START/STOP ====================

    public void start() throws Exception {
        if (running.get()) return;

        socket = new DatagramSocket();
        socket.setSoTimeout(100);
        socket.setReceiveBufferSize(MAX_PACKET_SIZE);
        socket.setSendBufferSize(MAX_PACKET_SIZE);

        serverAddress = InetAddress.getByName(serverIP);

        if (!initializeMicrophone() || !initializeSpeakers()) {
            throw new Exception("Không thể khởi tạo thiết bị âm thanh");
        }

        running.set(true);

        // Audio threads
        Thread sendThread = new Thread(this::sendAudioLoop, "AudioSender");
        sendThread.setPriority(Thread.MAX_PRIORITY);
        executor.submit(sendThread);

        Thread receiveThread = new Thread(this::receiveMediaLoop, "MediaReceiver");
        receiveThread.setPriority(Thread.MAX_PRIORITY);
        executor.submit(receiveThread);

        System.out.println("✅ UDP Media Client started");
        System.out.println("   Server: " + serverIP + ":" + serverPort);
        System.out.println("   Video: " + (isVideo ? "ENABLED" : "DISABLED"));
    }

    public void stop() {
        if (!running.get()) return;

        running.set(false);

        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }

        if (speakers != null) {
            speakers.drain();
            speakers.stop();
            speakers.close();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        executor.shutdownNow();

        System.out.println("✅ UDP Media Client stopped");
    }

    // ==================== ✅ SEND VIDEO ====================

    /**
     * Gửi video frame tới server
     */
    public void sendVideoFrame(BufferedImage frame) {
        if (!running.get() || !videoEnabled.get() || frame == null) {
            return;
        }

        executor.submit(() -> {
            try {
                // Compress image to JPEG
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(frame, "jpg", baos);
                byte[] imageData = baos.toByteArray();

                // Check size limit
                if (imageData.length > MAX_PACKET_SIZE - 20) {
                    System.err.println("⚠️ Video frame too large: " + imageData.length + " bytes");
                    return;
                }

                // Create video packet
                byte[] packet = createVideoPacket(imageData);

                // Send to server
                DatagramPacket dgPacket = new DatagramPacket(
                        packet, packet.length, serverAddress, serverPort
                );
                socket.send(dgPacket);

            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("⚠️ Error sending video frame: " + e.getMessage());
                }
            }
        });
    }

    private byte[] createVideoPacket(byte[] videoData) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(0x02); // VIDEO type
            baos.write(longToBytes(System.currentTimeMillis()));
            baos.write(intToBytes(videoData.length));
            baos.write(videoData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    // ==================== SEND AUDIO ====================

    private void sendAudioLoop() {
        byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
        byte[] silentBuffer = new byte[AUDIO_BUFFER_SIZE];
        long packetsSent = 0;

        System.out.println("🎤 Audio sender started");

        while (running.get()) {
            try {
                byte[] dataToSend;

                if (muted.get()) {
                    dataToSend = silentBuffer;
                } else {
                    int bytesRead = microphone.read(buffer, 0, AUDIO_BUFFER_SIZE);
                    if (bytesRead > 0) {
                        dataToSend = buffer;
                    } else {
                        continue;
                    }
                }

                byte[] packet = createAudioPacket(dataToSend, AUDIO_BUFFER_SIZE);
                DatagramPacket dgPacket = new DatagramPacket(
                        packet, packet.length, serverAddress, serverPort
                );
                socket.send(dgPacket);

                packetsSent++;

                if (packetsSent % 500 == 0) {
                    String status = muted.get() ? "MUTED" : "ACTIVE";
                    System.out.println(String.format("📤 Audio sent: %d packets [%s]",
                            packetsSent, status));
                }

            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("⚠️ Error sending audio: " + e.getMessage());
                }
            }
        }

        System.out.println("🎤 Audio sender stopped. Total: " + packetsSent);
    }

    private byte[] createAudioPacket(byte[] audioData, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(0x01); // AUDIO type
            baos.write(longToBytes(System.currentTimeMillis()));
            baos.write(intToBytes(length));
            baos.write(audioData, 0, length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    // ==================== ✅ RECEIVE MEDIA (AUDIO + VIDEO) ====================

    private void receiveMediaLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        long audioPackets = 0;
        long videoPackets = 0;

        System.out.println("🔊 Media receiver started");

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] data = packet.getData();
                int offset = packet.getOffset();
                int length = packet.getLength();

                if (length < 13) continue;

                byte type = data[offset];

                if (type == 0x01) {
                    // ✅ AUDIO PACKET
                    audioPackets++;
                    processAudioPacket(data, offset, length);

                    if (audioPackets % 500 == 0) {
                        String status = speakerEnabled.get() ? "PLAYING" : "MUTED";
                        System.out.println(String.format("📥 Audio received: %d [%s]",
                                audioPackets, status));
                    }

                } else if (type == 0x02) {
                    // ✅ VIDEO PACKET
                    videoPackets++;
                    processVideoPacket(data, offset, length);

                    if (videoPackets % 30 == 0) {
                        System.out.println(String.format("📥 Video frames received: %d",
                                videoPackets));
                    }
                }

            } catch (java.net.SocketTimeoutException e) {
                continue;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("⚠️ Error receiving media: " + e.getMessage());
                }
            }
        }

        System.out.println("🔊 Media receiver stopped");
        System.out.println("   Audio packets: " + audioPackets);
        System.out.println("   Video frames: " + videoPackets);
    }

    private void processAudioPacket(byte[] data, int offset, int length) {
        try {
            int audioLength = bytesToInt(data, offset + 9);
            int audioOffset = offset + 13;

            if (audioOffset + audioLength <= offset + length) {
                if (speakerEnabled.get() && speakers != null) {
                    speakers.write(data, audioOffset, audioLength);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error processing audio: " + e.getMessage());
        }
    }

    /**
     * ✅ Xử lý video packet nhận được
     */
    private void processVideoPacket(byte[] data, int offset, int length) {
        try {
            int videoLength = bytesToInt(data, offset + 9);
            int videoOffset = offset + 13;

            if (videoOffset + videoLength <= offset + length) {
                // Decode JPEG image
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        data, videoOffset, videoLength
                );
                BufferedImage frame = ImageIO.read(bais);

                if (frame != null && onVideoFrameReceived != null) {
                    onVideoFrameReceived.onFrameReceived(frame);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error processing video: " + e.getMessage());
        }
    }

    // ==================== CONTROLS ====================

    public void setMuted(boolean muted) {
        this.muted.set(muted);
        System.out.println(muted ? "🔇 Mic MUTED" : "🎤 Mic ACTIVE");
    }

    public void setSpeakerEnabled(boolean enabled) {
        this.speakerEnabled.set(enabled);
        System.out.println(enabled ? "🔊 Speaker ON" : "🔇 Speaker OFF");
    }

    public void setVideoEnabled(boolean enabled) {
        this.videoEnabled.set(enabled);
        System.out.println(enabled ? "📹 Video ON" : "📷 Video OFF");
    }

    public void switchCamera() {
        System.out.println("🔄 Switch camera");
    }

    // ==================== CALLBACKS ====================

    /**
     * ✅ Set callback nhận video frame
     */
    public void setOnVideoFrameReceived(VideoFrameCallback callback) {
        this.onVideoFrameReceived = callback;
    }

    @FunctionalInterface
    public interface VideoFrameCallback {
        void onFrameReceived(BufferedImage frame);
    }

    // ==================== UTILITIES ====================

    private byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (value >> (56 - i * 8));
        }
        return bytes;
    }

    private byte[] intToBytes(int value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (value >> (24 - i * 8));
        }
        return bytes;
    }

    private int bytesToInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
                ((bytes[offset + 1] & 0xFF) << 16) |
                ((bytes[offset + 2] & 0xFF) << 8) |
                (bytes[offset + 3] & 0xFF);
    }

    // Getters
    public boolean isMuted() { return muted.get(); }
    public boolean isSpeakerEnabled() { return speakerEnabled.get(); }
    public boolean isVideoEnabled() { return videoEnabled.get(); }
    public boolean isRunning() { return running.get(); }
}