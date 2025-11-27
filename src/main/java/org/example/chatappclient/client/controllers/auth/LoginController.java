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
import org.example.chatappclient.client.models.Session;
import org.example.chatappclient.client.models.User;
import org.example.chatappclient.client.services.AuthService;
import org.example.chatappclient.client.utils.storage.PreferencesManager;
import org.example.chatappclient.client.utils.ui.AlertUtil;
import org.example.chatappclient.client.utils.validation.ValidationUtil;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private AuthService authService;
    private PreferencesManager preferencesManager;
    private SocketClient socketClient;
    private ExecutorService executorService;

    @FXML
    public void initialize() {
        authService = AuthService.getInstance();
        preferencesManager = PreferencesManager.getInstance();
        socketClient = SocketClient.getInstance();
        executorService = Executors.newCachedThreadPool();

        // Set primary stage for toast notifications - DEFER THIS
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) loginButton.getScene().getWindow();
                if (stage != null) {
                    AlertUtil.setPrimaryStage(stage);
                }
            } catch (Exception e) {
                // Ignore if stage not ready
            }
        });

        // Hide error label and loading indicator initially
        errorLabel.setVisible(false);
        loadingIndicator.setVisible(false);

        // Load saved credentials if remember me was checked
        loadSavedCredentials();

        // Add input listeners
        setupInputListeners();

        // Setup enter key handler
        setupEnterKeyHandler();
    }

    /**
     * Load saved credentials
     */
    private void loadSavedCredentials() {
        if (preferencesManager.isRememberMeEnabled()) {
            String savedUsername = preferencesManager.getSavedUsername();
            String savedPassword = preferencesManager.getSavedPassword();

            if (!savedUsername.isEmpty() && !savedPassword.isEmpty()) {
                usernameField.setText(savedUsername);
                passwordField.setText(savedPassword);
                rememberMeCheckbox.setSelected(true);
            }
        }
    }

    /**
     * Setup input listeners to clear error when user types
     */
    private void setupInputListeners() {
        usernameField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });

        passwordField.textProperty().addListener((obs, old, newVal) -> {
            errorLabel.setVisible(false);
        });
    }

    /**
     * Setup enter key handler
     */
    private void setupEnterKeyHandler() {
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
    }

    /**
     * Handle login button click
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheckbox.isSelected();

        // Validate inputs
        if (!validateInputs(username, password)) {
            return;
        }

        // Disable UI during login
        setUIEnabled(false);
        showLoading(true);
        errorLabel.setVisible(false);

        // Perform login in background thread
        executorService.submit(() -> {
            try {
                // Check connection
                if (!socketClient.isConnected()) {
                    if (!socketClient.connect()) {
                        Platform.runLater(() -> {
                            showError("Không thể kết nối đến server. Vui lòng kiểm tra kết nối.");
                            setUIEnabled(true);
                            showLoading(false);
                        });
                        return;
                    }
                }

                // Attempt login
                AuthService.LoginResult result = authService.login(username, password, rememberMe);

                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        // Navigate IMMEDIATELY without waiting for toast
                        handleLoginSuccess(result.getUser(), result.getSession());
                    } else {
                        // Show error and re-enable UI
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
     * Validate login inputs
     */
    private boolean validateInputs(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showError(Constants.ERROR_REQUIRED_FIELD);
            return false;
        }

        String usernameError = ValidationUtil.getUsernameErrorMessage(username);
        if (usernameError != null) {
            showError(usernameError);
            return false;
        }

        String passwordError = ValidationUtil.getPasswordErrorMessage(password);
        if (passwordError != null) {
            showError(passwordError);
            return false;
        }

        return true;
    }

    /**
     * Handle successful login - Navigate FIRST, toast AFTER (like RegisterController)
     */
    private void handleLoginSuccess(User user, Session session) {
        try {
            // STEP 1: Load FXML
            URL fxmlUrl = getClass().getResource("/fxml/Home/main.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/main.fxml");
                if (fxmlUrl == null) {
                    throw new IOException("Main FXML file not found");
                }
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // STEP 2: Get current stage
            Stage stage = (Stage) loginButton.getScene().getWindow();

            // STEP 3: Create new scene with CSS
            Scene scene = new Scene(root);

            // Load CSS with fallback
            try {
                URL cssUrl = getClass().getResource("/css/home/styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    URL fallbackCss = getClass().getResource("/css/styles.css");
                    if (fallbackCss != null) {
                        scene.getStylesheets().add(fallbackCss.toExternalForm());
                    }
                }
            } catch (Exception e) {
                // Continue without CSS
            }

            // STEP 4: Switch scene IMMEDIATELY - NO TOAST BEFORE THIS
            stage.setScene(scene);
            stage.setTitle(Constants.APP_NAME + " - " + user.getDisplayNameOrUsername());

            System.out.println("Login successful: " + user.getUsername());

            // STEP 5: Show toast AFTER navigation completes
            // Schedule toast to show after scene is fully rendered
            Platform.runLater(() -> {
                AlertUtil.showToastSuccess("Đăng nhập thành công! Xin chào " + user.getDisplayNameOrUsername());
            });

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showToastError("Lỗi khi chuyển trang: " + e.getMessage());
            setUIEnabled(true);
            showLoading(false);
        }
    }

    /**
     * Handle register link click
     */
    @FXML
    private void handleRegister() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/auth/register.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Register FXML not found");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Stage stage = (Stage) registerLink.getScene().getWindow();
            Scene scene = new Scene(root);

            // Load CSS
            try {
                URL cssUrl = getClass().getResource("/css/auth/register.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) {
                // Continue without CSS
            }

            stage.setScene(scene);
            stage.setTitle("Đăng ký - " + Constants.APP_NAME);

        } catch (IOException e) {
            AlertUtil.showToastError("Lỗi khi mở trang đăng ký: " + e.getMessage());
        }
    }

    /**
     * Handle forgot password link click
     */
    @FXML
    private void handleForgotPassword() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/auth/forgot-password.fxml");
            if (fxmlUrl == null) {
                // Fallback to verify-email
                fxmlUrl = getClass().getResource("/fxml/auth/verify-email.fxml");
                if (fxmlUrl == null) {
                    throw new IOException("Forgot password FXML not found");
                }
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Stage stage = (Stage) forgotPasswordLink.getScene().getWindow();
            Scene scene = new Scene(root);

            // Load CSS
            try {
                URL cssUrl = getClass().getResource("/css/auth/forgot-password.css");
                if (cssUrl == null) {
                    cssUrl = getClass().getResource("/css/auth/verify-email.css");
                }
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) {
                // Continue without CSS
            }

            stage.setScene(scene);
            stage.setTitle("Quên mật khẩu - " + Constants.APP_NAME);

        } catch (IOException e) {
            AlertUtil.showToastError("Lỗi khi mở trang quên mật khẩu: " + e.getMessage());
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
        passwordField.setDisable(!enabled);
        rememberMeCheckbox.setDisable(!enabled);
        loginButton.setDisable(!enabled);
        registerLink.setDisable(!enabled);
        forgotPasswordLink.setDisable(!enabled);
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