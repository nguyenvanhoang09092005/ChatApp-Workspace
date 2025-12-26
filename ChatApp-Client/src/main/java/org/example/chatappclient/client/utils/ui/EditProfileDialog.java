package org.example.chatappclient.client.utils.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.example.chatappclient.client.models.User;
import java.io.File;
import java.time.LocalDate;

public class EditProfileDialog {

    private static String selectedAvatarPath = null;

    public static void show(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa thông tin cá nhân");
        dialog.setHeaderText(null);
        dialog.setResizable(true);

        // Main container
        BorderPane mainContainer = new BorderPane();
        mainContainer.setPrefSize(700, 600);
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");

        // Header với avatar
        VBox header = createHeader(user);
        mainContainer.setTop(header);

        // Content area với ScrollPane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5; -fx-background-color: #f5f5f5;");

        VBox content = createContent(user);
        scrollPane.setContent(content);
        mainContainer.setCenter(scrollPane);

        // Footer với buttons
        HBox footer = createFooter(dialog, user);
        mainContainer.setBottom(footer);

        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Ẩn default buttons
        dialog.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        dialog.showAndWait();
    }

    private static VBox createHeader(User user) {
        VBox header = new VBox(15);
        header.setPadding(new Insets(30, 20, 30, 20));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2);");

        // Avatar container
        StackPane avatarContainer = new StackPane();

        // Avatar circle
        Circle avatarCircle = new Circle(60);
        avatarCircle.setFill(Color.WHITE);
        avatarCircle.setStroke(Color.WHITE);
        avatarCircle.setStrokeWidth(4);

