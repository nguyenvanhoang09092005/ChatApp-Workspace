package org.example.chatappclient.client.utils.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
 * Desktop Window Style Incoming Call Dialog
 * Giao diện desktop hoàn chỉnh với title bar và window controls
 */
public class IncomingCallDialog {

    private Stage stage;
    private ImageView avatarView;
    private Label callerLabel;
    private Label statusLabel;
    private StackPane acceptButton;
    private StackPane declineButton;
    private double xOffset = 0;
    private double yOffset = 0;

    private Runnable onAccept;
    private Runnable onReject;

    private static final String ICON_PHONE_ACCEPT = "/icons/phone_accept.png";
    private static final String ICON_PHONE_DECLINE = "/icons/phone_decline.png";

    public IncomingCallDialog(String callerName, String callType, String avatarUrl) {
        createDialog(callerName, callType, avatarUrl);
    }

    private void createDialog(String callerName, String callType, String avatarUrl) {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        BorderPane root = new BorderPane();
        root.setPrefSize(420, 580);
        root.setStyle("""
            -fx-background-color: #ffffff;
            -fx-background-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0, 0, 10);
            """);

        // Title Bar
        HBox titleBar = createTitleBar(callType);
        root.setTop(titleBar);

        // Main Content
        VBox content = createMainContent(callerName, callType, avatarUrl);
        root.setCenter(content);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Window dragging
        setupWindowDragging(titleBar);

        // Entrance animation
        playEntranceAnimation(root);
    }

    // ==================== TITLE BAR ====================
    private HBox createTitleBar(String callType) {
        HBox titleBar = new HBox();
        titleBar.setPrefHeight(45);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("""
            -fx-background-color: linear-gradient(to right, #667eea, #764ba2);
            -fx-background-radius: 12 12 0 0;
            -fx-padding: 0 15 0 20;
            """);

        // Title
        Label title = new Label(callType.equals("audio") ? "📞 Cuộc gọi thoại đến" : "📹 Cuộc gọi video đến");
        title.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: 600;
            """);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Window Control Buttons
        HBox windowControls = createWindowControls();

        titleBar.getChildren().addAll(title, spacer, windowControls);
        return titleBar;
    }

    private HBox createWindowControls() {
        HBox controls = new HBox(5);
        controls.setAlignment(Pos.CENTER_RIGHT);

        // Minimize Button
        StackPane minBtn = createControlButton("—", "#667eea");
        minBtn.setOnMouseClicked(e -> stage.setIconified(true));

        // Maximize Button (disabled for call dialog)
        StackPane maxBtn = createControlButton("□", "#667eea");
        maxBtn.setOpacity(0.5);

        // Close Button
        StackPane closeBtn = createControlButton("✕", "#e53e3e");
        closeBtn.setOnMouseClicked(e -> reject());

        controls.getChildren().addAll(minBtn, maxBtn, closeBtn);
        return controls;
    }

    private StackPane createControlButton(String symbol, String hoverColor) {
        StackPane btn = new StackPane();
        btn.setPrefSize(35, 30);
        btn.setStyle("""
            -fx-background-color: transparent;
            -fx-background-radius: 5;
            -fx-cursor: hand;
            """);

        Label label = new Label(symbol);
        label.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            """);

        btn.getChildren().add(label);

        btn.setOnMouseEntered(e ->
                btn.setStyle("-fx-background-color: " + hoverColor + "; -fx-background-radius: 5; -fx-cursor: hand;")
        );

        btn.setOnMouseExited(e ->
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 5; -fx-cursor: hand;")
        );

