package org.example.chatappclient.client.controllers.main.handlers;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.protocol.Protocol;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.services.*;
import org.example.chatappclient.client.utils.helpers.SoundUtil;
import javafx.application.Platform;
import org.example.chatappclient.client.utils.ui.AlertUtil;
import org.example.chatappclient.client.controllers.main.MainController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Handler for processing incoming messages from server
 */
public class MessageHandler {

    private static volatile MessageHandler instance;
    private final Map<String, Consumer<String>> handlers;
    private MainController mainController;

    // Callbacks
    private Consumer<Message> onNewMessage;
    private Consumer<String> onTypingStart;
    private Consumer<String> onTypingStop;
    private BiConsumer<String, String> onMessageRead;
    private BiConsumer<String, Boolean> onUserStatusChange;
    private Consumer<CallInfo> onIncomingCall;
    private Consumer<String> onCallEnded;

    // Reply state
    private Message replyToMessage;

    public MessageHandler() {
        handlers = new HashMap<>();
        initializeHandlers();
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

    // Inject MainController dependency
    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    // ==================== INITIALIZE HANDLERS ====================

    private void initializeHandlers() {
        // Message handlers
        handlers.put(Protocol.MESSAGE_RECEIVE, this::handleNewMessage);
        handlers.put(Protocol.MESSAGE_READ, this::handleMessageRead);
        handlers.put(Protocol.MESSAGE_DELIVERED, this::handleMessageDelivered);

        // Typing handlers
        handlers.put(Protocol.TYPING_START, this::handleTypingStart);
        handlers.put(Protocol.TYPING_STOP, this::handleTypingStop);

        // User status handlers
        handlers.put(Protocol.USER_UPDATE_STATUS, this::handleUserStatusChange);
        handlers.put(Protocol.USER_GET_ONLINE_STATUS, this::handleOnlineStatus);

        // Call handlers
        handlers.put(Protocol.CALL_INCOMING, this::handleIncomingCall);
        handlers.put(Protocol.CALL_END, this::handleCallEnded);

        handlers.put(Protocol.CONVERSATION_GET_ALL, this::handleConversationGetAll);
        handlers.put(Protocol.CONVERSATION_GET, this::handleConversationGet);
        handlers.put(Protocol.CONVERSATION_CREATE, this::handleConversationCreate);

        // Notification handlers
        handlers.put(Protocol.NOTIFICATION_NEW, this::handleNewNotification);
    }

    private void handleConversationGet(String message) {
        // TODO: implement logic to fetch a single conversation
        System.out.println("Received CONVERSATION_GET: " + message);
    }

    private void handleConversationCreate(String message) {
        // TODO: implement logic to create a new conversation
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
            System.out.println("Unknown command: " + command);
        }
    }

    // ==================== MESSAGE HANDLERS ====================

    private void handleNewMessage(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 5) return;

            Message msg = new Message();
            msg.setMessageId(parts[1]);
            msg.setConversationId(parts[2]);
            msg.setSenderId(parts[3]);
            msg.setContent(parts[4]);

            if (parts.length > 5) msg.setMessageType(parts[5]);
            if (parts.length > 6) msg.setMediaUrl(parts[6]);
            if (parts.length > 7) msg.setSenderName(parts[7]);
            if (parts.length > 8) msg.setSenderAvatar(parts[8]);

            // Play notification sound
            SoundUtil.playMessageReceived();

            // Notify UI
            if (onNewMessage != null) {
                onNewMessage.accept(msg);
            }

