package database.dao;

import database.connection.DBConnection;
import models.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User table
 * Handles all database operations related to users
 */
public class UserDAO {

    // ==================== CREATE ====================

    /**
     * Create new user in database
     */
    public static boolean createUser(User user) {
        String sql = "INSERT INTO users (user_id, username, email, phone, password_hash, " +
                "salt, display_name, avatar_url, status_message, is_online, is_verified, is_active, " +
                "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUserId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getSalt());
            ps.setString(7, user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            ps.setString(8, user.getAvatarUrl());
            ps.setString(9, user.getStatusMessage());
            ps.setBoolean(10, false); // is_online default false
            ps.setBoolean(11, user.isVerified());
            ps.setBoolean(12, user.isActive());
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));

            int result = ps.executeUpdate();
            System.out.println("✅ User created: " + user.getUsername() + " (rows affected: " + result + ")");
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error creating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== READ ====================

    /**
     * Find user by username
     */
    public static User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding user by username: " + e.getMessage());
        }

        return null;
    }

    /**
     * Find user by email
     */
    public static User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ? AND is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding user by email: " + e.getMessage());
        }

        return null;
    }

    /**
     * Find user by ID
     */
    public static User findById(String userId) {
        String sql = "SELECT * FROM users WHERE user_id = ? AND is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding user by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Search users by username or display name
     */
    public static List<User> searchUsers(String keyword, int limit) {
        String sql = "SELECT * FROM users WHERE is_active = TRUE AND " +
                "(username LIKE ? OR display_name LIKE ?) LIMIT ?";
        List<User> users = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Get all online users
     */
    public static List<User> getAllOnlineUsers() {
        String sql = "SELECT * FROM users WHERE is_online = TRUE AND is_active = TRUE";
        List<User> users = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting online users: " + e.getMessage());
        }

        return users;
    }

    // ==================== UPDATE ====================

    /**
     * Update user profile
     */
    public static boolean updateProfile(String userId, String displayName,
                                        String avatarUrl, String statusMessage, String phone) {
        String sql = "UPDATE users SET display_name = ?, avatar_url = ?, " +
                "status_message = ?, phone = ?, updated_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, displayName);
            ps.setString(2, avatarUrl);
            ps.setString(3, statusMessage);
            ps.setString(4, phone);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(6, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update password
     */
    public static boolean updatePassword(String userId, String newPasswordHash, String newSalt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ?, updated_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPasswordHash);
            ps.setString(2, newSalt);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update online status
     */
    public static boolean updateOnlineStatus(String userId, boolean isOnline) {
        String sql = "UPDATE users SET is_online = ?, last_seen = ?, updated_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isOnline);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating online status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verify user account
     */
    public static boolean verifyUser(String userId) {
        String sql = "UPDATE users SET is_verified = TRUE, updated_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error verifying user: " + e.getMessage());
            return false;
        }
    }

    // ==================== DELETE ====================

    /**
     * Soft delete user (set is_active = false)
     */
    public static boolean deleteUser(String userId) {
        String sql = "UPDATE users SET is_active = FALSE, updated_at = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting user: " + e.getMessage());
            return false;
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Check if username exists
     */
    public static boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error checking username: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if email exists
     */
    public static boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error checking email: " + e.getMessage());
        }

        return false;
    }

    // ==================== MAPPING ====================

    /**
     * Map ResultSet to User object
     */
    private static User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setUserId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setDisplayName(rs.getString("display_name"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setStatusMessage(rs.getString("status_message"));
        user.setOnline(rs.getBoolean("is_online"));

        Timestamp lastSeen = rs.getTimestamp("last_seen");
        if (lastSeen != null) {
            user.setLastSeen(lastSeen.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        user.setActive(rs.getBoolean("is_active"));
        user.setVerified(rs.getBoolean("is_verified"));

        return user;
    }

    // ==================== UTILITIES ====================

    /**
     * Get user count
     */
    public static int getUserCount() {
        String sql = "SELECT COUNT(*) as count FROM users WHERE is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting user count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get user without sensitive data (for sending to clients)
     */
    public static User getUserPublicInfo(String userId) {
        User user = findById(userId);
        if (user != null) {
            // Clear sensitive data
            user.setPasswordHash(null);
            user.setSalt(null);
        }
        return user;
    }
}