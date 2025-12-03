package database.dao;

import database.connection.DBConnection;
import models.Contact;
import models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Contact table
 * Handles all database operations related to contacts
 */
public class ContactDAO {

    // ==================== CREATE ====================

    /**
     * Create new contact relationship
     */
    public static boolean createContact(Contact contact) {
        String sql = "INSERT INTO contacts (contact_id, user_id, contact_user_id, status, " +
                "nickname, is_favorite, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, contact.getContactId());
            ps.setString(2, contact.getUserId());
            ps.setString(3, contact.getContactUserId());
            ps.setString(4, contact.getStatus());
            ps.setString(5, contact.getNickname());
            ps.setBoolean(6, contact.isFavorite());
            ps.setTimestamp(7, Timestamp.valueOf(contact.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.valueOf(contact.getUpdatedAt()));

            int result = ps.executeUpdate();
            System.out.println("✅ Contact created: " + contact.getUserId() +
                    " -> " + contact.getContactUserId() + " (rows affected: " + result + ")");
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error creating contact: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send friend request (creates bidirectional contact)
     */
    public static boolean sendFriendRequest(String senderId, String receiverId) {
        // Create contact from sender to receiver (pending)
        Contact senderContact = new Contact(senderId, receiverId, Contact.STATUS_PENDING);

        // Create contact from receiver to sender (pending)
        Contact receiverContact = new Contact(receiverId, senderId, Contact.STATUS_PENDING);

        return createContact(senderContact) && createContact(receiverContact);
    }

    // ==================== READ ====================

    /**
     * Get all contacts for a user by status
     */
    public static List<Contact> getContactsByStatus(String userId, String status) {
        String sql = "SELECT * FROM contacts WHERE user_id = ? AND status = ? " +
                "ORDER BY updated_at DESC";
        List<Contact> contacts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, status);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                contacts.add(mapResultSetToContact(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting contacts by status: " + e.getMessage());
        }

        return contacts;
    }

    /**
     * Get all accepted contacts (friends)
     */
    public static List<Contact> getFriends(String userId) {
        return getContactsByStatus(userId, Contact.STATUS_ACCEPTED);
    }

    /**
     * Get pending friend requests
     */
    public static List<Contact> getPendingRequests(String userId) {
        return getContactsByStatus(userId, Contact.STATUS_PENDING);
    }

    /**
     * Get blocked contacts
     */
    public static List<Contact> getBlockedContacts(String userId) {
        return getContactsByStatus(userId, Contact.STATUS_BLOCKED);
    }

    /**
     * Get favorite contacts
     */
    public static List<Contact> getFavoriteContacts(String userId) {
        String sql = "SELECT * FROM contacts WHERE user_id = ? AND is_favorite = TRUE " +
                "AND status = ? ORDER BY updated_at DESC";
        List<Contact> contacts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, Contact.STATUS_ACCEPTED);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                contacts.add(mapResultSetToContact(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting favorite contacts: " + e.getMessage());
        }

        return contacts;
    }

    /**
     * Find specific contact relationship
     */
    public static Contact findContact(String userId, String contactUserId) {
        String sql = "SELECT * FROM contacts WHERE user_id = ? AND contact_user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, contactUserId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToContact(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding contact: " + e.getMessage());
        }

        return null;
    }

    /**
     * Check if contact relationship exists
     */
    public static boolean contactExists(String userId, String contactUserId) {
        return findContact(userId, contactUserId) != null;
    }

    /**
     * Check if users are friends
     */
    public static boolean areFriends(String userId1, String userId2) {
        Contact contact = findContact(userId1, userId2);
        return contact != null && contact.isAccepted();
    }

