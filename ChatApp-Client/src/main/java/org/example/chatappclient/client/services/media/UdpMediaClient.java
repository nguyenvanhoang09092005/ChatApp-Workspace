package org.example.chatappclient.client.services.media;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client Media UDP - TỐI ƯU CHO REAL-TIME, GIẢM DELAY
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
    private final AtomicBoolean videoEnabled;

    // BUFFER SIZE NHỎ HƠN = DELAY THẤP HƠN
    private static final int AUDIO_BUFFER_SIZE = 1024; // Giảm từ 4096 xuống 1024
    private static final int SPEAKER_BUFFER_SIZE = 2048; // Buffer loa nhỏ để giảm latency
    private static final int MAX_PACKET_SIZE = 65507;

    // Sampling rate thấp hơn = ít data hơn = nhanh hơn
    private static final float SAMPLE_RATE = 16000.0f; // 16kHz (tốt cho voice)

    public UdpMediaClient(String serverIP, int serverPort, boolean isVideo) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.isVideo = isVideo;
        this.executor = Executors.newFixedThreadPool(4); // Tăng thread pool
        this.running = new AtomicBoolean(false);
        this.muted = new AtomicBoolean(false);
        this.videoEnabled = new AtomicBoolean(isVideo);
        setupAudioFormat();
    }

    // ==================== CÀI ĐẶT ÂM THANH TỐI ƯU ====================
    private void setupAudioFormat() {
        // PCM 16-bit, 16kHz, Mono - Tối ưu cho voice chat
        audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,  // 16kHz
                16,           // 16-bit
                1,            // Mono
                2,            // Frame size
                SAMPLE_RATE,
                false         // Little endian
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

            // QUAN TRỌNG: Buffer nhỏ = delay thấp
            microphone.open(audioFormat, AUDIO_BUFFER_SIZE);
            microphone.start();

            System.out.println("✅ Microphone khởi tạo (buffer: " + AUDIO_BUFFER_SIZE + " bytes)");
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

            // Buffer nhỏ cho loa để giảm delay phát
            speakers.open(audioFormat, SPEAKER_BUFFER_SIZE);
            speakers.start();

            System.out.println("✅ Loa khởi tạo (buffer: " + SPEAKER_BUFFER_SIZE + " bytes)");
            return true;
        } catch (LineUnavailableException e) {
            System.err.println("❌ Không thể mở loa: " + e.getMessage());
            return false;
        }
    }

    // ==================== KHỞI ĐỘNG / DỪNG ====================
    public void start() throws Exception {
        if (running.get()) return;

        // Tạo socket UDP với timeout ngắn
        socket = new DatagramSocket();
        socket.setSoTimeout(100); // 100ms timeout
        socket.setReceiveBufferSize(AUDIO_BUFFER_SIZE * 4);
        socket.setSendBufferSize(AUDIO_BUFFER_SIZE * 4);

        serverAddress = InetAddress.getByName(serverIP);

        // Khởi tạo thiết bị âm thanh
        if (!initializeMicrophone() || !initializeSpeakers()) {
            throw new Exception("Không thể khởi tạo thiết bị âm thanh");
        }

        running.set(true);

        // Khởi chạy các luồng với độ ưu tiên cao
        Thread sendThread = new Thread(this::sendAudioLoop, "AudioSender");
        sendThread.setPriority(Thread.MAX_PRIORITY);
        executor.submit(sendThread);

        Thread receiveThread = new Thread(this::receiveAudioLoop, "AudioReceiver");
        receiveThread.setPriority(Thread.MAX_PRIORITY);
        executor.submit(receiveThread);

        if (isVideo && videoEnabled.get()) {
            executor.submit(this::sendVideoLoop);
        }

        System.out.println("✅ UDP Media Client đã khởi động (Real-time mode)");
    }

    public void stop() {
        if (!running.get()) return;

        running.set(false);

        // Đóng các thiết bị âm thanh
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }

        if (speakers != null) {
            speakers.drain(); // Đợi phát hết buffer
            speakers.stop();
            speakers.close();
        }

        // Đóng socket
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        // Tắt executor
        executor.shutdownNow();

        System.out.println("✅ UDP Media Client đã dừng");
    }

    // ==================== GỬI ÂM THANH (REAL-TIME) ====================
    private void sendAudioLoop() {
        byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
        long packetsSent = 0;
        long startTime = System.currentTimeMillis();

        System.out.println("🎤 Bắt đầu gửi audio...");

        while (running.get()) {
            try {
                if (muted.get()) {
                    Thread.sleep(10);
                    continue;
                }

                // Đọc audio từ mic
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    // Tạo packet và GỬI NGAY LẬP TỨC
                    byte[] packet = createAudioPacket(buffer, bytesRead);
                    DatagramPacket dgPacket = new DatagramPacket(
                            packet, packet.length, serverAddress, serverPort
                    );
                    socket.send(dgPacket);

                    packetsSent++;

                    // Log mỗi 100 packets
                    if (packetsSent % 100 == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double rate = (packetsSent * 1000.0) / elapsed;
                        System.out.println(String.format("📤 Sent %d packets (%.1f pkt/s)",
                                packetsSent, rate));
                    }
                }

            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("⚠️ Lỗi gửi audio: " + e.getMessage());
                }
            }
        }

        System.out.println("🎤 Dừng gửi audio. Total: " + packetsSent + " packets");
    }

    private byte[] createAudioPacket(byte[] audioData, int length) {
        // Header tối giản: [TYPE(1)][TIMESTAMP(8)][LENGTH(4)][DATA]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(0x01); // AUDIO
            baos.write(longToBytes(System.currentTimeMillis()));
            baos.write(intToBytes(length));
            baos.write(audioData, 0, length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    // ==================== NHẬN ÂM THANH (REAL-TIME) ====================
    private void receiveAudioLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        long packetsReceived = 0;
        long startTime = System.currentTimeMillis();

        System.out.println("🔊 Bắt đầu nhận audio...");

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                packetsReceived++;

                // Xử lý packet NGAY LẬP TỨC
                byte[] data = packet.getData();
                int offset = packet.getOffset();
                int length = packet.getLength();

                if (length < 13) continue; // Invalid packet

                byte type = data[offset];
                if (type == 0x01) { // AUDIO
                    int audioLength = bytesToInt(data, offset + 9);
                    int audioOffset = offset + 13;

                    if (audioOffset + audioLength <= offset + length) {
                        // PHÁT NGAY không buffer thêm
                        speakers.write(data, audioOffset, audioLength);
                    }
                }

                // Log mỗi 100 packets
                if (packetsReceived % 100 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double rate = (packetsReceived * 1000.0) / elapsed;
                    System.out.println(String.format("📥 Received %d packets (%.1f pkt/s)",
                            packetsReceived, rate));
                }

            } catch (java.net.SocketTimeoutException e) {
                // Timeout bình thường, tiếp tục
                continue;
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("⚠️ Lỗi nhận audio: " + e.getMessage());
                }
            }
        }

        System.out.println("🔊 Dừng nhận audio. Total: " + packetsReceived + " packets");
    }

    // ==================== GỬI VIDEO (Placeholder) ====================
    private void sendVideoLoop() {
        System.out.println("📹 Video loop chưa triển khai");
    }

    // ==================== ĐIỀU KHIỂN ====================
    public void setMuted(boolean muted) {
        this.muted.set(muted);
        System.out.println(muted ? "🔇 Đã TẮT tiếng" : "🔊 Đã BẬT tiếng");
    }

    public void setVideoEnabled(boolean enabled) {
        this.videoEnabled.set(enabled);
        System.out.println(enabled ? "📹 Video BẬT" : "📷 Video TẮT");
    }

    public void switchCamera() {
        System.out.println("🔄 Chuyển camera");
    }

    // ==================== HÀM HỖ TRỢ ====================
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
}