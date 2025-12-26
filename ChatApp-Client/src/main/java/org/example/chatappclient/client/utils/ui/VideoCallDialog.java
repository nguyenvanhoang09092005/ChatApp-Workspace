package org.example.chatappclient.client.utils.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

/**
 * ✅ FIXED v3: Video Call Dialog với Webcam Lock Management
 */
public class VideoCallDialog {

    private final Stage stage;
    private final String partnerName;

    // UI Components
    private StackPane mainVideoContainer;
    private StackPane selfVideoContainer;
    private ImageView mainVideoView;
    private ImageView selfVideoView;
    private Label statusLabel;
    private Label timerLabel;
    private VBox placeholder;

    // Control Buttons
    private Button muteButton;
    private Button videoButton;
    private Button switchCameraButton;
    private Button endCallButton;

    // State
    private boolean isMuted = false;
    private boolean isVideoEnabled = true;
    private boolean isSpeakerOn = true;
    private boolean isConnected = false;

    // Webcam
    private volatile Webcam webcam;
    private final ExecutorService executorService;
    private final AtomicBoolean isCapturing;
    private final AtomicBoolean isSwitchingCamera;
    private int currentCameraIndex = 0;

    // Timer
    private Timeline callTimer;
    private int callDurationSeconds = 0;

    // Callbacks
    private Runnable onMuteToggle;
    private Runnable onVideoToggle;
    private Runnable onSwitchCamera;
    private Runnable onEndCall;
    private VideoDataCallback onVideoData;

    public VideoCallDialog(String partnerName) {
        this.partnerName = partnerName;
        this.executorService = Executors.newFixedThreadPool(3);
        this.isCapturing = new AtomicBoolean(false);
        this.isSwitchingCamera = new AtomicBoolean(false);
        this.stage = createStage();
    }

    private Stage createStage() {
        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Video Call - " + partnerName);
        stage.setResizable(true);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #000000;");
        root.setPrefSize(1200, 720);

        root.setCenter(createVideoArea());
        root.setTop(createTopBar());
        root.setBottom(createControls());

        Scene scene = new Scene(root);
        scene.setFill(Color.BLACK);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> stopAllCapture());

