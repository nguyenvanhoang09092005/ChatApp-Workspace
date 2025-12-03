// ==================== ConversationService.java ====================
package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.protocol.Protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * Service xử lý các thao tác với Conversation
 */
public class ConversationService {

    private static volatile ConversationService instance;
    private final SocketClient socketClient;

    // Callbacks
    private BiConsumer<String, Message> onNewMessage;
    private TriConsumer<String, Boolean, String> onUserOnlineStatus;

    private ConversationService() {
        socketClient = SocketClient.getInstance();
        setupRealtimeHandlers();
    }

    public static ConversationService getInstance() {
        if (instance == null) {
            synchronized (ConversationService.class) {
                if (instance == null) {
                    instance = new ConversationService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD ====================

    /**
     * Lấy tất cả conversations của user
     */
    public List<Conversation> getAllConversations(String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_GET_ALL, userId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        String data = Protocol.getData(response);
        return parseConversations(data);
    }

    /**
     * Lấy conversation theo ID
     */
    public Conversation getConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_GET_BY_ID, conversationId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    /**
     * Tìm hoặc tạo conversation private với user
     */
    public Conversation findOrCreatePrivateChat(String userId, String targetQuery) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_CREATE, userId, targetQuery, "private");
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    /**
     * Tạo nhóm chat
     */
    public Conversation createGroup(String userId, String groupName, List<String> memberIds) throws Exception {
        String members = String.join(",", memberIds);
        String request = Protocol.buildRequest(Protocol.CONVERSATION_CREATE_GROUP, userId, groupName, members);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    /**
     * Xóa conversation
     */
    public void deleteConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_DELETE, conversationId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    // ==================== ACTIONS ====================

    /**
     * Đánh dấu đã đọc
     */
    public void markAsRead(String conversationId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_MARK_READ, conversationId, userId);
        socketClient.sendMessage(request);
    }

    /**
     * Tắt/bật thông báo
     */
    public void muteConversation(String conversationId, boolean mute) throws Exception {
        String command = mute ? Protocol.CONVERSATION_MUTE : Protocol.CONVERSATION_UNMUTE;
        String request = Protocol.buildRequest(command, conversationId);
        socketClient.sendMessage(request);
    }

    /**
     * Ghim conversation
     */
    public void pinConversation(String conversationId, boolean pin) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_PIN, conversationId, String.valueOf(pin));
        socketClient.sendMessage(request);
    }

    /**
     * Lưu trữ conversation
     */
    public void archiveConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_ARCHIVE, conversationId);
        socketClient.sendMessage(request);
    }

    // ==================== PARSING ====================

    /**
     * Parse danh sách conversations từ data
     */
    private List<Conversation> parseConversations(String data) {
        List<Conversation> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;

        for (String convData : Protocol.parseDataList(data)) {
            Conversation conv = parseConversation(convData);
            if (conv != null) list.add(conv);
        }
        return list;
    }

    /**
     * Parse một conversation từ data string
     * Format: conversationId,type,name,avatarUrl,lastMessage,lastMessageTime,unreadCount,memberCount,isOnline,lastSeenTime
     */
    private Conversation parseConversation(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] f = Protocol.parseFields(data);
        if (f.length < 4) return null;

        Conversation c = new Conversation();

        // Basic info
        c.setConversationId(f[0]);
        c.setType(f[1]);
        c.setName(f[2]);
        c.setAvatarUrl(f[3]);

        // Last message
        if (f.length > 4 && f[4] != null && !f[4].isEmpty()) {
            c.setLastMessage(f[4]);
        }

        // Last message time
        if (f.length > 5 && f[5] != null && !f[5].isEmpty()) {
            try {
                LocalDateTime time = LocalDateTime.parse(f[5], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                c.setLastMessageTime(time);
            } catch (Exception e) {
                System.err.println("Error parsing last message time: " + e.getMessage());
            }
        }

        // Unread count
        if (f.length > 6) {
            c.setUnreadCount(parseInt(f[6]));
        }

        // Skip memberCount (index 7) - sẽ tính từ memberIds

        // Online status
        if (f.length > 8) {
            c.setActive(Boolean.parseBoolean(f[8]));
        }

        // Last seen time
        if (f.length > 9 && f[9] != null && !f[9].isEmpty() && !f[9].equals("null")) {
            try {
                LocalDateTime lastSeen = LocalDateTime.parse(f[9], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                c.setLastSeenTime(lastSeen);
            } catch (Exception e) {
                System.err.println("Error parsing last seen time: " + e.getMessage());
            }
        }

        // **SỬA: Split theo ; thay vì ,**
        if (f.length > 10 && f[10] != null && !f[10].isEmpty()) {
            String[] memberIds = f[10].split(";"); // DÙNG ; thay vì ,
            List<String> members = new ArrayList<>();
            for (String id : memberIds) {
                if (id != null && !id.trim().isEmpty()) {
                    members.add(id.trim());
                }
            }
            c.setMemberIds(members);
            System.out.println("  → Parsed memberIds: " + members + " (count: " + members.size() + ")");
        } else {
            System.out.println("  ⚠️ No memberIds in conversation data!");
        }

        return c;
    }

    /**
     * Parse integer an toàn
     */
    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== REALTIME SETUP ====================

    /**
     * Setup các handler nhận real-time events từ server
     */
//    private void setupRealtimeHandlers() {
//        // Handler cho USER_STATUS_CHANGED
//        socketClient.registerHandler(Protocol.USER_STATUS_CHANGED, message -> {
//            handleUserStatusChange(message);
//        });
//
//        // Handler cho MESSAGE_NEW (nếu cần)
//        socketClient.registerHandler(Protocol.MESSAGE_NEW, message -> {
//            handleNewMessage(message);
//        });
//    }
//
//    /**
//     * Xử lý thông báo user status change từ server
//     * Format: USER_STATUS_CHANGED|||userId|||isOnline|||statusText|||lastSeen
//     */
//    private void handleUserStatusChange(String message) {
//        try {
//            String[] parts = message.split(Pattern.quote(Protocol.DELIMITER));
//            if (parts.length >= 5) {
//                String userId = parts[1];
//                boolean isOnline = Boolean.parseBoolean(parts[2]);
//                // String statusText = parts[3]; // Không cần dùng
//                String lastSeen = parts[4];
//
//                // Gọi callback nếu đã được đăng ký
//                if (onUserOnlineStatus != null) {
//                    onUserOnlineStatus.accept(userId, isOnline, lastSeen);
//                }
//
//                System.out.println("→ User status updated: " + userId + " - " +
//                        (isOnline ? "ONLINE" : "OFFLINE"));
//            }
//        } catch (Exception e) {
//            System.err.println("⚠️ Error handling user status change: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    private void setupRealtimeHandlers() {
        System.out.println("→ Đang thiết lập các handler realtime...");

        // Handler khi user thay đổi trạng thái online/offline
        socketClient.registerHandler(Protocol.USER_STATUS_CHANGED, message -> {
            System.out.println("→ ConversationService nhận USER_STATUS_CHANGED");
            handleUserStatusChange(message);
        });

        // Handler khi có tin nhắn mới
        socketClient.registerHandler(Protocol.MESSAGE_RECEIVE, message -> {
            System.out.println("→ ConversationService nhận MESSAGE_RECEIVE");
            handleNewMessage(message);
        });

        System.out.println("✅ Đã đăng ký các handler realtime");
    }

    /**
     * Xử lý sự kiện thay đổi trạng thái online của user từ server
     * Format dữ liệu gửi về:
     *    userId|||isOnline|||statusText|||lastSeen
     */
    private void handleUserStatusChange(String data) {
        try {
            System.out.println("→ handleUserStatusChange dữ liệu: " + data);

            // Tách dữ liệu theo "|||"
            String[] parts = data.split("\\|\\|\\|");

            if (parts.length >= 4) {
                String userId = parts[0];
                boolean isOnline = Boolean.parseBoolean(parts[1]);
                String statusText = parts[2]; // Ví dụ: "Đang hoạt động", "Vừa truy cập..."
                String lastSeen = parts[3];   // Ví dụ: "2025-12-03T08:20:15"

                System.out.println("  → Đã parse - UserID: " + userId + ", Online: " + isOnline);

                // Gọi callback nếu đã đăng ký
                if (onUserOnlineStatus != null) {
                    // Nếu callback có 3 tham số → bạn cần BiConsumer3 (tự định nghĩa)
                    onUserOnlineStatus.accept(userId, isOnline, lastSeen);
                    System.out.println("  ✅ Đã gọi callback cập nhật trạng thái online");
                } else {
                    System.out.println("  ⚠️ Chưa đăng ký callback xử lý trạng thái online");
                }
            } else {
                System.err.println("  ⚠️ Dữ liệu không đúng định dạng, chỉ có " + parts.length + " phần");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khi xử lý sự kiện user online: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Xử lý tin nhắn mới từ server
     */
    private void handleNewMessage(String message) {
        try {
            // Parse message và gọi callback
            // TODO: Implement based on your MESSAGE_NEW format
            if (onNewMessage != null) {
                // Message msg = parseMessage(message);
                // String conversationId = extractConversationId(message);
                // onNewMessage.accept(conversationId, msg);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error handling new message: " + e.getMessage());
        }
    }

    // ==================== CALLBACKS ====================

    /**
     * Đăng ký callback khi có tin nhắn mới
     */
    public void setOnNewMessage(BiConsumer<String, Message> callback) {
        this.onNewMessage = callback;
    }

    /**
     * Đăng ký callback khi có user thay đổi trạng thái online
     * @param callback (userId, isOnline, lastSeenStr)
     */
    public void setOnUserOnlineStatus(TriConsumer<String, Boolean, String> callback) {
        this.onUserOnlineStatus = callback;
        System.out.println("✅ Đã đăng ký callback cho user status changes");
    }

    // ==================== FUNCTIONAL INTERFACE ====================

    /**
     * Functional interface cho callback với 3 tham số
     */
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}