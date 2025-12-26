package database.dao;

import database.connection.DBConnection;
import models.CallHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý lịch sử cuộc gọi
 */
public class CallHistoryDAO {

    // ==================== CREATE ====================

    public static boolean createCallHistory(CallHistory call) {
        String sql = "INSERT INTO call_history (call_id, conversation_id, call_type, " +
                "status, caller_id, caller_name, start_time, is_incoming) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, call.getCallId());
            stmt.setString(2, call.getConversationId());
            stmt.setString(3, call.getCallType());
            stmt.setString(4, call.getStatus());
            stmt.setString(5, call.getCallerId());
            stmt.setString(6, call.getCallerName());
            stmt.setTimestamp(7, call.getStartTime());
            stmt.setBoolean(8, call.isIncoming());

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error creating call history: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== UPDATE ====================

    public static boolean updateCallStatus(String callId, String status,
                                           Timestamp endTime, int duration) {
        String sql = "UPDATE call_history SET status = ?, end_time = ?, " +
                "duration = ? WHERE call_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setTimestamp(2, endTime);
            stmt.setInt(3, duration);
            stmt.setString(4, callId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error updating call status: " + e.getMessage());
            return false;
        }
    }

    public static boolean endCall(String callId, String status) {
        CallHistory call = getCallById(callId);
        if (call == null) return false;

        call.endCall(status);
        return updateCallStatus(callId, status, call.getEndTime(), call.getDuration());
    }

    // ==================== READ ====================

    public static CallHistory getCallById(String callId) {
        String sql = "SELECT * FROM call_history WHERE call_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, callId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractCallHistory(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error getting call by ID: " + e.getMessage());
        }

        return null;
    }

    public static List<CallHistory> getCallsByConversation(String conversationId) {
        List<CallHistory> calls = new ArrayList<>();
        String sql = "SELECT * FROM call_history WHERE conversation_id = ? " +
                "ORDER BY start_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, conversationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                calls.add(extractCallHistory(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting calls by conversation: " + e.getMessage());
        }

        return calls;
    }

    public static List<CallHistory> getCallsByUser(String userId, int limit) {
        List<CallHistory> calls = new ArrayList<>();
        String sql = "SELECT ch.* FROM call_history ch " +
                "INNER JOIN call_participants cp ON ch.call_id = cp.call_id " +
                "WHERE cp.user_id = ? " +
                "ORDER BY ch.start_time DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                calls.add(extractCallHistory(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting calls by user: " + e.getMessage());
        }

        return calls;
    }

    public static List<CallHistory> getMissedCallsByUser(String userId) {
        List<CallHistory> calls = new ArrayList<>();
        String sql = "SELECT ch.* FROM call_history ch " +
                "INNER JOIN call_participants cp ON ch.call_id = cp.call_id " +
                "WHERE cp.user_id = ? AND cp.action = 'missed' " +
                "ORDER BY ch.start_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                calls.add(extractCallHistory(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting missed calls: " + e.getMessage());
        }

        return calls;
    }

    // ==================== DELETE ====================

    public static boolean deleteCall(String callId) {
        String sql = "DELETE FROM call_history WHERE call_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, callId);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting call: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteCallsByConversation(String conversationId) {
        String sql = "DELETE FROM call_history WHERE conversation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, conversationId);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error deleting calls by conversation: " + e.getMessage());
            return false;
        }
    }

    // ==================== STATISTICS ====================

    public static int getTotalCallsByUser(String userId) {
        String sql = "SELECT COUNT(*) FROM call_history ch " +
                "INNER JOIN call_participants cp ON ch.call_id = cp.call_id " +
                "WHERE cp.user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting total calls: " + e.getMessage());
        }

        return 0;
    }

    public static int getTotalDurationByUser(String userId) {
        String sql = "SELECT SUM(duration) FROM call_history ch " +
                "INNER JOIN call_participants cp ON ch.call_id = cp.call_id " +
                "WHERE cp.user_id = ? AND ch.status = 'completed'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting total duration: " + e.getMessage());
        }

        return 0;
    }

    // ==================== HELPER ====================

    private static CallHistory extractCallHistory(ResultSet rs) throws SQLException {
        CallHistory call = new CallHistory();
        call.setCallId(rs.getString("call_id"));
        call.setConversationId(rs.getString("conversation_id"));
        call.setCallType(rs.getString("call_type"));
        call.setStatus(rs.getString("status"));
        call.setCallerId(rs.getString("caller_id"));
        call.setCallerName(rs.getString("caller_name"));
        call.setStartTime(rs.getTimestamp("start_time"));
        call.setEndTime(rs.getTimestamp("end_time"));
        call.setDuration(rs.getInt("duration"));
        call.setIncoming(rs.getBoolean("is_incoming"));
        call.setCreatedAt(rs.getTimestamp("created_at"));
        call.setUpdatedAt(rs.getTimestamp("updated_at"));
        return call;
    }
}