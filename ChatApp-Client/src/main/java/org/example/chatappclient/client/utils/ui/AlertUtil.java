package org.example.chatappclient.client.utils.ui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.Optional;

public class AlertUtil {

    private static Stage primaryStage;

    /**
     * Set primary stage for positioning notifications
     */
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Show error alert (non-blocking with callback)
     */
    public static void showError(String title, String message) {
        showError(title, message, null);
    }

    public static void showError(String title, String message, Runnable onClose) {
        runOnFxThread(() -> {
            Alert alert = createStyledAlert(Alert.AlertType.ERROR, title, message);
            alert.show(); // Non-blocking
            if (onClose != null) {
                alert.setOnHidden(e -> onClose.run());
            }
        });
    }

    /**
     * Show warning alert (non-blocking)
     */
    public static void showWarning(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = createStyledAlert(Alert.AlertType.WARNING, title, message);
            alert.show(); // Non-blocking
        });
    }

    /**
     * Show info alert (non-blocking)
     */
    public static void showInfo(String title, String message) {
        showInfo(title, message, null);
    }

    public static void showInfo(String title, String message, Runnable onClose) {
        runOnFxThread(() -> {
            Alert alert = createStyledAlert(Alert.AlertType.INFORMATION, title, message);
            alert.show(); // Non-blocking
            if (onClose != null) {
                alert.setOnHidden(e -> onClose.run());
            }
        });
    }

    /**
     * Show success alert (non-blocking)
     */
    public static void showSuccess(String title, String message) {
        showSuccess(title, message, null);
    }

    public static void showSuccess(String title, String message, Runnable onClose) {
        runOnFxThread(() -> {
            Alert alert = createStyledAlert(Alert.AlertType.INFORMATION, title, message);
            alert.getDialogPane().getStyleClass().add("success-alert");
            alert.show(); // Non-blocking
            if (onClose != null) {
                alert.setOnHidden(e -> onClose.run());
            }
        });
    }

    /**
     * Show confirmation dialog
     */
    public static boolean showConfirmation(String title, String message) {
        final boolean[] result = {false};
        if (Platform.isFxApplicationThread()) {
            Alert alert = createStyledAlert(Alert.AlertType.CONFIRMATION, title, message);
            Optional<ButtonType> btnResult = alert.showAndWait();
            return btnResult.isPresent() && btnResult.get() == ButtonType.OK;
        } else {
            Platform.runLater(() -> {
                Alert alert = createStyledAlert(Alert.AlertType.CONFIRMATION, title, message);
                Optional<ButtonType> btnResult = alert.showAndWait();
                result[0] = btnResult.isPresent() && btnResult.get() == ButtonType.OK;
            });
            return result[0];
        }
    }

    /**
     * Show custom error with details
     */
    public static void showErrorWithDetails(String title, String message, String details) {
        runOnFxThread(() -> {
            Alert alert = createStyledAlert(Alert.AlertType.ERROR, title, message);
            alert.setHeaderText(message);
            alert.setContentText(details);
            alert.showAndWait();
        });
    }

    // ==================== TOAST NOTIFICATIONS (Non-blocking) ====================

    /**
     * Show toast notification - Success (non-blocking, auto-dismiss)
     */
    public static void showToastSuccess(String message) {
        showToast(message, ToastType.SUCCESS);
    }

    /**
     * Show toast notification - Error (non-blocking, auto-dismiss)
     */
    public static void showToastError(String message) {
        showToast(message, ToastType.ERROR);
    }

    /**
     * Show toast notification - Warning (non-blocking, auto-dismiss)
     */
    public static void showToastWarning(String message) {
        showToast(message, ToastType.WARNING);
    }

    /**
     * Show toast notification - Info (non-blocking, auto-dismiss)
     */
    public static void showToastInfo(String message) {
        showToast(message, ToastType.INFO);
    }

    // ==================== HELPER METHODS ====================

    private enum ToastType {
        SUCCESS("#4CAF50", "M9,20.42L2.79,14.21L5.62,11.38L9,14.77L18.88,4.88L21.71,7.71L9,20.42Z"),
        ERROR("#F44336", "M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"),
        WARNING("#FF9800", "M13,13H11V7H13M13,17H11V15H13M12,2L1,21H23M12,6L19.53,19H4.47M11,8V14H13V8H11M11,16V18H13V16H11Z"),
        INFO("#2196F3", "M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z");

        final String color;
        final String iconPath;

        ToastType(String color, String iconPath) {
            this.color = color;
            this.iconPath = iconPath;
        }
    }

    private static void showToast(String message, ToastType type) {
        runOnFxThread(() -> {
            Popup popup = new Popup();

            // Create icon
            SVGPath icon = new SVGPath();
            icon.setContent(type.iconPath);
            icon.setFill(Color.WHITE);
            icon.setScaleX(1.2);
            icon.setScaleY(1.2);

            // Create message label
            Label messageLabel = new Label(message);
            messageLabel.setStyle(
                    "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: 500;"
            );

            // Create container
            HBox contentBox = new HBox(12, icon, messageLabel);
            contentBox.setAlignment(Pos.CENTER_LEFT);
            contentBox.setStyle(
                    "-fx-background-color: " + type.color + "; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-padding: 16px 24px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);"
            );

            StackPane root = new StackPane(contentBox);
            popup.getContent().add(root);

            // Position popup
            if (primaryStage != null) {
                double x = primaryStage.getX() + primaryStage.getWidth() - 350;
                double y = primaryStage.getY() + 80;
                popup.show(primaryStage, x, y);
            } else {
                popup.show(new Stage());
            }

            // Slide in animation
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), root);
            slideIn.setFromX(400);
            slideIn.setToX(0);

            // Fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(3));
            fadeOut.setOnFinished(e -> popup.hide());

            slideIn.play();
            fadeOut.play();
        });
    }

    private static Alert createStyledAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Apply custom CSS
        alert.getDialogPane().setStyle(
                "-fx-background-color: #ffffff; " +
                        "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                        "-fx-font-size: 14px;"
        );

        // Style buttons
        alert.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8px 20px; " +
                        "-fx-background-radius: 4px; " +
                        "-fx-cursor: hand;"
        );

        if (type == Alert.AlertType.CONFIRMATION) {
            alert.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                    "-fx-background-color: #f5f5f5; " +
                            "-fx-text-fill: #333; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 8px 20px; " +
                            "-fx-background-radius: 4px; " +
                            "-fx-cursor: hand;"
            );
        }

        return alert;
    }

    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}