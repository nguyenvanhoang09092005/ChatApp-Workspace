package database.dao;

import database.connection.DBConnection;
import models.Conversation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Conversation table
 * ✅ Hỗ trợ xóa conversation riêng cho từng user
 */
public class ConversationDAO {

    // ==================== CREATE ====================

    public static boolean createConversation(Conversation conversation) {
        String sql = "INSERT INTO conversations (conversation_id, type, name, avatar_url, " +
                "description, member_ids, creator_id, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversation.getConversationId());
            ps.setString(2, conversation.getType());
            ps.setString(3, conversation.getName());
            ps.setString(4, conversation.getAvatarUrl());
            ps.setString(5, conversation.getDescription());
            ps.setString(6, conversation.getMemberIdsAsString());
            ps.setString(7, conversation.getCreatorId());
            ps.setBoolean(8, conversation.isActive());
            ps.setTimestamp(9, Timestamp.valueOf(conversation.getCreatedAt()));
            ps.setTimestamp(10, Timestamp.valueOf(conversation.getUpdatedAt()));

            int result = ps.executeUpdate();
            System.out.println("✅ Conversation created: " + conversation.getConversationId());
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error creating conversation: " + e.getMessage());
            return false;
        }
    }

    public static Conversation createPrivateConversation(String userId1, String userId2) {
        Conversation existing = findPrivateConversation(userId1, userId2);
        if (existing != null) {
            return existing;
        }

        Conversation conversation = new Conversation(Conversation.TYPE_PRIVATE, null, userId1);
        conversation.addMember(userId1);
        conversation.addMember(userId2);

        if (createConversation(conversation)) {
            return conversation;
        }

        return null;
    }

    public static Conversation createGroupConversation(String name,
                                                       String creatorId,
                                                       List<String> memberIds) {
        Conversation conversation = new Conversation(Conversation.TYPE_GROUP, name, creatorId);
        conversation.addMember(creatorId);

        if (memberIds != null) {
            for (String id : memberIds) {
                conversation.addMember(id);
            }
        }

        if (createConversation(conversation)) {
            return conversation;
        }

        return null;
    }

    // ==================== READ ====================

    public static Conversation findById(String conversationId) {
        String sql = "SELECT * FROM conversations " +
                "WHERE conversation_id = ? AND is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToConversation(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding conversation: " + e.getMessage());
        }

        return null;
    }

    public static Conversation getConversationById(String conversationId) {
        return findById(conversationId);
    }

    public static Conversation findPrivateConversation(String userId1, String userId2) {
        String sql = "SELECT * FROM conversations " +
                "WHERE type = ? AND is_active = TRUE " +
                "AND FIND_IN_SET(?, member_ids) > 0 " +
                "AND FIND_IN_SET(?, member_ids) > 0";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, Conversation.TYPE_PRIVATE);
            ps.setString(2, userId1);
            ps.setString(3, userId2);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Conversation conv = mapResultSetToConversation(rs);

                if (conv.getMemberCount() == 2 &&
                        conv.hasMember(userId1) &&
                        conv.hasMember(userId2)) {
                    return conv;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding private conversation: " + e.getMessage());
        }

        return null;
    }

    /**
     * ✅ UPDATED: Lấy conversations của user, loại trừ những cái đã bị xóa
     */
    public static List<Conversation> getUserConversations(String userId) {
        String sql = "SELECT c.* FROM conversations c " +
                "WHERE c.is_active = TRUE " +
                "AND FIND_IN_SET(?, c.member_ids) > 0 " +
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM conversation_deletions cd " +
                "  WHERE cd.conversation_id = c.conversation_id " +
                "  AND cd.user_id = ?" +
                ") " +
                "ORDER BY c.updated_at DESC";

        List<Conversation> conversations = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                conversations.add(mapResultSetToConversation(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting user conversations: " + e.getMessage());
        }

        return conversations;
    }

    /**
     * ✅ UPDATED: Lấy group conversations, loại trừ những cái đã bị xóa
     */
    public static List<Conversation> getUserGroupConversations(String userId) {
        String sql = "SELECT c.* FROM conversations c " +
                "WHERE c.type = ? AND c.is_active = TRUE " +
                "AND FIND_IN_SET(?, c.member_ids) > 0 " +
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM conversation_deletions cd " +
                "  WHERE cd.conversation_id = c.conversation_id " +
                "  AND cd.user_id = ?" +
                ") " +
                "ORDER BY c.updated_at DESC";

        List<Conversation> conversations = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, Conversation.TYPE_GROUP);
            ps.setString(2, userId);
            ps.setString(3, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                conversations.add(mapResultSetToConversation(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting group conversations: " + e.getMessage());
        }

        return conversations;
    }

    /**
     * ✅ UPDATED: Search conversations, loại trừ những cái đã bị xóa
     */
    public static List<Conversation> searchConversations(String userId, String keyword) {
        String sql = "SELECT c.* FROM conversations c " +
                "WHERE c.is_active = TRUE " +
                "AND FIND_IN_SET(?, c.member_ids) > 0 " +
                "AND c.name LIKE ? " +
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM conversation_deletions cd " +
                "  WHERE cd.conversation_id = c.conversation_id " +
                "  AND cd.user_id = ?" +
                ") " +
                "ORDER BY c.updated_at DESC";

        List<Conversation> conversations = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                conversations.add(mapResultSetToConversation(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching conversations: " + e.getMessage());
        }

        return conversations;
    }

    // ==================== UPDATE ====================

    public static boolean updateConversation(String conversationId,
                                             String name,
                                             String avatarUrl,
                                             String description) {
        String sql = "UPDATE conversations SET name = ?, avatar_url = ?, " +
                "description = ?, updated_at = ? " +
                "WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, avatarUrl);
            ps.setString(3, description);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, conversationId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating conversation: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateLastMessage(String conversationId,
                                            String message,
                                            LocalDateTime timestamp) {
        String sql = "UPDATE conversations SET last_message = ?, " +
                "last_message_time = ?, updated_at = ? " +
                "WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, message);
            ps.setTimestamp(2, Timestamp.valueOf(timestamp));
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, conversationId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating last message: " + e.getMessage());
            return false;
        }
    }

    // ==================== MEMBERS ====================

    public static boolean addMember(String conversationId, String userId) {
        Conversation conv = findById(conversationId);

        if (conv == null || !conv.isGroup()) return false;
        if (conv.hasMember(userId)) return true;

        conv.addMember(userId);
        return updateMembers(conversationId, conv.getMemberIdsAsString());
    }

    public static boolean removeMember(String conversationId, String userId) {
        Conversation conv = findById(conversationId);

        if (conv == null || !conv.isGroup()) return false;
        if (!conv.hasMember(userId)) return true;

        conv.removeMember(userId);
        return updateMembers(conversationId, conv.getMemberIdsAsString());
    }

    private static boolean updateMembers(String conversationId, String memberIds) {
        String sql = "UPDATE conversations SET member_ids = ?, updated_at = ? " +
                "WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memberIds);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, conversationId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating members: " + e.getMessage());
            return false;
        }
    }

    // ==================== DELETE ====================

    /**
     * ✅ CHANGED: Soft delete for all users (admin action)
     */
    public static boolean deleteConversation(String conversationId) {
        String sql = "UPDATE conversations SET is_active = FALSE, updated_at = ? " +
                "WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, conversationId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting conversation: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NEW: Xóa conversation chỉ cho một user cụ thể
     */
    public static boolean deleteConversationForUser(String conversationId, String userId) {
        return ConversationDeletionDAO.markConversationAsDeletedForUser(conversationId, userId);
    }

    public static boolean hardDeleteConversation(String conversationId) {
        String sql = "DELETE FROM conversations WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error hard deleting conversation: " + e.getMessage());
            return false;
        }
    }

    // ==================== STATISTICS ====================

    /**
     * ✅ UPDATED: Đếm conversations, loại trừ những cái đã bị xóa
     */
    public static int getUserConversationCount(String userId) {
        String sql = "SELECT COUNT(*) FROM conversations c " +
                "WHERE c.is_active = TRUE " +
                "AND FIND_IN_SET(?, c.member_ids) > 0 " +
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM conversation_deletions cd " +
                "  WHERE cd.conversation_id = c.conversation_id " +
                "  AND cd.user_id = ?" +
                ")";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, userId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("❌ Error getting conversation count: " + e.getMessage());
        }

        return 0;
    }

    // ==================== MAPPING ====================

    private static Conversation mapResultSetToConversation(ResultSet rs)
            throws SQLException {
        Conversation conversation = new Conversation();

        conversation.setConversationId(rs.getString("conversation_id"));
        conversation.setType(rs.getString("type"));
        conversation.setName(rs.getString("name"));
        conversation.setAvatarUrl(rs.getString("avatar_url"));
        conversation.setDescription(rs.getString("description"));
        conversation.setMemberIdsFromString(rs.getString("member_ids"));
        conversation.setCreatorId(rs.getString("creator_id"));
        conversation.setActive(rs.getBoolean("is_active"));
        conversation.setLastMessage(rs.getString("last_message"));

        Timestamp lastMessageTime = rs.getTimestamp("last_message_time");
        if (lastMessageTime != null) {
            conversation.setLastMessageTime(lastMessageTime.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            conversation.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            conversation.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return conversation;
    }
}