package org.example.chatappclient.client.utils.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.chatappclient.client.components.FileViewerComponent;
import org.example.chatappclient.client.models.Message;

import java.time.format.DateTimeFormatter;

public class MessageUIFactory {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Modern gradient colors
    private static final String SENT_BUBBLE_GRADIENT = "linear-gradient(to right, #0084ff, #00a8ff)";
    private static final String RECEIVED_BUBBLE_COLOR = "#f5f5f7";

    /**
     * Tạo message bubble dựa trên loại message
     */
    public static HBox createMessageBubble(Message message, boolean isSent) {
        HBox container = new HBox(12);
        container.setPadding(new Insets(8, 15, 8, 15));
        container.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox messageBox = new VBox(6);
        messageBox.setMaxWidth(420);

        // Avatar (chỉ hiển thị cho tin nhắn nhận)
        if (!isSent && message.getSenderAvatar() != null) {
            ImageView avatar = createAvatar(message.getSenderAvatar());
            container.getChildren().add(avatar);
        }

        // Tên người gửi (cho tin nhắn trong group)
        if (!isSent && message.getSenderName() != null) {
            Label senderLabel = new Label(message.getSenderName());
            senderLabel.setStyle(
                    "-fx-font-size: 12px; " +
                            "-fx-text-fill: #65676b; " +
                            "-fx-font-weight: 600; " +
                            "-fx-padding: 0 0 2 8;"
            );
            messageBox.getChildren().add(senderLabel);
        }

        // Content bubble dựa theo message type
        Region contentBubble = createContentBubble(message, isSent);

        // Add subtle shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.08));
        shadow.setRadius(8);
        shadow.setOffsetY(2);
        contentBubble.setEffect(shadow);

        messageBox.getChildren().add(contentBubble);

        // Timestamp with improved styling
        Label timeLabel = new Label(message.getTimestamp().format(TIME_FORMAT));
        timeLabel.setStyle(
                "-fx-font-size: 11px; " +
                        "-fx-text-fill: #8a8d91; " +
                        "-fx-font-weight: 500; " +
                        "-fx-padding: 2 8 0 8;"
        );
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
        label.setMaxWidth(360);
        label.setPadding(new Insets(12, 16, 12, 16));

        if (isSent) {
            label.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #0084ff, #0073e6); " +
                            "-fx-background-radius: 20 20 4 20; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: 400; " +
                            "-fx-line-spacing: 2px;"
            );
        } else {
            label.setStyle(
                    "-fx-background-color: " + RECEIVED_BUBBLE_COLOR + "; " +
                            "-fx-background-radius: 20 20 20 4; " +
                            "-fx-text-fill: #1c1e21; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: 400; " +
                            "-fx-line-spacing: 2px;"
            );
        }

