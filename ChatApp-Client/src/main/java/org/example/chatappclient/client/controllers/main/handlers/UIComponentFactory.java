package org.example.chatappclient.client.controllers.main.handlers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.utils.ui.ConversationInfoBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

/**
 * Factory class để tạo các UI components - Complete with Sticker/Emoji support
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

    /**
     * Format trạng thái hoạt động của user
     */
    public String formatUserStatus(boolean isOnline, LocalDateTime lastSeen) {
        if (isOnline) {
            return "Đang hoạt động";
        }

        if (lastSeen == null) {
            return "Không hoạt động";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = ChronoUnit.MINUTES.between(lastSeen, now);
        long hoursAgo = ChronoUnit.HOURS.between(lastSeen, now);
        long daysAgo = ChronoUnit.DAYS.between(lastSeen, now);

        if (daysAgo >= 1) {
            return "Không hoạt động";
        }

        if (hoursAgo < 1) {
            if (minutesAgo < 1) {
                return "Hoạt động vừa xong";
            }
            return "Hoạt động " + minutesAgo + " phút trước";
        }

        return "Hoạt động " + hoursAgo + " giờ trước";
    }

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

        LocalDateTime lastMsgTime = conv.getLastMessageTime();
        Label time = new Label(formatConvTime(lastMsgTime));
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
        String messageType = msg.getMessageType() != null ? msg.getMessageType().toLowerCase() : "text";

        switch (messageType) {
            case "sticker" -> buildStickerBubble(content, msg, isSent);
            case "emoji" -> buildEmojiMessage(content, msg);
            case "image" -> buildImageBubble(content, msg, isSent);
            case "file" -> buildFileBubble(content, msg, isSent);
            case "audio" -> buildAudioBubble(content, msg, isSent);
            default -> buildTextBubble(content, msg, isSent, consecutive);
        }

        // Time and status (not for emoji/sticker messages)
        if (!"emoji".equals(messageType) && !"sticker".equals(messageType)) {
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
        }

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

    /**
     * Build sticker message bubble
     */
    private void buildStickerBubble(VBox content, Message msg, boolean isSent) {
        VBox stickerBox = new VBox(4);
        stickerBox.setAlignment(Pos.CENTER);
        stickerBox.setPadding(new Insets(4));

        if (msg.getMediaUrl() != null && !msg.getMediaUrl().isEmpty()) {
            try {
                ImageView stickerImg = new ImageView();
                stickerImg.setPreserveRatio(true);
                stickerImg.setFitWidth(150);
                stickerImg.setFitHeight(150);

                Image image = new Image(msg.getMediaUrl(), true);
                stickerImg.setImage(image);

                stickerBox.getChildren().add(stickerImg);

                // Add time and status below sticker
                HBox meta = new HBox(4);
                meta.setAlignment(Pos.CENTER);
                meta.setPadding(new Insets(4, 0, 0, 0));

                Label time = new Label(formatMsgTime(msg.getTimestamp()));
                time.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676b;");
                meta.getChildren().add(time);

                if (isSent) {
                    Label status = new Label(msg.isRead() ? "✓✓" : "✓");
                    status.setStyle("-fx-font-size: 11px; -fx-text-fill: #0084ff;");
                    meta.getChildren().add(status);
                }

                stickerBox.getChildren().add(meta);

            } catch (Exception e) {
                // Fallback: show sticker name
                Label fallback = new Label("🎨 " + (msg.getFileName() != null ? msg.getFileName() : "Sticker"));
                fallback.setStyle("-fx-font-size: 48px;");
                stickerBox.getChildren().add(fallback);
            }
        } else {
            // No URL: show placeholder
            Label placeholder = new Label("🎨");
            placeholder.setStyle("-fx-font-size: 64px;");
            stickerBox.getChildren().add(placeholder);
        }

        content.getChildren().add(stickerBox);
    }

    /**
     * Build emoji message (large single emoji)
     */
    private void buildEmojiMessage(VBox content, Message msg) {
        VBox emojiBox = new VBox(4);
        emojiBox.setAlignment(Pos.CENTER);

        Label emoji = new Label(msg.getContent());
        emoji.setStyle("-fx-font-size: 64px; -fx-padding: 8;");

        emojiBox.getChildren().add(emoji);

        // Time below emoji
        Label time = new Label(formatMsgTime(msg.getTimestamp()));
        time.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676b;");
        emojiBox.getChildren().add(time);

        content.getChildren().add(emojiBox);
    }

    private void buildTextBubble(VBox content, Message msg, boolean isSent, boolean consecutive) {
        // Check if message is just emojis (1-3)
        String msgContent = msg.getContent();
        if (isOnlyEmojis(msgContent) && countEmojis(msgContent) <= 3) {
            Label emojiLabel = new Label(msgContent);
            emojiLabel.setStyle("-fx-font-size: 48px; -fx-padding: 4;");
            content.getChildren().add(emojiLabel);
            return;
        }

        // Normal text bubble
        Label bubble = new Label(msgContent);
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

    // ==================== EMOJI DETECTION ====================

    /**
     * Check if string contains only emojis
     */
    private boolean isOnlyEmojis(String text) {
        if (text == null || text.trim().isEmpty()) return false;

        // Remove all emojis and check if anything left
        String withoutEmojis = text.replaceAll("[\\p{So}\\p{Sk}\\p{Cn}]", "").trim();
        return withoutEmojis.isEmpty();
    }

    /**
     * Count emojis in string
     */
    private int countEmojis(String text) {
        if (text == null) return 0;

        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isHighSurrogate(text.charAt(i))) {
                count++;
                i++; // Skip low surrogate
            }
        }
        return count;
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

    // ==================== INFO SIDEBAR ====================

    public ScrollPane createInfoSidebarContent(Conversation conv) {
        ConversationInfoBuilder infoBuilder = new ConversationInfoBuilder();
        return infoBuilder.createInfoSidebarContent(conv);
    }

    // ==================== TIME FORMATTING ====================

    public String formatConvTime(LocalDateTime dt) {
        if (dt == null) return "";
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(dt.toLocalDate(), now.toLocalDate());

        if (days == 0) return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (days == 1) return "Hôm qua";
        if (days < 7) return dt.format(DateTimeFormatter.ofPattern("EEE"));
        return dt.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    public String formatMsgTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String extractDate(LocalDateTime dt) {
        if (dt == null) return "Hôm nay";
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(dt.toLocalDate(), now.toLocalDate());

        if (days == 0) return "Hôm nay";
        if (days == 1) return "Hôm qua";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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