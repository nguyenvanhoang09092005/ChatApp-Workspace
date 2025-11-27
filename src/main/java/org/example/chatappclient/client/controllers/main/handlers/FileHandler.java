package org.example.chatappclient.client.controllers.main.handlers;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.services.FileUploadService;
import org.example.chatappclient.client.utils.ui.AlertUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler xử lý upload file, hình ảnh, ghi âm
 */
public class FileHandler {

    private final MainController mainController;
    private final FileUploadService uploadService;
    private final ExecutorService executor;

    // Recording state
    private boolean isRecording = false;

    // File size limits (in bytes)
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;  // 10MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;   // 50MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    public FileHandler(MainController mainController) {
        this.mainController = mainController;
        this.uploadService = FileUploadService.getInstance();
        this.executor = Executors.newCachedThreadPool();
    }

    // ==================== IMAGE SELECTION ====================

    public void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Hình ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("Tất cả", "*.*")
        );

        Window window = mainController.getLeftSidebar().getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            if (file.length() > MAX_IMAGE_SIZE) {
                AlertUtil.showToastError("Hình ảnh quá lớn (tối đa 10MB)");
                return;
            }
            uploadImage(file);
        }
    }

    public void selectMultipleImages() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Hình ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        Window window = mainController.getLeftSidebar().getScene().getWindow();
        java.util.List<File> files = fileChooser.showOpenMultipleDialog(window);

        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                if (file.length() <= MAX_IMAGE_SIZE) {
                    uploadImage(file);
                }
            }
        }
    }

    private void uploadImage(File file) {
        AlertUtil.showToastInfo("Đang tải lên: " + file.getName());

        executor.submit(() -> {
            try {
                FileUploadService.UploadResult result = uploadService.uploadImage(file);

                if (result.isSuccess()) {
                    Platform.runLater(() -> {
                        MessageHandler.getInstance().sendImage(result.getUrl(), file.getName());
                        AlertUtil.showToastSuccess("Đã gửi hình ảnh");
                    });
                } else {
                    Platform.runLater(() -> AlertUtil.showToastError("Tải lên thất bại: " + result.getError()));
                }

            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Lỗi: " + e.getMessage()));
            }
        });
    }

    // ==================== FILE SELECTION ====================

    public void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file đính kèm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tài liệu", "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx", "*.ppt", "*.pptx"),
                new FileChooser.ExtensionFilter("Nén", "*.zip", "*.rar", "*.7z"),
                new FileChooser.ExtensionFilter("Tất cả", "*.*")
        );

        Window window = mainController.getLeftSidebar().getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            if (file.length() > MAX_FILE_SIZE) {
                AlertUtil.showToastError("File quá lớn (tối đa 50MB)");
                return;
            }
            uploadFile(file);
        }
    }

    private void uploadFile(File file) {
        AlertUtil.showToastInfo("Đang tải lên: " + file.getName());

        executor.submit(() -> {
            try {
                FileUploadService.UploadResult result = uploadService.uploadFile(file);

                if (result.isSuccess()) {
                    Platform.runLater(() -> {
                        MessageHandler.getInstance().sendFile(result.getUrl(), file.getName(), file.length());
                        AlertUtil.showToastSuccess("Đã gửi file");
                    });
                } else {
                    Platform.runLater(() -> AlertUtil.showToastError("Tải lên thất bại: " + result.getError()));
                }

            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Lỗi: " + e.getMessage()));
            }
        });
    }

    // ==================== VIDEO SELECTION ====================

    public void selectVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn video");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.webm")
        );

        Window window = mainController.getLeftSidebar().getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            if (file.length() > MAX_VIDEO_SIZE) {
                AlertUtil.showToastError("Video quá lớn (tối đa 100MB)");
                return;
            }
            uploadVideo(file);
        }
    }

    private void uploadVideo(File file) {
        AlertUtil.showToastInfo("Đang tải lên video: " + file.getName());

        executor.submit(() -> {
            try {
                FileUploadService.UploadResult result = uploadService.uploadVideo(file);

                if (result.isSuccess()) {
                    Platform.runLater(() -> {
                        MessageHandler.getInstance().sendVideo(result.getUrl(), file.getName(), file.length());
                        AlertUtil.showToastSuccess("Đã gửi video");
                    });
                } else {
                    Platform.runLater(() -> AlertUtil.showToastError("Tải lên thất bại: " + result.getError()));
                }

            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Lỗi: " + e.getMessage()));
            }
        });
    }

    // ==================== STICKER ====================

    public void showStickerPicker() {
        // TODO: Show sticker picker dialog
        AlertUtil.showToastInfo("Chọn sticker");
    }

    // ==================== VOICE RECORDING ====================

    public void recordVoice() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (mainController.getCurrentConversationId() == null) {
            AlertUtil.showToastWarning("Vui lòng chọn một cuộc trò chuyện");
            return;
        }

        isRecording = true;
        AlertUtil.showToastInfo("Đang ghi âm... Nhấn lại để dừng");

        // TODO: Implement actual audio recording
        // Use JavaFX MediaRecorder or javax.sound.sampled
    }

    private void stopRecording() {
        isRecording = false;
        AlertUtil.showToastInfo("Đang xử lý tin nhắn thoại...");

        // TODO: Stop recording, get audio file, upload and send
        executor.submit(() -> {
            try {
                // Simulate recording process
                Thread.sleep(500);

                // After getting audio file:
                // FileUploadService.UploadResult result = uploadService.uploadAudio(audioFile);
                // if (result.isSuccess()) {
                //     MessageHandler.getInstance().sendVoice(result.getUrl(), durationSeconds);
                // }

                Platform.runLater(() -> AlertUtil.showToastSuccess("Đã gửi tin nhắn thoại"));

            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Lỗi ghi âm"));
            }
        });
    }

    public boolean isRecording() {
        return isRecording;
    }

    // ==================== HELPERS ====================

    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ==================== CLEANUP ====================

    public void cleanup() {
        if (isRecording) {
            stopRecording();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}