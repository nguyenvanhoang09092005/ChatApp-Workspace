package server.handlers;

import database.dao.NotificationDAO;
import models.Notification;
import protocol.Protocol;
import server.ClientHandler;

import java.util.List;

/**
 * Handler for notification operations
 */
public class NotificationHandler {

    private final ClientHandler clientHandler;

    public NotificationHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void handle(String command, String[] parts) {
        switch (command) {
            case Protocol.NOTIFICATION_GET_ALL:
                handleGetAllNotifications(parts);
                break;
            case Protocol.NOTIFICATION_MARK_READ:
                handleMarkRead(parts);
                break;
            case Protocol.NOTIFICATION_DELETE:
                handleDeleteNotification(parts);
                break;
            case Protocol.NOTIFICATION_CLEAR_ALL:
                handleClearAll(parts);
                break;
            default:
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Unknown notification command"
                ));
        }
    }

    // ==================== GET NOTIFICATIONS ====================

    private void handleGetAllNotifications(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        List<Notification> notifications = NotificationDAO.getUserNotifications(userId, 1000);

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < notifications.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);

            Notification notif = notifications.get(i);
            data.append(notif.getNotificationId())
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.getType())
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.getTitle())
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.getContent())
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.getSenderId() != null ? notif.getSenderId() : "")
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.getSenderName() != null ? notif.getSenderName() : "")
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.getSenderAvatar() != null ? notif.getSenderAvatar() : "")
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.getRelatedId() != null ? notif.getRelatedId() : "")
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.isRead())
                    .append(Protocol.LIST_DELIMITER)
                    .append(notif.getCreatedAt().toString());
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Notifications retrieved",
                data.toString()
        ));
    }

    // ==================== MARK READ ====================

    private void handleMarkRead(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String notificationId = parts[1];

        if (NotificationDAO.markAsRead(notificationId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Notification marked as read"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to mark notification as read"
            ));
        }
    }

    // ==================== DELETE ====================

    private void handleDeleteNotification(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String notificationId = parts[1];

        if (NotificationDAO.deleteNotification(notificationId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Notification deleted"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to delete notification"
            ));
        }
    }

    // ==================== CLEAR ALL ====================

    private void handleClearAll(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];

        if (NotificationDAO.deleteAllNotifications(userId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "All notifications cleared"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to clear notifications"
            ));
        }
    }
}