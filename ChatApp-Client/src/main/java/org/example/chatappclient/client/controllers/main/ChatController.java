package org.example.chatappclient.client.controllers.main;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.services.MessageService;
import org.example.chatappclient.client.controllers.main.handlers.UIComponentFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ChatController - Smooth auto-scroll version
 * ✅ Tự động cuộn mượt mà không giật lag
 * ✅ Luôn hiển thị tin nhắn mới nhất khi vào chat
 * ✅ Tự động cuộn khi có tin nhắn mới
 */
public class ChatController {

    private final VBox chatMessagesContainer;
    private final ScrollPane chatScrollPane;
    private final String currentUserId;
    private final UIComponentFactory uiFactory;
    private final MessageService messageService;

    private String currentConversationId;
    private Conversation currentConversation;

    // Debounce scroll để tránh giật
    private volatile boolean isScrolling = false;

    public ChatController(VBox chatMessagesContainer,
                          ScrollPane chatScrollPane,
                          String currentUserId) {
        this.chatMessagesContainer = chatMessagesContainer;
        this.chatScrollPane = chatScrollPane;
        this.currentUserId = currentUserId;
        this.uiFactory = new UIComponentFactory();
        this.messageService = MessageService.getInstance();

        setupScrollPane();
    }

    /**
     * ✅ Cấu hình ScrollPane tối ưu
     */
    private void setupScrollPane() {
        chatMessagesContainer.setAlignment(Pos.TOP_LEFT);
        chatMessagesContainer.setSpacing(0);

        // Bind width để tránh horizontal scroll
        chatMessagesContainer.prefWidthProperty().bind(
                chatScrollPane.widthProperty().subtract(2)
        );

        // Smooth scroll behavior
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setFitToWidth(true);

        // ✅ QUAN TRỌNG: Đợi layout hoàn tất trước khi cuộn
        chatMessagesContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!isScrolling && chatMessagesContainer.getChildren().size() > 0) {
                smoothScrollToBottom();
            }
        });
    }

    /**
     * ✅ Mở cuộc trò chuyện - cuộn xuống cuối MƯỢT MÀ
     */
    public void openConversation(Conversation conversation) {
        this.currentConversation = conversation;
        this.currentConversationId = conversation.getConversationId();

        // Xóa tin nhắn cũ
        chatMessagesContainer.getChildren().clear();

        // Load tin nhắn mới
        loadMessages();
    }

    /**
     * ✅ Load tin nhắn và tự động cuộn xuống cuối
     */
    private void loadMessages() {
        try {
            List<Message> messages = messageService.getMessages(currentConversationId);
            displayMessages(messages);
        } catch (Exception e) {
            Platform.runLater(() -> showErrorMessage("Không thể tải tin nhắn: " + e.getMessage()));
        }
    }

    /**
     * ✅ Hiển thị tin nhắn với auto-scroll mượt mà
     */
    public void displayMessages(List<Message> messages) {
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().clear();

            if (messages == null || messages.isEmpty()) {
                showEmptyState();
                return;
            }

            // Sắp xếp tin nhắn theo thời gian
            messages.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));

            String lastDate = null;
            String lastSenderId = null;

            // Render tất cả tin nhắn
            for (Message msg : messages) {
                String msgDate = extractDate(msg.getTimestamp());

                // Thêm date separator nếu cần
                if (!msgDate.equals(lastDate)) {
                    chatMessagesContainer.getChildren().add(createDateSeparator(msgDate));
                    lastDate = msgDate;
                    lastSenderId = null;
                }

                // Tạo message bubble
                boolean isConsecutive = msg.getSenderId().equals(lastSenderId);
                HBox messageBubble = createMessageBubble(msg, isConsecutive);
                messageBubble.setUserData(msg);

                chatMessagesContainer.getChildren().add(messageBubble);
                lastSenderId = msg.getSenderId();
            }

            // ✅ Cuộn xuống cuối sau khi render xong HOÀN TOÀN
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        forceScrollToBottom();
                    });
                });
            });
        });
    }

    /**
     * ✅ Thêm tin nhắn mới - tự động cuộn mượt mà
     */
    public void addNewMessage(Message message) {
        Platform.runLater(() -> {
            String currentDate = extractDate(message.getTimestamp());

            // Tìm tin nhắn cuối cùng
            Message lastRealMessage = findLastRealMessage();

            // Thêm date separator nếu cần
            if (lastRealMessage == null ||
                    !extractDate(lastRealMessage.getTimestamp()).equals(currentDate)) {
                chatMessagesContainer.getChildren().add(createDateSeparator(currentDate));
            }

            // Tạo và thêm message bubble
            HBox messageBubble = createMessageBubble(message, false);
            messageBubble.setUserData(message);
            chatMessagesContainer.getChildren().add(messageBubble);

            // ✅ Cuộn mượt mà sau khi thêm tin nhắn
            smoothScrollToBottom();
        });
    }

    /**
     * ✅ Thêm loading view (cho file upload)
     */
    public void addLoadingView(VBox loadingView) {
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().add(loadingView);
            smoothScrollToBottom();
        });
    }

    /**
     * ✅ Xóa loading view
     */
    public void removeLoadingView(VBox loadingView) {
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().remove(loadingView);
        });
    }

    /**
     * ✅ SMOOTH SCROLL - Cuộn mượt mà với animation
     */
    private void smoothScrollToBottom() {
        if (isScrolling) return;

        isScrolling = true;

        Platform.runLater(() -> {
            // Force layout update
            chatMessagesContainer.applyCss();
            chatMessagesContainer.layout();
            chatScrollPane.applyCss();
            chatScrollPane.layout();

            // Smooth scroll animation
            double targetVvalue = 1.0;
            double currentVvalue = chatScrollPane.getVvalue();

            if (Math.abs(targetVvalue - currentVvalue) < 0.01) {
                // Đã ở cuối rồi, chỉ cần set
                chatScrollPane.setVvalue(1.0);
                isScrolling = false;
            } else {
                // Animate scroll
                animateScroll(currentVvalue, targetVvalue);
            }
        });
    }

    /**
     * ✅ FORCE SCROLL - Cuộn ngay lập tức (dùng khi load messages)
     */
    private void forceScrollToBottom() {
        // Force layout update
        chatMessagesContainer.applyCss();
        chatMessagesContainer.layout();
        chatScrollPane.applyCss();
        chatScrollPane.layout();

        // Set scroll to bottom
        chatScrollPane.setVvalue(1.0);

        // Double check sau 100ms
        new Thread(() -> {
            try {
                Thread.sleep(100);
                Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * ✅ Animate scroll để mượt mà
     */
    private void animateScroll(double from, double to) {
        final int steps = 5;
        final long delay = 10; // ms

        new Thread(() -> {
            try {
                double step = (to - from) / steps;
                for (int i = 1; i <= steps; i++) {
                    final double value = from + (step * i);
                    Platform.runLater(() -> chatScrollPane.setVvalue(value));
                    Thread.sleep(delay);
                }
                isScrolling = false;
            } catch (InterruptedException e) {
                isScrolling = false;
            }
        }).start();
    }

    /**
     * Tìm tin nhắn thực sự cuối cùng (bỏ qua separator, loading)
     */
    private Message findLastRealMessage() {
        for (int i = chatMessagesContainer.getChildren().size() - 1; i >= 0; i--) {
            Object data = chatMessagesContainer.getChildren().get(i).getUserData();
            if (data instanceof Message) {
                return (Message) data;
            }
        }
        return null;
    }

    /**
     * Tạo message bubble
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
        VBox errorBox = new VBox(8);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(20));

        Label errorLabel = new Label("⚠️ " + error);
        errorLabel.setStyle(
                "-fx-text-fill: #dc3545; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 500;"
        );

        errorBox.getChildren().add(errorLabel);
        chatMessagesContainer.getChildren().add(errorBox);
    }

    /**
     * Extract date
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
     * Format time
     */
    private String formatTime(LocalDateTime timestamp) {
        if (timestamp == null) return "";
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Kiểm tra group chat
     */
    private boolean isGroupChat() {
        return currentConversation != null &&
                "group".equalsIgnoreCase(currentConversation.getType());
    }

    // ==================== GETTERS ====================

    public String getCurrentConversationId() {
        return currentConversationId;
    }

    public void resetChat() {
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().clear();
            currentConversation = null;
            currentConversationId = null;
        });
    }
}