package org.example.chatappclient.client.controllers.main.handlers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

/**
 * Factory class để tạo các UI components
 */
public class UIComponentFactory {

    private static final String DEFAULT_AVATAR = "https://ui-avatars.com/api/?background=0084ff&color=fff&name=";

    // ==================== AVATAR ====================

    public void loadAvatar(ImageView imageView, String url, String name, int size) {
        try {
            String imgUrl = (url != null && !url.isEmpty())
                    ? url
                    : DEFAULT_AVATAR + name.replace(" ", "+") + "&size=" + size;
            imageView.setImage(new Image(imgUrl, true));
        } catch (Exception e) {
            imageView.setImage(new Image(DEFAULT_AVATAR + "U&size=" + size, true));
        }
    }

    public ImageView createAvatar(String url, String name, int size) {
        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        loadAvatar(iv, url, name, size);
        return iv;
    }

    // ==================== CONVERSATION ITEM ====================

    public HBox createConversationItem(Conversation conv, Consumer<Conversation> onClick) {
        HBox item = new HBox(12);
        item.getStyleClass().add("conversation-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));

        // Avatar with online indicator
        StackPane avatarPane = new StackPane();
        ImageView avatar = createAvatar(conv.getAvatarUrl(), conv.getName(), 52);
        avatarPane.getChildren().add(avatar);

        if (conv.isActive()) {
            Circle online = new Circle(7);
            online.setFill(Color.web("#31A24C"));
            online.setStroke(Color.WHITE);
            online.setStrokeWidth(2);
            online.setTranslateX(18);
            online.setTranslateY(18);
            avatarPane.getChildren().add(online);
        }

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Header row
        HBox header = new HBox(8);
        Label name = new Label(conv.getName());
        name.getStyleClass().add("conv-name");
        name.setMaxWidth(160);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label time = new Label(formatConvTime(conv.getLastMessageTime()));
        time.getStyleClass().add("conv-time");

        header.getChildren().addAll(name, spacer, time);

        // Message row
        HBox msgRow = new HBox(8);
        msgRow.setAlignment(Pos.CENTER_LEFT);

        Label lastMsg = new Label(conv.getLastMessagePreview());
        lastMsg.getStyleClass().add("conv-message");
        lastMsg.setMaxWidth(180);
        HBox.setHgrow(lastMsg, Priority.ALWAYS);
        msgRow.getChildren().add(lastMsg);

        if (conv.getUnreadCount() > 0) {
            item.getStyleClass().add("unread");
            Label badge = new Label(conv.getUnreadCount() > 99 ? "99+" : String.valueOf(conv.getUnreadCount()));
            badge.getStyleClass().add("unread-badge");
            msgRow.getChildren().add(badge);
        }

        info.getChildren().addAll(header, msgRow);
        item.getChildren().addAll(avatarPane, info);

        // Click handler
        item.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                onClick.accept(conv);
            }
        });

        return item;
    }

    // ==================== MESSAGE BUBBLE ====================

    public HBox createMessageBubble(Message msg, String currentUserId, boolean consecutive) {
        boolean isSent = msg.getSenderId().equals(currentUserId);

        HBox row = new HBox(8);
        row.getStyleClass().addAll("message-row", isSent ? "sent" : "received");
        row.setPadding(new Insets(1, 0, 1, 0));
        row.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox content = new VBox(2);
        content.setMaxWidth(400);
        content.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Build bubble based on type
        switch (msg.getMessageType() != null ? msg.getMessageType() : "text") {
            case "image" -> buildImageBubble(content, msg, isSent);
            case "file" -> buildFileBubble(content, msg, isSent);
            case "audio" -> buildAudioBubble(content, msg, isSent);
            case "emoji", "sticker" -> buildEmojiMessage(content, msg);
            default -> buildTextBubble(content, msg, isSent, consecutive);
        }

        // Time and status
        HBox meta = new HBox(4);
        meta.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label time = new Label(formatMsgTime(msg.getTimestamp()));
        time.getStyleClass().add("message-time");
        meta.getChildren().add(time);

        if (isSent) {
            Label status = new Label(msg.isRead() ? "✓✓" : "✓");
            status.getStyleClass().add("message-status");
            meta.getChildren().add(status);
        }

        content.getChildren().add(meta);

        // Layout
        if (isSent) {
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            row.getChildren().addAll(sp, content);
        } else {
            row.getChildren().add(content);
        }

        return row;
    }

    private void buildTextBubble(VBox content, Message msg, boolean isSent, boolean consecutive) {
        Label bubble = new Label(msg.getContent());
        bubble.getStyleClass().addAll("message-bubble", isSent ? "sent" : "received");
        if (consecutive) bubble.getStyleClass().add("consecutive");
        bubble.setWrapText(true);
        bubble.setMaxWidth(380);
        content.getChildren().add(bubble);
    }

    private void buildImageBubble(VBox content, Message msg, boolean isSent) {
        ImageView img = new ImageView();
        img.setPreserveRatio(true);
        img.setFitWidth(250);

        if (msg.getMediaUrl() != null) {
            try {
                img.setImage(new Image(msg.getMediaUrl(), true));
            } catch (Exception e) {
                Label error = new Label("Không thể tải ảnh");
                error.setStyle("-fx-text-fill: #dc3545;");
                content.getChildren().add(error);
                return;
            }
        }

        VBox imgBox = new VBox(4);
        imgBox.getStyleClass().addAll("message-image", isSent ? "sent" : "received");
        imgBox.getChildren().add(img);

        if (msg.getContent() != null && !msg.getContent().isEmpty() && !msg.getContent().equals("[Hình ảnh]")) {
            Label caption = new Label(msg.getContent());
            caption.setWrapText(true);
            caption.setMaxWidth(250);
            imgBox.getChildren().add(caption);
        }

        content.getChildren().add(imgBox);
    }

    private void buildFileBubble(VBox content, Message msg, boolean isSent) {
        HBox fileBox = new HBox(12);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setPadding(new Insets(10, 14, 10, 14));
        fileBox.setStyle("-fx-background-color: " + (isSent ? "#0084ff" : "#e4e6eb") +
                "; -fx-background-radius: 12;");

        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 24px;");

        VBox fileInfo = new VBox(2);
        Label fileName = new Label(msg.getFileName() != null ? msg.getFileName() : "File");
        fileName.setStyle("-fx-font-weight: 600; -fx-text-fill: " + (isSent ? "white" : "#050505") + ";");

        Label fileSize = new Label(formatFileSize(msg.getFileSize()));
        fileSize.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isSent ? "rgba(255,255,255,0.8)" : "#65676b") + ";");

        fileInfo.getChildren().addAll(fileName, fileSize);
        fileBox.getChildren().addAll(icon, fileInfo);
        content.getChildren().add(fileBox);
    }

    private void buildAudioBubble(VBox content, Message msg, boolean isSent) {
        HBox audioBox = new HBox(10);
        audioBox.setAlignment(Pos.CENTER_LEFT);
        audioBox.setPadding(new Insets(8, 12, 8, 12));
        audioBox.setStyle("-fx-background-color: " + (isSent ? "#0084ff" : "#e4e6eb") +
                "; -fx-background-radius: 20;");

        Label playBtn = new Label("▶");
        playBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (isSent ? "white" : "#050505") + ";");

        // Waveform
        HBox waveform = new HBox(2);
        for (int i = 0; i < 15; i++) {
            Region bar = new Region();
            bar.setMinWidth(3);
            bar.setMinHeight(5 + Math.random() * 15);
            bar.setStyle("-fx-background-color: " + (isSent ? "rgba(255,255,255,0.6)" : "#8d949e") +
                    "; -fx-background-radius: 2;");
            waveform.getChildren().add(bar);
        }

        Label duration = new Label(formatDuration(msg.getMediaDuration()));
        duration.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isSent ? "rgba(255,255,255,0.8)" : "#65676b") + ";");

        audioBox.getChildren().addAll(playBtn, waveform, duration);
        content.getChildren().add(audioBox);
    }

    private void buildEmojiMessage(VBox content, Message msg) {
        Label emoji = new Label(msg.getContent());
        emoji.setStyle("-fx-font-size: 48px;");
        content.getChildren().add(emoji);
    }

    // ==================== DATE SEPARATOR ====================

    public HBox createDateSeparator(String date) {
        HBox box = new HBox();
        box.getStyleClass().add("date-separator");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16, 0, 16, 0));

        Label label = new Label(date);
        label.getStyleClass().add("date-separator-label");
        box.getChildren().add(label);
        return box;
    }

    // ==================== EMPTY STATES ====================

    public VBox createEmptyState(String title, String subtitle) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #050505;");

        Label s = new Label(subtitle);
        s.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");

        box.getChildren().addAll(t, s);
        return box;
    }

    public Label createCenteredLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14px;");
        return label;
    }

    // ==================== INFO SIDEBAR ====================

    public VBox createInfoSidebarContent(Conversation conv) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);

        // Avatar
        ImageView avatar = createAvatar(conv.getAvatarUrl(), conv.getName(), 80);

        Label name = new Label(conv.getName());
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");

        Label status = new Label(conv.isActive() ? "Đang hoạt động" : "Không hoạt động");
        status.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");

        // Actions
        HBox actions = new HBox(16);
        actions.setAlignment(Pos.CENTER);
        actions.getChildren().addAll(
                createInfoAction("🔔", "Tắt thông báo"),
                createInfoAction("🔍", "Tìm kiếm"),
                createInfoAction("📌", "Ghim")
        );

        content.getChildren().addAll(avatar, name, status, actions);
        return content;
    }

    private VBox createInfoAction(String icon, String label) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setCursor(javafx.scene.Cursor.HAND);

        Label i = new Label(icon);
        i.setStyle("-fx-font-size: 20px; -fx-background-color: #e4e6eb; -fx-padding: 10; -fx-background-radius: 20;");

        Label t = new Label(label);
        t.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");

        box.getChildren().addAll(i, t);
        return box;
    }

    // ==================== TIME FORMATTING ====================

    public String formatConvTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(timestamp);
            LocalDateTime now = LocalDateTime.now();
            long days = ChronoUnit.DAYS.between(dt.toLocalDate(), now.toLocalDate());

            if (days == 0) return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (days == 1) return "Hôm qua";
            if (days < 7) return dt.format(DateTimeFormatter.ofPattern("EEE"));
            return dt.format(DateTimeFormatter.ofPattern("dd/MM"));
        } catch (Exception e) {
            return timestamp;
        }
    }

    public String formatMsgTime(String timestamp) {
        if (timestamp == null) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(timestamp);
            return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return "";
        }
    }

    public String extractDate(String timestamp) {
        if (timestamp == null) return "Hôm nay";
        try {
            LocalDateTime dt = LocalDateTime.parse(timestamp);
            LocalDateTime now = LocalDateTime.now();
            long days = ChronoUnit.DAYS.between(dt.toLocalDate(), now.toLocalDate());

            if (days == 0) return "Hôm nay";
            if (days == 1) return "Hôm qua";
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return "Hôm nay";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private String formatDuration(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }
}