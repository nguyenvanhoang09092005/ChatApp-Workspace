package org.example.chatappclient.client.utils.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dialog hiển thị cuộc gọi video
 */
public class VideoCallDialog {

    private final Stage stage;
    private final String partnerName;

    private Label statusLabel;
    private Button muteButton;
    private Button videoButton;
    private boolean isMuted = false;
    private boolean isVideoEnabled = true;

    private Runnable onMuteToggle;
    private Runnable onVideoToggle;
    private Runnable onSwitchCamera;
    private Runnable onEndCall;

    public VideoCallDialog(String partnerName) {
        this.partnerName = partnerName;
        this.stage = createStage();
    }

    private Stage createStage() {
        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Cuộc gọi video - " + partnerName);
        stage.setResizable(true);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");
        root.setPrefSize(800, 600);

        // Main video area
        StackPane videoArea = createVideoArea();
        root.setCenter(videoArea);

        // Controls
        HBox controls = createControls();
        root.setBottom(controls);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        return stage;
    }

    private StackPane createVideoArea() {
        StackPane videoArea = new StackPane();
        videoArea.setStyle("-fx-background-color: #2d2d2d;");

        // Main video placeholder
        VBox mainVideo = new VBox(10);
        mainVideo.setAlignment(Pos.CENTER);
        Label placeholder = new Label("📹");
        placeholder.setStyle("-fx-font-size: 64px;");
        Label nameLabel = new Label(partnerName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 700;");
        statusLabel = new Label("Đang gọi...");
        statusLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 14px;");
        mainVideo.getChildren().addAll(placeholder, nameLabel, statusLabel);
        videoArea.getChildren().add(mainVideo);

        // Self video (small preview)
        StackPane selfVideo = new StackPane();
        selfVideo.setStyle("-fx-background-color: #3d3d3d; -fx-background-radius: 8;");
        selfVideo.setPrefSize(200, 150);
        Label selfLabel = new Label("📷 Bạn");
        selfLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        selfVideo.getChildren().add(selfLabel);

        StackPane.setAlignment(selfVideo, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(selfVideo, new Insets(0, 20, 80, 0));
        videoArea.getChildren().add(selfVideo);

        return videoArea;
    }

    private HBox createControls() {
        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20));
        controls.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

        // Mute button
        muteButton = new Button("🔊");
        muteButton.setStyle(getButtonStyle());
        muteButton.setOnAction(e -> toggleMute());

        // Video button
        videoButton = new Button("📹");
        videoButton.setStyle(getButtonStyle());
        videoButton.setOnAction(e -> toggleVideo());

        // Switch camera button
        Button switchButton = new Button("🔄");
        switchButton.setStyle(getButtonStyle());
        switchButton.setOnAction(e -> {
            if (onSwitchCamera != null) {
                onSwitchCamera.run();
            }
        });

        // End call button
        Button endButton = new Button("📞");
        endButton.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 30; " +
                "-fx-pref-width: 60; -fx-pref-height: 60; -fx-font-size: 24px; " +
                "-fx-text-fill: white; -fx-cursor: hand;");
        endButton.setOnAction(e -> {
            if (onEndCall != null) {
                onEndCall.run();
            }
        });

        controls.getChildren().addAll(muteButton, videoButton, switchButton, endButton);
        return controls;
    }

    private String getButtonStyle() {
        return "-fx-background-color: rgba(255,255,255,0.2); " +
                "-fx-background-radius: 30; -fx-pref-width: 60; -fx-pref-height: 60; " +
                "-fx-font-size: 24px; -fx-text-fill: white; -fx-cursor: hand;";
    }

    private void toggleMute() {
        isMuted = !isMuted;
        muteButton.setText(isMuted ? "🔇" : "🔊");
        if (onMuteToggle != null) {
            onMuteToggle.run();
        }
    }

    private void toggleVideo() {
        isVideoEnabled = !isVideoEnabled;
        videoButton.setText(isVideoEnabled ? "📹" : "📷");
        if (onVideoToggle != null) {
            onVideoToggle.run();
        }
    }

    // ==================== PUBLIC METHODS ====================

    public void show() {
        stage.show();
    }

    public void close() {
        stage.close();
    }

    public void setRinging() {
        statusLabel.setText("Đang gọi...");
    }

    public void setConnected() {
        statusLabel.setText("Đang trong cuộc gọi");
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isVideoEnabled() {
        return isVideoEnabled;
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
}