        // Avatar image
        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(120);
        avatarView.setFitHeight(120);
        avatarView.setPreserveRatio(true);

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            try {
                avatarView.setImage(new Image(user.getAvatarUrl()));
            } catch (Exception e) {
                avatarView.setImage(null);
            }
        }

        Circle clip = new Circle(60);
        clip.setCenterX(60);
        clip.setCenterY(60);
        avatarView.setClip(clip);

        // Change avatar button overlay
        Button changeAvatarBtn = new Button("📷");
        changeAvatarBtn.setStyle(
                "-fx-background-color: rgba(0,0,0,0.6); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 20px; " +
                        "-fx-background-radius: 50%; " +
                        "-fx-min-width: 40px; " +
                        "-fx-min-height: 40px; " +
                        "-fx-max-width: 40px; " +
                        "-fx-max-height: 40px; " +
                        "-fx-cursor: hand;"
        );
        changeAvatarBtn.setTranslateX(35);
        changeAvatarBtn.setTranslateY(35);

        changeAvatarBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn ảnh đại diện");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(changeAvatarBtn.getScene().getWindow());
            if (selectedFile != null) {
                selectedAvatarPath = selectedFile.toURI().toString();
                avatarView.setImage(new Image(selectedAvatarPath));
            }
        });

        avatarContainer.getChildren().addAll(avatarView, changeAvatarBtn);

        // Username label
        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

        header.getChildren().addAll(avatarContainer, usernameLabel);

        return header;
    }

    private static VBox createContent(User user) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 40, 30, 40));

        // Thông tin cơ bản
        VBox basicInfo = createSection("Thông tin cơ bản");
        TextField displayName = createStyledTextField("Tên hiển thị", user.getDisplayName());
        TextField email = createStyledTextField("Email", user.getEmail());
        TextField phone = createStyledTextField("Số điện thoại", user.getPhone());

        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("Nam", "Nữ", "Khác");
        genderBox.setValue(user.getGender() != null ? user.getGender() : "Khác");
        genderBox.setPromptText("Giới tính");
        genderBox.setMaxWidth(Double.MAX_VALUE);
        styleComboBox(genderBox);

        DatePicker birthdayPicker = new DatePicker();
        if (user.getBirthday() != null) {
            birthdayPicker.setValue(user.getBirthday());
        }
        birthdayPicker.setPromptText("Ngày sinh");
        birthdayPicker.setMaxWidth(Double.MAX_VALUE);
        styleDatePicker(birthdayPicker);

        basicInfo.getChildren().addAll(
                createFieldLabel("Tên hiển thị *"), displayName,
                createFieldLabel("Email"), email,
                createFieldLabel("Số điện thoại"), phone,
                createFieldLabel("Giới tính"), genderBox,
                createFieldLabel("Ngày sinh"), birthdayPicker
        );

        // Giới thiệu
        VBox aboutSection = createSection("Giới thiệu bản thân");
        TextArea bio = createStyledTextArea("Viết vài dòng về bản thân...", user.getBio());
        TextField statusMessage = createStyledTextField("Trạng thái", user.getStatusMessage());

        aboutSection.getChildren().addAll(
                createFieldLabel("Tiểu sử"), bio,
                createFieldLabel("Tin nhắn trạng thái"), statusMessage
        );

        // Bảo mật (chỉ hiển thị, không cho edit)
        VBox securitySection = createSection("Bảo mật & Quyền riêng tư");
        Label verifiedLabel = new Label(user.isVerified() ? "✓ Tài khoản đã xác thực" : "⚠ Tài khoản chưa xác thực");
        verifiedLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (user.isVerified() ? "#27ae60" : "#e67e22") + ";");

        Label activeLabel = new Label(user.isActive() ? "✓ Tài khoản đang hoạt động" : "⚠ Tài khoản bị vô hiệu hóa");
        activeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (user.isActive() ? "#27ae60" : "#e74c3c") + ";");

        Button changePasswordBtn = createSecondaryButton("Đổi mật khẩu");
        changePasswordBtn.setOnAction(e -> {
            // TODO: Mở dialog đổi mật khẩu
            AlertUtil.showToastInfo("Chức năng đổi mật khẩu");
        });

        securitySection.getChildren().addAll(verifiedLabel, activeLabel, changePasswordBtn);

        content.getChildren().addAll(basicInfo, aboutSection, securitySection);

        // Store references for later use
        content.setUserData(new FormData(displayName, email, phone, genderBox, birthdayPicker, bio, statusMessage));

        return content;
    }

    private static VBox createSection(String title) {
        VBox section = new VBox(12);
        section.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-padding: 20px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #ecf0f1;");

        section.getChildren().addAll(titleLabel, separator);

        return section;
    }

    private static Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e; -fx-font-weight: 600;");
        return label;
    }

    private static TextField createStyledTextField(String prompt, String value) {
        TextField field = new TextField(value != null ? value : "");
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 10px 15px; " +
                        "-fx-font-size: 13px;"
        );
        field.setOnMouseEntered(e -> field.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #667eea; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 10px 15px; " +
                        "-fx-font-size: 13px;"
        ));
        field.setOnMouseExited(e -> {
            if (!field.isFocused()) {
                field.setStyle(
                        "-fx-background-color: #f8f9fa; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-border-color: #dee2e6; " +
                                "-fx-border-radius: 8px; " +
                                "-fx-padding: 10px 15px; " +
                                "-fx-font-size: 13px;"
                );
            }
        });
        return field;
    }

    private static TextArea createStyledTextArea(String prompt, String value) {
        TextArea area = new TextArea(value != null ? value : "");
        area.setPromptText(prompt);
        area.setPrefRowCount(4);
        area.setWrapText(true);
        area.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 10px 15px; " +
                        "-fx-font-size: 13px;"
        );
        return area;
    }

    private static void styleComboBox(ComboBox<String> comboBox) {
        comboBox.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 5px 15px; " +
                        "-fx-font-size: 13px;"
        );
    }

    private static void styleDatePicker(DatePicker datePicker) {
        datePicker.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-font-size: 13px;"
        );
    }

    private static HBox createFooter(Dialog<ButtonType> dialog, User user) {
        HBox footer = new HBox(15);
        footer.setPadding(new Insets(20, 40, 20, 40));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-width: 1px 0 0 0;");

        Button cancelBtn = createSecondaryButton("Hủy");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = createPrimaryButton("Lưu thay đổi");
        saveBtn.setOnAction(e -> {
            ScrollPane scrollPane = (ScrollPane) ((BorderPane) dialog.getDialogPane().getContent()).getCenter();
            VBox content = (VBox) scrollPane.getContent();
            FormData formData = (FormData) content.getUserData();

            if (formData.displayName.getText().trim().isEmpty()) {
                AlertUtil.showToastError("Tên hiển thị không được trống");
                return;
            }

            // Update user object
            user.setDisplayName(formData.displayName.getText().trim());
            user.setEmail(formData.email.getText().trim());
            user.setPhone(formData.phone.getText().trim());
            user.setGender(formData.genderBox.getValue());
            user.setBirthday(formData.birthdayPicker.getValue());
            user.setBio(formData.bio.getText().trim());
            user.setStatusMessage(formData.statusMessage.getText().trim());

            if (selectedAvatarPath != null) {
                user.setAvatarUrl(selectedAvatarPath);
            }

            // TODO: Call API to update user
            AlertUtil.showToastSuccess("Cập nhật thông tin thành công");
            dialog.close();
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);

        return footer;
    }

    private static Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(to right, #5568d3, #6a3f8f); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        ));
        return btn;
    }

    private static Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-text-fill: #495057; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #e9ecef; " +
                        "-fx-text-fill: #495057; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-text-fill: #495057; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-cursor: hand;"
        ));
        return btn;
    }

    // Helper class to store form field references
    private static class FormData {
        TextField displayName;
        TextField email;
        TextField phone;
        ComboBox<String> genderBox;
        DatePicker birthdayPicker;
        TextArea bio;
        TextField statusMessage;

        FormData(TextField displayName, TextField email, TextField phone,
                 ComboBox<String> genderBox, DatePicker birthdayPicker,
                 TextArea bio, TextField statusMessage) {
            this.displayName = displayName;
            this.email = email;
            this.phone = phone;
            this.genderBox = genderBox;
            this.birthdayPicker = birthdayPicker;
            this.bio = bio;
            this.statusMessage = statusMessage;
        }
    }
}