        return stage;
    }

    // ==================== TOP BAR ====================

    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 30, 15, 30));
        topBar.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.7), transparent);");

        VBox partnerInfo = new VBox(5);
        partnerInfo.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(partnerName);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setEffect(new DropShadow(5, Color.BLACK));

        statusLabel = new Label("Đang gọi...");
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        statusLabel.setTextFill(Color.web("#a0a0a0"));

        partnerInfo.getChildren().addAll(nameLabel, statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                "-fx-background-radius: 20; -fx-padding: 8 16;");
        timerLabel.setVisible(false);

        topBar.getChildren().addAll(partnerInfo, spacer, timerLabel);
        return topBar;
    }

    // ==================== VIDEO AREA ====================

    private StackPane createVideoArea() {
        StackPane videoArea = new StackPane();
        videoArea.setStyle("-fx-background-color: #000000;");

        mainVideoContainer = createMainVideoContainer();
        selfVideoContainer = createSelfVideoContainer();
        StackPane.setAlignment(selfVideoContainer, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(selfVideoContainer, new Insets(0, 30, 120, 0));

        videoArea.getChildren().addAll(mainVideoContainer, selfVideoContainer);
        return videoArea;
    }

    private StackPane createMainVideoContainer() {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #1a1a1a;");

        mainVideoView = new ImageView();
        mainVideoView.setPreserveRatio(true);
        mainVideoView.fitWidthProperty().bind(container.widthProperty());
        mainVideoView.fitHeightProperty().bind(container.heightProperty());

        placeholder = createPlaceholder();
        container.getChildren().addAll(mainVideoView, placeholder);

        return container;
    }

    private VBox createPlaceholder() {
        VBox placeholderBox = new VBox(30);
        placeholderBox.setAlignment(Pos.CENTER);

        Circle avatarBg = new Circle(100);
        avatarBg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#667eea")),
                new Stop(1, Color.web("#764ba2"))));

        Label avatarIcon = new Label("👤");
        avatarIcon.setFont(Font.font(72));
        avatarIcon.setTextFill(Color.WHITE);

        StackPane avatarStack = new StackPane(avatarBg, avatarIcon);

        Label nameLabel = new Label(partnerName);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        nameLabel.setTextFill(Color.WHITE);

        placeholderBox.getChildren().addAll(avatarStack, nameLabel);
        return placeholderBox;
    }

    private StackPane createSelfVideoContainer() {
        StackPane container = new StackPane();
        container.setPrefSize(280, 210);
        container.setMaxSize(280, 210);

        Rectangle background = new Rectangle(280, 210);
        background.setArcWidth(20);
        background.setArcHeight(20);
        background.setFill(Color.rgb(0, 0, 0, 0.5));
        background.setStroke(Color.rgb(255, 255, 255, 0.2));
        background.setStrokeWidth(2);

        selfVideoView = new ImageView();
        selfVideoView.setFitWidth(280);
        selfVideoView.setFitHeight(210);
        selfVideoView.setPreserveRatio(true);

        Rectangle clip = new Rectangle(280, 210);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        selfVideoView.setClip(clip);

        Label selfLabel = new Label("Bạn");
        selfLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        selfLabel.setTextFill(Color.WHITE);
        selfLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); " +
                "-fx-background-radius: 15; -fx-padding: 6 12;");
        StackPane.setAlignment(selfLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(selfLabel, new Insets(0, 0, 10, 0));

        container.getChildren().addAll(background, selfVideoView, selfLabel);
        container.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.7)));

        return container;
    }

    // ==================== CONTROLS ====================

    private HBox createControls() {
        HBox controlButtons = new HBox(20);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.setPadding(new Insets(30, 40, 40, 40));
        controlButtons.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.8), transparent);");

        muteButton = createRoundButton("🎤", "#4CAF50", 70);
        muteButton.setOnAction(e -> toggleMute());

        videoButton = createRoundButton("📹", "#2196F3", 70);
        videoButton.setOnAction(e -> toggleVideo());

        switchCameraButton = createRoundButton("🔄", "#FF9800", 70);
        switchCameraButton.setOnAction(e -> switchCamera());

        endCallButton = createRoundButton("📞", "#F44336", 80);
        endCallButton.setStyle(endCallButton.getStyle() +
                " -fx-effect: dropshadow(gaussian, rgba(244, 67, 54, 0.6), 20, 0, 0, 0);");
        endCallButton.setOnAction(e -> endCall());

        controlButtons.getChildren().addAll(
                muteButton, videoButton, switchCameraButton, endCallButton
        );

        return controlButtons;
    }

    private Button createRoundButton(String emoji, String color, int size) {
        Button button = new Button(emoji);
        button.setFont(Font.font(size * 0.4));
        button.setStyle(String.format(
                "-fx-background-color: %s; " +
                        "-fx-background-radius: 50; " +
                        "-fx-pref-width: %d; -fx-pref-height: %d; " +
                        "-fx-text-fill: white; -fx-cursor: hand;",
                color, size, size
        ));

        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.play();
        });

        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        return button;
    }

    // ==================== ✅ WEBCAM CONTROL - FIXED v3 ====================

    private void startWebcam() {
        executorService.submit(() -> {
            try {
                // ✅ Đợi nếu đang switch camera
                while (isSwitchingCamera.get()) {
                    Thread.sleep(50);
                }

                var webcams = Webcam.getWebcams();
                if (webcams.isEmpty()) {
                    Platform.runLater(() -> showToast("Không tìm thấy webcam"));
                    return;
                }

                // ✅ Đảm bảo webcam cũ đã đóng
                closeWebcamSafely();
                Thread.sleep(500); // Đợi lâu hơn để OS release resource

                // ✅ Lấy webcam mới
                Webcam newWebcam = webcams.get(currentCameraIndex);

                // ✅ Đảm bảo webcam này không bị lock
                if (newWebcam.isOpen()) {
                    System.out.println("⚠️ Webcam đang được sử dụng, đóng lại...");
                    newWebcam.close();
                    Thread.sleep(500);
                }

                // ✅ Set webcam trước khi mở
                this.webcam = newWebcam;
                webcam.setViewSize(WebcamResolution.VGA.getSize());
                webcam.open();

                isCapturing.set(true);
                System.out.println("✅ Webcam started - Sending video frames...");

                while (isCapturing.get() && webcam != null && webcam.isOpen()) {
                    // ✅ Check nếu đang switch thì dừng loop
                    if (isSwitchingCamera.get()) {
                        break;
                    }

                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        // Hiển thị trên self video view
                        Platform.runLater(() -> {
                            WritableImage fxImage = convertToFxImage(image);
                            selfVideoView.setImage(fxImage);
                        });

                        // ✅ Gửi video frame qua callback
                        if (onVideoData != null && isVideoEnabled) {
                            onVideoData.onVideoFrame(image);
                        }
                    }

                    Thread.sleep(33); // ~30 FPS
                }
            } catch (Exception e) {
                if (!e.getMessage().contains("already been locked")) {
                    e.printStackTrace();
                    Platform.runLater(() -> showToast("Lỗi webcam: " + e.getMessage()));
                }
            } finally {
                // ✅ Đảm bảo webcam được đóng khi thread kết thúc
                if (!isSwitchingCamera.get()) {
                    closeWebcamSafely();
                }
            }
        });
    }

    private void stopWebcam() {
        System.out.println("🛑 Stopping webcam...");
        isCapturing.set(false);

        // ✅ Đợi thread capture dừng
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        closeWebcamSafely();
    }

    /**
     * ✅ Đóng webcam an toàn với error handling
     */
    private synchronized void closeWebcamSafely() {
        if (webcam != null) {
            try {
                if (webcam.isOpen()) {
                    webcam.close();
                    System.out.println("✅ Webcam closed safely");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error closing webcam: " + e.getMessage());
            } finally {
                webcam = null;
            }
        }
    }

    private void switchCamera() {
        // ✅ Ngăn multiple switch cùng lúc
        if (isSwitchingCamera.get()) {
            System.out.println("⚠️ Already switching camera, please wait...");
            return;
        }

        isSwitchingCamera.set(true);
        System.out.println("🔄 Switching camera...");

        executorService.submit(() -> {
            try {
                // ✅ Dừng webcam hiện tại hoàn toàn
                isCapturing.set(false);
                Thread.sleep(200);
                closeWebcamSafely();
                Thread.sleep(500); // Đợi OS release resource

                var webcams = Webcam.getWebcams();
                if (webcams.isEmpty()) {
                    Platform.runLater(() -> showToast("Không tìm thấy webcam"));
                    return;
                }

                currentCameraIndex = (currentCameraIndex + 1) % webcams.size();

                Platform.runLater(() -> {
                    RotateTransition rotate = new RotateTransition(Duration.millis(500), switchCameraButton);
                    rotate.setByAngle(360);
                    rotate.play();
                });

                // ✅ Đợi animation + thêm delay
                Thread.sleep(800);

                // ✅ Chỉ bật lại nếu video đang enabled
                if (isVideoEnabled) {
                    startWebcam();
                    Platform.runLater(() -> showToast("Đã chuyển camera " + (currentCameraIndex + 1)));
                }

                if (onSwitchCamera != null) {
                    onSwitchCamera.run();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showToast("Lỗi chuyển camera: " + e.getMessage()));
            } finally {
                isSwitchingCamera.set(false);
            }
        });
    }

    private WritableImage convertToFxImage(BufferedImage bufferedImage) {
        WritableImage writableImage = new WritableImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight()
        );
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                int rgb = bufferedImage.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xff;
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;

                Color color = Color.rgb(red, green, blue, alpha / 255.0);
                pixelWriter.setColor(x, y, color);
            }
        }

        return writableImage;
    }

    // ==================== ✅ RECEIVE VIDEO ====================

    public void receiveVideoFrame(BufferedImage frame) {
        if (frame != null) {
            Platform.runLater(() -> {
                WritableImage fxImage = convertToFxImage(frame);
                mainVideoView.setImage(fxImage);

                // Ẩn placeholder khi có video
                if (placeholder != null && placeholder.isVisible()) {
                    placeholder.setVisible(false);
                }
            });
        }
    }

    // ==================== ACTIONS ====================

    private void toggleMute() {
        isMuted = !isMuted;

        if (isMuted) {
            muteButton.setText("🔇");
            muteButton.setStyle(muteButton.getStyle().replaceAll(
                    "-fx-background-color: #[0-9A-Fa-f]{6};",
                    "-fx-background-color: #E53935;"));
            showToast("Đã tắt micro");
        } else {
            muteButton.setText("🎤");
            muteButton.setStyle(muteButton.getStyle().replaceAll(
                    "-fx-background-color: #[0-9A-Fa-f]{6};",
                    "-fx-background-color: #4CAF50;"));
            showToast("Đã bật micro");
        }

        if (onMuteToggle != null) {
            onMuteToggle.run();
        }
    }

    private void toggleVideo() {
        isVideoEnabled = !isVideoEnabled;

        if (!isVideoEnabled) {
            videoButton.setText("📷");
            videoButton.setStyle(videoButton.getStyle().replaceAll(
                    "-fx-background-color: #[0-9A-Fa-f]{6};",
                    "-fx-background-color: #E53935;"));
            showToast("Đã tắt camera");
            stopWebcam();
            selfVideoView.setImage(null);
        } else {
            videoButton.setText("📹");
            videoButton.setStyle(videoButton.getStyle().replaceAll(
                    "-fx-background-color: #[0-9A-Fa-f]{6};",
                    "-fx-background-color: #2196F3;"));
            showToast("Đã bật camera");

            // ✅ Delay nhỏ trước khi bật lại
            executorService.submit(() -> {
                try {
                    Thread.sleep(300);
                    startWebcam();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        if (onVideoToggle != null) {
            onVideoToggle.run();
        }
    }

    private void endCall() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            stopAllCapture();
            if (onEndCall != null) {
                onEndCall.run();
            }
            stage.close();
        });
        fade.play();
    }

    private void stopAllCapture() {
        stopTimer();
        stopWebcam();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    // ==================== TIMER ====================

    private void startTimer() {
        callDurationSeconds = 0;
        timerLabel.setVisible(true);

        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            callDurationSeconds++;
            updateTimerDisplay();
        }));
        callTimer.setCycleCount(Timeline.INDEFINITE);
        callTimer.play();
    }

    private void stopTimer() {
        if (callTimer != null) {
            callTimer.stop();
        }
    }

    private void updateTimerDisplay() {
        int hours = callDurationSeconds / 3600;
        int minutes = (callDurationSeconds % 3600) / 60;
        int seconds = callDurationSeconds % 60;

        if (hours > 0) {
            timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    // ==================== TOAST ====================

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.setFont(Font.font("System", FontWeight.NORMAL, 14));
        toast.setTextFill(Color.WHITE);
        toast.setStyle("-fx-background-color: rgba(0,0,0,0.8); " +
                "-fx-background-radius: 20; -fx-padding: 10 20;");

        StackPane toastContainer = new StackPane(toast);
        toastContainer.setAlignment(Pos.TOP_CENTER);
        toastContainer.setPadding(new Insets(100, 0, 0, 0));
        toastContainer.setMouseTransparent(true);

        ((Pane) stage.getScene().getRoot()).getChildren().add(toastContainer);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toastContainer);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.setOnFinished(e ->
                ((Pane) stage.getScene().getRoot()).getChildren().remove(toastContainer));

        new SequentialTransition(fadeIn, fadeOut).play();
    }

    // ==================== PUBLIC METHODS ====================

    public void show() {
        stage.show();
        FadeTransition fade = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public void close() {
        stopAllCapture();
        stage.close();
    }

    public void setRinging() {
        statusLabel.setText("Đang gọi...");
        statusLabel.setTextFill(Color.web("#FF9800"));
        timerLabel.setVisible(false);
    }

    public void setConnected() {
        isConnected = true;
        statusLabel.setText("Đang trong cuộc gọi");
        statusLabel.setTextFill(Color.web("#4CAF50"));

        // Bắt đầu capture video khi kết nối
        startWebcam();
        startTimer();
    }

    // ==================== GETTERS ====================

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isVideoEnabled() {
        return isVideoEnabled;
    }

    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // ==================== CALLBACKS ====================

    public void setOnMuteToggle(Runnable callback) {
        this.onMuteToggle = callback;
    }

    public void setOnVideoToggle(Runnable callback) {
        this.onVideoToggle = callback;
    }

    public void setOnSwitchCamera(Runnable callback) {
        this.onSwitchCamera = callback;
    }

    public void setOnEndCall(Runnable callback) {
        this.onEndCall = callback;
    }

    public void setOnVideoData(VideoDataCallback callback) {
        this.onVideoData = callback;
    }

    // ==================== INTERFACES ====================

    @FunctionalInterface
    public interface VideoDataCallback {
        void onVideoFrame(BufferedImage frame);
    }
}