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
import org.example.chatappclient.client.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Factory class để tạo các Dialog
 * FIXED: Removed duplicate call dialog methods (use AudioCallDialog, VideoCallDialog, IncomingCallDialog classes instead)
 */
public class DialogFactory {

    // ==================== NEW CHAT DIALOG ====================

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

    // ==================== BASIC DIALOGS ====================

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
        } catch (Exception e) {
            // Ignore image load errors
        }

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
                createSettingsItem("👤", "Chỉnh sửa thông tin cá nhân",  () -> EditProfileDialog.show(currentUser)),
                createSettingsItem("🔐", "Đổi mật khẩu",
                        ChangePasswordDialog::show),
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


}