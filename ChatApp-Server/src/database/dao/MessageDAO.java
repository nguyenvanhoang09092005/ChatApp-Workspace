package database.dao;

import database.connection.DBConnection;
import models.Conversation;
import models.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ FIXED: Proper deletion filtering per user
 * - User A deletes → Only A loses old messages
 * - User B keeps all messages
 * - When A sends new message → Conversation reappears for A with only new messages
 */
public class MessageDAO {

    // ==================== CREATE ====================

    /**
     * Create new message
     * ✅ Auto-restore conversation for user who had deleted it
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
            System.out.println("✅ Message created: " + message.getMessageId());

            Conversation conv = ConversationDAO.findById(message.getConversationId());
            if (conv != null) {
                for (String memberId : conv.getMemberIds()) {
                    if (!memberId.equals(message.getSenderId())) {
                        boolean wasDeleted = ConversationDeletionDAO.isConversationDeletedByUser(
                                message.getConversationId(), memberId);

                        if (wasDeleted) {
                            // Remove deletion record → conversation reappears for this user
                            ConversationDeletionDAO.restoreConversationForUser(
                                    message.getConversationId(), memberId);
                            System.out.println("✅ Auto-restored conversation for user: " + memberId);
                        }
                    }
                }
            }

            // Update sender's deletion timestamp (if they had deleted before)
            ConversationDeletionDAO.updateDeletionTimestampToNow(
                    message.getConversationId(),
                    message.getSenderId()
            );

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
     * ✅ CORRECT: Get messages for specific user with deletion filtering
     * - User who deleted: See only messages AFTER deletion timestamp
     * - User who didn't delete: See ALL messages
     */
    public static List<Message> getMessagesForUser(String conversationId, String userId) {
        LocalDateTime deletedAt = ConversationDeletionDAO.getDeletionTimestamp(conversationId, userId);

        // 🔍 DEBUG LOG
        System.out.println("\n🔍 === GET MESSAGES DEBUG ===");
        System.out.println("  ConversationID: " + conversationId);
        System.out.println("  UserID: " + userId);
        System.out.println("  DeletedAt: " + deletedAt);

        String sql;
        if (deletedAt != null) {
            // User deleted conversation - only show messages AFTER deletion
            sql = "SELECT * FROM messages " +
                    "WHERE conversation_id = ? " +
                    "AND timestamp > ? " +
                    "ORDER BY timestamp ASC";
            System.out.println("  ⚠️ User deleted conversation - filtering messages after: " + deletedAt);
        } else {
            // User never deleted - show all messages
            sql = "SELECT * FROM messages " +
                    "WHERE conversation_id = ? " +
                    "ORDER BY timestamp ASC";
            System.out.println("  ✅ User never deleted - showing ALL messages");
        }

        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            if (deletedAt != null) {
                ps.setTimestamp(2, Timestamp.valueOf(deletedAt));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

            System.out.println("  📊 Retrieved: " + messages.size() + " messages");
            System.out.println("🔍 === END DEBUG ===\n");

        } catch (SQLException e) {
            System.err.println("❌ Error getting messages: " + e.getMessage());
            e.printStackTrace();
        }

        return messages;
    }

    /**
     * ✅ Get paginated messages with deletion filtering
     */
    public static List<Message> getMessagesPaginatedForUser(String conversationId,
                                                            int offset,
                                                            int limit,
                                                            String userId) {
        LocalDateTime deletedAt = ConversationDeletionDAO.getDeletionTimestamp(conversationId, userId);

        String sql;
        if (deletedAt != null) {
            sql = "SELECT * FROM messages " +
                    "WHERE conversation_id = ? " +
                    "AND timestamp > ? " +
                    "ORDER BY timestamp DESC " +
                    "LIMIT ? OFFSET ?";
        } else {
            sql = "SELECT * FROM messages " +
                    "WHERE conversation_id = ? " +
                    "ORDER BY timestamp DESC " +
                    "LIMIT ? OFFSET ?";
        }

        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);

