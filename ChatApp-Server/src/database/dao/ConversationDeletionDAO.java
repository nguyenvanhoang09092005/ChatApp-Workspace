package database.dao;

import database.connection.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * ✅ ENHANCED: Proper conversation deletion management
 * - Each user has their own deletion timestamp
 * - When user deletes: Store current timestamp
 * - When user sends message: Update timestamp to message time (auto-restore)
 */
public class ConversationDeletionDAO {

    /**
     * Mark conversation as deleted for specific user
     * Stores the CURRENT timestamp as deletion point
     */
    public static boolean markConversationAsDeletedForUser(String conversationId, String userId) {
        String sql = "INSERT INTO conversation_deletions (conversation_id, user_id, deleted_at) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE deleted_at = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, conversationId);
            ps.setString(2, userId);
            ps.setTimestamp(3, now);
            ps.setTimestamp(4, now);

            int result = ps.executeUpdate();
            System.out.println("✅ User " + userId + " deleted conversation at: " + now);
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking conversation as deleted: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NEW: Update deletion timestamp to current time
     * Called when user sends a new message after deleting
     * This makes the new message visible immediately
     */
    public static boolean updateDeletionTimestampToNow(String conversationId, String userId) {
        // First check if user has deletion record
        if (!isConversationDeletedByUser(conversationId, userId)) {
            return true; // No deletion record, nothing to update
        }

        String sql = "UPDATE conversation_deletions " +
                "SET deleted_at = ? " +
                "WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(1, now);
            ps.setString(2, conversationId);
            ps.setString(3, userId);

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ Updated deletion timestamp for user " + userId + " to: " + now);
            }
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating deletion timestamp: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if user has deleted this conversation
     */
    public static boolean isConversationDeletedByUser(String conversationId, String userId) {
        String sql = "SELECT COUNT(*) FROM conversation_deletions " +
                "WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setString(2, userId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error checking conversation deletion: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get the timestamp when user deleted this conversation
     * Returns null if never deleted
     */
    public static LocalDateTime getDeletionTimestamp(String conversationId, String userId) {
        String sql = "SELECT deleted_at FROM conversation_deletions " +
                "WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setString(2, userId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("deleted_at");
                return timestamp != null ? timestamp.toLocalDateTime() : null;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting deletion timestamp: " + e.getMessage());
        }

        return null;
    }

    /**
     * Alias for getDeletionTimestamp (for compatibility)
     */
    public static LocalDateTime getDeletionTime(String conversationId, String userId) {
        return getDeletionTimestamp(conversationId, userId);
    }

    /**
     * Completely restore conversation for user
     * Removes deletion record - user will see ALL messages again
     */
    public static boolean restoreConversationForUser(String conversationId, String userId) {
        String sql = "DELETE FROM conversation_deletions " +
                "WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            ps.setString(2, userId);

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ Fully restored conversation for user: " + userId);
                System.out.println("   ConversationID: " + conversationId);
            }
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error restoring conversation: " + e.getMessage());
            return false;
        }
    }
    public static boolean clearDeletionsForConversation(String conversationId) {
        String sql = "DELETE FROM conversation_deletions WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, conversationId);
            int result = ps.executeUpdate();
            System.out.println("✅ Cleared all deletion records for conversation");
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error clearing deletions: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ DEBUG: Get all deletion records for troubleshooting
     */
    public static void printAllDeletions() {
        String sql = "SELECT * FROM conversation_deletions ORDER BY deleted_at DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n📋 === ALL DELETION RECORDS ===");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("  " + count + ". ConvID: " + rs.getString("conversation_id"));
                System.out.println("     UserID: " + rs.getString("user_id"));
                System.out.println("     DeletedAt: " + rs.getTimestamp("deleted_at"));
                System.out.println();
            }
            System.out.println("Total: " + count + " records");
            System.out.println("================================\n");

        } catch (SQLException e) {
            System.err.println("❌ Error printing deletions: " + e.getMessage());
        }
    }
}