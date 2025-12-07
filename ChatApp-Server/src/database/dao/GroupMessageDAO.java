package database.dao;

import models.GroupMessage;
import database.connection.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GroupMessageDAO {

    private Connection connection;

    public GroupMessageDAO() {
        this.connection = DBConnection.getConnection();
    }

    // ==================== CREATE ====================

    public boolean saveMessage(GroupMessage message) {
        String sql = "INSERT INTO group_messages (message_id, group_id, sender_id, content, message_type, attachment_url, " +
                "is_edited, is_deleted, sent_at, edited_at, metadata) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, message.getMessageId());
            stmt.setString(2, message.getGroupId());
            stmt.setString(3, message.getSenderId());
            stmt.setString(4, message.getContent());
            stmt.setString(5, message.getMessageType());
            stmt.setString(6, message.getAttachmentUrl());
            stmt.setBoolean(7, message.isEdited());
            stmt.setBoolean(8, message.isDeleted());
            stmt.setTimestamp(9, new Timestamp(message.getSentAt().getTime()));
            stmt.setTimestamp(10, message.getEditedAt() != null ?
                    new Timestamp(message.getEditedAt().getTime()) : null);
            stmt.setString(11, message.getMetadata());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== READ ====================

    public GroupMessage getMessageById(String messageId) {
        String sql = "SELECT * FROM group_messages WHERE message_id = ? AND is_deleted = false";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractGroupMessageFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<GroupMessage> getMessages(String groupId, int limit, int offset) {
        String sql = "SELECT * FROM group_messages WHERE group_id = ? AND is_deleted = false " +
                "ORDER BY sent_at DESC LIMIT ? OFFSET ?";

        List<GroupMessage> messages = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(extractGroupMessageFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<GroupMessage> getMessagesBySender(String groupId, String senderId, int limit) {
        String sql = "SELECT * FROM group_messages WHERE group_id = ? AND sender_id = ? AND is_deleted = false " +
                "ORDER BY sent_at DESC LIMIT ?";

        List<GroupMessage> messages = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, senderId);
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(extractGroupMessageFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<GroupMessage> getMessagesAfterDate(String groupId, Date afterDate, int limit) {
        String sql = "SELECT * FROM group_messages WHERE group_id = ? AND sent_at > ? AND is_deleted = false " +
                "ORDER BY sent_at ASC LIMIT ?";

        List<GroupMessage> messages = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setTimestamp(2, new Timestamp(afterDate.getTime()));
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(extractGroupMessageFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<GroupMessage> getMessagesBeforeDate(String groupId, Date beforeDate, int limit) {
        String sql = "SELECT * FROM group_messages WHERE group_id = ? AND sent_at < ? AND is_deleted = false " +
                "ORDER BY sent_at DESC LIMIT ?";

        List<GroupMessage> messages = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setTimestamp(2, new Timestamp(beforeDate.getTime()));
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(extractGroupMessageFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<GroupMessage> searchMessages(String groupId, String keyword, int limit) {
        String sql = "SELECT * FROM group_messages WHERE group_id = ? AND content LIKE ? AND is_deleted = false " +
                "ORDER BY sent_at DESC LIMIT ?";

        List<GroupMessage> messages = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, "%" + keyword + "%");
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(extractGroupMessageFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public int countMessages(String groupId) {
        String sql = "SELECT COUNT(*) FROM group_messages WHERE group_id = ? AND is_deleted = false";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Date getLastMessageTime(String groupId) {
        String sql = "SELECT MAX(sent_at) as last_message FROM group_messages WHERE group_id = ? AND is_deleted = false";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("last_message");
                return timestamp != null ? new Date(timestamp.getTime()) : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==================== UPDATE ====================

    public boolean updateMessageContent(String messageId, String newContent) {
        String sql = "UPDATE group_messages SET content = ?, is_edited = true, edited_at = ? WHERE message_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newContent);
            stmt.setTimestamp(2, new Timestamp(new Date().getTime()));
            stmt.setString(3, messageId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAsEdited(String messageId) {
        String sql = "UPDATE group_messages SET is_edited = true, edited_at = ? WHERE message_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
            stmt.setString(2, messageId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAsDeleted(String messageId) {
        String sql = "UPDATE group_messages SET is_deleted = true WHERE message_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, messageId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== DELETE ====================

    public boolean deleteMessage(String messageId) {
        String sql = "DELETE FROM group_messages WHERE message_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, messageId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteMessagesByGroup(String groupId) {
        String sql = "DELETE FROM group_messages WHERE group_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteMessagesBySender(String groupId, String senderId) {
        String sql = "DELETE FROM group_messages WHERE group_id = ? AND sender_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, senderId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== UTILITY ====================

    public boolean canEditMessage(String messageId, String userId) {
        String sql = "SELECT sender_id, sent_at FROM group_messages WHERE message_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String senderId = rs.getString("sender_id");
                Timestamp sentAt = rs.getTimestamp("sent_at");

                // Only sender can edit, and within 15 minutes
                if (senderId.equals(userId)) {
                    long messageTime = sentAt.getTime();
                    long currentTime = System.currentTimeMillis();
                    long fifteenMinutes = 15 * 60 * 1000;

                    return (currentTime - messageTime) <= fifteenMinutes;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean canDeleteMessage(String messageId, String userId) {
        String sql = "SELECT sender_id FROM group_messages WHERE message_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String senderId = rs.getString("sender_id");

                // Only sender can delete their own messages
                return senderId.equals(userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private GroupMessage extractGroupMessageFromResultSet(ResultSet rs) throws SQLException {
        GroupMessage message = new GroupMessage();
        message.setMessageId(rs.getString("message_id"));
        message.setGroupId(rs.getString("group_id"));
        message.setSenderId(rs.getString("sender_id"));
        message.setContent(rs.getString("content"));
        message.setMessageType(rs.getString("message_type"));
        message.setAttachmentUrl(rs.getString("attachment_url"));
        message.setEdited(rs.getBoolean("is_edited"));
        message.setDeleted(rs.getBoolean("is_deleted"));
        message.setSentAt(rs.getTimestamp("sent_at"));

        Timestamp editedAt = rs.getTimestamp("edited_at");
        if (!rs.wasNull()) {
            message.setEditedAt(editedAt);
        }

        message.setMetadata(rs.getString("metadata"));
        return message;
    }
}