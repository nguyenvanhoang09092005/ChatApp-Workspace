package org.example.chatappclient.client.utils.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.chatappclient.client.models.User;
import org.example.chatappclient.client.services.UserService;

import java.util.function.Consumer;

/**
 * Dialog tìm kiếm người dùng để bắt đầu cuộc trò chuyện mới
 */
public class UserSearchDialog {

    private Stage dialog;
    private TextField searchField;
    private VBox resultContainer;
    private Label statusLabel;
    private Button confirmButton;

    private User selectedUser;
    private Consumer<User> onUserSelected;

    public UserSearchDialog(Consumer<User> onUserSelected) {
        this.onUserSelected = onUserSelected;
        createDialog();
    }

    private void createDialog() {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setTitle("Tìm kiếm người dùng");
        dialog.setWidth(500);
        dialog.setHeight(600);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #ffffff;");

        // Header
        Label titleLabel = new Label("Bắt đầu cuộc trò chuyện mới");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Nhập email hoặc số điện thoại để tìm kiếm");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");

        // Search field
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER);

        searchField = new TextField();
        searchField.setPromptText("Nhập email hoặc số điện thoại...");
        searchField.setPrefHeight(40);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setStyle(
                "-fx-background-color: #f0f2f5;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 10 15;" +
                        "-fx-font-size: 14px;"
        );

        Button searchButton = new Button("Tìm kiếm");
        searchButton.setPrefHeight(40);
        searchButton.setPrefWidth(100);
        searchButton.setStyle(
                "-fx-background-color: #0084ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        searchButton.setOnAction(e -> searchUser());

        searchBox.getChildren().addAll(searchField, searchButton);

        // Status label
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 12px;");

        // Results container
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        resultContainer = new VBox(10);
        resultContainer.setPadding(new Insets(10));
        resultContainer.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 10;");

        scrollPane.setContent(resultContainer);

        // Show initial message
        showEmptyState();

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton = new Button("Hủy");
        cancelButton.setPrefWidth(100);
        cancelButton.setStyle(
                "-fx-background-color: #e4e6eb;" +
                        "-fx-text-fill: #050505;" +
                        "-fx-background-radius: 5;" +
                        "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> dialog.close());

        confirmButton = new Button("Nhắn tin");
        confirmButton.setPrefWidth(100);
        confirmButton.setDisable(true);
        confirmButton.setStyle(
                "-fx-background-color: #0084ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-cursor: hand;"
        );
        confirmButton.setOnAction(e -> {
            if (selectedUser != null && onUserSelected != null) {
                onUserSelected.accept(selectedUser);
                dialog.close();
            }
        });

        actionButtons.getChildren().addAll(cancelButton, confirmButton);

        // Add all to root
        root.getChildren().addAll(
                titleLabel,
                subtitleLabel,
                searchBox,
                statusLabel,
                scrollPane,
                actionButtons
        );

        // Handle Enter key in search field
        searchField.setOnAction(e -> searchUser());

        Scene scene = new Scene(root);
        dialog.setScene(scene);
    }

    private void searchUser() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            statusLabel.setText("⚠️ Vui lòng nhập email hoặc số điện thoại");
            statusLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }

        // Show loading
        showLoading();
        statusLabel.setText("🔍 Đang tìm kiếm...");
        statusLabel.setStyle("-fx-text-fill: #0084ff;");

        // Search in background thread
        new Thread(() -> {
            try {
                User user = UserService.getInstance().findUserByEmailOrPhone(query);

                Platform.runLater(() -> {
                    if (user != null) {
                        displayUserResult(user);
                        statusLabel.setText("✅ Tìm thấy 1 kết quả");
                        statusLabel.setStyle("-fx-text-fill: #31a24c;");
                    } else {
                        showNotFound();
                        statusLabel.setText("❌ Không tìm thấy người dùng");
                        statusLabel.setStyle("-fx-text-fill: #f44336;");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    statusLabel.setText("⚠️ Lỗi: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #f44336;");
                });
            }
        }).start();
    }

    private void displayUserResult(User user) {
        resultContainer.getChildren().clear();
        selectedUser = null;
        confirmButton.setDisable(true);

        VBox userCard = createUserCard(user);
        resultContainer.getChildren().add(userCard);
    }

    private VBox createUserCard(User user) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #e4e6eb;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
        );

        // Avatar and info container
        HBox topSection = new HBox(15);
        topSection.setAlignment(Pos.CENTER_LEFT);

        // Avatar with online indicator
        StackPane avatarContainer = new StackPane();

        Circle avatarCircle = new Circle(35);
        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(70);
        avatarView.setFitHeight(70);
        avatarView.setPreserveRatio(true);

        // Load avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            try {
                Image avatarImage = new Image(user.getAvatarUrl(), true);
                avatarView.setImage(avatarImage);
            } catch (Exception e) {
                // Use default avatar
                setDefaultAvatar(avatarView, user.getUsername());
            }
        } else {
            setDefaultAvatar(avatarView, user.getUsername());
        }

        avatarView.setClip(avatarCircle);

        // Online indicator
        Circle onlineIndicator = new Circle(8);
        onlineIndicator.setFill(user.isOnline() ? Color.web("#31a24c") : Color.web("#b0b3b8"));
        onlineIndicator.setStroke(Color.WHITE);
        onlineIndicator.setStrokeWidth(2);
        StackPane.setAlignment(onlineIndicator, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(onlineIndicator, new Insets(0, 5, 0, 0));

        avatarContainer.getChildren().addAll(avatarView, onlineIndicator);

        // User info
        VBox userInfo = new VBox(5);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        Label nameLabel = new Label(user.getDisplayName() != null ?
                user.getDisplayName() : user.getUsername());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Edit icon (just for display)
        HBox nameContainer = new HBox(5);
        nameContainer.setAlignment(Pos.CENTER_LEFT);
        Label editIcon = new Label("✏️");
        editIcon.setStyle("-fx-font-size: 12px;");
        nameContainer.getChildren().addAll(nameLabel, editIcon);

        userInfo.getChildren().add(nameContainer);

        topSection.getChildren().addAll(avatarContainer, userInfo);

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button friendButton = new Button("Kết bạn");
        friendButton.setPrefWidth(150);
        friendButton.setStyle(
                "-fx-background-color: #e4e6eb;" +
                        "-fx-text-fill: #050505;" +
                        "-fx-background-radius: 5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );

        Button messageButton = new Button("Nhắn tin");
        messageButton.setPrefWidth(200);
        messageButton.setStyle(
                "-fx-background-color: #0084ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        messageButton.setOnAction(e -> {
            selectedUser = user;
            confirmButton.setDisable(false);
            if (onUserSelected != null) {
                onUserSelected.accept(user);
                dialog.close();
            }
        });

        buttonBox.getChildren().addAll(friendButton, messageButton);

        // Personal info section
        VBox infoSection = new VBox(10);
        infoSection.setPadding(new Insets(10, 0, 0, 0));

        Label infoTitle = new Label("Thông tin cá nhân");
        infoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox infoList = new VBox(8);

        // Gender
        if (user.getGender() != null && !user.getGender().isEmpty()) {
            HBox genderRow = createInfoRow("Giới tính", user.getGender());
            infoList.getChildren().add(genderRow);
        }

        // Birthday
        if (user.getBirthday() != null) {
            HBox birthdayRow = createInfoRow("Ngày sinh", "••/••/••••");
            infoList.getChildren().add(birthdayRow);
        }

        infoSection.getChildren().addAll(infoTitle, infoList);

        // Additional options
        VBox optionsSection = new VBox(8);
        optionsSection.setPadding(new Insets(10, 0, 0, 0));

        Button groupButton = createOptionButton("👥 Nhóm chung (0)");
        Button shareButton = createOptionButton("📋 Chia sẻ danh thiếp");
        Button blockButton = createOptionButton("🚫 Chặn tin nhắn và cuộc gọi");
        Button reportButton = createOptionButton("⚠️ Báo xấu");

        optionsSection.getChildren().addAll(
                groupButton, shareButton, blockButton, reportButton
        );

        card.getChildren().addAll(
                topSection,
                buttonBox,
                new Separator(),
                infoSection,
                new Separator(),
                optionsSection
        );

        return card;
    }

    private HBox createInfoRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelText = new Label(label);
        labelText.setStyle("-fx-text-fill: #65676b; -fx-font-size: 13px;");
        labelText.setPrefWidth(100);

        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 13px;");
        HBox.setHgrow(valueText, Priority.ALWAYS);

        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    private Button createOptionButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #050505;" +
                        "-fx-padding: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );
        button.setOnMouseEntered(e ->
                button.setStyle(
                        "-fx-background-color: #f0f2f5;" +
                                "-fx-text-fill: #050505;" +
                                "-fx-padding: 10;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-size: 13px;" +
                                "-fx-background-radius: 5;"
                )
        );
        button.setOnMouseExited(e ->
                button.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: #050505;" +
                                "-fx-padding: 10;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-size: 13px;"
                )
        );
        return button;
    }

    private void setDefaultAvatar(ImageView imageView, String username) {
        // Create colored background with initial
        // This is simplified - you might want to use Canvas for better rendering
        try {
            String initial = username.substring(0, 1).toUpperCase();
            // Use a default image or generate one programmatically
        } catch (Exception e) {
            // Fallback to default
        }
    }

    private void showEmptyState() {
        resultContainer.getChildren().clear();
        Label emptyLabel = new Label("👤 Nhập email hoặc số điện thoại để tìm kiếm");
        emptyLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14px;");
        VBox emptyBox = new VBox(emptyLabel);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(40));
        resultContainer.getChildren().add(emptyBox);
    }

    private void showLoading() {
        resultContainer.getChildren().clear();
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(50, 50);
        Label loadingLabel = new Label("Đang tìm kiếm...");
        loadingLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14px;");
        VBox loadingBox = new VBox(10, progress, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(40));
        resultContainer.getChildren().add(loadingBox);
    }

    private void showNotFound() {
        resultContainer.getChildren().clear();
        Label notFoundLabel = new Label("❌ Không tìm thấy người dùng");
        notFoundLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label hintLabel = new Label("Vui lòng kiểm tra lại email hoặc số điện thoại");
        hintLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 12px;");
        VBox notFoundBox = new VBox(10, notFoundLabel, hintLabel);
        notFoundBox.setAlignment(Pos.CENTER);
        notFoundBox.setPadding(new Insets(40));
        resultContainer.getChildren().add(notFoundBox);
    }

    private void showError(String message) {
        resultContainer.getChildren().clear();
        Label errorLabel = new Label("⚠️ Lỗi: " + message);
        errorLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 14px;");
        errorLabel.setWrapText(true);
        VBox errorBox = new VBox(errorLabel);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(40));
        resultContainer.getChildren().add(errorBox);
    }

    public void show() {
        dialog.showAndWait();
    }
}