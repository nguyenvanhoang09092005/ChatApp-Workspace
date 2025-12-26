package org.example.chatappclient.client.controllers.main.handlers;

import javafx.scene.layout.VBox;
import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.protocol.Protocol;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.services.*;
import org.example.chatappclient.client.utils.data.StickerData;
import org.example.chatappclient.client.utils.helpers.SoundUtil;
import javafx.application.Platform;
import org.example.chatappclient.client.utils.ui.AlertUtil;
import org.example.chatappclient.client.controllers.main.MainController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * MessageHandler - KHÔNG xử lý MESSAGE_RECEIVE (để ConversationService tự động xử lý)
 */
public class MessageHandler {

    private static volatile MessageHandler instance;
    private final Map<String, Consumer<String>> handlers;
    private MainController mainController;
    private MessageService messageService;

    // Reply state
    private Message replyToMessage;

    public MessageHandler() {
        handlers = new HashMap<>();
        messageService = MessageService.getInstance();
        initializeHandlers();
        // ❌ KHÔNG setup MessageService callbacks ở đây nữa
        // ConversationService sẽ tự động xử lý MESSAGE_RECEIVE
    }

    public static MessageHandler getInstance() {
        if (instance == null) {
            synchronized (MessageHandler.class) {
                if (instance == null) {
                    instance = new MessageHandler();
                }
            }
        }
        return instance;
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    // ==================== INITIALIZE HANDLERS ====================

    private void initializeHandlers() {
        // ❌ REMOVED: MESSAGE_RECEIVE - ConversationService xử lý rồi
        // handlers.put(Protocol.MESSAGE_RECEIVE, this::handleNewMessage);

        handlers.put(Protocol.MESSAGE_READ, this::handleMessageRead);
        handlers.put(Protocol.MESSAGE_DELIVERED, this::handleMessageDelivered);
        handlers.put(Protocol.TYPING_START, this::handleTypingStart);
        handlers.put(Protocol.TYPING_STOP, this::handleTypingStop);
        handlers.put(Protocol.USER_UPDATE_STATUS, this::handleUserStatusChange);
        handlers.put(Protocol.USER_GET_ONLINE_STATUS, this::handleOnlineStatus);
        handlers.put(Protocol.CALL_INCOMING, this::handleIncomingCall);
        handlers.put(Protocol.CALL_END, this::handleCallEnded);
        handlers.put(Protocol.CONVERSATION_GET_ALL, this::handleConversationGetAll);
        handlers.put(Protocol.CONVERSATION_GET, this::handleConversationGet);
        handlers.put(Protocol.CONVERSATION_CREATE, this::handleConversationCreate);
        handlers.put(Protocol.NOTIFICATION_NEW, this::handleNewNotification);

        System.out.println("✅ MessageHandler initialized (MESSAGE_RECEIVE removed)");
    }

    private void handleConversationGet(String message) {
        System.out.println("Received CONVERSATION_GET: " + message);
    }

    private void handleConversationCreate(String message) {
        System.out.println("Received CONVERSATION_CREATE: " + message);
    }

    // ==================== PROCESS MESSAGE ====================

    public void processMessage(String message) {
        if (message == null || message.isEmpty()) return;

        String command = Protocol.getCommand(message);
        Consumer<String> handler = handlers.get(command);

        if (handler != null) {
            Platform.runLater(() -> handler.accept(message));
        } else {
            // ✅ MESSAGE_RECEIVE sẽ không được log ở đây nữa
            if (!Protocol.MESSAGE_RECEIVE.equals(command)) {
                System.out.println("Unknown command: " + command);
            }
        }
    }

    // ==================== MESSAGE HANDLERS ====================

    // ❌ REMOVED: handleNewMessage - ConversationService xử lý rồi

    private void handleMessageRead(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 3) return;

            String messageId = parts[1];
            String userId = parts[2];

            Platform.runLater(() -> {
                System.out.println("Message " + messageId + " read by " + userId);
            });
        } catch (Exception e) {
            System.err.println("Error handling message read: " + e.getMessage());
        }
    }

    private void handleMessageDelivered(String message) {
        System.out.println("Message delivered: " + message);
    }

    // ==================== TYPING HANDLERS ====================

    private void handleTypingStart(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 3) return;

            String conversationId = parts[1];
            String userId = parts[2];

            Platform.runLater(() -> {
                if (mainController != null &&
                        conversationId.equals(mainController.getCurrentConversationId())) {
                    try {
                        String userName = UserService.getInstance()
                                .getUser(userId).getUsername();
                        mainController.showTypingIndicator(userName);
                    } catch (Exception e) {
                        mainController.showTypingIndicator("Ai đó");
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error handling typing start: " + e.getMessage());
        }
    }

    private void handleTypingStop(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 2) return;

            String conversationId = parts[1];

            Platform.runLater(() -> {
                if (mainController != null &&
                        conversationId.equals(mainController.getCurrentConversationId())) {
                    mainController.hideTypingIndicator();
                }
            });
        } catch (Exception e) {
            System.err.println("Error handling typing stop: " + e.getMessage());
        }
    }

    // ==================== USER STATUS HANDLERS ====================

    private void handleUserStatusChange(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 3) return;

            String userId = parts[1];
            boolean isOnline = "online".equals(parts[2]);

            Platform.runLater(() -> {
                System.out.println("User " + userId + " is " + (isOnline ? "online" : "offline"));
            });
        } catch (Exception e) {
            System.err.println("Error handling user status: " + e.getMessage());
        }
    }

    private void handleOnlineStatus(String message) {
        handleUserStatusChange(message);
    }

    // ==================== CALL HANDLERS ====================

    private void handleIncomingCall(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 5) return;

            CallInfo callInfo = new CallInfo();
            callInfo.callId = parts[1];
            callInfo.callerId = parts[2];
            callInfo.callerName = parts[3];
            callInfo.callType = parts[4];

            try {
                SoundUtil.playCallRing();
            } catch (Exception e) {
                System.err.println("Cannot play ring sound");
            }

            Platform.runLater(() -> {
                System.out.println("Incoming " + callInfo.callType + " call from " + callInfo.callerName);
            });
        } catch (Exception e) {
            System.err.println("Error handling incoming call: " + e.getMessage());
        }
    }

    private void handleCallEnded(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 2) return;

            String callId = parts[1];

            Platform.runLater(() -> {
                System.out.println("Call ended: " + callId);
            });
        } catch (Exception e) {
            System.err.println("Error handling call ended: " + e.getMessage());
        }
    }

    private void handleConversationGetAll(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 2) return;

            String userId = parts[1];
            List<Conversation> conversations = ConversationService.getInstance()
                    .getAllConversations(userId);

            StringBuilder sb = new StringBuilder();
            sb.append(Protocol.CONVERSATION_GET_ALL).append(Protocol.DELIMITER).append("SUCCESS");

            for (Conversation conv : conversations) {
                sb.append(Protocol.LIST_DELIMITER)
                        .append(conv.getConversationId()).append(Protocol.FIELD_DELIMITER)
                        .append(conv.getName()).append(Protocol.FIELD_DELIMITER)
                        .append(conv.getLastMessage());
            }

            SocketClient.getInstance().sendMessage(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleNewNotification(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 4) return;

            String title = parts[1];
            String content = parts[2];
            String type = parts[3];

            NotificationService.getInstance().showSystemNotification(title, content);

            try {
                SoundUtil.playNotification();
            } catch (Exception e) {
                System.err.println("Cannot play notification sound");
            }

        } catch (Exception e) {
            System.err.println("Error handling notification: " + e.getMessage());
        }
    }

    // ==================== MESSAGES ACTIONS ====================

    public void sendTextMessage() {
        if (mainController == null) {
            System.err.println("❌ MainController not set");
            return;
        }

        String convId = mainController.getCurrentConversationId();
        String content = mainController.getMessageInputArea().getText();

        if (convId == null || content == null || content.trim().isEmpty()) {
            System.err.println("❌ Invalid conversation or empty message");
            return;
        }

        try {
            System.out.println("📤 Sending message: " + content);

            Message sentMessage = messageService.sendMessage(
                    convId,
                    AuthService.getInstance().getCurrentUser().getUserId(),
                    content,
                    "text",
                    replyToMessage != null ? replyToMessage.getMessageId() : null
            );

            System.out.println("✅ Message sent successfully: " + sentMessage.getMessageId());

            mainController.getMessageInputArea().clear();

            if (replyToMessage != null) {
                cancelReply();
            }

            // ✅ Thêm tin nhắn ngay lập tức (người gửi)
            Platform.runLater(() -> {
                mainController.addMessageToUI(sentMessage);
            });

        } catch (Exception e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                AlertUtil.showError("Lỗi", "Không thể gửi tin nhắn: " + e.getMessage());
            });
        }
    }

    public void sendLike(String conversationId) {
        if (conversationId == null) {
            System.err.println("ConversationId is null");
            return;
        }

        try {
            messageService.sendLike(conversationId);
        } catch (Exception e) {
            System.err.println("Error sending like: " + e.getMessage());
        }
    }

    public void sendSticker(String conversationId, StickerData.Sticker sticker) {
        if (conversationId == null || sticker == null) {
            System.err.println("❌ Invalid conversation or sticker");
            return;
        }

        try {
            System.out.println("📤 Sending sticker: " + sticker.getName() + " to conversation: " + conversationId);

            Message sentMessage = messageService.sendMediaMessage(
                    conversationId,
                    AuthService.getInstance().getCurrentUser().getUserId(),
                    "sticker",
                    sticker.getUrl(),
                    sticker.getName(),
                    0
            );

            System.out.println("✅ Sticker sent successfully: " + sentMessage.getMessageId());

        } catch (Exception e) {
            System.err.println("❌ Error sending sticker: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                AlertUtil.showError("Lỗi", "Không thể gửi sticker: " + e.getMessage());
            });
        }
    }

    public void loadMessages(String conversationId) {
        if (conversationId == null || mainController == null) {
            System.err.println("❌ Cannot load messages: conversationId or mainController is null");
            return;
        }

        try {
            System.out.println("📥 Loading messages for conversation: " + conversationId);

            List<Message> messages = messageService.getMessages(conversationId);

            System.out.println("✅ Loaded " + messages.size() + " messages");

            Platform.runLater(() -> {
                mainController.displayMessages(messages);
            });

        } catch (Exception e) {
            System.err.println("❌ Error loading messages: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                AlertUtil.showError("Lỗi", "Không thể tải tin nhắn: " + e.getMessage());
            });
        }
    }

    // ==================== REPLY HANDLING ====================

    public void setReplyTo(Message message) {
        this.replyToMessage = message;
        if (mainController != null) {
            mainController.getReplyPreview().setVisible(true);
            mainController.getReplyToName().setText(message.getSenderName());
            mainController.getReplyToContent().setText(message.getDisplayContent());
        }
    }

    public void cancelReply() {
        this.replyToMessage = null;
        if (mainController != null) {
            mainController.getReplyPreview().setVisible(false);
        }
    }

    public Message getReplyToMessage() {
        return replyToMessage;
    }

    // ==================== MEDIA MESSAGES ====================

    public void sendImage(String imageUrl, String fileName) {
        if (mainController == null) return;
        String convId = mainController.getCurrentConversationId();
        if (convId == null) return;

        try {
            Message sentMessage = messageService.sendMediaMessage(
                    convId,
                    AuthService.getInstance().getCurrentUser().getUserId(),
                    "image",
                    imageUrl,
                    fileName,
                    0
            );

            Platform.runLater(() -> {
                mainController.addMessageToUI(sentMessage);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String fileUrl, String fileName, long fileSize) {
        if (mainController == null) return;
        String convId = mainController.getCurrentConversationId();
        if (convId == null) return;

        try {
            Message sentMessage = messageService.sendMediaMessage(
                    convId,
                    AuthService.getInstance().getCurrentUser().getUserId(),
                    "file",
                    fileUrl,
                    fileName,
                    fileSize
            );

            Platform.runLater(() -> {
                mainController.addMessageToUI(sentMessage);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== CLEANUP ====================

    public void cleanup() {
        handlers.clear();
        replyToMessage = null;
        mainController = null;
    }

    // ==================== HELPER CLASSES ====================

    public static class CallInfo {
        public String callId;
        public String callerId;
        public String callerName;
        public String callType;
    }
}