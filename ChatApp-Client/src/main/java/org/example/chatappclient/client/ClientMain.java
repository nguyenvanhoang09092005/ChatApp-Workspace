package org.example.chatappclient.client;


import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
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
            // Load config
            AppConfig config = AppConfig.getInstance();

            // Load login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/login.fxml"));
            Parent root = loader.load();

            // Lấy kích thước màn hình
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            double width = screenBounds.getWidth() * 0.5;
            double height = screenBounds.getHeight() * 0.55;
            Scene scene = new Scene(root, width, height);


            // Add CSS
            scene.getStylesheets().add(getClass().getResource("/css/auth/login.css").toExternalForm());

            // Setup stage
            primaryStage.setTitle(Constants.APP_NAME);
            primaryStage.setScene(scene);

            // Minimum size
            primaryStage.setMinWidth(Constants.WINDOW_MIN_WIDTH);
            primaryStage.setMinHeight(Constants.WINDOW_MIN_HEIGHT);

            primaryStage.show();     // Quan trọng: phải show trước

            // Căn giữa cửa sổ trên màn hình
            primaryStage.centerOnScreen();

            // Handle close
            primaryStage.setOnCloseRequest(event -> handleClose());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start application: " + e.getMessage());
        }
    }

    private void handleClose() {
        try {
            // Disconnect
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

