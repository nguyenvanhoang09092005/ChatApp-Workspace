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

            // Kích thước cửa sổ = 2/3 màn hình
            double width = screenBounds.getWidth() * 0.66;
            double height = screenBounds.getHeight() * 0.66;

            // Tạo scene
            Scene scene = new Scene(root, width, height);

            // Add CSS
            scene.getStylesheets().add(getClass().getResource("/css/auth/login.css").toExternalForm());

            primaryStage.setTitle(Constants.APP_NAME);
            primaryStage.setScene(scene);

            primaryStage.setMinWidth(Constants.WINDOW_MIN_WIDTH);
            primaryStage.setMinHeight(Constants.WINDOW_MIN_HEIGHT);

            primaryStage.show();

            // Căn giữa màn hình
            primaryStage.setX((screenBounds.getWidth() - primaryStage.getWidth()) / 2);
            primaryStage.setY((screenBounds.getHeight() - primaryStage.getHeight()) / 2);

            primaryStage.setOnCloseRequest(event -> handleClose());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start application: " + e.getMessage());
        }
    }

    private void handleClose() {
        try {
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
