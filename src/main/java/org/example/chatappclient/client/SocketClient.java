package org.example.chatappclient.client;

import org.example.chatappclient.client.config.AppConfig;
import org.example.chatappclient.client.protocol.Protocol;
import org.example.chatappclient.client.utils.network.ZeroTierHelper;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SocketClient {
    private static SocketClient instance;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean isConnected;
    private boolean isRunning;

    private Thread listenerThread;
    private ConcurrentHashMap<String, Consumer<String>> responseHandlers;
    private Consumer<String> messageCallback;

    private final AppConfig config;

    private SocketClient() {
        this.config = AppConfig.getInstance();
        this.responseHandlers = new ConcurrentHashMap<>();
        this.isConnected = false;
        this.isRunning = false;
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    /**
     * Kết nối đến server qua ZeroTier
     * Hỗ trợ kết nối qua IP ZeroTier hoặc IP thông thường
     */
    public boolean connect() {
        String host = config.getServerHost();
        int port = config.getServerPort();

        try {
            // Kiểm tra ZeroTier nếu được bật
            if (config.isUseZeroTier()) {
                System.out.println("🔍 Kiểm tra ZeroTier...");



                // Kiểm tra ZeroTier đã cài đặt chưa
                if (!ZeroTierHelper.isZeroTierInstalled()) {
                    System.out.println("⚠️ Không tìm thấy zerotier-cli trong PATH");
                    System.out.println("ℹ️ Nếu ZeroTier đã cài đặt, bỏ qua cảnh báo này");
                    System.out.println("ℹ️ Đang thử kết nối trực tiếp đến server...");
                    // Không return false, vẫn tiếp tục kết nối
                }

                // Kiểm tra đã join network chưa
                String networkId = config.getZeroTierNetworkId();
                if (!ZeroTierHelper.isJoinedNetwork(networkId)) {
                    System.out.println("⚠️ Chưa join ZeroTier network: " + networkId);
                    System.out.println("ℹ️ Đang thử kết nối trực tiếp...");
                    // Không auto-join, để user tự join
                } else {
                    System.out.println("✅ Đã join ZeroTier network: " + networkId);

                    // Hiển thị thông tin ZeroTier
                    String ztIP = ZeroTierHelper.getZeroTierIP();
                    if (ztIP != null) {
                        System.out.println("✅ ZeroTier IP của bạn: " + ztIP);
                    }
                }
            }

            // Thực hiện kết nối socket
            System.out.println("🔌 Đang kết nối đến " + host + ":" + port + " ...");
            socket = new Socket(host, port);
            //socket.setSoTimeout(config.getReadTimeout());
            socket.setKeepAlive(true); // Giữ kết nối sống
            socket.setTcpNoDelay(true); // Tắt Nagle's algorithm để giảm độ trễ

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            isConnected = true;
            isRunning = true;

            startListening();
            System.out.println("✅ Kết nối đến server thành công!");

            // Hiển thị thông tin kết nối
            System.out.println("📍 Server: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            System.out.println("📍 Client: " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());

            return true;

        } catch (IOException e) {
            System.err.println("❌ Kết nối thất bại: " + e.getMessage());
            System.err.println("ℹ️ Vui lòng kiểm tra:");
            System.err.println("   - Server đã được khởi động chưa");
            System.err.println("   - IP server trong config có đúng không");
            System.err.println("   - Cả client và server đã join cùng ZeroTier network chưa");
            System.err.println("   - Thiết bị đã được authorized trên ZeroTier Central chưa");
            isConnected = false;
            return false;
        }
    }

    /**
     * Bắt đầu lắng nghe tin nhắn từ server
     */
    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String message;
                while (isRunning && (message = reader.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (SocketException e) {
                if (isRunning) {
                    System.err.println("⚠️ Socket đã đóng: " + e.getMessage());
                    handleDisconnection();
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("⚠️ Lỗi đọc dữ liệu từ server: " + e.getMessage());
                    handleDisconnection();
                }
            }
        }, "SocketListenerThread");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Xử lý tin nhắn nhận được từ server
     */
    /**
     * Xử lý tin nhắn nhận được từ server
     */
    private void handleMessage(String message) {
        System.out.println("📩 Nhận: " + message);

        // Thử tìm handler cho command cụ thể
        String command = Protocol.getCommand(message);

        Consumer<String> handler = responseHandlers.get(command);
        if (handler != null) {
            handler.accept(message);
            return;
        }

        // Nếu không tìm thấy handler cho command, thử tìm trong tất cả handlers
        // (cho trường hợp dùng unique key)
        if (!responseHandlers.isEmpty()) {
            for (String key : responseHandlers.keySet()) {
                if (key.startsWith("REQ_")) {
                    responseHandlers.get(key).accept(message);
                    return;
                }
            }
        }

        if (messageCallback != null) {
            messageCallback.accept(message);
        }
    }

    public String sendRequest(String request, long timeoutMillis) {
        if (!sendMessage(request)) {
            return null;
        }

        final String[] response = {null};
        final Object lock = new Object();

        // Tạo unique key cho request này
        String requestKey = "REQ_" + System.currentTimeMillis() + "_" + request.hashCode();

        // Đăng ký handler tạm thời
        responseHandlers.put(requestKey, msg -> {
            synchronized (lock) {
                response[0] = msg;
                lock.notify();
            }
        });

        synchronized (lock) {
            try {
                lock.wait(timeoutMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        responseHandlers.remove(requestKey);
        return response[0];
    }

    /**
     * Gửi tin nhắn đến server
     */
    public boolean sendMessage(String message) {
        if (!isConnected || writer == null) {
            System.err.println("❌ Chưa kết nối đến server");
            return false;
        }

        try {
            writer.println(message);
            System.out.println("📤 Gửi: " + message);
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khi gửi tin nhắn: " + e.getMessage());
            return false;
        }
    }



    /**
     * Đăng ký handler xử lý cho một command cụ thể
     */
    public void registerHandler(String command, Consumer<String> handler) {
        responseHandlers.put(command, handler);
    }

    /**
     * Hủy đăng ký handler
     */
    public void unregisterHandler(String command) {
        responseHandlers.remove(command);
    }

    /**
     * Đặt callback xử lý tin nhắn chung
     */
    public void setMessageCallback(Consumer<String> callback) {
        this.messageCallback = callback;
    }

    /**
     * Xử lý khi mất kết nối
     */
    private void handleDisconnection() {
        isConnected = false;
        if (messageCallback != null) {
            messageCallback.accept("DISCONNECTED");
        }

        if (config.isAutoReconnect()) {
            attemptReconnect();
        }
    }

    /**
     * Thử kết nối lại tự động
     */
    private void attemptReconnect() {
        new Thread(() -> {
            int attempts = 0;
            int maxAttempts = config.getMaxReconnectAttempts();
            int delay = config.getReconnectDelay();

            while (attempts < maxAttempts && !isConnected) {
                attempts++;
                System.out.println("🔄 Đang thử kết nối lại lần " + attempts + "/" + maxAttempts);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (connect()) {
                    System.out.println("✅ Kết nối lại thành công!");
                    if (messageCallback != null) {
                        messageCallback.accept("RECONNECTED");
                    }
                    break;
                }
            }

            if (!isConnected) {
                System.err.println("❌ Không thể kết nối lại sau " + maxAttempts + " lần thử");
            }
        }, "ReconnectThread").start();
    }

    /**
     * Ngắt kết nối khỏi server
     */
    public void disconnect() {
        isRunning = false;
        isConnected = false;

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
            if (listenerThread != null) listenerThread.interrupt();

            System.out.println("🔌 Đã ngắt kết nối khỏi server");
        } catch (IOException e) {
            System.err.println("⚠️ Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra trạng thái kết nối
     */
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    public Socket getSocket() {
        return socket;
    }
}