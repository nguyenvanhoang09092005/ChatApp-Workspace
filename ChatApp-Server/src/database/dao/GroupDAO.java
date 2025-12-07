package database.dao;

import models.Group;
import database.connection.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GroupDAO {

    private Connection connection;

    public GroupDAO() {
        this.connection = DBConnection.getConnection();
    }

    // ==================== CREATE ====================

    public boolean createGroup(Group group) {
        String sql = "INSERT INTO groups (group_id, group_name, group_avatar, description, creator_id, created_at, updated_at, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group.getGroupId());
            stmt.setString(2, group.getGroupName());
            stmt.setString(3, group.getGroupAvatar());
            stmt.setString(4, group.getDescription());
            stmt.setString(5, group.getCreatorId());
            stmt.setTimestamp(6, new Timestamp(group.getCreatedAt().getTime()));
            stmt.setTimestamp(7, new Timestamp(group.getUpdatedAt().getTime()));
            stmt.setBoolean(8, group.isActive());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== READ ====================

    public Group getGroupById(String groupId) {
        String sql = "SELECT * FROM groups WHERE group_id = ? AND is_active = true";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractGroupFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Group> getGroupsByUser(String userId) {
        String sql = "SELECT g.* FROM groups g " +
                "JOIN group_members gm ON g.group_id = gm.group_id " +
                "WHERE gm.user_id = ? AND g.is_active = true " +
                "ORDER BY g.updated_at DESC";

        List<Group> groups = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                groups.add(extractGroupFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }

    public List<Group> getGroupsByCreator(String creatorId) {
        String sql = "SELECT * FROM groups WHERE creator_id = ? AND is_active = true ORDER BY created_at DESC";

        List<Group> groups = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, creatorId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                groups.add(extractGroupFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }

    public List<Group> searchGroups(String keyword, int limit) {
        String sql = "SELECT * FROM groups WHERE is_active = true AND " +
                "(group_name LIKE ? OR description LIKE ?) " +
                "ORDER BY group_name LIMIT ?";

        List<Group> groups = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setInt(3, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                groups.add(extractGroupFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }

    public List<Group> getAllActiveGroups() {
        String sql = "SELECT * FROM groups WHERE is_active = true ORDER BY created_at DESC";

        List<Group> groups = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                groups.add(extractGroupFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }

    public boolean existsByName(String groupName) {
        String sql = "SELECT COUNT(*) FROM groups WHERE group_name = ? AND is_active = true";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== UPDATE ====================

    public boolean updateGroup(Group group) {
        String sql = "UPDATE groups SET group_name = ?, group_avatar = ?, description = ?, " +
                "updated_at = ?, is_active = ? WHERE group_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group.getGroupName());
            stmt.setString(2, group.getGroupAvatar());
            stmt.setString(3, group.getDescription());
            stmt.setTimestamp(4, new Timestamp(new Date().getTime()));
            stmt.setBoolean(5, group.isActive());
            stmt.setString(6, group.getGroupId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateGroupAvatar(String groupId, String avatarUrl) {
        String sql = "UPDATE groups SET group_avatar = ?, updated_at = ? WHERE group_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, avatarUrl);
            stmt.setTimestamp(2, new Timestamp(new Date().getTime()));
            stmt.setString(3, groupId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateGroupDescription(String groupId, String description) {
        String sql = "UPDATE groups SET description = ?, updated_at = ? WHERE group_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, description);
            stmt.setTimestamp(2, new Timestamp(new Date().getTime()));
            stmt.setString(3, groupId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== DELETE ====================

    public boolean deleteGroup(String groupId) {
        String sql = "UPDATE groups SET is_active = false, updated_at = ? WHERE group_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
            stmt.setString(2, groupId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hardDeleteGroup(String groupId) {
        String sql = "DELETE FROM groups WHERE group_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== UTILITY ====================

    public int countGroups() {
        String sql = "SELECT COUNT(*) FROM groups WHERE is_active = true";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countGroupsByUser(String userId) {
        String sql = "SELECT COUNT(DISTINCT g.group_id) FROM groups g " +
                "JOIN group_members gm ON g.group_id = gm.group_id " +
                "WHERE gm.user_id = ? AND g.is_active = true";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isGroupActive(String groupId) {
        String sql = "SELECT is_active FROM groups WHERE group_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("is_active");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private Group extractGroupFromResultSet(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setGroupId(rs.getString("group_id"));
        group.setGroupName(rs.getString("group_name"));
        group.setGroupAvatar(rs.getString("group_avatar"));
        group.setDescription(rs.getString("description"));
        group.setCreatorId(rs.getString("creator_id"));
        group.setCreatedAt(rs.getTimestamp("created_at"));
        group.setUpdatedAt(rs.getTimestamp("updated_at"));
        group.setActive(rs.getBoolean("is_active"));
        return group;
    }
}