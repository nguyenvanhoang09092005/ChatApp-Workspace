
package models;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Model lưu trữ thông tin tổng quan về cuộc gọi
 * Sử dụng múi giờ Việt Nam (Asia/Ho_Chi_Minh)
 */
public class CallHistory {
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private String callId;
    private String conversationId;
    private String callType; // 'audio' hoặc 'video'
    private String status; // 'completed', 'missed', 'rejected', 'cancelled', 'failed'

    // Thông tin người khởi tạo
    private String callerId;
    private String callerName;

    // Thời gian
    private Timestamp startTime;
    private Timestamp endTime;
    private int duration; // giây

    // Trạng thái
    private boolean isIncoming;

    // Metadata
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public CallHistory() {}

    public CallHistory(String callId, String conversationId, String callType,
                       String callerId, String callerName) {
        this.callId = callId;
        this.conversationId = conversationId;
        this.callType = callType;
        this.callerId = callerId;
        this.callerName = callerName;
        this.status = "ongoing";
        this.startTime = getCurrentVietnamTimestamp();
        this.duration = 0;
        this.isIncoming = false;
    }

    // ==================== VIETNAM TIMEZONE UTILITIES ====================

    /**
     * Lấy timestamp hiện tại theo giờ Việt Nam
     */
    public static Timestamp getCurrentVietnamTimestamp() {
        ZonedDateTime vietnamTime = ZonedDateTime.now(VIETNAM_ZONE);
        return Timestamp.from(vietnamTime.toInstant());
    }

    /**
     * Chuyển đổi Instant sang Timestamp theo giờ Việt Nam
     */
    public static Timestamp toVietnamTimestamp(Instant instant) {
        if (instant == null) return null;
        return Timestamp.from(instant);
    }

    /**
     * Lấy ZonedDateTime từ Timestamp (theo giờ Việt Nam)
     */
    public static ZonedDateTime toVietnamZonedDateTime(Timestamp timestamp) {
        if (timestamp == null) return null;
        return ZonedDateTime.ofInstant(timestamp.toInstant(), VIETNAM_ZONE);
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public void setIncoming(boolean incoming) {
        isIncoming = incoming;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Tính toán thời lượng cuộc gọi
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            long diff = endTime.getTime() - startTime.getTime();
            this.duration = (int) (diff / 1000); // convert to seconds
        }
    }

    /**
     * Kết thúc cuộc gọi với thời gian Việt Nam
     */
    public void endCall(String status) {
        this.endTime = getCurrentVietnamTimestamp();
        this.status = status;
        calculateDuration();
    }

    /**
     * Lấy thời gian bắt đầu theo giờ Việt Nam
     */
    public ZonedDateTime getStartTimeVietnam() {
        return toVietnamZonedDateTime(startTime);
    }

    /**
     * Lấy thời gian kết thúc theo giờ Việt Nam
     */
    public ZonedDateTime getEndTimeVietnam() {
        return toVietnamZonedDateTime(endTime);
    }

    @Override
    public String toString() {
        return "CallHistory{" +
                "callId='" + callId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", callType='" + callType + '\'' +
                ", status='" + status + '\'' +
                ", callerId='" + callerId + '\'' +
                ", callerName='" + callerName + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + duration +
                ", isIncoming=" + isIncoming +
                '}';
    }
}
