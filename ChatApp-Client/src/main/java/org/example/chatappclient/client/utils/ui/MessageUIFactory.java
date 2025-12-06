package org.example.chatappclient.client.utils.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.chatappclient.client.components.FileViewerComponent;
import org.example.chatappclient.client.models.Message;

import java.time.format.DateTimeFormatter;

/**
 * Factory để tạo UI components cho các loại message khác nhau
 */
public class MessageUIFactory {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SENT_BUBBLE_COLOR = "#0084ff";
    private static final String RECEIVED_BUBBLE_COLOR = "#f0f0f0";

    /**
     * Tạo message bubble dựa trên loại message
     */
    public static HBox createMessageBubble(Message message, boolean isSent) {
        HBox container = new HBox(10);
        container.setPadding(new Insets(5, 10, 5, 10));
        container.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox messageBox = new VBox(5);
        messageBox.setMaxWidth(400);

        // Avatar (chỉ hiển thị cho tin nhắn nhận)
        if (!isSent && message.getSenderAvatar() != null) {
            ImageView avatar = createAvatar(message.getSenderAvatar());
            container.getChildren().add(avatar);
        }

        // Tên người gửi (cho tin nhắn trong group)
        if (!isSent && message.getSenderName() != null) {
            Label senderLabel = new Label(message.getSenderName());
            senderLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-weight: bold;");
            messageBox.getChildren().add(senderLabel);
        }

        // Content bubble dựa theo message type
        Region contentBubble = createContentBubble(message, isSent);
        messageBox.getChildren().add(contentBubble);

        // Timestamp
        Label timeLabel = new Label(message.getTimestamp().format(TIME_FORMAT));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
        timeLabel.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.getChildren().add(timeLabel);

        container.getChildren().add(messageBox);

        // Avatar for sent messages (optional)
        if (isSent && message.getSenderAvatar() != null) {
            ImageView avatar = createAvatar(message.getSenderAvatar());
            container.getChildren().add(avatar);
        }

        return container;
    }

    /**
     * Tạo content bubble theo message type
     */
    private static Region createContentBubble(Message message, boolean isSent) {
        String type = message.getMessageType() != null ? message.getMessageType().toLowerCase() : "text";

        return switch (type) {
            case "image" -> createImageBubble(message, isSent);
            case "video" -> createVideoBubble(message, isSent);
            case "audio", "voice" -> createAudioBubble(message, isSent);
            case "file", "document", "archive" -> createFileBubble(message, isSent);
            case "like" -> createLikeBubble(isSent);
            default -> createTextBubble(message, isSent);
        };
    }

    // ==================== TEXT BUBBLE ====================

    private static Region createTextBubble(Message message, boolean isSent) {
        Label label = new Label(message.getContent());
        label.setWrapText(true);
        label.setMaxWidth(350);
        label.setPadding(new Insets(10, 15, 10, 15));

        String bgColor = isSent ? SENT_BUBBLE_COLOR : RECEIVED_BUBBLE_COLOR;
        String textColor = isSent ? "white" : "black";

        label.setStyle(String.format(
                "-fx-background-color: %s; " +
                        "-fx-background-radius: 18; " +
                        "-fx-text-fill: %s; " +
                        "-fx-font-size: 14px;",
                bgColor, textColor
        ));

        return label;
    }

    // ==================== IMAGE BUBBLE ====================

    private static Region createImageBubble(Message message, boolean isSent) {
        VBox container = new VBox(5);

        // Use FileViewerComponent for images
        FileViewerComponent fileViewer = new FileViewerComponent(message);
        container.getChildren().add(fileViewer);

        // Caption nếu có
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            Label caption = new Label(message.getContent());
            caption.setWrapText(true);
            caption.setMaxWidth(280);
            caption.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");
            container.getChildren().add(caption);
        }

        return container;
    }

    // ==================== VIDEO BUBBLE ====================

    private static Region createVideoBubble(Message message, boolean isSent) {
        VBox container = new VBox(5);

        FileViewerComponent fileViewer = new FileViewerComponent(message);
        container.getChildren().add(fileViewer);

        if (message.getContent() != null && !message.getContent().isEmpty()) {
            Label caption = new Label(message.getContent());
            caption.setWrapText(true);
            caption.setMaxWidth(280);
            caption.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");
            container.getChildren().add(caption);
        }

        return container;
    }

    // ==================== AUDIO BUBBLE ====================

    private static Region createAudioBubble(Message message, boolean isSent) {
        FileViewerComponent fileViewer = new FileViewerComponent(message);
        return fileViewer;
    }

    // ==================== FILE BUBBLE ====================

    private static Region createFileBubble(Message message, boolean isSent) {
        FileViewerComponent fileViewer = new FileViewerComponent(message);
        return fileViewer;
    }

    // ==================== LIKE BUBBLE ====================

    private static Region createLikeBubble(boolean isSent) {
        Label likeLabel = new Label("👍");
        likeLabel.setStyle("-fx-font-size: 48px;");
        likeLabel.setPadding(new Insets(5));
        return likeLabel;
    }

    // ==================== AVATAR ====================

    private static ImageView createAvatar(String avatarUrl) {
        ImageView avatar = new ImageView();
        avatar.setFitWidth(32);
        avatar.setFitHeight(32);
        avatar.setPreserveRatio(true);

        // Circular clip
        Circle clip = new Circle(16, 16, 16);
        avatar.setClip(clip);

        // Load avatar async
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    Image image = new Image(avatarUrl, true);
                    javafx.application.Platform.runLater(() -> avatar.setImage(image));
                } catch (Exception e) {
                    // Use default avatar
                    javafx.application.Platform.runLater(() -> {
                        avatar.setImage(getDefaultAvatar());
                    });
                }
            }).start();
        } else {
            avatar.setImage(getDefaultAvatar());
        }

        return avatar;
    }

    private static Image getDefaultAvatar() {
        // Return a simple colored circle as default
        // In production, use actual default avatar image
        return null; // TODO: Add default avatar resource
    }

    /**
     * Create system message (cho các thông báo hệ thống)
     */
    public static HBox createSystemMessage(String text) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(10));

        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #999; " +
                        "-fx-background-color: #f5f5f5; " +
                        "-fx-background-radius: 12; " +
                        "-fx-padding: 5 10 5 10;"
        );

        container.getChildren().add(label);
        return container;
    }

    /**
     * Create date separator
     */
    public static HBox createDateSeparator(String dateText) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(15, 0, 15, 0));

        Label label = new Label(dateText);
        label.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #666; " +
                        "-fx-background-color: #e0e0e0; " +
                        "-fx-background-radius: 10; " +
                        "-fx-padding: 4 12 4 12; " +
                        "-fx-font-weight: bold;"
        );

        container.getChildren().add(label);
        return container;
    }
}