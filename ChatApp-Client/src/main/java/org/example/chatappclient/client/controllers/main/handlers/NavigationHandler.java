package org.example.chatappclient.client.controllers.main.handlers;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.chatappclient.client.config.Constants;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.services.AuthService;
import org.example.chatappclient.client.utils.ui.AlertUtil;
import org.example.chatappclient.client.utils.ui.DialogFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Handler xử lý điều hướng và chuyển đổi màn hình
 */
public class NavigationHandler {

    private final MainController mainController;
    private final AuthService authService;

    public NavigationHandler(MainController mainController) {
        this.mainController = mainController;
        this.authService = AuthService.getInstance();
    }

    // ==================== NAVIGATION METHODS ====================

    public void showMessages() {
        mainController.setActiveNavButton(mainController.getMessagesButton());
        mainController.setPanelTitle("Tin nhắn");
        // Refresh conversation list if needed
    }

    public void showContacts() {
        mainController.setActiveNavButton(mainController.getContactsButton());
        mainController.setPanelTitle("Danh bạ");
        AlertUtil.showToastInfo("Chức năng Danh bạ đang được phát triển");
    }

    public void showGroups() {
        mainController.setActiveNavButton(mainController.getGroupsButton());
        mainController.setPanelTitle("Nhóm");
        AlertUtil.showToastInfo("Chức năng Nhóm đang được phát triển");
    }

    public void showNotifications() {
        mainController.setActiveNavButton(mainController.getNotificationsButton());
        mainController.setPanelTitle("Thông báo");
        AlertUtil.showToastInfo("Chức năng Thông báo đang được phát triển");
    }

    public void showCloudStorage() {
        AlertUtil.showToastInfo("Cloud của tôi đang được phát triển");
    }

    public void showSettings() {
        DialogFactory.showSettingsDialog(mainController.getCurrentUser(), this::handleLogout);
    }

    // ==================== AUTH NAVIGATION ====================

    public void navigateToLogin(VBox container) {
        Platform.runLater(() -> {
            try {
                URL fxmlUrl = getClass().getResource("/fxml/auth/login.fxml");
                if (fxmlUrl == null) {
                    throw new IOException("Login FXML not found");
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent root = loader.load();

                Stage stage = (Stage) container.getScene().getWindow();
                Scene scene = new Scene(root);

                URL cssUrl = getClass().getResource("/css/auth/login.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }

                stage.setScene(scene);
                stage.setTitle("Đăng nhập - " + Constants.APP_NAME);
                stage.centerOnScreen();

            } catch (IOException e) {
                AlertUtil.showError("Lỗi", "Không thể chuyển đến màn hình đăng nhập: " + e.getMessage());
            }
        });
    }

    public void handleLogout() {
        boolean confirm = AlertUtil.showConfirmation("Đăng xuất", "Bạn có chắc chắn muốn đăng xuất?");
        if (!confirm) return;

        new Thread(() -> {
            try {
                authService.logout();
                mainController.cleanup();
                navigateToLogin(mainController.getLeftSidebar());
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Lỗi đăng xuất: " + e.getMessage()));
            }
        }).start();
    }

    // ==================== SCREEN NAVIGATION ====================

    public void navigateToScreen(String fxmlPath, String cssPath, String title) {
        Platform.runLater(() -> {
            try {
                URL fxmlUrl = getClass().getResource(fxmlPath);
                if (fxmlUrl == null) {
                    throw new IOException("FXML not found: " + fxmlPath);
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent root = loader.load();

                Stage stage = (Stage) mainController.getLeftSidebar().getScene().getWindow();
                Scene scene = new Scene(root);

                if (cssPath != null) {
                    URL cssUrl = getClass().getResource(cssPath);
                    if (cssUrl != null) {
                        scene.getStylesheets().add(cssUrl.toExternalForm());
                    }
                }

                stage.setScene(scene);
                stage.setTitle(title + " - " + Constants.APP_NAME);

            } catch (IOException e) {
                AlertUtil.showError("Lỗi", "Không thể mở màn hình: " + e.getMessage());
            }
        });
    }
}