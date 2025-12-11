package org.example.chatappclient.client.utils.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Modern Audio Call Dialog - Hoàn chỉnh với PNG Icons
 */
public class AudioCallDialog {

    private Stage stage;
    private Label statusLabel;
    private Label durationLabel;
    private Button muteBtn;
    private Button speakerBtn;
    private Button endCallBtn;
    private ImageView avatarView;
    private StackPane waveformContainer;

    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private Timeline callTimer;
    private int seconds = 0;

    private Runnable onMuteToggle;
    private Runnable onSpeakerToggle;
    private Runnable onEndCall;

    // ICON PATHS - Sử dụng PNG icons
    private static final String ICON_MIC_ON = "/icons/mic_on.png";
    private static final String ICON_MIC_OFF = "/icons/mic_off.png";
    private static final String ICON_SPEAKER_ON = "/icons/speaker_on.png";
    private static final String ICON_SPEAKER_OFF = "/icons/speaker_off.png";
    private static final String ICON_HANG_UP = "/icons/hang_up.png";

    public AudioCallDialog(String partnerName, String avatarUrl) {
        createDialog(partnerName, avatarUrl);
    }

    private void createDialog(String partnerName, String avatarUrl) {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.NONE);

        // Main container
        StackPane root = new StackPane();
        root.setPrefSize(420, 680);

        // Background gradient với blur
        VBox background = createGradientBackground();

