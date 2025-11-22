package org.example.chatappclient.client.controllers.auth;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.config.Constants;
import org.example.chatappclient.client.utils.ui.AlertUtil;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VerifyEmailController {

    @FXML private Label emailLabel;
    @FXML private TextField codeField;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private AuthService authService;
    private SocketClient socketClient;
    private ExecutorService executorService;
    private String email;
    private int remainingSeconds = 60;
    private Thread timerThread;

    @FXML
    public void initialize() {
        authService = AuthService.getInstance();
        socketClient = SocketClient.getInstance();
        executorService = Executors.newCachedThreadPool();

        // Set primary stage for toast notifications
        Platform.runLater(() -> {
            Stage stage = (Stage) verifyButton.getScene().getWindow();
            AlertUtil.setPrimaryStage(stage);
        });

        // Hide elements initially
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        loadingIndicator.setVisible(false);
        timerLabel.setVisible(false);

        // Setup listeners
        setupInputListeners();
        setupEnterKeyHandler();

        // Start countdown for resend
        startCountdownTimer();
    }

    /**
     * Set email for verification
     */
    public void setEmail(String email) {
        this.email = email;
        emailLabel.setText("Mã xác thực đã được gửi đến: " + email);
    }

    /**
     * Setup input listeners
     */
    private void setupInputListeners() {
        codeField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
            successLabel.setVisible(false);

            // Auto-format code (only digits, max 6)
            if (!newVal.matches("\\d*")) {
                codeField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (newVal.length() > 6) {
                codeField.setText(newVal.substring(0, 6));
            }
        });
    }

    /**
     * Setup enter key handler
     */
    private void setupEnterKeyHandler() {
        codeField.setOnAction(e -> handleVerify());
    }

    /**
     * Handle verify button click
     */
    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();

        // Validate code
        if (!validateCode(code)) {
            return;
        }

        // Disable UI during verification
        setUIEnabled(false);
        showLoading(true);
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Verify email in background thread
        executorService.submit(() -> {
            try {
                // Check connection
                if (!socketClient.isConnected()) {
                    if (!socketClient.connect()) {
                        Platform.runLater(() -> {
                            AlertUtil.showToastError("Không thể kết nối đến server");
                            setUIEnabled(true);
                            showLoading(false);
                        });
                        return;
                    }
                }

                // Attempt email verification
                boolean success = authService.verifyEmail(email, code);

                Platform.runLater(() -> {
                    showLoading(false);

                    if (success) {
                        handleVerifySuccess();
                    } else {
                        AlertUtil.showToastError("Mã xác thực không đúng hoặc đã hết hạn");
                        setUIEnabled(true);
                        codeField.clear();
                        codeField.requestFocus();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertUtil.showToastError("Lỗi: " + e.getMessage());
                    setUIEnabled(true);
                    showLoading(false);
                });
            }
        });
    }

    /**
     * Handle verify success
     */
    private void handleVerifySuccess() {
        // Show toast NGAY - không chặn UI
        AlertUtil.showToastSuccess("Xác thực thành công! Bạn có thể đăng nhập ngay.");

        // Stop timer
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt();
        }

        // Delay 1 giây để user đọc toast, rồi chuyển trang
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            Platform.runLater(this::handleBackToLogin);
        }).start();
    }

    /**
     * Handle resend code button click
     */
    @FXML
    private void handleResend() {
        // Disable UI during operation
        setUIEnabled(false);
        showLoading(true);
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Resend code in background thread
        executorService.submit(() -> {
            try {
                // Check connection
                if (!socketClient.isConnected()) {
                    if (!socketClient.connect()) {
                        Platform.runLater(() -> {
                            AlertUtil.showToastError("Không thể kết nối đến server");
                            setUIEnabled(true);
                            showLoading(false);
                        });
                        return;
                    }
                }

                // Request resend verification code
                boolean success = authService.resendVerificationCode(email);

                Platform.runLater(() -> {
                    showLoading(false);

                    if (success) {
                        AlertUtil.showToastSuccess("Mã xác thực mới đã được gửi đến email");
                        codeField.clear();
                        codeField.requestFocus();

                        // Restart countdown timer
                        startCountdownTimer();

                        setUIEnabled(true);
                        resendButton.setDisable(true);
                    } else {
                        AlertUtil.showToastError("Không thể gửi lại mã. Vui lòng thử lại sau.");
                        setUIEnabled(true);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertUtil.showToastError("Lỗi: " + e.getMessage());
                    setUIEnabled(true);
                    showLoading(false);
                });
            }
        });
    }

    /**
     * Start countdown timer for resend button
     */
    private void startCountdownTimer() {
        remainingSeconds = 60;
        timerLabel.setVisible(true);
        resendButton.setDisable(true);

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
                resendButton.setDisable(false);
            });
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    /**
     * Validate verification code
     */
    private boolean validateCode(String code) {
        if (code.isEmpty()) {
            AlertUtil.showToastWarning("Vui lòng nhập mã xác thực");
            codeField.requestFocus();
            return false;
        }

        if (!ValidationUtil.isValidVerificationCode(code)) {
            AlertUtil.showToastWarning("Mã xác thực phải có 6 chữ số");
            codeField.requestFocus();
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

            // Clean up executor service
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginLink.getScene().getWindow();
            Scene scene = new Scene(root);

            scene.getStylesheets().add(getClass().getResource("/css/auth/login.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Đăng nhập - " + Constants.APP_NAME);

        } catch (IOException e) {
            AlertUtil.showToastError("Lỗi khi quay lại trang đăng nhập");
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
        codeField.setDisable(!enabled);
        verifyButton.setDisable(!enabled);

        // Resend button controlled by timer
        if (enabled && remainingSeconds <= 0) {
            resendButton.setDisable(false);
        }

        loginLink.setDisable(!enabled);
    }

    /**
     * Clean up resources when controller is destroyed
     */
    public void cleanup() {
        // Stop timer thread
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt();
        }

        // Shutdown executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}