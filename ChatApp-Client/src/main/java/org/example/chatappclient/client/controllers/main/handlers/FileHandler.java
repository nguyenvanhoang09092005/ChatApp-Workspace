package org.example.chatappclient.client.controllers.main.handlers;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.protocol.Protocol;
import org.example.chatappclient.client.utils.ui.AlertUtil;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * FileHandler - Upload file với đồng bộ hoàn hảo
 * FIX: Lưu MESSAGE_RECEIVE nếu gặp khi chờ SUCCESS
 */
public class FileHandler {

    private final MainController mainController;
    private final SocketClient socketClient;
    private final ExecutorService executor;

    // File size limits
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;
    private static final long MAX_AUDIO_SIZE = 20 * 1024 * 1024;
    private static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024;
    private static final long MAX_ARCHIVE_SIZE = 100 * 1024 * 1024;

    public FileHandler(MainController mainController) {
        this.mainController = mainController;
        this.socketClient = SocketClient.getInstance();
        this.executor = Executors.newCachedThreadPool();
    }

    public void selectImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn hình ảnh");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Hình ảnh",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp", "*.svg")
        );

        File file = showFileChooser(chooser);
        if (file != null) {
            if (file.length() > MAX_IMAGE_SIZE) {
                AlertUtil.showToastError("Hình ảnh quá lớn (tối đa 10MB)");
                return;
            }
            uploadFileToServer(file, FileType.IMAGE);
        }
    }

    public void selectFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tất cả file", "*.*"),
                new FileChooser.ExtensionFilter("Hình ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = showFileChooser(chooser);
        if (file != null) {
            FileType type = detectFileType(file);
            long maxSize = getMaxSize(type);

            if (file.length() > maxSize) {
                AlertUtil.showToastError("File quá lớn (tối đa " + formatSize(maxSize) + ")");
                return;
            }

            uploadFileToServer(file, type);
        }
    }

    // Thay thế toàn bộ method uploadFileToServer bằng đoạn này

    private void uploadFileToServer(File file, FileType type) {
        String conversationId = mainController.getCurrentConversationId();
        if (conversationId == null) {
            AlertUtil.showToastError("Vui lòng chọn cuộc trò chuyện");
            return;
        }

        String userId = mainController.getCurrentUser().getUserId();
        String tempId = "loading-" + System.currentTimeMillis();

        System.out.println("\nUPLOAD FILE START ==========");
        System.out.println("File: " + file.getName());
        System.out.println("Size: " + formatSize(file.length()));
        System.out.println("Temp ID: " + tempId);

        // Tạo và hiển thị loading ngay lập tức (chỉ ở người gửi)
        VBox loadingView = createSimpleLoadingPreview(file, type);
        loadingView.setId(tempId);

        Platform.runLater(() -> {
            mainController.addLoadingMessageToUI(loadingView);
            mainController.scrollToBottom();
        });

        executor.submit(() -> {
            try {
                // Gửi lệnh upload
                String request = Protocol.buildRequest(
                        Protocol.FILE_UPLOAD,
                        conversationId,
                        userId,
                        file.getName(),
                        type.name().toLowerCase(),
                        String.valueOf(file.length())
                );

                socketClient.sendMessage(request);
                Thread.sleep(100);
                sendFileData(file);

                System.out.println("File sent, waiting for MESSAGE_RECEIVE...");

                // CHỜ MESSAGE_RECEIVE - đây là tin nhắn thật từ server
                String messageReceive = null;
                int attempts = 0;
                while (attempts < 20 && messageReceive == null) {
                    String resp = socketClient.receiveMessage();
                    if (resp != null && resp.startsWith("MESSAGE_RECEIVE")) {
                        messageReceive = resp;
                        System.out.println("Received MESSAGE_RECEIVE → done!");
                    }
                    attempts++;
                    Thread.sleep(200);
                }

                if (messageReceive == null) {
                    throw new Exception("Timeout: Không nhận được phản hồi từ server");
                }

                Message realMessage = parseMessageReceive(messageReceive);
                if (realMessage == null) throw new Exception("Parse MESSAGE_RECEIVE thất bại");

                // XÓA LOADING + THÊM TIN NHẮN THẬT (chỉ chạy ở người gửi)
                Platform.runLater(() -> {
                    mainController.removeLoadingMessageFromUI(loadingView);
                    mainController.addMessageToUI(realMessage);
                    mainController.scrollToBottom();
                });

            } catch (Exception e) {
                System.err.println("Upload failed: " + e.getMessage());
                Platform.runLater(() -> {
                    mainController.removeLoadingMessageFromUI(loadingView);
                    AlertUtil.showToastError("Gửi thất bại: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Parse MESSAGE_RECEIVE thành Message object
     */
    private Message parseMessageReceive(String messageReceive) {
        try {
            String[] parts = messageReceive.split(Pattern.quote(Protocol.DELIMITER));

            if (parts.length < 11) {
                System.err.println("❌ Invalid MESSAGE_RECEIVE format");
                return null;
            }

            String messageId = parts[1];
            String conversationId = parts[2];
            String senderId = parts[3];
            String content = parts[4];
            String messageType = parts[5];
            String mediaUrl = parts[6];
            String senderName = parts[7];
            String senderAvatar = parts[8];
            String fileName = parts.length > 9 ? parts[9] : "";
            long fileSize = parts.length > 10 ? Long.parseLong(parts[10]) : 0;

            Message message = new Message(conversationId, senderId, content);
            message.setMessageId(messageId);
            message.setMessageType(messageType);
            message.setMediaUrl(mediaUrl);
            message.setSenderName(senderName);
            message.setSenderAvatar(senderAvatar);
            message.setFileName(fileName);
            message.setFileSize(fileSize);
            message.setTimestamp(LocalDateTime.now());
            message.setRead(false);

            return message;

        } catch (Exception e) {
            System.err.println("❌ Error parsing MESSAGE_RECEIVE: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private VBox createSimpleLoadingPreview(File file, FileType type) {
        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER_RIGHT);
        container.setPadding(new Insets(8, 16, 8, 16));
        container.setId("loading-" + System.currentTimeMillis());

        HBox loadingBox = new HBox(12);
        loadingBox.setAlignment(Pos.CENTER_LEFT);
        loadingBox.setStyle(
                "-fx-background-color: #E3F2FD; " +
                        "-fx-background-radius: 18; " +
                        "-fx-padding: 12 16 12 16; " +
                        "-fx-max-width: 400px;"
        );

        VBox content = new VBox(8);

        if (type == FileType.IMAGE) {
            try {
                ImageView preview = new ImageView(new Image(file.toURI().toString()));
                preview.setFitWidth(200);
                preview.setPreserveRatio(true);
                preview.setStyle("-fx-opacity: 0.7;");
                content.getChildren().add(preview);
            } catch (Exception e) {
                System.err.println("⚠️ Cannot load image preview");
            }
        }

        HBox fileInfo = new HBox(8);
        fileInfo.setAlignment(Pos.CENTER_LEFT);

        Label fileIcon = new Label(getFileIcon(type));
        fileIcon.setStyle("-fx-font-size: 24px;");

        VBox fileDetails = new VBox(2);

        Label fileNameLabel = new Label(file.getName());
        fileNameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1565C0;");

        Label sizeLabel = new Label(formatSize(file.length()));
        sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64B5F6;");

        fileDetails.getChildren().addAll(fileNameLabel, sizeLabel);
        fileInfo.getChildren().addAll(fileIcon, fileDetails);

        Label statusLabel = new Label("Đang gửi...");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1976D2; -fx-font-weight: 500;");

        content.getChildren().addAll(fileInfo, statusLabel);
        loadingBox.getChildren().add(content);
        container.getChildren().add(loadingBox);

        return container;
    }

    private String getFileIcon(FileType type) {
        return switch (type) {
            case IMAGE -> "🖼️";
            case VIDEO -> "🎥";
            case AUDIO -> "🎵";
            case DOCUMENT -> "📄";
            case ARCHIVE -> "📦";
            case OTHER -> "📎";
        };
    }

    private void sendFileData(File file) throws IOException {
        try (InputStream fileIn = new FileInputStream(file)) {
            OutputStream out = socketClient.getRawOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalSent = 0;

            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
            }

            out.flush();
            System.out.println("✅ Sent " + totalSent + " bytes");
        }
    }

    private FileType detectFileType(File file) {
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp|svg)$")) return FileType.IMAGE;
        if (name.matches(".*\\.(mp4|avi|mov|mkv|webm)$")) return FileType.VIDEO;
        if (name.matches(".*\\.(mp3|wav|ogg|m4a|flac)$")) return FileType.AUDIO;
        if (name.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt)$")) return FileType.DOCUMENT;
        if (name.matches(".*\\.(zip|rar|7z|tar|gz)$")) return FileType.ARCHIVE;
        return FileType.OTHER;
    }

    private long getMaxSize(FileType type) {
        return switch (type) {
            case IMAGE -> MAX_IMAGE_SIZE;
            case VIDEO -> MAX_VIDEO_SIZE;
            case AUDIO -> MAX_AUDIO_SIZE;
            case DOCUMENT -> MAX_DOCUMENT_SIZE;
            case ARCHIVE -> MAX_ARCHIVE_SIZE;
            case OTHER -> MAX_DOCUMENT_SIZE;
        };
    }

    private File showFileChooser(FileChooser chooser) {
        Window window = mainController.getLeftSidebar().getScene().getWindow();
        return chooser.showOpenDialog(window);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private enum FileType {
        IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER
    }
}