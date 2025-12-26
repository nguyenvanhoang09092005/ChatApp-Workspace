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
import java.util.function.Consumer;

/**
 * Service xử lý các thao tác với Conversation - WITH AUTO MESSAGE UPDATES
 */
public class ConversationService {

    private static volatile ConversationService instance;
    private final SocketClient socketClient;
    private Consumer<String> onConversationRestored;

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

    public List<Conversation> getAllConversations(String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_GET_ALL, userId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        String data = Protocol.getData(response);
        return parseConversations(data);
    }

    public Conversation getConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_GET_BY_ID, conversationId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    public Conversation findOrCreatePrivateChat(String userId, String targetQuery) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_CREATE, userId, targetQuery, "private");
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    public Conversation createGroup(String userId, String groupName, List<String> memberIds) throws Exception {
        String members = String.join(",", memberIds);
        String request = Protocol.buildRequest(Protocol.CONVERSATION_CREATE_GROUP, userId, groupName, members);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    public void deleteConversationForCurrentUser(String conversationId) throws Exception {
        String userId = AuthService.getInstance().getCurrentUser().getUserId();
        String request = Protocol.buildRequest(
                Protocol.CONVERSATION_DELETE_FOR_USER,
                conversationId,
                userId
        );

        String response = socketClient.sendRequest(request, 10000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }

        if (!Protocol.isSuccess(response)) {
            throw new Exception(Protocol.getErrorMessage(response));
        }

        System.out.println("✅ Conversation deleted for user: " + userId);
    }

    public void deleteConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_DELETE, conversationId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    // ==================== ACTIONS ====================

    public void markAsRead(String conversationId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_MARK_READ, conversationId, userId);
        socketClient.sendMessage(request);
    }

    public void muteConversation(String conversationId, boolean mute) throws Exception {
        String command = mute ? Protocol.CONVERSATION_MUTE : Protocol.CONVERSATION_UNMUTE;
        String request = Protocol.buildRequest(command, conversationId);
        socketClient.sendMessage(request);
    }

    public void pinConversation(String conversationId, boolean pin) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_PIN, conversationId, String.valueOf(pin));
        socketClient.sendMessage(request);
    }

    public void archiveConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_ARCHIVE, conversationId);
        socketClient.sendMessage(request);
    }

    // ==================== PARSING ====================

    private List<Conversation> parseConversations(String data) {
        List<Conversation> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;

        for (String convData : Protocol.parseDataList(data)) {
            Conversation conv = parseConversation(convData);
            if (conv != null) list.add(conv);
        }
        return list;
    }

    private Conversation parseConversation(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] f = Protocol.parseFields(data);
        if (f.length < 4) return null;

        Conversation c = new Conversation();

        c.setConversationId(f[0]);
        c.setType(f[1]);
        c.setName(f[2]);
        c.setAvatarUrl(f[3]);

        if (f.length > 4 && f[4] != null && !f[4].isEmpty()) {
            c.setLastMessage(f[4]);
        }

        if (f.length > 5 && f[5] != null && !f[5].isEmpty()) {
            try {
                LocalDateTime time = LocalDateTime.parse(f[5], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                c.setLastMessageTime(time);
            } catch (Exception e) {
                System.err.println("Error parsing last message time: " + e.getMessage());
            }
        }

        if (f.length > 6) {
            c.setUnreadCount(parseInt(f[6]));
        }

        if (f.length > 8) {
            c.setActive(Boolean.parseBoolean(f[8]));
        }

        if (f.length > 9 && f[9] != null && !f[9].isEmpty() && !f[9].equals("null")) {
            try {
                LocalDateTime lastSeen = LocalDateTime.parse(f[9], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                c.setLastSeenTime(lastSeen);
            } catch (Exception e) {
                System.err.println("Error parsing last seen time: " + e.getMessage());
            }
        }

        if (f.length > 10 && f[10] != null && !f[10].isEmpty()) {
            String[] memberIds = f[10].split(";");
            List<String> members = new ArrayList<>();
            for (String id : memberIds) {
                if (id != null && !id.trim().isEmpty()) {
                    members.add(id.trim());
                }
            }
            c.setMemberIds(members);
        }

        return c;
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    public void setOnConversationRestored(Consumer<String> callback) {
        this.onConversationRestored = callback;
    }

    public void handleConversationRestored(String message) {
        if (onConversationRestored != null) {
            onConversationRestored.accept(message);
        }
    }

    // ==================== REALTIME SETUP ====================

    /**
     * ✅ FIXED: Setup handlers và TỰ ĐỘNG xử lý tin nhắn mới
     */
    private void setupRealtimeHandlers() {
        System.out.println("→ Thiết lập các handler realtime trong ConversationService...");

        // ✅ Handler cho USER_STATUS_CHANGED
        socketClient.registerHandler(Protocol.USER_STATUS_CHANGED, message -> {
            System.out.println("→ ConversationService nhận USER_STATUS_CHANGED");
            handleUserStatusChange(message);
        });

        // ✅ Handler cho MESSAGE_RECEIVE - TỰ ĐỘNG XỬ LÝ
        socketClient.registerHandler(Protocol.MESSAGE_RECEIVE, message -> {
            System.out.println("→ ConversationService nhận MESSAGE_RECEIVE: " + message);
            handleNewMessage(message);
        });

        // ✅ Handler cho CONVERSATION_RESTORED
        socketClient.registerHandler(Protocol.CONVERSATION_RESTORED, message -> {
            System.out.println("→ ConversationService nhận CONVERSATION_RESTORED");
            handleConversationRestored(message);
        });

        System.out.println("✅ Đã đăng ký các handler realtime trong ConversationService");
    }

    /**
     * ✅ FIXED: Xử lý tin nhắn mới - TỰ ĐỘNG GỌI CALLBACK
     */
    private void handleNewMessage(String data) {
        try {
            System.out.println("→ ConversationService đang parse MESSAGE_RECEIVE: " + data);

            String[] parts = data.split("\\|\\|\\|");

            if (parts.length < 5) {
                System.err.println("❌ Invalid MESSAGE_RECEIVE format, parts: " + parts.length);
                return;
            }

            // Parse message
            Message message = new Message();
            message.setMessageId(parts[1]);
            message.setConversationId(parts[2]);
            message.setSenderId(parts[3]);
            message.setContent(parts[4]);

            if (parts.length > 5 && parts[5] != null && !parts[5].isEmpty()) {
                message.setMessageType(parts[5]);
            } else {
                message.setMessageType("text");
            }

            if (parts.length > 6 && parts[6] != null && !parts[6].isEmpty()) {
                message.setMediaUrl(parts[6]);
            }

            if (parts.length > 7 && parts[7] != null && !parts[7].isEmpty()) {
                message.setSenderName(parts[7]);
            }

            if (parts.length > 8 && parts[8] != null && !parts[8].isEmpty()) {
                message.setSenderAvatar(parts[8]);
            }

            if (parts.length > 9 && parts[9] != null && !parts[9].isEmpty()) {
                message.setFileName(parts[9]);
            }

            if (parts.length > 10) {
                try {
                    message.setFileSize(Long.parseLong(parts[10]));
                } catch (Exception ignored) {}
            }

            message.setTimestamp(LocalDateTime.now());

            System.out.println("✅ ConversationService parsed message:");
            System.out.println("   ID: " + message.getMessageId());
            System.out.println("   ConversationID: " + message.getConversationId());
            System.out.println("   Content: " + message.getContent());

            // ✅ TỰ ĐỘNG GỌI CALLBACK
            if (onNewMessage != null) {
                System.out.println("→ Calling onNewMessage callback từ ConversationService");
                onNewMessage.accept(message.getConversationId(), message);
                System.out.println("  ✅ Callback executed successfully");
            } else {
                System.err.println("⚠️ onNewMessage callback is NULL trong ConversationService!");
            }

        } catch (Exception e) {
            System.err.println("❌ Error handling new message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Xử lý thay đổi trạng thái user
     */
    private void handleUserStatusChange(String data) {
        try {
            System.out.println("→ handleUserStatusChange dữ liệu: " + data);

            String[] parts = data.split("\\|\\|\\|");

            if (parts.length >= 4) {
                String userId = parts[0];
                boolean isOnline = Boolean.parseBoolean(parts[1]);
                String statusText = parts[2];
                String lastSeen = parts[3];

                System.out.println("  → Đã parse - UserID: " + userId + ", Online: " + isOnline);

                if (onUserOnlineStatus != null) {
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

    // ==================== CALLBACKS ====================

    /**
     * ✅ Đăng ký callback khi có tin nhắn mới
     */
    public void setOnNewMessage(BiConsumer<String, Message> callback) {
        this.onNewMessage = callback;
        System.out.println("✅ Đã đăng ký callback onNewMessage trong ConversationService");
    }

    /**
     * Đăng ký callback khi có user thay đổi trạng thái online
     */
    public void setOnUserOnlineStatus(TriConsumer<String, Boolean, String> callback) {
        this.onUserOnlineStatus = callback;
        System.out.println("✅ Đã đăng ký callback cho user status changes");
    }

    // ==================== FUNCTIONAL INTERFACE ====================

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}