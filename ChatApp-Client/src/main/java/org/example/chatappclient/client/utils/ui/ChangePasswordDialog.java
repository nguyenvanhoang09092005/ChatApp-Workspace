package org.example.chatappclient.client.utils.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ChangePasswordDialog {

    public static void show() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText(null);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setPrefWidth(380);

        PasswordField oldPass = new PasswordField();
        PasswordField newPass = new PasswordField();
        PasswordField confirm = new PasswordField();

        Button save = new Button("Đổi mật khẩu");
        Button cancel = new Button("Hủy");

        save.setOnAction(e -> {
            if (oldPass.getText().isEmpty()
                    || newPass.getText().isEmpty()
                    || confirm.getText().isEmpty()) {

                AlertUtil.showToastError("Nhập đầy đủ thông tin");
                return;
            }

            if (!newPass.getText().equals(confirm.getText())) {
                AlertUtil.showToastError("Mật khẩu xác nhận không khớp");
                return;
            }

            // TODO: gọi API đổi mật khẩu
            AlertUtil.showToastSuccess("Đổi mật khẩu thành công");
            dialog.close();
        });

        cancel.setOnAction(e -> dialog.close());

        HBox actions = new HBox(10, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
                new Label("Mật khẩu cũ"), oldPass,
                new Label("Mật khẩu mới"), newPass,
                new Label("Xác nhận mật khẩu"), confirm,
                actions
        );

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