    /**
     * Search contacts by name
     */
    public static List<Contact> searchContacts(String userId, String keyword) {
        String sql = "SELECT c.* FROM contacts c " +
                "JOIN users u ON c.contact_user_id = u.user_id " +
                "WHERE c.user_id = ? AND c.status = ? AND " +
                "(u.username LIKE ? OR u.display_name LIKE ? OR c.nickname LIKE ?) " +
                "ORDER BY c.is_favorite DESC, c.updated_at DESC";
        List<Contact> contacts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, userId);
            ps.setString(2, Contact.STATUS_ACCEPTED);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, pattern);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                contacts.add(mapResultSetToContact(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching contacts: " + e.getMessage());
        }

        return contacts;
    }

    // ==================== UPDATE ====================

    /**
     * Update contact status
     */
    public static boolean updateContactStatus(String userId, String contactUserId, String status) {
        String sql = "UPDATE contacts SET status = ?, updated_at = ? " +
                "WHERE user_id = ? AND contact_user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, userId);
            ps.setString(4, contactUserId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating contact status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Accept friend request (bidirectional)
     */
    public static boolean acceptFriendRequest(String userId, String contactUserId) {
        return updateContactStatus(userId, contactUserId, Contact.STATUS_ACCEPTED) &&
                updateContactStatus(contactUserId, userId, Contact.STATUS_ACCEPTED);
    }

    /**
     * Block contact (bidirectional)
     */
    public static boolean blockContact(String userId, String contactUserId) {
        return updateContactStatus(userId, contactUserId, Contact.STATUS_BLOCKED) &&
                updateContactStatus(contactUserId, userId, Contact.STATUS_BLOCKED);
    }

    /**
     * Unblock contact
     */
    public static boolean unblockContact(String userId, String contactUserId) {
        return updateContactStatus(userId, contactUserId, Contact.STATUS_ACCEPTED) &&
                updateContactStatus(contactUserId, userId, Contact.STATUS_ACCEPTED);
    }

    /**
     * Update contact nickname
     */
    public static boolean updateNickname(String userId, String contactUserId, String nickname) {
        String sql = "UPDATE contacts SET nickname = ?, updated_at = ? " +
                "WHERE user_id = ? AND contact_user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nickname);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, userId);
            ps.setString(4, contactUserId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating nickname: " + e.getMessage());
            return false;
        }
    }

    /**
     * Toggle favorite status
     */
    public static boolean toggleFavorite(String userId, String contactUserId) {
        String sql = "UPDATE contacts SET is_favorite = NOT is_favorite, updated_at = ? " +
                "WHERE user_id = ? AND contact_user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, userId);
            ps.setString(3, contactUserId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error toggling favorite: " + e.getMessage());
            return false;
        }
    }

    // ==================== DELETE ====================

    /**
     * Delete contact relationship (unfriend)
     */
    public static boolean deleteContact(String userId, String contactUserId) {
        String sql = "DELETE FROM contacts WHERE user_id = ? AND contact_user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, contactUserId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting contact: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unfriend (bidirectional delete)
     */
    public static boolean unfriend(String userId, String contactUserId) {
        return deleteContact(userId, contactUserId) &&
                deleteContact(contactUserId, userId);
    }

    /**
     * Reject friend request
     */
    public static boolean rejectFriendRequest(String userId, String requesterId) {
        return unfriend(userId, requesterId);
    }

    // ==================== STATISTICS ====================

    /**
     * Get friend count
     */
    public static int getFriendCount(String userId) {
        String sql = "SELECT COUNT(*) as count FROM contacts " +
                "WHERE user_id = ? AND status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, Contact.STATUS_ACCEPTED);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting friend count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get pending request count
     */
    public static int getPendingRequestCount(String userId) {
        String sql = "SELECT COUNT(*) as count FROM contacts " +
                "WHERE user_id = ? AND status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, Contact.STATUS_PENDING);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting pending request count: " + e.getMessage());
        }

        return 0;
    }

    // ==================== MAPPING ====================

    /**
     * Map ResultSet to Contact object
     */
    private static Contact mapResultSetToContact(ResultSet rs) throws SQLException {
        Contact contact = new Contact();

        contact.setContactId(rs.getString("contact_id"));
        contact.setUserId(rs.getString("user_id"));
        contact.setContactUserId(rs.getString("contact_user_id"));
        contact.setStatus(rs.getString("status"));
        contact.setNickname(rs.getString("nickname"));
        contact.setFavorite(rs.getBoolean("is_favorite"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            contact.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            contact.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return contact;
    }

    // ==================== UTILITIES ====================

    /**
     * Get contacts with user details
     */
    public static List<Contact> getContactsWithUserDetails(String userId, String status) {
        List<Contact> contacts = getContactsByStatus(userId, status);

        for (Contact contact : contacts) {
            User user = UserDAO.getUserPublicInfo(contact.getContactUserId());
            // Store user details if needed
        }

        return contacts;
    }
}