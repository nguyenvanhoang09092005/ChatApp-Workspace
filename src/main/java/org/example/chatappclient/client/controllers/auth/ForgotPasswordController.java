package org.example.chatappclient.client.controllers.auth;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.config.Constants;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.chatappclient.client.services.AuthService;
import org.example.chatappclient.client.utils.validation.ValidationUtil;

import java.io.IOException;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button sendCodeButton;
    @FXML private Button resetPasswordButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label infoLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label timerLabel;

    private AuthService authService;
    private SocketClient socketClient;
    private boolean codeSent = false;
    private int remainingSeconds = 60;
    private Thread timerThread;

    @FXML
    public void initialize() {
        authService = AuthService.getInstance();
        socketClient = SocketClient.getInstance();

        // Hide elements initially
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        loadingIndicator.setVisible(false);
        timerLabel.setVisible(false);

        // Disable reset fields initially
        setResetFieldsEnabled(false);

        // Setup listeners
        setupInputListeners();
        setupEnterKeyHandler();
    }

    /**
     * Setup input listeners
     */
    private void setupInputListeners() {
        emailField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
            successLabel.setVisible(false);
        });

        codeField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });

        newPasswordField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });

        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });
    }

    /**
     * Setup enter key handler
     */
    private void setupEnterKeyHandler() {
        emailField.setOnAction(e -> handleSendCode());
        codeField.setOnAction(e -> newPasswordField.requestFocus());
        newPasswordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> handleResetPassword());
    }

    /**
     * Handle send code button click
     */
    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();

        // Validate email
        if (!validateEmail(email)) {
            return;
        }

        // Disable UI during operation
        setUIEnabled(false);
        showLoading(true);
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Send code in background thread
        new Thread(() -> {
            try {
                // Check connection
                if (!socketClient.isConnected()) {
                    if (!socketClient.connect()) {
                        Platform.runLater(() -> {
                            showError("Không thể kết nối đến server.");
                            setUIEnabled(true);
                            showLoading(false);
                        });
                        return;
                    }
                }

                // Request password reset
                boolean success = authService.forgotPassword(email);

                Platform.runLater(() -> {
                    showLoading(false);

                    if (success) {
                        handleSendCodeSuccess();
                    } else {
                        showError("Không tìm thấy email này trong hệ thống.");
                        setUIEnabled(true);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi: " + e.getMessage());
                    setUIEnabled(true);
                    showLoading(false);
                });
            }
        }).start();
    }

    /**
     * Handle send code success
     */
    private void handleSendCodeSuccess() {
        codeSent = true;

        showSuccess("Mã xác thực đã được gửi đến email của bạn.");

        // Enable reset fields
        setResetFieldsEnabled(true);
        emailField.setDisable(true);
        sendCodeButton.setDisable(true);

        // Start countdown timer
        startCountdownTimer();

        // Focus on code field
        codeField.requestFocus();
    }

    /**
     * Start countdown timer for resend code
     */
    private void startCountdownTimer() {
        remainingSeconds = 60;
        timerLabel.setVisible(true);

        timerThread = new Thread(() -> {
            while (remainingSeconds > 0) {
                Platform.runLater(() -> {
                    timerLabel.setText("Gửi lại mã sau " + remainingSeconds + "s");
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }

                remainingSeconds--;
            }

            Platform.runLater(() -> {
                timerLabel.setVisible(false);
                sendCodeButton.setDisable(false);
                sendCodeButton.setText("Gửi lại mã");
            });
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    /**
     * Handle reset password button click
     */
    @FXML
    private void handleResetPassword() {
        String email = emailField.getText().trim();
        String code = codeField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate inputs
        if (!validateResetInputs(code, newPassword, confirmPassword)) {
            return;
        }

        // Disable UI during operation
        setUIEnabled(false);
        showLoading(true);
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Reset password in background thread
        new Thread(() -> {
            try {
                // Attempt password reset
                boolean success = authService.resetPassword(email, code, newPassword);

                Platform.runLater(() -> {
                    showLoading(false);

                    if (success) {
                        handleResetPasswordSuccess();
                    } else {
                        showError("Mã xác thực không đúng hoặc đã hết hạn.");
                        setUIEnabled(true);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi: " + e.getMessage());
                    setUIEnabled(true);
                    showLoading(false);
                });
            }
        }).start();
    }

    /**
     * Handle reset password success
     */
    private void handleResetPasswordSuccess() {
        // Show success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đặt lại mật khẩu thành công");
        alert.setHeaderText(Constants.SUCCESS_PASSWORD_RESET);
        alert.setContentText("Bạn có thể đăng nhập với mật khẩu mới.");
        alert.showAndWait();

        // Navigate back to login
        handleBackToLogin();
    }

    /**
     * Validate email
     */
    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            showError("Vui lòng nhập email");
            emailField.requestFocus();
            return false;
        }

        String emailError = ValidationUtil.getEmailErrorMessage(email);
        if (emailError != null) {
            showError(emailError);
            emailField.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Validate reset password inputs
     */
    private boolean validateResetInputs(String code, String newPassword, String confirmPassword) {
        // Check empty fields
        if (!ValidationUtil.areFieldsFilled(code, newPassword, confirmPassword)) {
            showError(Constants.ERROR_REQUIRED_FIELD);
            return false;
        }

        // Validate code format
        if (!ValidationUtil.isValidVerificationCode(code)) {
            showError("Mã xác thực phải có 6 chữ số");
            codeField.requestFocus();
            return false;
        }

        // Validate password
        String passwordError = ValidationUtil.getPasswordErrorMessage(newPassword);
        if (passwordError != null) {
            showError(passwordError);
            newPasswordField.requestFocus();
            return false;
        }

        // Check password match
        if (!newPassword.equals(confirmPassword)) {
            showError(Constants.ERROR_PASSWORD_MISMATCH);
            confirmPasswordField.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Handle back to login link click
     */
    @FXML
    private void handleBackToLogin() {
        try {
            // Stop timer thread if running
            if (timerThread != null && timerThread.isAlive()) {
                timerThread.interrupt();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginLink.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Đăng nhập - " + Constants.APP_NAME);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi khi quay lại trang đăng nhập");
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        loadingIndicator.setVisible(show);
    }

    /**
     * Enable/disable UI controls
     */
    private void setUIEnabled(boolean enabled) {
        if (!emailField.isDisable()) {
            emailField.setDisable(!enabled);
        }
        if (!sendCodeButton.isDisable() || enabled) {
            sendCodeButton.setDisable(!enabled);
        }

        if (codeSent) {
            codeField.setDisable(!enabled);
            newPasswordField.setDisable(!enabled);
            confirmPasswordField.setDisable(!enabled);
            resetPasswordButton.setDisable(!enabled);
        }

        loginLink.setDisable(!enabled);
    }

    /**
     * Enable/disable reset password fields
     */
    private void setResetFieldsEnabled(boolean enabled) {
        codeField.setDisable(!enabled);
        newPasswordField.setDisable(!enabled);
        confirmPasswordField.setDisable(!enabled);
        resetPasswordButton.setDisable(!enabled);
    }
}