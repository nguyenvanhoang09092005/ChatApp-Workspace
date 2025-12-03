package org.example.chatappclient.client.utils.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.chatappclient.client.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory class để tạo các Dialog
 */
public class DialogFactory {

    // ==================== NEW CHAT DIALOG ====================

    /**
     * Hiển thị dialog tìm kiếm và tạo cuộc trò chuyện mới (legacy - trả về String)
     */
    public static void showNewChatDialog(Consumer<String> onResult) {
        // Không sử dụng nữa - giữ lại để tương thích
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Tin nhắn mới");
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);

        Label title = new Label("Bắt đầu cuộc trò chuyện mới");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        TextField searchField = new TextField();
        searchField.setPromptText("Nhập tên, email hoặc số điện thoại...");
        searchField.setStyle("-fx-padding: 12; -fx-font-size: 14px;");

        content.getChildren().addAll(title, searchField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Bắt đầu chat");

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? searchField.getText() : null);
        dialog.showAndWait().ifPresent(onResult);
    }

    /**
     * Hiển thị dialog tìm kiếm người dùng mới (trả về User object)
     */
    public static void showUserSearchDialog(Consumer<User> onUserSelected) {
        UserSearchDialog dialog = new UserSearchDialog(onUserSelected);
        dialog.show();
    }

    /**
     * Hiển thị dialog tạo nhóm
     */
    public static void showCreateGroupDialog(BiConsumer<String, List<String>> onConfirm) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Tạo nhóm mới");
        dialog.setWidth(400);
        dialog.setHeight(500);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // Group name
        Label nameLabel = new Label("Tên nhóm:");
        TextField nameField = new TextField();
        nameField.setPromptText("Nhập tên nhóm...");

        // Member selection
        Label memberLabel = new Label("Thêm thành viên:");

        // Search field for members
        TextField searchField = new TextField();
        searchField.setPromptText("Tìm kiếm bạn bè...");

        // Selected members list
        ListView<String> selectedMembers = new ListView<>();
        selectedMembers.setPrefHeight(200);

        // Add member button
        Button addMemberButton = new Button("Thêm thành viên");
        addMemberButton.setOnAction(e -> {
            // TODO: Implement member selection dialog
            AlertUtil.showToastInfo("Chức năng đang phát triển");
        });

        // Action buttons
        HBox buttons = new HBox(10);
        Button cancelButton = new Button("Hủy");
        Button createButton = new Button("Tạo nhóm");
        createButton.setDefaultButton(true);

        cancelButton.setOnAction(e -> dialog.close());
        createButton.setOnAction(e -> {
            String groupName = nameField.getText().trim();
            if (groupName.isEmpty()) {
                AlertUtil.showToastError("Vui lòng nhập tên nhóm");
                return;
            }

            List<String> memberIds = new ArrayList<>(selectedMembers.getItems());
            if (onConfirm != null) {
                onConfirm.accept(groupName, memberIds);
            }
            dialog.close();
        });

        buttons.getChildren().addAll(cancelButton, createButton);

        root.getChildren().addAll(
                nameLabel, nameField,
                memberLabel, searchField,
                selectedMembers,
                addMemberButton,
                buttons
        );

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Hiển thị dialog xác nhận
     */
    public static boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }

    /**
     * Hiển thị dialog thông tin
     */
    public static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị dialog lỗi
     */
    public static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị dialog nhập text
     */
    public static String showInputDialog(String title, String message, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);

        return dialog.showAndWait().orElse(null);
    }
    // ==================== SETTINGS DIALOG ====================

    public static void showSettingsDialog(User currentUser, Runnable onLogout) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cài đặt");
        dialog.setHeaderText(null);

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setPrefWidth(450);

        // Profile section
        HBox profile = new HBox(16);
        profile.setAlignment(Pos.CENTER_LEFT);

        ImageView avatar = new ImageView();
        avatar.setFitWidth(70);
        avatar.setFitHeight(70);
        avatar.setClip(new Circle(35, 35, 35));
        // Load avatar
        String avatarUrl = currentUser.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            avatarUrl = "https://ui-avatars.com/api/?name=" + currentUser.getUsername() + "&size=70";
        }
        try {
            avatar.setImage(new javafx.scene.image.Image(avatarUrl, true));
        } catch (Exception e) {}

        VBox userInfo = new VBox(4);
        Label name = new Label(currentUser.getDisplayNameOrUsername());
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");
        Label email = new Label(currentUser.getEmail());
        email.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");
        userInfo.getChildren().addAll(name, email);

        profile.getChildren().addAll(avatar, userInfo);

        // Settings options
        VBox options = new VBox(4);

        options.getChildren().addAll(
                createSettingsItem("👤", "Chỉnh sửa thông tin cá nhân", () -> {}),
                createSettingsItem("🔐", "Đổi mật khẩu", () -> {}),
                createSettingsItem("🔔", "Cài đặt thông báo", () -> {}),
                createSettingsItem("🌙", "Chế độ tối", () -> {}),
                createSettingsItem("🌐", "Ngôn ngữ", () -> {}),
                new Separator(),
                createSettingsItem("❓", "Trợ giúp & Hỗ trợ", () -> {}),
                createSettingsItem("📋", "Điều khoản sử dụng", () -> {}),
                new Separator()
        );

        // Logout button
        Button logoutBtn = new Button("Đăng xuất");
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc3545; " +
                "-fx-font-size: 15px; -fx-font-weight: 600; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            dialog.close();
            onLogout.run();
        });

        HBox logoutBox = new HBox(logoutBtn);
        logoutBox.setAlignment(Pos.CENTER);
        logoutBox.setPadding(new Insets(10, 0, 0, 0));

        content.getChildren().addAll(profile, new Separator(), options, logoutBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static HBox createSettingsItem(String icon, String text, Runnable onClick) {
        HBox item = new HBox(16);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #f2f2f2; -fx-background-radius: 8; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-radius: 8; -fx-cursor: hand;"));
        item.setOnMouseClicked(e -> onClick.run());

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 15px;");

        Label arrow = new Label("›");
        arrow.setStyle("-fx-font-size: 18px; -fx-text-fill: #8e8e93;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        item.getChildren().addAll(iconLabel, textLabel, spacer, arrow);
        return item;
    }

    // ==================== AUDIO CALL DIALOG ====================

    public static void showAudioCallDialog(String partnerName, Runnable onMute,
                                           Runnable onEnd, Supplier<Boolean> isMuted) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Cuộc gọi thoại");

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea, #764ba2);");
        root.setPrefSize(350, 450);

        // Avatar placeholder
        Circle avatar = new Circle(50);
        avatar.setFill(javafx.scene.paint.Color.web("#ffffff33"));

        Label avatarIcon = new Label("👤");
        avatarIcon.setStyle("-fx-font-size: 48px;");

        StackPane avatarPane = new StackPane(avatar, avatarIcon);

        Label nameLabel = new Label(partnerName);
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: white;");

        Label statusLabel = new Label("Đang gọi...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.8);");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Control buttons
        HBox controls = new HBox(30);
        controls.setAlignment(Pos.CENTER);

        Button muteBtn = createCallButton("🔇", "Tắt mic", onMute);
        Button endBtn = createCallButton("📞", "Kết thúc", () -> {
            onEnd.run();
            stage.close();
        });
        endBtn.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 30; " +
                "-fx-pref-width: 60; -fx-pref-height: 60;");

        controls.getChildren().addAll(muteBtn, endBtn);

        root.getChildren().addAll(avatarPane, nameLabel, statusLabel, spacer, controls);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // ==================== VIDEO CALL DIALOG ====================

    public static void showVideoCallDialog(String partnerName, Runnable onMute,
                                           Runnable onVideo, Runnable onSwitch,
                                           Runnable onEnd, Supplier<Boolean> isMuted,
                                           Supplier<Boolean> isVideoOn) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Cuộc gọi video - " + partnerName);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");
        root.setPrefSize(800, 600);

        // Main video area (placeholder)
        StackPane videoArea = new StackPane();
        videoArea.setStyle("-fx-background-color: #2d2d2d;");
        Label placeholder = new Label("📹 Video " + partnerName);
        placeholder.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        videoArea.getChildren().add(placeholder);

        // Self video (small)
        StackPane selfVideo = new StackPane();
        selfVideo.setStyle("-fx-background-color: #3d3d3d; -fx-background-radius: 8;");
        selfVideo.setPrefSize(150, 100);
        Label selfLabel = new Label("Bạn");
        selfLabel.setStyle("-fx-text-fill: white;");
        selfVideo.getChildren().add(selfLabel);

        StackPane.setAlignment(selfVideo, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(selfVideo, new Insets(0, 20, 20, 0));
        videoArea.getChildren().add(selfVideo);

        root.setCenter(videoArea);

        // Controls
        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20));
        controls.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

        Button muteBtn = createCallButton("🔇", "Mic", onMute);
        Button videoBtn = createCallButton("📹", "Camera", onVideo);
        Button switchBtn = createCallButton("🔄", "Đổi cam", onSwitch);
        Button endBtn = createCallButton("📞", "Kết thúc", () -> {
            onEnd.run();
            stage.close();
        });
        endBtn.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 25; " +
                "-fx-pref-width: 50; -fx-pref-height: 50;");

        controls.getChildren().addAll(muteBtn, videoBtn, switchBtn, endBtn);
        root.setBottom(controls);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // ==================== INCOMING CALL DIALOG ====================

    public static void showIncomingCallDialog(String callerName, String callType,
                                              Runnable onAnswer, Runnable onReject) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setAlwaysOnTop(true);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 16;");
        root.setPrefSize(300, 350);

        Label callIcon = new Label("video".equals(callType) ? "📹" : "📞");
        callIcon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Cuộc gọi đến");
        title.setStyle("-fx-font-size: 14px; -fx-text-fill: #65676b;");

        Label nameLabel = new Label(callerName);
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");

        Label typeLabel = new Label("video".equals(callType) ? "Cuộc gọi video" : "Cuộc gọi thoại");
        typeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #65676b;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(30);
        buttons.setAlignment(Pos.CENTER);

        Button rejectBtn = new Button("✕");
        rejectBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; " +
                "-fx-font-size: 20px; -fx-pref-width: 60; -fx-pref-height: 60; " +
                "-fx-background-radius: 30;");
        rejectBtn.setOnAction(e -> {
            onReject.run();
            stage.close();
        });

        Button answerBtn = new Button("✓");
        answerBtn.setStyle("-fx-background-color: #31a24c; -fx-text-fill: white; " +
                "-fx-font-size: 20px; -fx-pref-width: 60; -fx-pref-height: 60; " +
                "-fx-background-radius: 30;");
        answerBtn.setOnAction(e -> {
            onAnswer.run();
            stage.close();
        });

        buttons.getChildren().addAll(rejectBtn, answerBtn);

        root.getChildren().addAll(callIcon, title, nameLabel, typeLabel, spacer, buttons);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private static Button createCallButton(String icon, String tooltip, Runnable onClick) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 25; " +
                "-fx-pref-width: 50; -fx-pref-height: 50; -fx-font-size: 18px;");
        btn.setOnAction(e -> onClick.run());
        Tooltip.install(btn, new Tooltip(tooltip));
        return btn;
    }
}