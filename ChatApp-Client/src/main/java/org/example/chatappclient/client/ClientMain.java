package org.example.chatappclient.client;


import org.example.chatappclient.client.config.AppConfig;
import org.example.chatappclient.client.config.Constants;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load configuration
            AppConfig config = AppConfig.getInstance();

            // Load login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/login.fxml"));
            Parent root = loader.load();

            // Create scene
            Scene scene = new Scene(root, 900, 600);

            // Add CSS
            scene.getStylesheets().add(getClass().getResource("/css/auth/login.css").toExternalForm());

            // Setup stage
            primaryStage.setTitle(Constants.APP_NAME);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(Constants.WINDOW_MIN_WIDTH);
            primaryStage.setMinHeight(Constants.WINDOW_MIN_HEIGHT);

            // Show stage
            primaryStage.show();

            // Handle close request
            primaryStage.setOnCloseRequest(event -> {
                handleClose();
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start application: " + e.getMessage());
        }
    }

    /**
     * Handle application close
     */
    private void handleClose() {
        try {
            // Disconnect from server
            SocketClient client = SocketClient.getInstance();
            if (client.isConnected()) {
                client.disconnect();
            }

            System.out.println("Application closed");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        handleClose();
    }

    public static void main(String[] args) {
        launch(args);
    }
}