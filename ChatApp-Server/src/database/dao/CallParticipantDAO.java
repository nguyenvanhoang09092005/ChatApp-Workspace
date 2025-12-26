package database.dao;

import database.connection.DBConnection;
import models.CallParticipant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý người tham gia cuộc gọi
 */
public class CallParticipantDAO {

    // ==================== CREATE ====================

    public static boolean addParticipant(CallParticipant participant) {
        String sql = "INSERT INTO call_participants (call_id, user_id, role, action) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, participant.getCallId());
            stmt.setString(2, participant.getUserId());
            stmt.setString(3, participant.getRole());
            stmt.setString(4, participant.getAction());

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    participant.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error adding participant: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== UPDATE ====================

    public static boolean updateParticipantAction(String callId, String userId, String action) {
        String sql = "UPDATE call_participants SET action = ? " +
                "WHERE call_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, action);
            stmt.setString(2, callId);
            stmt.setString(3, userId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error updating participant action: " + e.getMessage());
            return false;
        }
    }

    public static boolean setJoinedTime(String callId, String userId) {
        String sql = "UPDATE call_participants SET joined_at = ? " +
                "WHERE call_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, callId);
            stmt.setString(3, userId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error setting joined time: " + e.getMessage());
            return false;
        }
    }

    public static boolean setLeftTime(String callId, String userId) {
        String sql = "UPDATE call_participants SET left_at = ? " +
                "WHERE call_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, callId);
            stmt.setString(3, userId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error setting left time: " + e.getMessage());
            return false;
        }
    }

    // ==================== READ ====================

    public static List<CallParticipant> getParticipantsByCall(String callId) {
        List<CallParticipant> participants = new ArrayList<>();
        String sql = "SELECT * FROM call_participants WHERE call_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, callId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                participants.add(extractParticipant(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting participants by call: " + e.getMessage());
        }

        return participants;
    }

    public static CallParticipant getParticipant(String callId, String userId) {
        String sql = "SELECT * FROM call_participants WHERE call_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, callId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractParticipant(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error getting participant: " + e.getMessage());
        }

        return null;
    }

    // ==================== DELETE ====================

    public static boolean deleteParticipant(String callId, String userId) {
        String sql = "DELETE FROM call_participants WHERE call_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, callId);
            stmt.setString(2, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting participant: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteParticipantsByCall(String callId) {
        String sql = "DELETE FROM call_participants WHERE call_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, callId);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error deleting participants by call: " + e.getMessage());
            return false;
        }
    }

    // ==================== HELPER ====================

    private static CallParticipant extractParticipant(ResultSet rs) throws SQLException {
        CallParticipant participant = new CallParticipant();
        participant.setId(rs.getInt("id"));
        participant.setCallId(rs.getString("call_id"));
        participant.setUserId(rs.getString("user_id"));
        participant.setRole(rs.getString("role"));
        participant.setAction(rs.getString("action"));
        participant.setJoinedAt(rs.getTimestamp("joined_at"));
        participant.setLeftAt(rs.getTimestamp("left_at"));
        participant.setCreatedAt(rs.getTimestamp("created_at"));
        return participant;
    }
}