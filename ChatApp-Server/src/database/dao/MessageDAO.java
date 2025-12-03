package database.dao;

import database.connection.DBConnection;
import models.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Message table
 * Handles all database operations related to messages
 */
public class MessageDAO {

    // ==================== CREATE ====================

    /**
     * Create new message
     */
    public static boolean createMessage(Message message) {
        String sql = "INSERT INTO messages (message_id, conversation_id, sender_id, " +
                "sender_name, sender_avatar, content, message_type, media_url, file_name, " +
                "file_size, thumbnail_url, media_duration, timestamp, is_read, is_delivered, " +
                "is_edited, is_recalled, reply_to_message_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, message.getMessageId());
            ps.setString(2, message.getConversationId());
            ps.setString(3, message.getSenderId());
            ps.setString(4, message.getSenderName());
            ps.setString(5, message.getSenderAvatar());
            ps.setString(6, message.getContent());
            ps.setString(7, message.getMessageType());
            ps.setString(8, message.getMediaUrl());
            ps.setString(9, message.getFileName());
            ps.setLong(10, message.getFileSize());
            ps.setString(11, message.getThumbnailUrl());
            ps.setInt(12, message.getMediaDuration());
            ps.setTimestamp(13, Timestamp.valueOf(message.getTimestamp()));
            ps.setBoolean(14, message.isRead());
            ps.setBoolean(15, message.isDelivered());
            ps.setBoolean(16, message.isEdited());
            ps.setBoolean(17, message.isRecalled());
            ps.setString(18, message.getReplyToMessageId());

            int result = ps.executeUpdate();
            System.out.println("✅ Message created: " + message.getMessageId() +
                    " (rows affected: " + result + ")");

            // Update conversation last message
            ConversationDAO.updateLastMessage(message.getConversationId(),
                    message.getDisplayContent(), message.getTimestamp());

            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error creating message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== READ ====================

    /**
     * Find message by ID
     */
    public static Message findById(String messageId) {
        String sql = "SELECT * FROM messages WHERE message_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, messageId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToMessage(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding message: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get messages for conversation
     */
    public static List<Message> getConversationMessages(String conversationId, int limit) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? " +
                "ORDER BY timestamp DESC LIMIT ?";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting conversation messages: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Get messages with pagination
     */
    public static List<Message> getMessagesPaginated(String conversationId, int offset, int limit) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? " +
                "ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting paginated messages: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Get messages after timestamp
     */
    public static List<Message> getMessagesAfter(String conversationId, LocalDateTime after) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? AND timestamp > ? " +
                "ORDER BY timestamp ASC";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setTimestamp(2, Timestamp.valueOf(after));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting messages after timestamp: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Get unread messages for user in conversation
     */
    public static List<Message> getUnreadMessages(String conversationId, String userId) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? " +
                "AND sender_id != ? AND is_read = FALSE ORDER BY timestamp ASC";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting unread messages: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Search messages in conversation
     */
    public static List<Message> searchMessages(String conversationId, String keyword, int limit) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? " +
                "AND content LIKE ? AND is_recalled = FALSE " +
                "ORDER BY timestamp DESC LIMIT ?";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setString(2, "%" + keyword + "%");
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching messages: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Get media messages (images, videos, files)
     */
    public static List<Message> getMediaMessages(String conversationId, String messageType) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? " +
                "AND message_type = ? AND is_recalled = FALSE " +
                "ORDER BY timestamp DESC";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setString(2, messageType);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting media messages: " + e.getMessage());
        }

        return messages;
    }

    // ==================== UPDATE ====================

    /**
     * Mark message as delivered
     */
    public static boolean markAsDelivered(String messageId) {
        String sql = "UPDATE messages SET is_delivered = TRUE WHERE message_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, messageId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking as delivered: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark message as read
     */
    public static boolean markAsRead(String messageId) {
        String sql = "UPDATE messages SET is_read = TRUE, is_delivered = TRUE " +
                "WHERE message_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, messageId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking as read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark all conversation messages as read for user
     */
    public static boolean markAllAsRead(String conversationId, String userId) {
        String sql = "UPDATE messages SET is_read = TRUE, is_delivered = TRUE " +
                "WHERE conversation_id = ? AND sender_id != ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking all as read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Edit message content
     */
    public static boolean editMessage(String messageId, String newContent) {
        String sql = "UPDATE messages SET content = ?, is_edited = TRUE " +
                "WHERE message_id = ? AND is_recalled = FALSE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newContent);
            ps.setString(2, messageId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error editing message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recall message
     */
    public static boolean recallMessage(String messageId) {
        String sql = "UPDATE messages SET is_recalled = TRUE, " +
                "content = 'Tin nhắn đã được thu hồi' WHERE message_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, messageId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error recalling message: " + e.getMessage());
            return false;
        }
    }

    // ==================== DELETE ====================

    /**
     * Delete message
     */
    public static boolean deleteMessage(String messageId) {
        String sql = "DELETE FROM messages WHERE message_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, messageId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete all messages in conversation
     */
    public static boolean deleteConversationMessages(String conversationId) {
        String sql = "DELETE FROM messages WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting conversation messages: " + e.getMessage());
            return false;
        }
    }

    // ==================== STATISTICS ====================

    /**
     * Get message count for conversation
     */
    public static int getMessageCount(String conversationId) {
        String sql = "SELECT COUNT(*) as count FROM messages WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting message count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get unread message count for user in conversation
     */
    public static int getUnreadCount(String conversationId, String userId) {
        String sql = "SELECT COUNT(*) as count FROM messages " +
                "WHERE conversation_id = ? AND sender_id != ? AND is_read = FALSE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setString(2, userId);
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
     * Get last message in conversation
     */
    public static Message getLastMessage(String conversationId) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? " +
                "ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToMessage(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting last message: " + e.getMessage());
        }

        return null;
    }

    // ==================== MAPPING ====================

    /**
     * Map ResultSet to Message object
     */
    private static Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();

        message.setMessageId(rs.getString("message_id"));
        message.setConversationId(rs.getString("conversation_id"));
        message.setSenderId(rs.getString("sender_id"));
        message.setSenderName(rs.getString("sender_name"));
        message.setSenderAvatar(rs.getString("sender_avatar"));
        message.setContent(rs.getString("content"));
        message.setMessageType(rs.getString("message_type"));
        message.setMediaUrl(rs.getString("media_url"));
        message.setFileName(rs.getString("file_name"));
        message.setFileSize(rs.getLong("file_size"));
        message.setThumbnailUrl(rs.getString("thumbnail_url"));
        message.setMediaDuration(rs.getInt("media_duration"));
        message.setRead(rs.getBoolean("is_read"));
        message.setDelivered(rs.getBoolean("is_delivered"));
        message.setEdited(rs.getBoolean("is_edited"));
        message.setRecalled(rs.getBoolean("is_recalled"));
        message.setReplyToMessageId(rs.getString("reply_to_message_id"));

        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            message.setTimestamp(timestamp.toLocalDateTime());
        }

        return message;
    }
}