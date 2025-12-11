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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Modern Incoming Call Dialog - Hoàn chỉnh với PNG Icons
 */
public class IncomingCallDialog {

    private Stage stage;
    private ImageView avatarView;
    private Label callerLabel;
    private Label callTypeLabel;
    private StackPane acceptButton;
    private StackPane declineButton;

    private Runnable onAccept;
    private Runnable onReject;

    // ICON PATHS - Sử dụng PNG icons
    private static final String ICON_PHONE_ACCEPT = "/icons/phone_accept.png";
    private static final String ICON_PHONE_DECLINE = "/icons/phone_decline.png";

    public IncomingCallDialog(String callerName, String callType, String avatarUrl) {
        createDialog(callerName, callType, avatarUrl);
    }

    private void createDialog(String callerName, String callType, String avatarUrl) {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setAlwaysOnTop(true);

        StackPane root = new StackPane();
        root.setPrefSize(400, 620);

        // Background
        VBox background = createBackground(callType);

        // Content
        VBox content = new VBox(35);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50, 30, 50, 30));

        // Call type icon
        Label iconLabel = new Label(callType.equals("audio") ? "📞" : "📹");
        iconLabel.setStyle("""
            -fx-font-size: 56px;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);
            """);

        // Animated avatar
        StackPane avatarSection = createAvatarSection(avatarUrl);

        // Caller info
        callerLabel = new Label(callerName);
        callerLabel.setStyle("""
            -fx-font-size: 32px;
            -fx-font-weight: 700;
            -fx-text-fill: white;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3);
            """);

        callTypeLabel = new Label(callType.equals("audio")
                ? "Cuộc gọi thoại đến..."
                : "Cuộc gọi video đến...");
        callTypeLabel.setStyle("""
            -fx-font-size: 17px;
            -fx-text-fill: rgba(255, 255, 255, 0.9);
            -fx-font-weight: 500;
            """);

        // Blinking animation
        FadeTransition blink = new FadeTransition(Duration.seconds(1.2), callTypeLabel);
        blink.setFromValue(1.0);
        blink.setToValue(0.6);
        blink.setCycleCount(Timeline.INDEFINITE);
        blink.setAutoReverse(true);
        blink.play();

        // Ringing indicator
        HBox ringingIndicator = createRingingIndicator();

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox actionButtons = createActionButtons();

        content.getChildren().addAll(
                iconLabel,
                avatarSection,
                callerLabel,
                callTypeLabel,
                ringingIndicator,
                spacer,
                actionButtons
        );

        root.getChildren().addAll(background, content);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Entrance animation
        playEntranceAnimation(root);
    }

    // ==================== BACKGROUND ====================
    private VBox createBackground(String callType) {
        VBox bg = new VBox();

        String gradient = callType.equals("audio")
                ? "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
                : "linear-gradient(135deg, #4776E6 0%, #8E54E9 100%)";

        bg.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 35;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 40, 0, 0, 15);
            """, gradient));

        return bg;
    }

    // ==================== AVATAR ====================
    private StackPane createAvatarSection(String avatarUrl) {
        StackPane container = new StackPane();
        container.setPrefSize(180, 180);

        // Pulse circles (3 waves)
        for (int i = 0; i < 3; i++) {
            Circle pulse = createPulseCircle(90, i);
            container.getChildren().add(pulse);
            animatePulse(pulse, i * 0.8);
        }

        // Avatar
        Circle avatarClip = new Circle(80);

        avatarView = new ImageView();
        avatarView.setFitWidth(160);
        avatarView.setFitHeight(160);
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
        Circle border = new Circle(80);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.WHITE);
        border.setStrokeWidth(5);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(30);
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));
        border.setEffect(shadow);

        container.getChildren().addAll(avatarView, border);

        return container;
    }

    private Circle createPulseCircle(double radius, int index) {
        Circle circle = new Circle(radius);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.rgb(255, 255, 255, 0.4 - index * 0.1));
        circle.setStrokeWidth(3);
        return circle;
    }

    private void animatePulse(Circle circle, double delay) {
        ScaleTransition scale = new ScaleTransition(Duration.seconds(2.5), circle);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.8);
        scale.setToY(1.8);
        scale.setDelay(Duration.seconds(delay));
        scale.setCycleCount(Timeline.INDEFINITE);

        FadeTransition fade = new FadeTransition(Duration.seconds(2.5), circle);
        fade.setFromValue(0.5);
        fade.setToValue(0.0);
        fade.setDelay(Duration.seconds(delay));
        fade.setCycleCount(Timeline.INDEFINITE);

        new ParallelTransition(scale, fade).play();
    }

    private Image createDefaultAvatar() {
        return new Image("https://ui-avatars.com/api/?name=User&size=160&background=fff&color=667eea");
    }

    // ==================== RINGING INDICATOR ====================
    private HBox createRingingIndicator() {
        HBox indicator = new HBox(8);
        indicator.setAlignment(Pos.CENTER);

        Label bell = new Label("🔔");
        bell.setStyle("-fx-font-size: 24px;");

        // Shake animation
        RotateTransition shake = new RotateTransition(Duration.millis(150), bell);
        shake.setFromAngle(-15);
        shake.setToAngle(15);
        shake.setCycleCount(Timeline.INDEFINITE);
        shake.setAutoReverse(true);
        shake.play();

        Label text = new Label("Đang đổ chuông...");
        text.setStyle("-fx-font-size: 15px; -fx-text-fill: rgba(255,255,255,0.85);");

        indicator.getChildren().addAll(bell, text);
        return indicator;
    }

    // ==================== ACTION BUTTONS ====================
    private HBox createActionButtons() {
        HBox buttons = new HBox(50);
        buttons.setAlignment(Pos.CENTER);

        // Decline button
        VBox declineBox = new VBox(8);
        declineBox.setAlignment(Pos.CENTER);

        declineButton = createIconButton(ICON_PHONE_DECLINE, "#FF3B30");
        declineButton.setOnMouseClicked(e -> reject());

        Label declineLabel = new Label("Từ chối");
        declineLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600;");

        declineBox.getChildren().addAll(declineButton, declineLabel);

        // Accept button
        VBox acceptBox = new VBox(8);
        acceptBox.setAlignment(Pos.CENTER);

        acceptButton = createIconButton(ICON_PHONE_ACCEPT, "#34C759");
        acceptButton.setOnMouseClicked(e -> accept());

        // Pulse animation for accept button
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(0.8), acceptButton);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.15);
        pulse.setToY(1.15);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        Label acceptLabel = new Label("Trả lời");
        acceptLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600;");

        acceptBox.getChildren().addAll(acceptButton, acceptLabel);

        buttons.getChildren().addAll(declineBox, acceptBox);
        return buttons;
    }

    /**
     * Tạo button với icon PNG
     */
    private StackPane createIconButton(String iconPath, String color) {
        StackPane btn = new StackPane();
        btn.setPrefSize(85, 85);
        btn.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 42.5;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 8);
            -fx-cursor: hand;
            """, color));

        // Load và set icon
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
            icon.setFitWidth(40);
            icon.setFitHeight(40);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            btn.getChildren().add(icon);
        } catch (Exception e) {
            System.err.println("⚠️ Không thể load icon: " + iconPath);
            Label fallback = new Label("?");
            fallback.setStyle("-fx-font-size: 36px; -fx-text-fill: white;");
            btn.getChildren().add(fallback);
        }

        // Hover effect
        btn.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.play();
        });

        btn.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), btn);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        // Press effect
        btn.setOnMousePressed(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), btn);
            scale.setToX(0.95);
            scale.setToY(0.95);
            scale.play();
        });

        btn.setOnMouseReleased(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), btn);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        return btn;
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

    // ==================== ANIMATIONS ====================
    private void playEntranceAnimation(StackPane root) {
        root.setTranslateY(-100);
        root.setOpacity(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(500), root);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(500), root);
        fade.setToValue(1.0);

        // Bounce effect
        ScaleTransition bounce = new ScaleTransition(Duration.millis(500), root);
        bounce.setFromX(0.8);
        bounce.setFromY(0.8);
        bounce.setToX(1.0);
        bounce.setToY(1.0);
        bounce.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(slide, fade, bounce).play();
    }

    private void playExitAnimation(Runnable onComplete) {
        StackPane root = (StackPane) stage.getScene().getRoot();

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), root);
        slide.setToY(100);

        FadeTransition fade = new FadeTransition(Duration.millis(300), root);
        fade.setToValue(0);

        ParallelTransition exit = new ParallelTransition(slide, fade);
        exit.setOnFinished(e -> onComplete.run());
        exit.play();
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