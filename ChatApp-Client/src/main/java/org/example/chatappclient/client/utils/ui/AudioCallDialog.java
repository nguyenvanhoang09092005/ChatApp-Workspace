package org.example.chatappclient.client.utils.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Desktop Landscape Audio Call Dialog
 * Giao diện ngang hiện đại với title bar và gradient đẹp mắt
 * FIXED VERSION: Loa BẬT mặc định + đồng bộ UI/Logic
 */
public class AudioCallDialog {

    private Stage stage;
    private Label statusLabel;
    private Label durationLabel;
    private Button muteBtn;
    private Button speakerBtn;
    private Button endCallBtn;
    private ImageView avatarView;
    private double xOffset = 0;
    private double yOffset = 0;

    private boolean isMuted = false;
    private boolean isSpeakerOn = true; // ✅ FIX: MẶC ĐỊNH BẬT LOA (thay vì false)
    private Timeline callTimer;
    private int seconds = 0;

    private Runnable onMuteToggle;
    private Runnable onSpeakerToggle;
    private Runnable onEndCall;

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
        stage.setAlwaysOnTop(true);

        BorderPane root = new BorderPane();
        root.setPrefSize(720, 480);
        root.setStyle("""
            -fx-background-color: #ffffff;
            -fx-background-radius: 15;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 35, 0, 0, 12);
            """);

        // Title Bar
        HBox titleBar = createTitleBar();
        root.setTop(titleBar);