            // Show system notification
            NotificationService.getInstance().showMessageNotification(
                    msg.getSenderName(),
                    msg.getDisplayContent()
            );

        } catch (Exception e) {
            System.err.println("Error handling new message: " + e.getMessage());
        }
    }

    private void handleMessageRead(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 3) return;

            String messageId = parts[1];
            String userId = parts[2];

            if (onMessageRead != null) {
                onMessageRead.accept(messageId, userId);
            }
        } catch (Exception e) {
            System.err.println("Error handling message read: " + e.getMessage());
        }
    }

    private void handleMessageDelivered(String message) {
        // Similar to handleMessageRead
        System.out.println("Message delivered: " + message);
    }

    // ==================== TYPING HANDLERS ====================

    private void handleTypingStart(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 3) return;

            String conversationId = parts[1];
            String userId = parts[2];

            if (onTypingStart != null) {
                onTypingStart.accept(conversationId + ":" + userId);
            }
        } catch (Exception e) {
            System.err.println("Error handling typing start: " + e.getMessage());
        }
    }

    private void handleTypingStop(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 2) return;

            String conversationId = parts[1];

            if (onTypingStop != null) {
                onTypingStop.accept(conversationId);
            }
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

            if (onUserStatusChange != null) {
                onUserStatusChange.accept(userId, isOnline);
            }
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
            callInfo.callType = parts[4]; // "audio" or "video"

            // Play ring tone
            SoundUtil.playCallRing();

            if (onIncomingCall != null) {
                onIncomingCall.accept(callInfo);
            }
        } catch (Exception e) {
            System.err.println("Error handling incoming call: " + e.getMessage());
        }
    }

    private void handleCallEnded(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 2) return;

            String callId = parts[1];

            if (onCallEnded != null) {
                onCallEnded.accept(callId);
            }
        } catch (Exception e) {
            System.err.println("Error handling call ended: " + e.getMessage());
        }
    }
    
    //
    private void handleConversationGetAll(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 2) return;

            String userId = parts[1];
            List<Conversation> conversations = ConversationService.getInstance().getAllConversations(userId);

            // Build response
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


    // ==================== NOTIFICATION HANDLERS ====================

    private void handleNewNotification(String message) {
        try {
            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 4) return;

            String title = parts[1];
            String content = parts[2];
            String type = parts[3];

            NotificationService.getInstance().showSystemNotification(title, content);
            SoundUtil.playNotification();

        } catch (Exception e) {
            System.err.println("Error handling notification: " + e.getMessage());
        }
    }

    // ==================== SETTERS ====================

    public void setOnNewMessage(Consumer<Message> callback) {
        this.onNewMessage = callback;
    }

    public void setOnTypingStart(Consumer<String> callback) {
        this.onTypingStart = callback;
    }

    public void setOnTypingStop(Consumer<String> callback) {
        this.onTypingStop = callback;
    }

    public void setOnMessageRead(BiConsumer<String, String> callback) {
        this.onMessageRead = callback;
    }

    public void setOnUserStatusChange(BiConsumer<String, Boolean> callback) {
        this.onUserStatusChange = callback;
    }

    public void setOnIncomingCall(Consumer<CallInfo> callback) {
        this.onIncomingCall = callback;
    }

    public void setOnCallEnded(Consumer<String> callback) {
        this.onCallEnded = callback;
    }

    // ==================== MESSAGES ACTIONS ====================

    public void sendTextMessage() {
        if (mainController == null) {
            System.err.println("MainController not set");
            return;
        }

        String convId = mainController.getCurrentConversationId();
        String content = mainController.getMessageInputArea().getText();

        if (convId == null || content == null || content.trim().isEmpty()) {
            return;
        }

        MessageService.getInstance().sendTextMessage(convId, content);
        mainController.getMessageInputArea().clear();
    }

    public void sendLike(String conversationId) {
        if (conversationId == null) {
            System.err.println("ConversationId is null");
            return;
        }
        MessageService.getInstance().sendLike(conversationId);
    }

    public void loadMessages(String conversationId) {
        List<Message> messages = MessageService.getInstance().getMessages(conversationId);
        if (onNewMessage != null) {
            messages.forEach(onNewMessage);
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

    // ==================== CLEANUP ====================

    public void cleanup() {
        handlers.clear();
        onNewMessage = null;
        onTypingStart = null;
        onTypingStop = null;
        onMessageRead = null;
        onUserStatusChange = null;
        onIncomingCall = null;
        onCallEnded = null;
        replyToMessage = null;
        mainController = null;
    }

    public void sendImage(String imageUrl, String fileName) {
        if (mainController == null) return;
        String convId = mainController.getCurrentConversationId();
        if (convId == null) return;

        try {
            MessageService.getInstance().sendMediaMessage(
                    convId,
                    AuthService.getInstance().getCurrentUser().getUserId(),
                    "image",
                    imageUrl,
                    fileName,
                    0
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String fileUrl, String fileName, long fileSize) {
        if (mainController == null) return;
        String convId = mainController.getCurrentConversationId();
        if (convId == null) return;

        try {
            MessageService.getInstance().sendMediaMessage(
                    convId,
                    AuthService.getInstance().getCurrentUser().getUserId(),
                    "file",
                    fileUrl,
                    fileName,
                    fileSize
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendVideo(String videoUrl, String fileName, long fileSize) {
        if (mainController == null) return;
        String convId = mainController.getCurrentConversationId();
        if (convId == null) return;

        try {
            MessageService.getInstance().sendMediaMessage(
                    convId,
                    AuthService.getInstance().getCurrentUser().getUserId(),
                    "video",
                    videoUrl,
                    fileName,
                    fileSize
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendVoice(String audioUrl, int durationSeconds) {
        if (mainController == null) return;
        String convId = mainController.getCurrentConversationId();
        if (convId == null) return;

        try {
            MessageService.getInstance().sendMediaMessage(
                    convId,
                    AuthService.getInstance().getCurrentUser().getUserId(),
                    "voice",
                    audioUrl,
                    "voice_message.mp3",
                    0
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // ==================== HELPER CLASSES ====================

    public static class CallInfo {
        public String callId;
        public String callerId;
        public String callerName;
        public String callType;
    }

    @FunctionalInterface
    public interface BiConsumer<T, U> {
        void accept(T t, U u);
    }
}