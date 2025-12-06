package org.example.chatappclient.client.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.utils.ui.AlertUtil;

import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Component hiển thị file trong message với khả năng preview và download
 */
public class FileViewerComponent extends VBox {

    private final Message message;
    private final String fileUrl;
    private final String fileName;
    private final long fileSize;
    private final FileType fileType;

    private ProgressBar downloadProgress;
    private Label statusLabel;

    public FileViewerComponent(Message message) {
        this.message = message;
        this.fileUrl = message.getMediaUrl();
        this.fileName = message.getFileName() != null ? message.getFileName() : "file";
        this.fileSize = message.getFileSize();
        this.fileType = detectFileType(fileName);

        setupUI();
    }

    private void setupUI() {
        setSpacing(8);
        setPadding(new Insets(10));
        setMaxWidth(300);
        setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 10;");

        switch (fileType) {
            case IMAGE -> addImagePreview();
            case VIDEO -> addVideoPreview();
            case AUDIO -> addAudioPreview();
            case DOCUMENT -> addDocumentPreview();
            case ARCHIVE -> addArchivePreview();
            default -> addGenericFilePreview();
        }

        addFileInfo();
        addActionButtons();
    }

    // ==================== IMAGE PREVIEW ====================

    private void addImagePreview() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-radius: 8;");

        // Load image async
        new Thread(() -> {
            try {
                Image image = new Image(fileUrl, true);
                javafx.application.Platform.runLater(() -> {
                    imageView.setImage(image);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    addGenericFilePreview();
                });
            }
        }).start();

        imageView.setCursor(Cursor.HAND);
        imageView.setOnMouseClicked(e -> openImageViewer());