        // Main Content - Split layout
        HBox mainContent = createMainContent(partnerName, avatarUrl);
        root.setCenter(mainContent);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        setupWindowDragging(titleBar);
        playEntranceAnimation(root);
    }

    // ==================== TITLE BAR ====================
    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setPrefHeight(50);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("""
            -fx-background-color: linear-gradient(to right, #667eea, #764ba2);
            -fx-background-radius: 15 15 0 0;
            -fx-padding: 0 20 0 25;
            """);

        // Icon + Title
        HBox leftSection = new HBox(12);
        leftSection.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("📞");
        icon.setStyle("-fx-font-size: 20px;");

        Label title = new Label("Cuộc gọi thoại");
        title.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 15px;
            -fx-font-weight: 600;
            """);

        leftSection.getChildren().addAll(icon, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Window Controls
        HBox windowControls = createWindowControls();

        titleBar.getChildren().addAll(leftSection, spacer, windowControls);
        return titleBar;
    }

    private HBox createWindowControls() {
        HBox controls = new HBox(5);
        controls.setAlignment(Pos.CENTER_RIGHT);

        // Minimize
        StackPane minBtn = createControlButton("—", "#667eea");
        minBtn.setOnMouseClicked(e -> stage.setIconified(true));

        // Maximize (disabled)
        StackPane maxBtn = createControlButton("□", "#667eea");
        maxBtn.setOpacity(0.5);

        // Close
        StackPane closeBtn = createControlButton("✕", "#e53e3e");
        closeBtn.setOnMouseClicked(e -> endCall());

        controls.getChildren().addAll(minBtn, maxBtn, closeBtn);
        return controls;
    }

    private StackPane createControlButton(String symbol, String hoverColor) {
        StackPane btn = new StackPane();
        btn.setPrefSize(38, 35);
        btn.setStyle("""
            -fx-background-color: transparent;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            """);

        Label label = new Label(symbol);
        label.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 17px;
            -fx-font-weight: bold;
            """);

        btn.getChildren().add(label);

        btn.setOnMouseEntered(e ->
                btn.setStyle("-fx-background-color: " + hoverColor + "; -fx-background-radius: 6; -fx-cursor: hand;")
        );

        btn.setOnMouseExited(e ->
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;")
        );

        return btn;
    }

    // ==================== MAIN CONTENT ====================
    private HBox createMainContent(String partnerName, String avatarUrl) {
        HBox content = new HBox(0);
        content.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 0 0 15 15;");

        // Left Panel - Gradient với avatar
        VBox leftPanel = createLeftPanel(avatarUrl);
        leftPanel.setPrefWidth(320);

        // Right Panel - Controls
        VBox rightPanel = createRightPanel(partnerName);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        content.getChildren().addAll(leftPanel, rightPanel);
        return content;
    }

    // ==================== LEFT PANEL ====================
    private VBox createLeftPanel(String avatarUrl) {
        VBox panel = new VBox();
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #667eea, #764ba2);
            -fx-background-radius: 0 0 0 15;
            -fx-padding: 40;
            """);

        // Avatar với animated rings
        StackPane avatarSection = createAvatarSection(avatarUrl);

        // Call duration
        durationLabel = new Label("00:00");
        durationLabel.setStyle("""
            -fx-font-size: 36px;
            -fx-font-weight: 700;
            -fx-text-fill: white;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3);
            -fx-padding: 25 0 0 0;
            """);
        durationLabel.setVisible(false);

        panel.getChildren().addAll(avatarSection, durationLabel);
        return panel;
    }

    private StackPane createAvatarSection(String avatarUrl) {
        StackPane container = new StackPane();
        container.setPrefSize(200, 200);

        // Animated pulse rings
        for (int i = 0; i < 3; i++) {
            Circle ring = new Circle(100 + i * 15);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(Color.rgb(255, 255, 255, 0.25 - i * 0.05));
            ring.setStrokeWidth(2.5);

            ScaleTransition scale = new ScaleTransition(Duration.seconds(2.5), ring);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.5);
            scale.setToY(1.5);
            scale.setDelay(Duration.seconds(i * 0.8));
            scale.setCycleCount(Timeline.INDEFINITE);

            FadeTransition fade = new FadeTransition(Duration.seconds(2.5), ring);
            fade.setFromValue(0.4);
            fade.setToValue(0.0);
            fade.setDelay(Duration.seconds(i * 0.8));
            fade.setCycleCount(Timeline.INDEFINITE);

            new ParallelTransition(scale, fade).play();
            container.getChildren().add(ring);
        }

        // Avatar
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

        // White border
        Circle border = new Circle(85);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.WHITE);
        border.setStrokeWidth(5);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(30);
        shadow.setColor(Color.rgb(0, 0, 0, 0.4));
        border.setEffect(shadow);

        container.getChildren().addAll(avatarView, border);
        return container;
    }

    private Image createDefaultAvatar() {
        return new Image("https://ui-avatars.com/api/?name=User&size=170&background=667eea&color=fff");
    }

    // ==================== RIGHT PANEL ====================
    private VBox createRightPanel(String partnerName) {
        VBox panel = new VBox(30);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(50, 40, 50, 40));
        panel.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 0 0 15 0;");

        // Partner info
        VBox infoSection = new VBox(12);
        infoSection.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(partnerName);
        nameLabel.setStyle("""
            -fx-font-size: 32px;
            -fx-font-weight: 700;
            -fx-text-fill: #2d3748;
            """);

        statusLabel = new Label("Đang kết nối...");
        statusLabel.setStyle("""
            -fx-font-size: 17px;
            -fx-text-fill: #718096;
            -fx-font-weight: 500;
            """);

        // Blinking animation
        FadeTransition blink = new FadeTransition(Duration.seconds(1.2), statusLabel);
        blink.setFromValue(1.0);
        blink.setToValue(0.5);
        blink.setCycleCount(Timeline.INDEFINITE);
        blink.setAutoReverse(true);
        blink.play();

        infoSection.getChildren().addAll(nameLabel, statusLabel);

        // Call quality indicator
        HBox qualityBox = createQualityIndicator();

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Control buttons
        VBox controlsSection = createControlsSection();

        panel.getChildren().addAll(infoSection, qualityBox, spacer, controlsSection);
        return panel;
    }

    private HBox createQualityIndicator() {
        HBox quality = new HBox(8);
        quality.setAlignment(Pos.CENTER);
        quality.setStyle("""
            -fx-background-color: #f7fafc;
            -fx-background-radius: 20;
            -fx-padding: 12 20 12 20;
            """);

        // Signal bars
        HBox bars = new HBox(3);
        bars.setAlignment(Pos.CENTER);
        for (int i = 0; i < 4; i++) {
            VBox bar = new VBox();
            bar.setPrefWidth(4);
            bar.setPrefHeight(8 + i * 4);
            bar.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 2;");
            bars.getChildren().add(bar);
        }

        Label qualityLabel = new Label("Chất lượng tốt");
        qualityLabel.setStyle("""
            -fx-font-size: 14px;
            -fx-text-fill: #4a5568;
            -fx-font-weight: 500;
            """);

        quality.getChildren().addAll(bars, qualityLabel);
        return quality;
    }

    // ==================== CONTROLS SECTION ====================
    private VBox createControlsSection() {
        VBox section = new VBox(25);
        section.setAlignment(Pos.CENTER);

        // Mute & Speaker buttons
        HBox auxButtons = new HBox(20);
        auxButtons.setAlignment(Pos.CENTER);

        muteBtn = createControlButton(ICON_MIC_ON, "Tắt tiếng", "#4CAF50", false);
        muteBtn.setOnAction(e -> toggleMute());

        // ✅ FIX: Khởi tạo với LOA BẬT (SPEAKER_ON + màu XANH + label "Tắt loa")
        speakerBtn = createControlButton(ICON_SPEAKER_ON, "Tắt loa", "#2196F3", false);
        speakerBtn.setOnAction(e -> toggleSpeaker());

        auxButtons.getChildren().addAll(muteBtn, speakerBtn);

        // End call button (prominent)
        endCallBtn = createEndCallButton();
        endCallBtn.setOnAction(e -> endCall());

        section.getChildren().addAll(auxButtons, endCallBtn);
        return section;
    }

    private Button createControlButton(String iconPath, String tooltip, String color, boolean isEndCall) {
        Button btn = new Button();

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER);

        // Icon
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
            icon.setFitWidth(28);
            icon.setFitHeight(28);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            content.getChildren().add(icon);
        } catch (Exception e) {
            Label fallback = new Label("?");
            fallback.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
            content.getChildren().add(fallback);
        }

        // Label
        Label label = new Label(tooltip);
        label.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-font-weight: 600;
            """);
        content.getChildren().add(label);

        btn.setGraphic(content);
        btn.setPrefSize(110, 90);
        btn.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 12;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);
            -fx-border-color: transparent;
            """, color));

        // Hover
        btn.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });

        btn.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        return btn;
    }

    private Button createEndCallButton() {
        Button btn = new Button();

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER);

        // Icon
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(ICON_HANG_UP)));
            icon.setFitWidth(32);
            icon.setFitHeight(32);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            content.getChildren().add(icon);
        } catch (Exception e) {
            Label fallback = new Label("✕");
            fallback.setStyle("-fx-font-size: 28px; -fx-text-fill: white;");
            content.getChildren().add(fallback);
        }

        Label label = new Label("Kết thúc");
        label.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-weight: 700;
            """);
        content.getChildren().add(label);

        btn.setGraphic(content);
        btn.setPrefSize(240, 60);
        btn.setStyle("""
            -fx-background-color: #e53e3e;
            -fx-background-radius: 30;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(229,62,62,0.4), 15, 0, 0, 6);
            """);

        // Hover
        btn.setOnMouseEntered(e -> {
            btn.setStyle("""
                -fx-background-color: #dc2626;
                -fx-background-radius: 30;
                -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian, rgba(229,62,62,0.5), 18, 0, 0, 8);
                """);
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle("""
                -fx-background-color: #e53e3e;
                -fx-background-radius: 30;
                -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian, rgba(229,62,62,0.4), 15, 0, 0, 6);
                """);
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        return btn;
    }

    // ==================== ACTIONS ====================
    private void toggleMute() {
        isMuted = !isMuted;

        // Cập nhật UI
        VBox content = (VBox) muteBtn.getGraphic();
        ImageView iconView = (ImageView) content.getChildren().get(0);
        Label labelView = (Label) content.getChildren().get(1);

        String newIcon = isMuted ? ICON_MIC_OFF : ICON_MIC_ON;
        String newColor = isMuted ? "#757575" : "#4CAF50";
        String newLabel = isMuted ? "Bật tiếng" : "Tắt tiếng";

        try {
            iconView.setImage(new Image(getClass().getResourceAsStream(newIcon)));
        } catch (Exception e) {
            System.err.println("⚠️ Không thể load icon: " + newIcon);
        }

        labelView.setText(newLabel);
        muteBtn.setStyle(String.format("""
        -fx-background-color: %s;
        -fx-background-radius: 12;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);
        """, newColor));

        // ✅ Gọi callback SAU KHI đã cập nhật UI
        if (onMuteToggle != null) {
            onMuteToggle.run();
        }
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;

        VBox content = (VBox) speakerBtn.getGraphic();
        ImageView iconView = (ImageView) content.getChildren().get(0);
        Label labelView = (Label) content.getChildren().get(1);

        // ✅ FIX: Logic đảo ngược đúng
        // Khi isSpeakerOn = true  → Icon BẬT,  màu XANH, label "Tắt loa"
        // Khi isSpeakerOn = false → Icon TẮT, màu XÁM,  label "Bật loa"
        String newIcon = isSpeakerOn ? ICON_SPEAKER_ON : ICON_SPEAKER_OFF;
        String newLabel = isSpeakerOn ? "Tắt loa" : "Bật loa";
        String newColor = isSpeakerOn ? "#2196F3" : "#757575";

        try {
            iconView.setImage(new Image(getClass().getResourceAsStream(newIcon)));
        } catch (Exception e) {
            System.err.println("⚠️ Không thể load icon: " + newIcon);
        }

        labelView.setText(newLabel);

        // ✅ Cập nhật màu button
        speakerBtn.setStyle(String.format("""
        -fx-background-color: %s;
        -fx-background-radius: 12;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);
        -fx-border-color: transparent;
        """, newColor));

        // ✅ Gọi callback để thực sự tắt/bật loa
        if (onSpeakerToggle != null) {
            onSpeakerToggle.run();
        }

        // ✅ THÊM LOG ĐỂ DEBUG
        System.out.println(isSpeakerOn ? "🔊 Đã bật loa" : "🔇 Đã tắt loa");
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

    // ==================== WINDOW DRAGGING ====================
    private void setupWindowDragging(HBox titleBar) {
        titleBar.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        titleBar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
    }

    // ==================== ANIMATIONS ====================
    private void playEntranceAnimation(BorderPane root) {
        root.setScaleX(0.85);
        root.setScaleY(0.85);
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
        BorderPane root = (BorderPane) stage.getScene().getRoot();

        ScaleTransition scale = new ScaleTransition(Duration.millis(250), root);
        scale.setToX(0.9);
        scale.setToY(0.9);

        FadeTransition fade = new FadeTransition(Duration.millis(250), root);
        fade.setToValue(0);

        ParallelTransition exit = new ParallelTransition(scale, fade);
        exit.setOnFinished(e -> onComplete.run());
        exit.play();
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

        // ✅ THÊM: Debug log
        System.out.println("✅ AudioCallDialog: Cuộc gọi đã connected");
        System.out.println("   Trạng thái loa: " + (isSpeakerOn ? "BẬT" : "TẮT"));
    }

    public void setRinging() {
        statusLabel.setText("Đang gọi...");
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