        return label;
    }

    // ==================== IMAGE BUBBLE ====================
    private static Region createImageBubble(Message message, boolean isSent) {
        VBox container = new VBox(8);
        container.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 16;"
        );

        // Use FileViewerComponent for images
        FileViewerComponent fileViewer = new FileViewerComponent(message);
        fileViewer.setStyle("-fx-background-radius: 16;");
        container.getChildren().add(fileViewer);

        // Caption nếu có
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            Label caption = new Label(message.getContent());
            caption.setWrapText(true);
            caption.setMaxWidth(300);
            caption.setPadding(new Insets(8, 12, 8, 12));

            String captionBg = isSent ? "#0084ff" : "#f0f0f0";
            String captionColor = isSent ? "white" : "#1c1e21";

            caption.setStyle(
                    "-fx-font-size: 13px; " +
                            "-fx-text-fill: " + captionColor + "; " +
                            "-fx-background-color: " + captionBg + "; " +
                            "-fx-background-radius: 12; " +
                            "-fx-font-weight: 400;"
            );
            container.getChildren().add(caption);
        }

        return container;
    }

    // ==================== VIDEO BUBBLE ====================
    private static Region createVideoBubble(Message message, boolean isSent) {
        VBox container = new VBox(8);
        container.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 16;"
        );

        FileViewerComponent fileViewer = new FileViewerComponent(message);
        fileViewer.setStyle("-fx-background-radius: 16;");
        container.getChildren().add(fileViewer);

        if (message.getContent() != null && !message.getContent().isEmpty()) {
            Label caption = new Label(message.getContent());
            caption.setWrapText(true);
            caption.setMaxWidth(300);
            caption.setPadding(new Insets(8, 12, 8, 12));

            String captionBg = isSent ? "#0084ff" : "#f0f0f0";
            String captionColor = isSent ? "white" : "#1c1e21";

            caption.setStyle(
                    "-fx-font-size: 13px; " +
                            "-fx-text-fill: " + captionColor + "; " +
                            "-fx-background-color: " + captionBg + "; " +
                            "-fx-background-radius: 12; " +
                            "-fx-font-weight: 400;"
            );
            container.getChildren().add(caption);
        }

        return container;
    }

    // ==================== AUDIO BUBBLE ====================
    private static Region createAudioBubble(Message message, boolean isSent) {
        FileViewerComponent fileViewer = new FileViewerComponent(message);

        String bgColor = isSent ? "#0084ff" : "#f5f5f7";
        fileViewer.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 10;"
        );

        return fileViewer;
    }

    // ==================== FILE BUBBLE ====================
    private static Region createFileBubble(Message message, boolean isSent) {
        FileViewerComponent fileViewer = new FileViewerComponent(message);

        String bgColor = isSent ? "#e8f4ff" : "#f5f5f7";
        fileViewer.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-background-radius: 16; " +
                        "-fx-padding: 12; " +
                        "-fx-border-color: " + (isSent ? "#b3d9ff" : "#e0e0e0") + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 16;"
        );

        return fileViewer;
    }

    // ==================== LIKE BUBBLE ====================
    private static Region createLikeBubble(boolean isSent) {
        StackPane container = new StackPane();

        Label likeLabel = new Label("👍");
        likeLabel.setStyle(
                "-fx-font-size: 56px; " +
                        "-fx-padding: 8;"
        );

        // Add pulse effect background
        Circle background = new Circle(40);
        background.setFill(Color.rgb(0, 132, 255, 0.1));

        container.getChildren().addAll(background, likeLabel);

        return container;
    }

    // ==================== AVATAR ====================
    private static ImageView createAvatar(String avatarUrl) {
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);

        // Circular clip
        Circle clip = new Circle(18, 18, 18);
        avatar.setClip(clip);

        // Add subtle border effect
        DropShadow avatarShadow = new DropShadow();
        avatarShadow.setColor(Color.rgb(0, 0, 0, 0.12));
        avatarShadow.setRadius(4);
        avatar.setEffect(avatarShadow);

        // Load avatar async
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    Image image = new Image(avatarUrl, true);
                    javafx.application.Platform.runLater(() -> avatar.setImage(image));
                } catch (Exception e) {
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
        container.setPadding(new Insets(12));

        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: #65676b; " +
                        "-fx-background-color: rgba(0, 0, 0, 0.05); " +
                        "-fx-background-radius: 14; " +
                        "-fx-padding: 6 14 6 14; " +
                        "-fx-font-weight: 500;"
        );

        // Subtle shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.05));
        shadow.setRadius(4);
        label.setEffect(shadow);

        container.getChildren().add(label);
        return container;
    }

    /**
     * Create date separator
     */
    public static HBox createDateSeparator(String dateText) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20, 0, 20, 0));

        Label label = new Label(dateText);
        label.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-color: linear-gradient(to right, #8e8e93, #6c6c70); " +
                        "-fx-background-radius: 12; " +
                        "-fx-padding: 6 16 6 16; " +
                        "-fx-font-weight: 600; " +
                        "-fx-letter-spacing: 0.3px;"
        );

        // Enhanced shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        shadow.setRadius(6);
        shadow.setOffsetY(2);
        label.setEffect(shadow);

        container.getChildren().add(label);
        return container;
    }
}