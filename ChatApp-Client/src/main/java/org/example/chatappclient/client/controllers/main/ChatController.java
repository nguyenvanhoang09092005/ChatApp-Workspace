package org.example.chatappclient.client.controllers.main;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.services.MessageService;
import org.example.chatappclient.client.controllers.main.handlers.UIComponentFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ChatController - Quản lý hiển thị tin nhắn trong chat panel
 * Tin nhắn của người gửi hiển thị bên PHẢI (màu xanh)
 * Tin nhắn của người nhận hiển thị bên TRÁI (màu xám)
 */
public class ChatController {

    private final VBox chatMessagesContainer;
    private final ScrollPane chatScrollPane;
    private final String currentUserId;
    private final UIComponentFactory uiFactory;
    private final MessageService messageService;

    private String currentConversationId;
    private Conversation currentConversation;

    public ChatController(VBox chatMessagesContainer,
                          ScrollPane chatScrollPane,
                          String currentUserId) {
        this.chatMessagesContainer = chatMessagesContainer;
        this.chatScrollPane = chatScrollPane;
        this.currentUserId = currentUserId;
        this.uiFactory = new UIComponentFactory();
        this.messageService = MessageService.getInstance();
    }

    /**
     * Mở cuộc trò chuyện và load tin nhắn
     */
    public void openConversation(Conversation conversation) {
        this.currentConversation = conversation;
        this.currentConversationId = conversation.getConversationId();

        // Clear old messages
        chatMessagesContainer.getChildren().clear();

        // Load messages
        loadMessages();
    }

    /**
     * Load tin nhắn từ server
     */
    private void loadMessages() {
        Platform.runLater(() -> {
            try {
                List<Message> messages = messageService.getMessages(currentConversationId);
                displayMessages(messages);
            } catch (Exception e) {
                showErrorMessage("Không thể tải tin nhắn: " + e.getMessage());
            }
        });
    }

