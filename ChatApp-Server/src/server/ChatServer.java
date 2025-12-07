package server;

import config.ServerConfig;
import utils.ZeroTierMonitor;
import server.handlers.GroupChatHandler;
import protocol.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

public class ChatServer {

    private ServerSocket serverSocket;
    private boolean isRunning;
    private ExecutorService clientThreadPool;
    private ConcurrentHashMap<String, ClientHandler> connectedClients;
    private ZeroTierMonitor zeroTierMonitor;
    private ConcurrentHashMap<String, RequestHandler> requestHandlers;
    private GroupChatHandler groupChatHandler;

    public ChatServer() {
        this.isRunning = false;
        this.connectedClients = new ConcurrentHashMap<>();
        this.requestHandlers = new ConcurrentHashMap<>();
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.zeroTierMonitor = new ZeroTierMonitor();
        this.groupChatHandler = new GroupChatHandler();

        // Thiết lập các handlers
        setupHandlers();
    }

    /**
     * Thiết lập các request handlers
     */
    private void setupHandlers() {
        System.out.println("🔄 Đang thiết lập request handlers...");

        // Đăng ký GroupChatHandler cho các command group chat
        requestHandlers.put(Protocol.GROUP_CREATE, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_UPDATE, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_DELETE, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_GET_INFO, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_GET_MEMBERS, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_ADD_MEMBER, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_REMOVE_MEMBER, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_LEAVE, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_CHANGE_ROLE, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_UPDATE_AVATAR, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_SEARCH, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_MESSAGE_SEND, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_MESSAGE_GET_HISTORY, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_MESSAGE_DELETE, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_MESSAGE_EDIT, groupChatHandler);
        requestHandlers.put(Protocol.GROUP_MESSAGE_MARK_READ, groupChatHandler);

        System.out.println("✅ Đã đăng ký " + requestHandlers.size() + " handlers cho Group Chat");
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
            System.out.println("║  🚀 Group Chat: ĐÃ KÍCH HOẠT                  ║");
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
     * Lấy request handler cho command cụ thể
     */
    public RequestHandler getRequestHandler(String command) {
        return requestHandlers.get(command);
    }

    /**
     * Xử lý request từ client
     */
    public String processRequest(String request, ClientHandler client) {
        String command = Protocol.getCommand(request);
        RequestHandler handler = getRequestHandler(command);

        if (handler != null) {
            return handler.handleRequest(request, client);
        } else {
            // Nếu không tìm thấy handler, có thể là command khác (auth, message, etc.)
            // Các handlers khác sẽ được xử lý trong ClientHandler
            return Protocol.buildErrorResponse(
                    Protocol.INVALID_REQUEST,
                    "No handler for command: " + command
            );
        }
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
     * Broadcast tin nhắn đến các thành viên trong group
     */
    public void broadcastToGroupMembers(String groupId, String message) {
        // Phương thức này cần được GroupChatHandler gọi
        // Implementation sẽ được thêm sau khi có GroupMemberDAO
        System.out.println("📢 Broadcasting to group " + groupId + ": " + message);

        // TODO: Lấy danh sách thành viên từ GroupMemberDAO và gửi tin nhắn
        // Ví dụ:
        // List<String> memberIds = groupMemberDAO.getMemberIds(groupId);
        // for (String memberId : memberIds) {
        //     sendToClient(memberId, message);
        // }
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

    public void broadcastUserStatus(String userId, boolean isOnline) {
        if (userId == null) {
            return;
        }

        System.out.println("→ Đang phát trạng thái cho người dùng " + userId + ": " +
                (isOnline ? "TRỰC TUYẾN" : "NGOẠI TUYẾN"));

        // Lấy thông tin người dùng để đưa vào bản tin broadcast
        database.dao.UserDAO userDAO = new database.dao.UserDAO();
        models.User user = userDAO.findById(userId);
        if (user == null) {
            System.err.println("⚠️ Không tìm thấy người dùng để phát trạng thái: " + userId);
            return;
        }

        // Tạo thông điệp trạng thái
        // Định dạng: USER_STATUS_CHANGED|||userId|||isOnline|||statusText|||lastSeen
        String statusMessage =
                Protocol.USER_STATUS_CHANGED + Protocol.DELIMITER +
                        userId + Protocol.DELIMITER +
                        isOnline + Protocol.DELIMITER +
                        user.getStatusText() + Protocol.DELIMITER +
                        (user.getLastSeen() != null ? user.getLastSeen().toString() : "");


        // Gửi tới tất cả client đang kết nối, ngoại trừ chính người dùng đó
        int sentCount = 0;
        synchronized (connectedClients) {
            for (Map.Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
                String clientId = entry.getKey();
                ClientHandler handler = entry.getValue();

                // Không gửi cập nhật trạng thái cho chính người dùng
                if (!clientId.equals(userId) && handler.isConnected()) {
                    if (handler.sendMessage(statusMessage)) {
                        sentCount++;
                    }
                }
            }
        }

        System.out.println("✅ Đã gửi trạng thái tới " + sentCount + " client");
    }
}