            if (deletedAt != null) {
                ps.setTimestamp(2, Timestamp.valueOf(deletedAt));
                ps.setInt(3, limit);
                ps.setInt(4, offset);
            } else {
                ps.setInt(2, limit);
                ps.setInt(3, offset);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting paginated messages: " + e.getMessage());
            e.printStackTrace();
        }

        return messages;
    }

    /**
     * Get messages after specific timestamp
     */
    public static List<Message> getMessagesAfter(String conversationId, LocalDateTime after) {
        String sql = "SELECT * FROM messages " +
                "WHERE conversation_id = ? AND timestamp > ? " +
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
     * ✅ Search messages with deletion filtering
     */
    public static List<Message> searchMessages(String conversationId,
                                               String keyword,
                                               int limit,
                                               String userId) {
        LocalDateTime deletionTime = ConversationDeletionDAO.getDeletionTimestamp(conversationId, userId);

        String sql;
        if (deletionTime != null) {
            sql = "SELECT * FROM messages " +
                    "WHERE conversation_id = ? " +
                    "AND timestamp > ? " +
                    "AND content LIKE ? " +
                    "AND is_recalled = FALSE " +
                    "ORDER BY timestamp DESC LIMIT ?";
        } else {
            sql = "SELECT * FROM messages " +
                    "WHERE conversation_id = ? " +
                    "AND content LIKE ? " +
                    "AND is_recalled = FALSE " +
                    "ORDER BY timestamp DESC LIMIT ?";
        }

        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);

            if (deletionTime != null) {
                ps.setTimestamp(2, Timestamp.valueOf(deletionTime));
                ps.setString(3, "%" + keyword + "%");
                ps.setInt(4, limit);
            } else {
                ps.setString(2, "%" + keyword + "%");
                ps.setInt(3, limit);
            }

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
     * ✅ Get unread messages with deletion filtering
     */
    public static List<Message> getUnreadMessages(String conversationId, String userId) {
        LocalDateTime deletionTime = ConversationDeletionDAO.getDeletionTimestamp(conversationId, userId);

        String sql;
        if (deletionTime != null) {
            sql = "SELECT * FROM messages " +
                    "WHERE conversation_id = ? " +
                    "AND timestamp > ? " +
                    "AND sender_id != ? " +
                    "AND is_read = FALSE " +
                    "ORDER BY timestamp ASC";
        } else {
            sql = "SELECT * FROM messages " +
                    "WHERE conversation_id = ? " +
                    "AND sender_id != ? " +
                    "AND is_read = FALSE " +
                    "ORDER BY timestamp ASC";
        }

        List<Message> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);

            if (deletionTime != null) {
                ps.setTimestamp(2, Timestamp.valueOf(deletionTime));
                ps.setString(3, userId);
            } else {
                ps.setString(2, userId);
            }

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
     * Get media messages (images, files, etc.)
     */
    public static List<Message> getMediaMessages(String conversationId, String messageType) {
        String sql = "SELECT * FROM messages " +
                "WHERE conversation_id = ? " +
                "AND message_type = ? " +
                "AND is_recalled = FALSE " +
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
     * Recall message (thu hồi tin nhắn)
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
     * Delete message permanently
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
     * ✅ Get message count with deletion filtering
     */
    public static int getMessageCount(String conversationId, String userId) {
        LocalDateTime deletionTime = ConversationDeletionDAO.getDeletionTimestamp(conversationId, userId);

        String sql;
        if (deletionTime != null) {
            sql = "SELECT COUNT(*) as count FROM messages " +
                    "WHERE conversation_id = ? AND timestamp > ?";
        } else {
            sql = "SELECT COUNT(*) as count FROM messages " +
                    "WHERE conversation_id = ?";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);

            if (deletionTime != null) {
                ps.setTimestamp(2, Timestamp.valueOf(deletionTime));
            }

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