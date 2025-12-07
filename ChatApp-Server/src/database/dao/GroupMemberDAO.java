package database.dao;

import models.GroupMember;
import database.connection.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GroupMemberDAO {

    private Connection connection;

    public GroupMemberDAO() {
        this.connection = DBConnection.getConnection();
    }

    // ==================== CREATE ====================

    public boolean addMember(GroupMember member) {
        return addMember(member.getGroupId(), member.getUserId(), member.getRole());
    }

    public boolean addMember(String groupId, String userId, String role) {
        String sql = "INSERT INTO group_members (group_id, user_id, role, joined_at, last_seen, is_muted, is_banned) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, userId);
            stmt.setString(3, role);
            stmt.setTimestamp(4, new Timestamp(new Date().getTime()));
            stmt.setTimestamp(5, new Timestamp(new Date().getTime()));
            stmt.setBoolean(6, false);
            stmt.setBoolean(7, false);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Check if member already exists
            if (e.getMessage().contains("Duplicate")) {
                return updateRole(groupId, userId, role);
            }
            e.printStackTrace();
        }
        return false;
    }

    public boolean addMembers(String groupId, List<String> userIds, String role) {
        String sql = "INSERT INTO group_members (group_id, user_id, role, joined_at, last_seen) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);

            Timestamp now = new Timestamp(new Date().getTime());
            for (String userId : userIds) {
                stmt.setString(1, groupId);
                stmt.setString(2, userId);
                stmt.setString(3, role);
                stmt.setTimestamp(4, now);
                stmt.setTimestamp(5, now);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

            // Check if all inserts were successful
            for (int result : results) {
                if (result <= 0) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // ==================== READ ====================

    public GroupMember getMember(String groupId, String userId) {
        String sql = "SELECT * FROM group_members WHERE group_id = ? AND user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractGroupMemberFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<GroupMember> getGroupMembers(String groupId) {
        String sql = "SELECT * FROM group_members WHERE group_id = ? AND is_banned = false ORDER BY role DESC, joined_at";

        List<GroupMember> members = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                members.add(extractGroupMemberFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public List<GroupMember> getGroupAdmins(String groupId) {
        String sql = "SELECT * FROM group_members WHERE group_id = ? AND role = 'admin' AND is_banned = false";

        List<GroupMember> admins = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                admins.add(extractGroupMemberFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return admins;
    }

    public List<GroupMember> getUserGroups(String userId) {
        String sql = "SELECT * FROM group_members WHERE user_id = ? AND is_banned = false";

        List<GroupMember> groups = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                groups.add(extractGroupMemberFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }

    public List<String> getMemberIds(String groupId) {
        String sql = "SELECT user_id FROM group_members WHERE group_id = ? AND is_banned = false";

        List<String> memberIds = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                memberIds.add(rs.getString("user_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return memberIds;
    }

    public int countMembers(String groupId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND is_banned = false";

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

    public boolean isMember(String groupId, String userId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ? AND is_banned = false";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isAdmin(String groupId, String userId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ? AND role = 'admin' AND is_banned = false";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, userId);
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

    public boolean updateRole(String groupId, String userId, String newRole) {
        String sql = "UPDATE group_members SET role = ? WHERE group_id = ? AND user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newRole);
            stmt.setString(2, groupId);
            stmt.setString(3, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateLastSeen(String groupId, String userId) {
        String sql = "UPDATE group_members SET last_seen = ? WHERE group_id = ? AND user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
            stmt.setString(2, groupId);
            stmt.setString(3, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean muteMember(String groupId, String userId, boolean muted) {
        String sql = "UPDATE group_members SET is_muted = ? WHERE group_id = ? AND user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, muted);
            stmt.setString(2, groupId);
            stmt.setString(3, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean banMember(String groupId, String userId, boolean banned) {
        String sql = "UPDATE group_members SET is_banned = ? WHERE group_id = ? AND user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, banned);
            stmt.setString(2, groupId);
            stmt.setString(3, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== DELETE ====================

    public boolean removeMember(String groupId, String userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeAllMembers(String groupId) {
        String sql = "DELETE FROM group_members WHERE group_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== UTILITY ====================

    public int countAdmins(String groupId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND role = 'admin' AND is_banned = false";

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

    public boolean hasPermission(String groupId, String userId, String requiredRole) {
        String sql = "SELECT role FROM group_members WHERE group_id = ? AND user_id = ? AND is_banned = false";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String userRole = rs.getString("role");

                // Check role hierarchy: admin > moderator > member
                if ("admin".equals(requiredRole)) {
                    return "admin".equals(userRole);
                } else if ("moderator".equals(requiredRole)) {
                    return "admin".equals(userRole) || "moderator".equals(userRole);
                } else if ("member".equals(requiredRole)) {
                    return true; // All roles have at least member permission
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private GroupMember extractGroupMemberFromResultSet(ResultSet rs) throws SQLException {
        GroupMember member = new GroupMember();
        member.setGroupId(rs.getString("group_id"));
        member.setUserId(rs.getString("user_id"));
        member.setRole(rs.getString("role"));
        member.setJoinedAt(rs.getTimestamp("joined_at"));
        member.setLastSeen(rs.getTimestamp("last_seen"));
        member.setMuted(rs.getBoolean("is_muted"));
        member.setBanned(rs.getBoolean("is_banned"));
        return member;
    }
}