    /**
     * Hiển thị danh sách tin nhắn
     */
    public void displayMessages(List<Message> messages) {
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().clear();

            if (messages == null || messages.isEmpty()) {
                showEmptyState();
                return;
            }

            String lastDate = null;
            String lastSenderId = null;

            for (Message msg : messages) {
                // Date separator
                String msgDate = extractDate(msg.getTimestamp());
                if (!msgDate.equals(lastDate)) {
                    chatMessagesContainer.getChildren().add(createDateSeparator(msgDate));
                    lastDate = msgDate;
                    lastSenderId = null;
                }

                // Message bubble
                boolean isConsecutive = msg.getSenderId().equals(lastSenderId);
                HBox messageBubble = createMessageBubble(msg, isConsecutive);
                chatMessagesContainer.getChildren().add(messageBubble);

                lastSenderId = msg.getSenderId();
            }

            scrollToBottom();
        });
    }

    /**
     * Thêm tin nhắn mới vào UI
     */
    public void addNewMessage(Message message) {
        Platform.runLater(() -> {
            // Check if need date separator
            if (chatMessagesContainer.getChildren().isEmpty()) {
                String msgDate = extractDate(message.getTimestamp());
                chatMessagesContainer.getChildren().add(createDateSeparator(msgDate));
            }

            HBox messageBubble = createMessageBubble(message, false);
            chatMessagesContainer.getChildren().add(messageBubble);
            scrollToBottom();
        });
    }

    /**
     * Tạo message bubble - PHẢI (người gửi) hoặc TRÁI (người nhận)
     */
    private HBox createMessageBubble(Message message, boolean isConsecutive) {
        boolean isSentByMe = message.getSenderId().equals(currentUserId);

        HBox messageRow = new HBox(10);
        messageRow.setPadding(new Insets(2, 0, 2, 0));

        if (isSentByMe) {
            // TIN NHẮN BÊN PHẢI - Người gửi
            messageRow.setAlignment(Pos.CENTER_RIGHT);
            VBox messageContent = createSentMessageContent(message, isConsecutive);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            messageRow.getChildren().addAll(spacer, messageContent);

        } else {
            // TIN NHẮN BÊN TRÁI - Người nhận
            messageRow.setAlignment(Pos.CENTER_LEFT);

            // Avatar (chỉ hiển thị nếu không phải tin nhắn liên tiếp)
            if (!isConsecutive) {
                ImageView avatar = createAvatar(message.getSenderAvatar(), message.getSenderName(), 36);
                avatar.setTranslateY(10); // Căn xuống dưới
                messageRow.getChildren().add(avatar);
            } else {
                // Spacer thay avatar
                Region avatarSpacer = new Region();
                avatarSpacer.setMinWidth(36);
                avatarSpacer.setPrefWidth(36);
                messageRow.getChildren().add(avatarSpacer);
            }

            VBox messageContent = createReceivedMessageContent(message, isConsecutive);
            messageRow.getChildren().add(messageContent);
        }

        return messageRow;
    }

    /**
     * Tạo nội dung tin nhắn NGƯỜI GỬI (bên phải, màu xanh)
     */
    private VBox createSentMessageContent(Message message, boolean isConsecutive) {
        VBox content = new VBox(4);
        content.setAlignment(Pos.CENTER_RIGHT);
        content.setMaxWidth(400);

        // Message bubble
        Label bubble = new Label(message.getContent());
        bubble.setWrapText(true);
        bubble.setMaxWidth(380);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle(
                "-fx-background-color: #0084FF; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 18; " +
                        "-fx-font-size: 15px; " +
                        "-fx-line-spacing: 2px;"
        );

        // Consecutive message - less radius on top right
        if (isConsecutive) {
            bubble.setStyle(bubble.getStyle() + "-fx-background-radius: 18 4 18 18;");
        }

        // Time & Status
        HBox meta = new HBox(4);
        meta.setAlignment(Pos.CENTER_RIGHT);

        Label time = new Label(formatTime(message.getTimestamp()));
        time.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676B;");

        Label status = new Label(message.isRead() ? "✓✓" : "✓");
        status.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                (message.isRead() ? "#0084FF" : "#65676B") + ";");

        meta.getChildren().addAll(time, status);

        content.getChildren().addAll(bubble, meta);
        return content;
    }

    /**
     * Tạo nội dung tin nhắn NGƯỜI NHẬN (bên trái, màu xám)
     */
    private VBox createReceivedMessageContent(Message message, boolean isConsecutive) {
        VBox content = new VBox(4);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(400);

        // Sender name (chỉ hiển thị nếu không phải tin nhắn liên tiếp và là group chat)
        if (!isConsecutive && isGroupChat()) {
            Label senderName = new Label(message.getSenderName());
            senderName.setStyle(
                    "-fx-font-size: 13px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-text-fill: #65676B; " +
                            "-fx-padding: 0 0 2 14;"
            );
            content.getChildren().add(senderName);
        }

        // Message bubble
        Label bubble = new Label(message.getContent());
        bubble.setWrapText(true);
        bubble.setMaxWidth(380);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle(
                "-fx-background-color: #E4E6EB; " +
                        "-fx-text-fill: #050505; " +
                        "-fx-background-radius: 18; " +
                        "-fx-font-size: 15px; " +
                        "-fx-line-spacing: 2px;"
        );

        // Consecutive message - less radius on top left
        if (isConsecutive) {
            bubble.setStyle(bubble.getStyle() + "-fx-background-radius: 4 18 18 18;");
        }

        // Time
        Label time = new Label(formatTime(message.getTimestamp()));
        time.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676B; -fx-padding: 0 0 0 14;");

        content.getChildren().addAll(bubble, time);
        return content;
    }

    /**
     * Tạo date separator
     */
    private HBox createDateSeparator(String date) {
        HBox separator = new HBox();
        separator.setAlignment(Pos.CENTER);
        separator.setPadding(new Insets(16, 0, 16, 0));

        Label dateLabel = new Label(date);
        dateLabel.setStyle(
                "-fx-background-color: #E4E6EB; " +
                        "-fx-text-fill: #65676B; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 6 12 6 12; " +
                        "-fx-background-radius: 12;"
        );

        separator.getChildren().add(dateLabel);
        return separator;
    }

    /**
     * Tạo avatar
     */
    private ImageView createAvatar(String url, String name, int size) {
        ImageView avatar = new ImageView();
        avatar.setFitWidth(size);
        avatar.setFitHeight(size);
        avatar.setPreserveRatio(true);

        Circle clip = new Circle(size / 2.0, size / 2.0, size / 2.0);
        avatar.setClip(clip);

        try {
            String imgUrl = (url != null && !url.isEmpty())
                    ? url
                    : "https://ui-avatars.com/api/?background=0084ff&color=fff&name=" +
                    name.replace(" ", "+") + "&size=" + size;
            avatar.setImage(new Image(imgUrl, true));
        } catch (Exception e) {
            avatar.setImage(new Image(
                    "https://ui-avatars.com/api/?background=0084ff&color=fff&name=U&size=" + size,
                    true
            ));
        }

        return avatar;
    }

    /**
     * Hiển thị empty state
     */
    private void showEmptyState() {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(60));

        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Chưa có tin nhắn");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #65676B;");

        Label subtitle = new Label("Hãy bắt đầu cuộc trò chuyện!");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #8A8D91;");

        emptyState.getChildren().addAll(icon, title, subtitle);
        chatMessagesContainer.getChildren().add(emptyState);
    }

    /**
     * Hiển thị lỗi
     */
    private void showErrorMessage(String error) {
        Label errorLabel = new Label(error);
        errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14px;");
        chatMessagesContainer.getChildren().add(errorLabel);
    }

    /**
     * Scroll xuống dưới cùng
     */
    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    /**
     * Format time - HH:mm
     */
    private String formatTime(LocalDateTime timestamp) {
        if (timestamp == null) return "";
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Extract date for separator
     */
    private String extractDate(LocalDateTime timestamp) {
        if (timestamp == null) return "Hôm nay";

        LocalDateTime now = LocalDateTime.now();
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(
                timestamp.toLocalDate(),
                now.toLocalDate()
        );

        if (daysDiff == 0) return "Hôm nay";
        if (daysDiff == 1) return "Hôm qua";

        return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Check if current conversation is group chat
     */
    private boolean isGroupChat() {
        return currentConversation != null &&
                "group".equalsIgnoreCase(currentConversation.getType());
    }

    // Getters
    public String getCurrentConversationId() {
        return currentConversationId;
    }
}