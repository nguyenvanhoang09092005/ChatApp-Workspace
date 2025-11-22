
package org.example.chatappclient.client.services;

import javafx.application.Platform;
import org.example.chatappclient.client.utils.ui.AlertUtil;

public class NotificationService {

    private static volatile NotificationService instance;
    private boolean notificationsEnabled = true;

    private NotificationService() {}

    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) {
                    instance = new NotificationService();
                }
            }
        }
        return instance;
    }

    /**
     * Show message notification
     */
    public void showMessageNotification(String senderName, String message) {
        if (!notificationsEnabled) return;

        Platform.runLater(() -> {
            AlertUtil.showToastInfo(senderName + ": " + message);
        });
    }

    /**
     * Show system notification
     */
    public void showSystemNotification(String title, String content) {
        if (!notificationsEnabled) return;

        Platform.runLater(() -> {
            AlertUtil.showToastInfo(title + "\n" + content);
        });
    }

    /**
     * Show call notification
     */
    public void showCallNotification(String callerName, String callType) {
        Platform.runLater(() -> {
            String type = "audio".equals(callType) ? "Cuộc gọi thoại" : "Cuộc gọi video";
            AlertUtil.showToastInfo(callerName + " - " + type);
        });
    }

    /**
     * Enable/disable notifications
     */
    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }
}