        // Content container
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50, 40, 40, 40));

        // Header với close button
        HBox header = createHeader();

        // Avatar với animated waveform
        StackPane avatarSection = createAvatarSection(avatarUrl);

        // User info
        VBox userInfo = new VBox(8);
        userInfo.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(partnerName);
        nameLabel.setStyle("""
            -fx-font-size: 28px;
            -fx-font-weight: 700;
            -fx-text-fill: white;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);
            """);

        statusLabel = new Label("Đang kết nối...");
        statusLabel.setStyle("""
            -fx-font-size: 16px;
            -fx-text-fill: rgba(255, 255, 255, 0.85);
            """);

        durationLabel = new Label("00:00");
        durationLabel.setStyle("""
            -fx-font-size: 18px;
            -fx-font-weight: 600;
            -fx-text-fill: white;
            """);
        durationLabel.setVisible(false);

        userInfo.getChildren().addAll(nameLabel, statusLabel, durationLabel);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Call controls
        HBox controls = createControlButtons();

        content.getChildren().addAll(
                header,
                avatarSection,
                userInfo,
                spacer,
                controls
        );

        root.getChildren().addAll(background, content);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Make draggable
        makeDraggable(root);

        // Entrance animation
        playEntranceAnimation(root);
    }

    // ==================== GRADIENT BACKGROUND ====================
    private VBox createGradientBackground() {
        VBox bg = new VBox();
        bg.setStyle("""
            -fx-background-color: linear-gradient(135deg, 
                #667eea 0%, 
                #764ba2 50%, 
                #f093fb 100%);
            -fx-background-radius: 30;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 30, 0, 0, 10);
            """);

        return bg;
    }

    // ==================== HEADER ====================
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("""
            -fx-background-color: rgba(255,255,255,0.2);
            -fx-text-fill: white;
            -fx-font-size: 18px;
            -fx-font-weight: bold;
            -fx-pref-width: 36;
            -fx-pref-height: 36;
            -fx-background-radius: 18;
            -fx-cursor: hand;
            -fx-border-color: rgba(255,255,255,0.3);
            -fx-border-radius: 18;
            -fx-border-width: 1;
            """);

        closeBtn.setOnAction(e -> endCall());

        // Hover effect
        closeBtn.setOnMouseEntered(e ->
                closeBtn.setStyle(closeBtn.getStyle() + "-fx-background-color: rgba(255,255,255,0.3);"));
        closeBtn.setOnMouseExited(e ->
                closeBtn.setStyle(closeBtn.getStyle() + "-fx-background-color: rgba(255,255,255,0.2);"));

        header.getChildren().add(closeBtn);
        return header;
    }

    // ==================== AVATAR SECTION ====================
    private StackPane createAvatarSection(String avatarUrl) {
        StackPane container = new StackPane();
        container.setPrefSize(200, 200);

        // Animated waveform background
        waveformContainer = createWaveform();

        // Avatar circle
        Circle avatarClip = new Circle(85);

        avatarView = new ImageView();
        avatarView.setFitWidth(170);
        avatarView.setFitHeight(170);
        avatarView.setPreserveRatio(true);
        avatarView.setClip(avatarClip);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                avatarView.setImage(new Image(avatarUrl, true));
            } catch (Exception e) {
                avatarView.setImage(createDefaultAvatar());
            }
        } else {
            avatarView.setImage(createDefaultAvatar());
        }

        // White border with shadow
        Circle border = new Circle(85);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.WHITE);
        border.setStrokeWidth(5);

        DropShadow borderShadow = new DropShadow();
        borderShadow.setRadius(30);
        borderShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        border.setEffect(borderShadow);

        container.getChildren().addAll(waveformContainer, avatarView, border);

        return container;
    }

    // ==================== WAVEFORM ANIMATION ====================
    private StackPane createWaveform() {
        StackPane container = new StackPane();

        // Create 3 concentric circles
        for (int i = 0; i < 3; i++) {
            Circle wave = new Circle(100 + i * 20);
            wave.setFill(Color.TRANSPARENT);
            wave.setStroke(Color.rgb(255, 255, 255, 0.3));
            wave.setStrokeWidth(2);

            // Scale animation
            ScaleTransition scale = new ScaleTransition(Duration.seconds(2 + i * 0.3), wave);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.5);
            scale.setToY(1.5);
            scale.setDelay(Duration.seconds(i * 0.7));
            scale.setCycleCount(Timeline.INDEFINITE);

            // Fade animation
            FadeTransition fade = new FadeTransition(Duration.seconds(2 + i * 0.3), wave);
            fade.setFromValue(0.4);
            fade.setToValue(0.0);
            fade.setDelay(Duration.seconds(i * 0.7));
            fade.setCycleCount(Timeline.INDEFINITE);

            new ParallelTransition(scale, fade).play();

            container.getChildren().add(wave);
        }

        return container;
    }

    private Image createDefaultAvatar() {
        return new Image("https://ui-avatars.com/api/?name=User&size=170&background=fff&color=667eea&bold=true");
    }

    // ==================== CONTROL BUTTONS ====================
    private HBox createControlButtons() {
        HBox controls = new HBox(25);
        controls.setAlignment(Pos.CENTER);

        // Mute button
        muteBtn = createIconButton(ICON_MIC_ON, "#4CAF50", 70);
        muteBtn.setOnAction(e -> toggleMute());

        // Speaker button
        speakerBtn = createIconButton(ICON_SPEAKER_OFF, "#2196F3", 70);
        speakerBtn.setOnAction(e -> toggleSpeaker());

        // End call button (larger, red)
        endCallBtn = createIconButton(ICON_HANG_UP, "#FF3B30", 80);
        endCallBtn.setOnAction(e -> endCall());

        controls.getChildren().addAll(muteBtn, speakerBtn, endCallBtn);
        return controls;
    }

    /**
     * Tạo button với icon PNG
     */
    private Button createIconButton(String iconPath, String color, int size) {
        Button btn = new Button();

        // Load và set icon
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
            icon.setFitWidth(size * 0.45);
            icon.setFitHeight(size * 0.45);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            btn.setGraphic(icon);
        } catch (Exception e) {
            System.err.println("⚠️ Không thể load icon: " + iconPath);
            btn.setText("?");
        }

        btn.setPrefSize(size, size);
        btn.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: %d;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 5);
            -fx-border-color: rgba(255,255,255,0.2);
            -fx-border-width: 2;
            -fx-border-radius: %d;
            """, color, size/2, size/2));

        // Hover animation
        btn.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.play();

            btn.setStyle(btn.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 8);");
        });

        btn.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();

            btn.setStyle(btn.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 5);");
        });

        // Press animation
        btn.setOnMousePressed(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), btn);
            scale.setToX(0.95);
            scale.setToY(0.95);
            scale.play();
        });

        btn.setOnMouseReleased(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), btn);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.play();
        });

        return btn;
    }

    // ==================== ACTIONS ====================
    private void toggleMute() {
        isMuted = !isMuted;

        // Đổi icon
        String newIcon = isMuted ? ICON_MIC_OFF : ICON_MIC_ON;
        String newColor = isMuted ? "#757575" : "#4CAF50";

        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(newIcon)));
            icon.setFitWidth(31.5);
            icon.setFitHeight(31.5);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            muteBtn.setGraphic(icon);
        } catch (Exception e) {
            System.err.println("⚠️ Không thể load icon: " + newIcon);
        }

        // Đổi màu nền
        muteBtn.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 35;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 5);
            -fx-border-color: rgba(255,255,255,0.2);
            -fx-border-width: 2;
            -fx-border-radius: 35;
            """, newColor));

        if (onMuteToggle != null) {
            onMuteToggle.run();
        }

        System.out.println(isMuted ? "🔇 Đã TẮT tiếng" : "🔊 Đã BẬT tiếng");
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;

        // Đổi icon
        String newIcon = isSpeakerOn ? ICON_SPEAKER_ON : ICON_SPEAKER_OFF;

        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(newIcon)));
            icon.setFitWidth(31.5);
            icon.setFitHeight(31.5);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            speakerBtn.setGraphic(icon);
        } catch (Exception e) {
            System.err.println("⚠️ Không thể load icon: " + newIcon);
        }

        if (onSpeakerToggle != null) {
            onSpeakerToggle.run();
        }

        System.out.println(isSpeakerOn ? "🔊 Loa ngoài BẬT" : "🔈 Loa ngoài TẮT");
    }

    private void endCall() {
        if (callTimer != null) {
            callTimer.stop();
        }

        playExitAnimation(() -> {
            if (onEndCall != null) {
                onEndCall.run();
            }
            stage.close();
        });
    }

    // ==================== ANIMATIONS ====================
    private void playEntranceAnimation(StackPane root) {
        root.setScaleX(0.8);
        root.setScaleY(0.8);
        root.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(400), root);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(400), root);
        fade.setToValue(1.0);

        new ParallelTransition(scale, fade).play();
    }

    private void playExitAnimation(Runnable onComplete) {
        StackPane root = (StackPane) stage.getScene().getRoot();

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), root);
        scale.setToX(0.8);
        scale.setToY(0.8);

        FadeTransition fade = new FadeTransition(Duration.millis(300), root);
        fade.setToValue(0);

        ParallelTransition exit = new ParallelTransition(scale, fade);
        exit.setOnFinished(e -> onComplete.run());
        exit.play();
    }

    // ==================== DRAGGABLE ====================
    private void makeDraggable(StackPane root) {
        final double[] xOffset = {0};
        final double[] yOffset = {0};

        root.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });
    }

    // ==================== PUBLIC METHODS ====================
    public void show() {
        stage.show();
        stage.centerOnScreen();
    }

    public void close() {
        endCall();
    }

    public void setConnected() {
        statusLabel.setText("Đang trò chuyện");
        durationLabel.setVisible(true);
        startCallTimer();
    }

    public void setRinging() {
        statusLabel.setText("Đang gọi...");

        // Blinking animation
        FadeTransition blink = new FadeTransition(Duration.seconds(1), statusLabel);
        blink.setFromValue(1.0);
        blink.setToValue(0.5);
        blink.setCycleCount(Timeline.INDEFINITE);
        blink.setAutoReverse(true);
        blink.play();
    }

    private void startCallTimer() {
        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            int mins = seconds / 60;
            int secs = seconds % 60;
            durationLabel.setText(String.format("%02d:%02d", mins, secs));
        }));
        callTimer.setCycleCount(Timeline.INDEFINITE);
        callTimer.play();
    }

    // Callbacks
    public void setOnMuteToggle(Runnable action) {
        this.onMuteToggle = action;
    }

    public void setOnSpeakerToggle(Runnable action) {
        this.onSpeakerToggle = action;
    }

    public void setOnEndCall(Runnable action) {
        this.onEndCall = action;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }
}