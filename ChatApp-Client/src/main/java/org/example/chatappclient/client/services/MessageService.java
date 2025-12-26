package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.protocol.Protocol;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Service xử lý các thao tác với Message
 */
public class MessageService {

    private static volatile MessageService instance;
    private final SocketClient socketClient;
    private final AuthService authService;

    // Callbacks
    private BiConsumer<String, Message> onNewMessage;
    private BiConsumer<String, String> onTypingStart;
    private Consumer<String> onTypingStop;
    private BiConsumer<String, String> onMessageRead;

    private MessageService() {
        socketClient = SocketClient.getInstance();
        authService = AuthService.getInstance();
    }

    public static MessageService getInstance() {
        if (instance == null) {
            synchronized (MessageService.class) {
                if (instance == null) {
                    instance = new MessageService();
                }
            }
        }
        return instance;
    }

    // ==================== GET MESSAGES ====================

    public List<Message> getMessages(String conversationId, int offset, int limit) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_GET_HISTORY,
                conversationId, String.valueOf(offset), String.valueOf(limit));
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseMessages(Protocol.getData(response));
    }

    // ==================== SEND MESSAGES ====================

    public Message sendMessage(String conversationId, String senderId, String content,
                               String type, String replyToId) throws Exception {
        // FIXED: Sanitize content - loại bỏ ký tự xuống dòng
        content = sanitizeContent(content);

        String request = Protocol.buildRequest(Protocol.MESSAGE_SEND,
                conversationId, senderId, content, type, replyToId != null ? replyToId : "");

        System.out.println("🔍 DEBUG - Request being sent: " + request);

        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseMessage(Protocol.getData(response));
    }

    public Message sendMediaMessage(String conversationId, String senderId, String type,
                                    String mediaUrl, String fileName, long fileSize) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_SEND,
                conversationId, senderId, "", type, "", mediaUrl, fileName, String.valueOf(fileSize));
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseMessage(Protocol.getData(response));
    }

    // ==================== MESSAGE ACTIONS ====================

    public void editMessage(String messageId, String newContent) throws Exception {
        newContent = sanitizeContent(newContent);
        String request = Protocol.buildRequest(Protocol.MESSAGE_EDIT, messageId, newContent);
        String response = socketClient.sendRequest(request, 10000);

        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    public void recallMessage(String messageId) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_RECALL, messageId);
        String response = socketClient.sendRequest(request, 10000);

        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    public void deleteMessage(String messageId) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_DELETE, messageId);
        String response = socketClient.sendRequest(request, 10000);

        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    public void forwardMessage(String messageId, String targetConversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_FORWARD, messageId, targetConversationId);
        String response = socketClient.sendRequest(request, 10000);

        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    public void reactToMessage(String messageId, String userId, String emoji) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_REACT, messageId, userId, emoji);
        socketClient.sendMessage(request);
    }

    // ==================== TYPING ====================

    public void sendTypingStart(String conversationId, String userId) {
        String request = Protocol.buildRequest(Protocol.TYPING_START, conversationId, userId);
        socketClient.sendMessage(request);
    }

    public void sendTypingStop(String conversationId, String userId) {
        String request = Protocol.buildRequest(Protocol.TYPING_STOP, conversationId, userId);
        socketClient.sendMessage(request);
    }


    // ==================== HELPER METHODS ====================

    /**
     * FIXED: Sanitize content to remove problematic characters
     */
    private String sanitizeContent(String content) {
        if (content == null) return "";

        // Loại bỏ các ký tự xuống dòng và carriage return
        content = content.replace("\r\n", " ");
        content = content.replace("\n", " ");
        content = content.replace("\r", " ");

        // Trim khoảng trắng thừa
        content = content.trim();

        return content;
    }

    // ==================== PARSING ====================

    private List<Message> parseMessages(String data) {
        List<Message> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;

        for (String msgData : Protocol.parseDataList(data)) {
            Message msg = parseMessage(msgData);
            if (msg != null) list.add(msg);
        }
        return list;
    }

    private Message parseMessage(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] f = Protocol.parseFields(data);
        if (f.length < 4) return null;

        Message m = new Message();
        m.setMessageId(f[0]);
        m.setSenderId(f[1]);
        m.setSenderName(f[2]);
        m.setContent(f[3]);
        if (f.length > 4) m.setMessageType(f[4]);
        if (f.length > 5) m.setMediaUrl(f[5]);
        if (f.length > 6) {
            String ts = f[6];
            if (ts != null && !ts.isEmpty() && !ts.equals("0")) {
                try {
                    m.setTimestamp(LocalDateTime.parse(ts));
                } catch (Exception e) {
                    // fallback: dùng thời gian hiện tại
                    m.setTimestamp(LocalDateTime.now());
                }
            } else {
                // timestamp rỗng → gán thời điểm hiện tại
                m.setTimestamp(LocalDateTime.now());
            }
        }
        if (f.length > 7) m.setRead(Boolean.parseBoolean(f[7]));
        if (f.length > 8) m.setSenderAvatar(f[8]);
        if (f.length > 9) m.setFileName(f[9]);
        if (f.length > 10) m.setFileSize(parseLong(f[10]));
        return m;
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0; }
    }

    // ==================== CALLBACKS ====================

    public void setOnNewMessage(BiConsumer<String, Message> callback) {
        this.onNewMessage = callback;
    }

    public void setOnTypingStart(BiConsumer<String, String> callback) {
        this.onTypingStart = callback;
    }

    public void setOnTypingStop(Consumer<String> callback) {
        this.onTypingStop = callback;
    }

    public void setOnMessageRead(BiConsumer<String, String> callback) {
        this.onMessageRead = callback;
    }

    // ==================== CONVENIENCE METHODS ====================

    public void sendTextMessage(String conversationId, String content) {
        if (conversationId == null || content == null || content.trim().isEmpty()) return;
        try {
            // Gửi message kiểu text, không reply
            Message msg = sendMessage(conversationId,
                    authService.getCurrentUser().getUserId(),
                    content,
                    "text",
                    null);
            // Có thể trigger callback ngay lập tức nếu muốn
            if (onNewMessage != null) {
                onNewMessage.accept(conversationId, msg);
            }
        } catch (Exception e) {
            System.err.println("❌ Error sending text message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendLike(String conversationId) {
        if (conversationId == null) return;
        try {
            Message msg = sendMessage(conversationId,
                    authService.getCurrentUser().getUserId(),
                    "👍",
                    "like",
                    null);
            if (onNewMessage != null) {
                onNewMessage.accept(conversationId, msg);
            }
        } catch (Exception e) {
            System.err.println("❌ Error sending like: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Lấy tin nhắn cho conversation
    public List<Message> getMessages(String conversationId) {
        try {
            return getMessages(conversationId, 0, 50);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void deleteAllMessages(String conversationId) throws Exception {
        String userId = authService.getCurrentUser().getUserId();
        String request = Protocol.buildRequest(Protocol.MESSAGE_DELETE_ALL,
                conversationId, userId);

        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }
}