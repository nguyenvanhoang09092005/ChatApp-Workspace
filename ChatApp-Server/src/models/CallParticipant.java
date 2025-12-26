
package models;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CallParticipant {
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private int id;
    private String callId;
    private String userId;

    private String role; // 'caller', 'receiver'
    private String action; // 'answered', 'rejected', 'missed', 'left', 'initiated', 'pending'

    // Thời gian
    private Timestamp joinedAt;
    private Timestamp leftAt;

    // Metadata
    private Timestamp createdAt;

    // Constructors
    public CallParticipant() {}

    public CallParticipant(String callId, String userId, String role, String action) {
        this.callId = callId;
        this.userId = userId;
        this.role = role;
        this.action = action;
        this.createdAt = getCurrentVietnamTimestamp();
    }

    // ==================== VIETNAM TIMEZONE UTILITIES ====================

    public static Timestamp getCurrentVietnamTimestamp() {
        ZonedDateTime vietnamTime = ZonedDateTime.now(VIETNAM_ZONE);
        return Timestamp.from(vietnamTime.toInstant());
    }

    public static ZonedDateTime toVietnamZonedDateTime(Timestamp timestamp) {
        if (timestamp == null) return null;
        return ZonedDateTime.ofInstant(timestamp.toInstant(), VIETNAM_ZONE);
    }

    // ==================== GETTERS AND SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Timestamp getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Timestamp leftAt) {
        this.leftAt = leftAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Set thời gian tham gia theo giờ Việt Nam
     */
    public void setJoinedNow() {
        this.joinedAt = getCurrentVietnamTimestamp();
    }

    /**
     * Set thời gian rời đi theo giờ Việt Nam
     */
    public void setLeftNow() {
        this.leftAt = getCurrentVietnamTimestamp();
    }

    /**
     * Lấy thời gian tham gia theo giờ Việt Nam
     */
    public ZonedDateTime getJoinedAtVietnam() {
        return toVietnamZonedDateTime(joinedAt);
    }

    /**
     * Lấy thời gian rời đi theo giờ Việt Nam
     */
    public ZonedDateTime getLeftAtVietnam() {
        return toVietnamZonedDateTime(leftAt);
    }

    /**
     * Tính thời gian tham gia cuộc gọi (giây)
     */
    public int getParticipationDuration() {
        if (joinedAt == null) return 0;

        Timestamp endTime = leftAt != null ? leftAt : getCurrentVietnamTimestamp();
        long diff = endTime.getTime() - joinedAt.getTime();
        return (int) (diff / 1000);
    }

    @Override
    public String toString() {
        return "CallParticipant{" +
                "id=" + id +
                ", callId='" + callId + '\'' +
                ", userId='" + userId + '\'' +
                ", role='" + role + '\'' +
                ", action='" + action + '\'' +
                ", joinedAt=" + joinedAt +
                ", leftAt=" + leftAt +
                ", createdAt=" + createdAt +
                '}';
    }
}