        return btn;
    }

    // ==================== MAIN CONTENT ====================
    private VBox createMainContent(String callerName, String callType, String avatarUrl) {
        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 30, 35, 30));
        content.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 0 0 12 12;");

        // Avatar with animation
        StackPane avatarSection = createAvatarSection(avatarUrl);

        // Caller name
        callerLabel = new Label(callerName);
        callerLabel.setStyle("""
            -fx-font-size: 28px;
            -fx-font-weight: 700;
            -fx-text-fill: #2d3748;
            """);

        // Status
        String statusText = callType.equals("audio")
                ? "Đang gọi đến..."
                : "Cuộc gọi video đến...";

        statusLabel = new Label(statusText);
        statusLabel.setStyle("""
            -fx-font-size: 15px;
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

        // Call info card
        VBox infoCard = createInfoCard(callType);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox actionButtons = createActionButtons();

        content.getChildren().addAll(
                avatarSection,
                callerLabel,
                statusLabel,
                infoCard,
                spacer,
                actionButtons
        );

        return content;
    }

    // ==================== AVATAR ====================
    private StackPane createAvatarSection(String avatarUrl) {
        StackPane container = new StackPane();
        container.setPrefSize(160, 160);

        // Animated rings
        for (int i = 0; i < 2; i++) {
            Circle ring = new Circle(80 + i * 10);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(Color.rgb(102, 126, 234, 0.3 - i * 0.1));
            ring.setStrokeWidth(2);

            ScaleTransition scale = new ScaleTransition(Duration.seconds(2), ring);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.4);
            scale.setToY(1.4);
            scale.setDelay(Duration.seconds(i * 0.5));
            scale.setCycleCount(Timeline.INDEFINITE);

            FadeTransition fade = new FadeTransition(Duration.seconds(2), ring);
            fade.setFromValue(0.5);
            fade.setToValue(0.0);
            fade.setDelay(Duration.seconds(i * 0.5));
            fade.setCycleCount(Timeline.INDEFINITE);

            new ParallelTransition(scale, fade).play();
            container.getChildren().add(ring);
        }

        // Avatar
        Circle avatarClip = new Circle(70);

        avatarView = new ImageView();
        avatarView.setFitWidth(140);
        avatarView.setFitHeight(140);
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

        // Border
        Circle border = new Circle(70);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.WHITE);
        border.setStrokeWidth(5);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(25);
        shadow.setColor(Color.rgb(102, 126, 234, 0.3));
        border.setEffect(shadow);

        container.getChildren().addAll(avatarView, border);
        return container;
    }

    private Image createDefaultAvatar() {
        return new Image("https://ui-avatars.com/api/?name=User&size=140&background=667eea&color=fff");
    }

    // ==================== INFO CARD ====================
    private VBox createInfoCard(String callType) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
            -fx-background-color: #f7fafc;
            -fx-background-radius: 12;
            -fx-padding: 18 25 18 25;
            """);

        HBox iconRow = new HBox(10);
        iconRow.setAlignment(Pos.CENTER);

        Label icon = new Label(callType.equals("audio") ? "🔊" : "📹");
        icon.setStyle("-fx-font-size: 24px;");

        Label typeLabel = new Label(callType.equals("audio") ? "Cuộc gọi thoại" : "Cuộc gọi video");
        typeLabel.setStyle("""
            -fx-font-size: 15px;
            -fx-text-fill: #4a5568;
            -fx-font-weight: 600;
            """);

        iconRow.getChildren().addAll(icon, typeLabel);

        card.getChildren().add(iconRow);
        return card;
    }

    // ==================== ACTION BUTTONS ====================
    private HBox createActionButtons() {
        HBox buttons = new HBox(30);
        buttons.setAlignment(Pos.CENTER);

        // Decline
        VBox declineBox = createButtonBox(
                ICON_PHONE_DECLINE,
                "Từ chối",
                "#e53e3e",
                () -> reject()
        );

        // Accept
        VBox acceptBox = createButtonBox(
                ICON_PHONE_ACCEPT,
                "Trả lời",
                "#667eea",
                () -> accept()
        );

        // Pulse animation for accept
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1), acceptBox.getChildren().get(0));
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        buttons.getChildren().addAll(declineBox, acceptBox);
        return buttons;
    }

    private VBox createButtonBox(String iconPath, String text, String color, Runnable action) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);

        StackPane btn = new StackPane();
        btn.setPrefSize(75, 75);
        btn.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 37.5;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 5);
            """, color));

        // Icon
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
            icon.setFitWidth(35);
            icon.setFitHeight(35);
            icon.setPreserveRatio(true);
            btn.getChildren().add(icon);
        } catch (Exception e) {
            Label fallback = new Label("?");
            fallback.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-weight: bold;");
            btn.getChildren().add(fallback);
        }

        btn.setOnMouseClicked(e -> action.run());

        // Hover
        btn.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.08);
            scale.setToY(1.08);
            scale.play();
        });

        btn.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        Label label = new Label(text);
        label.setStyle("""
            -fx-text-fill: #4a5568;
            -fx-font-size: 14px;
            -fx-font-weight: 600;
            """);

        box.getChildren().addAll(btn, label);
        return box;
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
        root.setScaleX(0.7);
        root.setScaleY(0.7);
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
        scale.setToX(0.8);
        scale.setToY(0.8);

        FadeTransition fade = new FadeTransition(Duration.millis(250), root);
        fade.setToValue(0);

        ParallelTransition exit = new ParallelTransition(scale, fade);
        exit.setOnFinished(e -> onComplete.run());
        exit.play();
    }

    // ==================== ACTIONS ====================
    private void accept() {
        playExitAnimation(() -> {
            if (onAccept != null) {
                onAccept.run();
            }
            stage.close();
        });
    }

    private void reject() {
        playExitAnimation(() -> {
            if (onReject != null) {
                onReject.run();
            }
            stage.close();
        });
    }

    // ==================== PUBLIC METHODS ====================
    public void show() {
        stage.show();
        stage.centerOnScreen();
    }

    public void close() {
        stage.close();
    }

    public void setOnAccept(Runnable action) {
        this.onAccept = action;
    }

    public void setOnReject(Runnable action) {
        this.onReject = action;
    }
}