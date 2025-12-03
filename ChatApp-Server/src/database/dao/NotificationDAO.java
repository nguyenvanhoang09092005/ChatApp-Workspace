package database.dao;

import database.connection.DBConnection;
import models.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Notification table
 * Handles all database operations related to notifications
 */
public class NotificationDAO {

    // ==================== CREATE ====================

    /**
     * Create new notification
     */
    public static boolean createNotification(Notification notification) {
        String sql = "INSERT INTO notifications (notification_id, user_id, type, title, " +
                "content, sender_id, sender_name, sender_avatar, related_id, action_url, " +
                "is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, notification.getNotificationId());
            ps.setString(2, notification.getUserId());
            ps.setString(3, notification.getType());
            ps.setString(4, notification.getTitle());
            ps.setString(5, notification.getContent());
            ps.setString(6, notification.getSenderId());
            ps.setString(7, notification.getSenderName());
            ps.setString(8, notification.getSenderAvatar());
            ps.setString(9, notification.getRelatedId());
            ps.setString(10, notification.getActionUrl());
            ps.setBoolean(11, notification.isRead());
            ps.setTimestamp(12, Timestamp.valueOf(notification.getCreatedAt()));

            int result = ps.executeUpdate();
            System.out.println("✅ Notification created: " + notification.getNotificationId() +
                    " for user " + notification.getUserId() + " (rows affected: " + result + ")");
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error creating notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create message notification
     */
    public static boolean createMessageNotification(String userId, String senderId,
                                                    String senderName, String messageContent, String conversationId) {
        Notification notification = Notification.createMessageNotification(
                userId, senderId, senderName, messageContent, conversationId);
        return createNotification(notification);
    }

    /**
     * Create friend request notification
     */
    public static boolean createFriendRequestNotification(String userId, String senderId,
                                                          String senderName) {
        Notification notification = Notification.createFriendRequestNotification(
                userId, senderId, senderName);
        return createNotification(notification);
    }

    /**
     * Create friend accept notification
     */
    public static boolean createFriendAcceptNotification(String userId, String senderId,
                                                         String senderName) {
        Notification notification = Notification.createFriendAcceptNotification(
                userId, senderId, senderName);
        return createNotification(notification);
    }

    /**
     * Create group invite notification
     */
    public static boolean createGroupInviteNotification(String userId, String senderId,
                                                        String senderName, String groupName, String groupId) {
        Notification notification = Notification.createGroupInviteNotification(
                userId, senderId, senderName, groupName, groupId);
        return createNotification(notification);
    }

    // ==================== READ ====================

    /**
     * Find notification by ID
     */
    public static Notification findById(String notificationId) {
        String sql = "SELECT * FROM notifications WHERE notification_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, notificationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToNotification(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding notification: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get all notifications for user
     */
    public static List<Notification> getUserNotifications(String userId, int limit) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? " +
                "ORDER BY created_at DESC LIMIT ?";
        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting user notifications: " + e.getMessage());
        }

        return notifications;
    }

    /**
     * Get unread notifications for user
     */
    public static List<Notification> getUnreadNotifications(String userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE " +
                "ORDER BY created_at DESC";
        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting unread notifications: " + e.getMessage());
        }

        return notifications;
    }

    /**
     * Get notifications by type
     */
    public static List<Notification> getNotificationsByType(String userId, String type, int limit) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND type = ? " +
                "ORDER BY created_at DESC LIMIT ?";
        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, type);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting notifications by type: " + e.getMessage());
        }

        return notifications;
    }

    /**
     * Get notifications with pagination
     */
    public static List<Notification> getNotificationsPaginated(String userId, int offset, int limit) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? " +
                "ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting paginated notifications: " + e.getMessage());
        }

        return notifications;
    }

    /**
     * Get recent notifications (last 24 hours)
     */
    public static List<Notification> getRecentNotifications(String userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? " +
                "AND created_at > ? ORDER BY created_at DESC";
        List<Notification> notifications = new ArrayList<>();
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(yesterday));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting recent notifications: " + e.getMessage());
        }

        return notifications;
    }

    // ==================== UPDATE ====================

    /**
     * Mark notification as read
     */
    public static boolean markAsRead(String notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE, read_at = ? " +
                "WHERE notification_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, notificationId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking notification as read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark notification as unread
     */
    public static boolean markAsUnread(String notificationId) {
        String sql = "UPDATE notifications SET is_read = FALSE, read_at = NULL " +
                "WHERE notification_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, notificationId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking notification as unread: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark all notifications as read for user
     */
    public static boolean markAllAsRead(String userId) {
        String sql = "UPDATE notifications SET is_read = TRUE, read_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking all as read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark notifications by type as read
     */
    public static boolean markTypeAsRead(String userId, String type) {
        String sql = "UPDATE notifications SET is_read = TRUE, read_at = ? " +
                "WHERE user_id = ? AND type = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, userId);
            ps.setString(3, type);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking type as read: " + e.getMessage());
            return false;
        }
    }

    // ==================== DELETE ====================

    /**
     * Delete notification
     */
    public static boolean deleteNotification(String notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, notificationId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting notification: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete all notifications for user
     */
    public static boolean deleteAllNotifications(String userId) {
        String sql = "DELETE FROM notifications WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting all notifications: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete read notifications older than specified days
     */
    public static boolean deleteOldNotifications(int daysOld) {
        String sql = "DELETE FROM notifications WHERE is_read = TRUE " +
                "AND created_at < ?";
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(cutoffDate));
            int deleted = ps.executeUpdate();
            System.out.println("✅ Deleted " + deleted + " old notifications");
            return deleted > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting old notifications: " + e.getMessage());
            return false;
        }
    }

    // ==================== STATISTICS ====================

    /**
     * Get unread notification count
     */
    public static int getUnreadCount(String userId) {
        String sql = "SELECT COUNT(*) as count FROM notifications " +
                "WHERE user_id = ? AND is_read = FALSE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting unread count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get total notification count for user
     */
    public static int getTotalCount(String userId) {
        String sql = "SELECT COUNT(*) as count FROM notifications WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting total count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get unread count by type
     */
    public static int getUnreadCountByType(String userId, String type) {
        String sql = "SELECT COUNT(*) as count FROM notifications " +
                "WHERE user_id = ? AND type = ? AND is_read = FALSE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, type);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting unread count by type: " + e.getMessage());
        }

        return 0;
    }

    // ==================== MAPPING ====================

    /**
     * Map ResultSet to Notification object
     */
    private static Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();

        notification.setNotificationId(rs.getString("notification_id"));
        notification.setUserId(rs.getString("user_id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setContent(rs.getString("content"));
        notification.setSenderId(rs.getString("sender_id"));
        notification.setSenderName(rs.getString("sender_name"));
        notification.setSenderAvatar(rs.getString("sender_avatar"));
        notification.setRelatedId(rs.getString("related_id"));
        notification.setActionUrl(rs.getString("action_url"));
        notification.setRead(rs.getBoolean("is_read"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            notification.setReadAt(readAt.toLocalDateTime());
        }

        return notification;
    }
}