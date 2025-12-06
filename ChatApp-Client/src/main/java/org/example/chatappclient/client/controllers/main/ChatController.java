package org.example.chatappclient.client.controllers.main;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

        // Đảm bảo ScrollPane luôn theo dõi kích thước content
        setupScrollPane();
    }

    /**
     * Cấu hình ScrollPane để tự động cuộn
     */
    private void setupScrollPane() {
        chatMessagesContainer.setAlignment(Pos.TOP_LEFT);

        // Đảm bảo ScrollPane luôn fit content width
        chatMessagesContainer.prefWidthProperty().bind(chatScrollPane.widthProperty().subtract(2));

        // Lắng nghe thay đổi số lượng children để tự động cuộn
        chatMessagesContainer.getChildren().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    Platform.runLater(() -> scrollToBottom());
                }
            }
        });
    }

    /**
     * Mở cuộc trò chuyện và load tin nhắn
     */
    public void openConversation(Conversation conversation) {
        this.currentConversation = conversation;
        this.currentConversationId = conversation.getConversationId();

        // Xóa các tin nhắn cũ
        chatMessagesContainer.getChildren().clear();

        // Load tin nhắn từ server
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

            // Đảm bảo tin nhắn được sắp xếp theo thời gian tăng dần (cũ → mới)
            messages.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));

            String lastDate = null;
            String lastSenderId = null;

            for (Message msg : messages) {
                String msgDate = extractDate(msg.getTimestamp());

                if (!msgDate.equals(lastDate)) {
                    chatMessagesContainer.getChildren().add(createDateSeparator(msgDate));
                    lastDate = msgDate;
                    lastSenderId = null;
                }

                boolean isConsecutive = msg.getSenderId().equals(lastSenderId);
                HBox messageBubble = createMessageBubble(msg, isConsecutive);
                messageBubble.setUserData(msg); // Quan trọng: lưu data để kiểm tra

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
            String currentDate = extractDate(message.getTimestamp());

            // --- Tìm tin nhắn cuối cùng thực sự ---
            Message lastRealMessage = null;
            for (int i = chatMessagesContainer.getChildren().size() - 1; i >= 0; i--) {
                Object data = chatMessagesContainer.getChildren().get(i).getUserData();
                if (data instanceof Message) {
                    lastRealMessage = (Message) data;
                    break;
                }
            }

            // --- Nếu tin nhắn mới khác ngày: thêm Date Separator ---
            if (lastRealMessage == null ||
                    !extractDate(lastRealMessage.getTimestamp()).equals(currentDate)) {
                chatMessagesContainer.getChildren().add(createDateSeparator(currentDate));
            }

            // --- Tạo bubble cho tin nhắn mới ---
            HBox messageBubble = createMessageBubble(message, false);
            messageBubble.setUserData(message);
            chatMessagesContainer.getChildren().add(messageBubble);

            scrollToBottom();
        });
    }


    /**
     * Tạo message bubble sử dụng UIComponentFactory
     */
    private HBox createMessageBubble(Message message, boolean isConsecutive) {
        return uiFactory.createMessageBubble(message, currentUserId, isConsecutive);
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
     * Hiển thị empty state khi chưa có tin nhắn
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

    public void addLoadingView(VBox loadingView) {
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().add(loadingView);
            scrollToBottom();
        });
    }

    /**
     * Xóa loading view khỏi chat
     */
    public void removeLoadingView(VBox loadingView) {
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().remove(loadingView);
        });
    }

    /**
     * Hiển thị lỗi
     */
    private void showErrorMessage(String error) {
        Label errorLabel = new Label(error);
        errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14px;");
        chatMessagesContainer.getChildren().add(errorLabel);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatMessagesContainer.applyCss();
            chatMessagesContainer.layout();
            chatScrollPane.applyCss();
            chatScrollPane.layout();

            chatScrollPane.setVvalue(chatScrollPane.getVmax());
        });
    }


    /**
     * Extract date từ LocalDateTime
     */
    private String extractDate(LocalDateTime timestamp) {
        if (timestamp == null) return "Hôm nay";

        LocalDateTime now = LocalDateTime.now();
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(timestamp.toLocalDate(), now.toLocalDate());

        if (daysDiff == 0) return "Hôm nay";
        if (daysDiff == 1) return "Hôm qua";

        return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Format thời gian HH:mm
     */
    private String formatTime(LocalDateTime timestamp) {
        if (timestamp == null) return "";
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Kiểm tra cuộc trò chuyện nhóm
     */
    private boolean isGroupChat() {
        return currentConversation != null && "group".equalsIgnoreCase(currentConversation.getType());
    }

    // Getter
    public String getCurrentConversationId() {
        return currentConversationId;
    }
}