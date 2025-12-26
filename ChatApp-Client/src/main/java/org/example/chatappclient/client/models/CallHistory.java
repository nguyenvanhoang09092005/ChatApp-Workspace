package org.example.chatappclient.client.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model lưu trữ thông tin cuộc gọi cho client
 */
public class CallHistory {
    private String callId;
    private String conversationId;
    private String callType; // "audio" hoặc "video"
    private String status; // "completed", "missed", "rejected", "cancelled"

    private String callerId;
    private String callerName;
    private String partnerName; // Tên người còn lại trong cuộc gọi
    private String partnerAvatar;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration; // giây

    private boolean isIncoming; // cuộc gọi đến hay đi

    // Constructors
    public CallHistory() {}

    public CallHistory(String callId, String conversationId, String callType,
                       String callerId, String callerName, boolean isIncoming) {
        this.callId = callId;
        this.conversationId = conversationId;
        this.callType = callType;
        this.callerId = callerId;
        this.callerName = callerName;
        this.isIncoming = isIncoming;
        this.status = "ongoing";
        this.startTime = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getPartnerAvatar() {
        return partnerAvatar;
    }

    public void setPartnerAvatar(String partnerAvatar) {
        this.partnerAvatar = partnerAvatar;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
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

    // Utility methods
    public String getFormattedStartTime() {
        if (startTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
        return startTime.format(formatter);
    }

    public String getFormattedDuration() {
        if (duration < 60) {
            return duration + " giây";
        } else if (duration < 3600) {
            int minutes = duration / 60;
            int seconds = duration % 60;
            return String.format("%d:%02d", minutes, seconds);
        } else {
            int hours = duration / 3600;
            int minutes = (duration % 3600) / 60;
            int seconds = duration % 60;
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    public String getCallTypeDisplay() {
        return "video".equals(callType) ? "Gọi video" : "Gọi thoại";
    }

    public String getStatusDisplay() {
        switch (status) {
            case "completed": return "Đã hoàn thành";
            case "missed": return "Nhỡ";
            case "rejected": return "Từ chối";
            case "cancelled": return "Đã hủy";
            default: return "Không xác định";
        }
    }

    public String getDirectionDisplay() {
        return isIncoming ? "Gọi đến" : "Gọi đi";
    }

    @Override
    public String toString() {
        return "CallHistory{" +
                "callId='" + callId + '\'' +
                ", callType='" + callType + '\'' +
                ", status='" + status + '\'' +
                ", partnerName='" + partnerName + '\'' +
                ", duration=" + duration +
                ", isIncoming=" + isIncoming +
                '}';
    }
}