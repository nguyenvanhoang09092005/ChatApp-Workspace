package server;

import config.ServerConfig;
import utils.ZeroTierMonitor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class ChatServer {

    private ServerSocket serverSocket;
    private boolean isRunning;
    private ExecutorService clientThreadPool;
    private ConcurrentHashMap<String, ClientHandler> connectedClients;
    private ZeroTierMonitor zeroTierMonitor;

    public ChatServer() {
        this.isRunning = false;
        this.connectedClients = new ConcurrentHashMap<>();
        // Sử dụng thread pool để quản lý nhiều client đồng thời
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.zeroTierMonitor = new ZeroTierMonitor();
    }

    /**
     * Khởi động server
     */
    public void start() {
        try {
            int port = ServerConfig.getServerPort();

            // In thông tin cấu hình
            ServerConfig.printConfig();

            // Khởi động ZeroTier monitor nếu được bật
            if (ServerConfig.isZeroTierEnabled()) {
                System.out.println("🔄 Đang khởi động ZeroTier monitor...");
                zeroTierMonitor.startMonitoring();

                String networkId = ServerConfig.getZeroTierNetworkId();
                System.out.println("✅ ZeroTier Network ID: " + networkId);

                // Hiển thị ZeroTier IP
                String ztIP = zeroTierMonitor.getZeroTierIP();
                if (ztIP != null) {


                    System.out.println("✅ Server ZeroTier IP: " + ztIP);
                } else {
                    System.out.println("⚠️ Không tìm thấy ZeroTier IP.");
                    System.out.println("ℹ️ Vui lòng kiểm tra:");
                    System.out.println("   - ZeroTier đã được cài đặt chưa");
                    System.out.println("   - Đã join network " + networkId + " chưa");
                    System.out.println("   - Thiết bị đã được authorized trên ZeroTier Central chưa");
                }
            }

            // Tạo server socket với backlog để hỗ trợ nhiều kết nối đồng thời
            serverSocket = new ServerSocket(port, 50); // backlog = 50
            isRunning = true;

            System.out.println("╔═══════════════════════════════════════════════╗");
            System.out.println("║  ✅ Server đã khởi động thành công!          ║");
            System.out.println("║  📡 Đang lắng nghe trên port: " + port + "              ║");
            System.out.println("║  👥 Tối đa: " + ServerConfig.getMaxClients() + " clients                    ║");
            System.out.println("║  🌐 IP local: " + getLocalIP() + "           ║");
            if (ServerConfig.isZeroTierEnabled()) {
                String ztIP = zeroTierMonitor.getZeroTierIP();
                if (ztIP != null) {
                    System.out.println("║  🔗 ZeroTier IP: " + ztIP + "         ║");
                }
            }
            System.out.println("╚═══════════════════════════════════════════════╝\n");

            // Lắng nghe và chấp nhận kết nối từ client
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    // Kiểm tra số lượng client đã kết nối
                    if (connectedClients.size() >= ServerConfig.getMaxClients()) {
                        System.err.println("⚠️ Đã đạt số lượng client tối đa. Từ chối kết nối từ: "
                                + clientSocket.getInetAddress().getHostAddress());
                        clientSocket.close();
                        continue;
                    }

                    // Cấu hình socket timeout
                    clientSocket.setSoTimeout(ServerConfig.getServerTimeout());
                    clientSocket.setKeepAlive(true);
                    clientSocket.setTcpNoDelay(true);

                    System.out.println("📥 Kết nối mới từ: " + clientSocket.getInetAddress().getHostAddress()
                            + ":" + clientSocket.getPort());

                    // Tạo handler cho client và chạy trong thread pool
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clientThreadPool.execute(handler);

                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("⚠️ Lỗi khi chấp nhận kết nối: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Lỗi khởi động server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Dừng server
     */
    public void stop() {
        System.out.println("\n⚠️ Đang dừng server...");
        isRunning = false;

        // Ngắt kết nối tất cả client
        for (ClientHandler handler : connectedClients.values()) {
            handler.disconnect();
        }
        connectedClients.clear();

        // Dừng thread pool
        clientThreadPool.shutdown();
        try {
            if (!clientThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientThreadPool.shutdownNow();
        }

        // Dừng ZeroTier monitor
        if (zeroTierMonitor != null) {
            zeroTierMonitor.stopMonitoring();
        }

        // Đóng server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("⚠️ Lỗi khi đóng server socket: " + e.getMessage());
        }

        System.out.println("✅ Server đã dừng");
    }

    /**
     * Thêm client đã kết nối
     */
    public void addClient(String userId, ClientHandler handler) {
        connectedClients.put(userId, handler);
        System.out.println("✅ Client đã kết nối - User ID: " + userId
                + " (Tổng: " + connectedClients.size() + ")");
    }

    /**
     * Xóa client đã ngắt kết nối
     */
    public void removeClient(String userId) {
        connectedClients.remove(userId);
        System.out.println("❌ Client đã ngắt kết nối - User ID: " + userId
                + " (Còn lại: " + connectedClients.size() + ")");
    }

    /**
     * Lấy handler của client
     */
    public ClientHandler getClientHandler(String userId) {
        return connectedClients.get(userId);
    }

    /**
     * Gửi tin nhắn đến một client cụ thể
     */
    public boolean sendToClient(String userId, String message) {
        ClientHandler handler = connectedClients.get(userId);
        if (handler != null) {
            return handler.sendMessage(message);
        }
        return false;
    }

    /**
     * Broadcast tin nhắn đến tất cả client
     */
    public void broadcastMessage(String message, String excludeUserId) {
        for (String userId : connectedClients.keySet()) {
            if (!userId.equals(excludeUserId)) {
                sendToClient(userId, message);
            }
        }
    }

    /**
     * Kiểm tra client có online không
     */
    public boolean isClientOnline(String userId) {
        return connectedClients.containsKey(userId);
    }

    /**
     * Lấy số lượng client đang kết nối
     */
    public int getConnectedClientsCount() {
        return connectedClients.size();
    }

    /**
     * Lấy danh sách client đang kết nối
     */
    public ConcurrentHashMap<String, ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    /**
     * Kiểm tra server có đang chạy không
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Lấy IP local của server
     */
    private String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}