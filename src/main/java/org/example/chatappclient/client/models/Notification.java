package org.example.chatappclient.client.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_FRIEND_REQUEST = "friend_request";
    public static final String TYPE_CALL = "call";
    public static final String TYPE_SYSTEM = "system";

    private String notificationId;
    private String userId;
    private String type;
    private String title;
    private String content;
    private String referenceId; // ID of message, user, etc.
    private boolean isRead;
    private LocalDateTime createdAt;
    private Map<String, Object> data;

    public Notification() {
        this.notificationId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
        this.data = new HashMap<>();
    }

    public Notification(String userId, String type, String title, String content) {
        this();
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    // Getters
    public String getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getReferenceId() { return referenceId; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Map<String, Object> getData() { return data; }

    // Setters
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public void setRead(boolean isRead) { this.isRead = isRead; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setData(Map<String, Object> data) { this.data = data; }

    // Utility methods
    public void markAsRead() {
        this.isRead = true;
    }

    public String getFormattedTime() {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(createdAt, now).toMinutes();

        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";

        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ trước";

        long days = hours / 24;
        if (days < 7) return days + " ngày trước";

        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(notificationId, that.notificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId='" + notificationId + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                '}';
    }

}
