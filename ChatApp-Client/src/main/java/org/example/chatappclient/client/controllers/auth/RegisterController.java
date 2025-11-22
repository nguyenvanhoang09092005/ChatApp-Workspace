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
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label errorLabel;
    @FXML private Label passwordStrengthLabel;
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private CheckBox termsCheckbox;

    private AuthService authService;
    private SocketClient socketClient;
    private ExecutorService executorService;

    @FXML
    public void initialize() {
        authService = AuthService.getInstance();
        socketClient = SocketClient.getInstance();
        executorService = Executors.newCachedThreadPool();

        // Set primary stage for toast notifications - DEFER THIS
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) registerButton.getScene().getWindow();
                if (stage != null) {
                    AlertUtil.setPrimaryStage(stage);
                }
            } catch (Exception e) {
                // Ignore if stage not ready
            }
        });

        // Hide elements initially
        errorLabel.setVisible(false);
        loadingIndicator.setVisible(false);
        passwordStrengthLabel.setVisible(false);
        passwordStrengthBar.setVisible(false);

        // Setup listeners
        setupInputListeners();
        setupPasswordStrengthIndicator();
        setupEnterKeyHandler();
    }

    /**
     * Setup input listeners
     */
    private void setupInputListeners() {
        usernameField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });

        emailField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });

        passwordField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });

        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });
    }

    /**
     * Setup password strength indicator
     */
    private void setupPasswordStrengthIndicator() {
        passwordField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal.isEmpty()) {
                passwordStrengthLabel.setVisible(false);
                passwordStrengthBar.setVisible(false);
                return;
            }

            ValidationUtil.PasswordStrength strength =
                    ValidationUtil.checkPasswordStrength(newVal);

            passwordStrengthLabel.setVisible(true);
            passwordStrengthBar.setVisible(true);

            switch (strength) {
                case WEAK:
                    passwordStrengthLabel.setText("Mật khẩu yếu");
                    passwordStrengthLabel.setStyle("-fx-text-fill: #F44336;");
                    passwordStrengthBar.setProgress(0.33);
                    passwordStrengthBar.setStyle("-fx-accent: #F44336;");
                    break;
                case MEDIUM:
                    passwordStrengthLabel.setText("Mật khẩu trung bình");
                    passwordStrengthLabel.setStyle("-fx-text-fill: #FF9800;");
                    passwordStrengthBar.setProgress(0.66);
                    passwordStrengthBar.setStyle("-fx-accent: #FF9800;");
                    break;
                case STRONG:
                    passwordStrengthLabel.setText("Mật khẩu mạnh");
                    passwordStrengthLabel.setStyle("-fx-text-fill: #4CAF50;");
                    passwordStrengthBar.setProgress(1.0);
                    passwordStrengthBar.setStyle("-fx-accent: #4CAF50;");
                    break;
            }
        });
    }

    /**
     * Setup enter key handler
     */
    private void setupEnterKeyHandler() {
        usernameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> handleRegister());
    }

    /**
     * Handle register button click
     */
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate inputs
        if (!validateInputs(username, email, password, confirmPassword)) {
            return;
        }

        // Check terms agreement
        if (!termsCheckbox.isSelected()) {
            AlertUtil.showToastWarning("Bạn cần đồng ý với điều khoản sử dụng");
            return;
        }

        // Disable UI during registration
        setUIEnabled(false);
        showLoading(true);
        errorLabel.setVisible(false);

        // Perform registration in background thread
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

                // Attempt registration
                AuthService.RegisterResult result =
                        authService.register(username, email, password, confirmPassword);

                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        handleRegisterSuccess(email);
                    } else {
                        AlertUtil.showToastError(result.getMessage());
                        setUIEnabled(true);
                        showLoading(false);
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
     * Handle successful registration - Navigate FIRST, toast AFTER
     */
    private void handleRegisterSuccess(String email) {
        try {
            // STEP 1: Load FXML
            URL fxmlUrl = getClass().getResource("/fxml/auth/verify-email.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Verify email FXML not found");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // STEP 2: Pass email to verify controller
            VerifyEmailController controller = loader.getController();
            controller.setEmail(email);

            // STEP 3: Get stage and create scene
            Stage stage = (Stage) registerButton.getScene().getWindow();
            Scene scene = new Scene(root);

            // Load CSS with fallback
            try {
                URL cssUrl = getClass().getResource("/css/auth/verify-email.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    URL fallbackCss = getClass().getResource("/css/auth/login.css");
                    if (fallbackCss != null) {
                        scene.getStylesheets().add(fallbackCss.toExternalForm());
                    }
                }
            } catch (Exception e) {
                // Continue without CSS
            }

            // STEP 4: Switch scene IMMEDIATELY
            stage.setScene(scene);
            stage.setTitle("Xác thực Email - " + Constants.APP_NAME);

            // STEP 5: Show toast AFTER navigation completes
            Platform.runLater(() -> {
                AlertUtil.showToastSuccess("Đăng ký thành công! Kiểm tra email để xác thực.");
            });

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showToastError("Lỗi khi chuyển trang: " + e.getMessage());
            setUIEnabled(true);
            showLoading(false);
        }
    }

    /**
     * Validate registration inputs
     */
    private boolean validateInputs(String username, String email,
                                   String password, String confirmPassword) {
        // Check empty fields
        if (!ValidationUtil.areFieldsFilled(username, email, password, confirmPassword)) {
            AlertUtil.showToastWarning(Constants.ERROR_REQUIRED_FIELD);
            return false;
        }

        // Validate username
        String usernameError = ValidationUtil.getUsernameErrorMessage(username);
        if (usernameError != null) {
            AlertUtil.showToastWarning(usernameError);
            usernameField.requestFocus();
            return false;
        }

        // Validate email
        String emailError = ValidationUtil.getEmailErrorMessage(email);
        if (emailError != null) {
            AlertUtil.showToastWarning(emailError);
            emailField.requestFocus();
            return false;
        }

        // Validate password
        String passwordError = ValidationUtil.getPasswordErrorMessage(password);
        if (passwordError != null) {
            AlertUtil.showToastWarning(passwordError);
            passwordField.requestFocus();
            return false;
        }

        // Check password match
        if (!password.equals(confirmPassword)) {
            AlertUtil.showToastWarning(Constants.ERROR_PASSWORD_MISMATCH);
            confirmPasswordField.requestFocus();
            return false;
        }

        // Check password strength
        ValidationUtil.PasswordStrength strength =
                ValidationUtil.checkPasswordStrength(password);
        if (strength == ValidationUtil.PasswordStrength.WEAK) {
            AlertUtil.showToastWarning("Mật khẩu quá yếu. Vui lòng sử dụng mật khẩu mạnh hơn.");
            passwordField.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Handle login link click
     */
    @FXML
    private void handleBackToLogin() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/auth/login.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Login FXML not found");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Stage stage = (Stage) loginLink.getScene().getWindow();
            Scene scene = new Scene(root);

            // Load CSS
            try {
                URL cssUrl = getClass().getResource("/css/auth/login.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) {
                // Continue without CSS
            }

            stage.setScene(scene);
            stage.setTitle("Đăng nhập - " + Constants.APP_NAME);

        } catch (IOException e) {
            AlertUtil.showToastError("Lỗi khi quay lại trang đăng nhập: " + e.getMessage());
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
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
        usernameField.setDisable(!enabled);
        emailField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        confirmPasswordField.setDisable(!enabled);
        termsCheckbox.setDisable(!enabled);
        registerButton.setDisable(!enabled);
        loginLink.setDisable(!enabled);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}