        getChildren().add(imageView);
    }

    // ==================== VIDEO PREVIEW ====================

    private void addVideoPreview() {
        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(280, 150);
        container.setStyle("-fx-background-color: #2c2c2c; -fx-background-radius: 8;");

        // Play icon
        Label playIcon = new Label("▶");
        playIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
        playIcon.setCursor(Cursor.HAND);

        Label videoLabel = new Label("Video: " + fileName);
        videoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        container.getChildren().addAll(playIcon, videoLabel);
        container.setOnMouseClicked(e -> openVideo());

        getChildren().add(container);
    }

    // ==================== AUDIO PREVIEW ====================

    private void addAudioPreview() {
        HBox container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8;");

        // Audio icon
        Label icon = new Label("🎵");
        icon.setStyle("-fx-font-size: 32px;");

        VBox info = new VBox(5);
        Label nameLabel = new Label(fileName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label durationLabel = new Label("Audio file • " + formatSize(fileSize));
        durationLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        info.getChildren().addAll(nameLabel, durationLabel);

        Button playBtn = new Button("▶");
        playBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");
        playBtn.setOnAction(e -> openAudio());

        container.getChildren().addAll(icon, info, playBtn);
        getChildren().add(container);
    }

    // ==================== DOCUMENT PREVIEW ====================

    private void addDocumentPreview() {
        HBox container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 8;");

        // Icon dựa theo loại file
        String icon = getDocumentIcon(fileName);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px;");

        VBox info = new VBox(5);
        Label nameLabel = new Label(fileName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(200);

        Label sizeLabel = new Label(formatSize(fileSize));
        sizeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        info.getChildren().addAll(nameLabel, sizeLabel);

        container.getChildren().addAll(iconLabel, info);
        container.setCursor(Cursor.HAND);
        container.setOnMouseClicked(e -> downloadAndOpen());

        getChildren().add(container);
    }

    // ==================== ARCHIVE PREVIEW ====================

    private void addArchivePreview() {
        HBox container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #f3e5f5; -fx-background-radius: 8;");

        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 32px;");

        VBox info = new VBox(5);
        Label nameLabel = new Label(fileName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(200);

        Label typeLabel = new Label("File nén • " + formatSize(fileSize));
        typeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        info.getChildren().addAll(nameLabel, typeLabel);

        container.getChildren().addAll(icon, info);
        container.setCursor(Cursor.HAND);
        container.setOnMouseClicked(e -> downloadFile());

        getChildren().add(container);
    }

    // ==================== GENERIC FILE ====================

    private void addGenericFilePreview() {
        HBox container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8;");

        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 32px;");

        VBox info = new VBox(5);
        Label nameLabel = new Label(fileName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(200);

        Label sizeLabel = new Label(formatSize(fileSize));
        sizeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        info.getChildren().addAll(nameLabel, sizeLabel);

        container.getChildren().addAll(icon, info);
        getChildren().add(container);
    }

    // ==================== FILE INFO ====================

    private void addFileInfo() {
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        statusLabel.setVisible(false);
        getChildren().add(statusLabel);

        downloadProgress = new ProgressBar(0);
        downloadProgress.setPrefWidth(280);
        downloadProgress.setVisible(false);
        getChildren().add(downloadProgress);
    }

    // ==================== ACTION BUTTONS ====================

    private void addActionButtons() {
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);

        Button downloadBtn = new Button("Tải xuống");
        downloadBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");
        downloadBtn.setOnAction(e -> downloadFile());

        Button openBtn = new Button("Mở");
        openBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
        openBtn.setOnAction(e -> downloadAndOpen());

        buttons.getChildren().addAll(downloadBtn, openBtn);
        getChildren().add(buttons);
    }

    // ==================== DOWNLOAD ====================

    private void downloadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(fileName);
        File saveFile = chooser.showSaveDialog(getScene().getWindow());

        if (saveFile != null) {
            downloadProgress.setVisible(true);
            statusLabel.setVisible(true);
            statusLabel.setText("Đang tải xuống...");

            new Thread(() -> {
                try {
                    downloadFileToPath(fileUrl, saveFile.toPath());

                    javafx.application.Platform.runLater(() -> {
                        downloadProgress.setVisible(false);
                        statusLabel.setText("✅ Đã lưu: " + saveFile.getName());
                        AlertUtil.showToastSuccess("Đã tải xuống: " + fileName);
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        downloadProgress.setVisible(false);
                        statusLabel.setText("❌ Lỗi tải xuống");
                        AlertUtil.showToastError("Không thể tải: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    private void downloadAndOpen() {
        try {
            Path tempFile = Files.createTempFile("chat_", "_" + fileName);

            downloadProgress.setVisible(true);
            statusLabel.setVisible(true);
            statusLabel.setText("Đang tải xuống...");

            new Thread(() -> {
                try {
                    downloadFileToPath(fileUrl, tempFile);

                    javafx.application.Platform.runLater(() -> {
                        downloadProgress.setVisible(false);
                        statusLabel.setText("Đang mở file...");
                        openFileWithDefaultApp(tempFile.toFile());
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        downloadProgress.setVisible(false);
                        statusLabel.setText("❌ Lỗi");
                        AlertUtil.showToastError("Không thể mở: " + e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            AlertUtil.showToastError("Lỗi: " + e.getMessage());
        }
    }

    private void downloadFileToPath(String urlStr, Path destination) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        long fileSize = conn.getContentLengthLong();

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(destination))) {

            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                if (fileSize > 0) {
                    double progress = (double) totalRead / fileSize;
                    javafx.application.Platform.runLater(() ->
                            downloadProgress.setProgress(progress)
                    );
                }
            }
        }
    }

    // ==================== OPEN FILES ====================

    private void openFileWithDefaultApp(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
                statusLabel.setText("✅ Đã mở file");
            } else {
                statusLabel.setText("Không hỗ trợ mở file");
            }
        } catch (Exception e) {
            AlertUtil.showToastError("Không thể mở: " + e.getMessage());
        }
    }

    private void openImageViewer() {
        // TODO: Implement image viewer dialog
        downloadAndOpen();
    }

    private void openVideo() {
        try {
            Desktop.getDesktop().browse(new URI(fileUrl));
        } catch (Exception e) {
            downloadAndOpen();
        }
    }

    private void openAudio() {
        downloadAndOpen();
    }

    // ==================== HELPERS ====================

    private FileType detectFileType(String fileName) {
        String name = fileName.toLowerCase();

        if (name.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp|svg)$")) {
            return FileType.IMAGE;
        } else if (name.matches(".*\\.(mp4|avi|mov|mkv|webm|flv)$")) {
            return FileType.VIDEO;
        } else if (name.matches(".*\\.(mp3|wav|ogg|m4a|flac)$")) {
            return FileType.AUDIO;
        } else if (name.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt)$")) {
            return FileType.DOCUMENT;
        } else if (name.matches(".*\\.(zip|rar|7z|tar|gz)$")) {
            return FileType.ARCHIVE;
        }
        return FileType.OTHER;
    }

    private String getDocumentIcon(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".pdf")) return "📕";
        if (name.endsWith(".doc") || name.endsWith(".docx")) return "📘";
        if (name.endsWith(".xls") || name.endsWith(".xlsx")) return "📊";
        if (name.endsWith(".ppt") || name.endsWith(".pptx")) return "📙";
        if (name.endsWith(".txt")) return "📄";
        return "📋";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private enum FileType {
        IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER
    }
}