package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Notification model for ChatApp Server
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    // Notification types
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    public static final String TYPE_FRIEND_ACCEPT = "FRIEND_ACCEPT";
    public static final String TYPE_GROUP_INVITE = "GROUP_INVITE";
    public static final String TYPE_GROUP_ADDED = "GROUP_ADDED";
    public static final String TYPE_GROUP_REMOVED = "GROUP_REMOVED";
    public static final String TYPE_MENTION = "MENTION";
    public static final String TYPE_SYSTEM = "SYSTEM";

    private String notificationId;
    private String userId; // Recipient
    private String type;
    private String title;
    private String content;
    private String senderId; // Sender/Actor
    private String senderName;
    private String senderAvatar;
    private String relatedId; // Related conversation/message/user ID
    private String actionUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Map<String, Object> data; // Additional data

    // ==================== CONSTRUCTORS ====================

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

    // ==================== GETTERS ====================

    public String getNotificationId() {
        return notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public Map<String, Object> getData() {
        return data;
    }

    // ==================== SETTERS ====================

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public void setRead(boolean read) {
        isRead = read;
        if (read && readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Mark as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Mark as unread
     */
    public void markAsUnread() {
        this.isRead = false;
        this.readAt = null;
    }

    /**
     * Add data field
     */
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }

    /**
     * Get data field
     */
    public Object getData(String key) {
        return this.data.get(key);
    }

    /**
     * Check if notification is message type
     */
    public boolean isMessageNotification() {
        return TYPE_MESSAGE.equals(type) || TYPE_MENTION.equals(type);
    }

    /**
     * Check if notification is friend request type
     */
    public boolean isFriendRequestNotification() {
        return TYPE_FRIEND_REQUEST.equals(type) || TYPE_FRIEND_ACCEPT.equals(type);
    }

    /**
     * Check if notification is group type
     */
    public boolean isGroupNotification() {
        return TYPE_GROUP_INVITE.equals(type) ||
                TYPE_GROUP_ADDED.equals(type) ||
                TYPE_GROUP_REMOVED.equals(type);
    }

    /**
     * Create message notification
     */
    public static Notification createMessageNotification(String userId, String senderId,
                                                         String senderName, String messageContent, String conversationId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_MESSAGE);
        notification.setTitle("Tin nhắn mới từ " + senderName);
        notification.setContent(messageContent);
        notification.setSenderId(senderId);
        notification.setSenderName(senderName);
        notification.setRelatedId(conversationId);
        notification.setActionUrl("/conversations/" + conversationId);
        return notification;
    }

    /**
     * Create friend request notification
     */
    public static Notification createFriendRequestNotification(String userId, String senderId,
                                                               String senderName) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_FRIEND_REQUEST);
        notification.setTitle("Lời mời kết bạn");
        notification.setContent(senderName + " đã gửi lời mời kết bạn");
        notification.setSenderId(senderId);
        notification.setSenderName(senderName);
        notification.setRelatedId(senderId);
        notification.setActionUrl("/friends/requests");
        return notification;
    }

    /**
     * Create friend accept notification
     */
    public static Notification createFriendAcceptNotification(String userId, String senderId,
                                                              String senderName) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_FRIEND_ACCEPT);
        notification.setTitle("Kết bạn thành công");
        notification.setContent(senderName + " đã chấp nhận lời mời kết bạn của bạn");
        notification.setSenderId(senderId);
        notification.setSenderName(senderName);
        notification.setRelatedId(senderId);
        notification.setActionUrl("/friends");
        return notification;
    }

    /**
     * Create group invite notification
     */
    public static Notification createGroupInviteNotification(String userId, String senderId,
                                                             String senderName, String groupName, String groupId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_GROUP_INVITE);
        notification.setTitle("Lời mời vào nhóm");
        notification.setContent(senderName + " đã mời bạn vào nhóm " + groupName);
        notification.setSenderId(senderId);
        notification.setSenderName(senderName);
        notification.setRelatedId(groupId);
        notification.setActionUrl("/conversations/" + groupId);
        return notification;
    }

    /**
     * Create system notification
     */
    public static Notification createSystemNotification(String userId, String title,
                                                        String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(TYPE_SYSTEM);
        notification.setTitle(title);
        notification.setContent(content);
        return notification;
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId='" + notificationId + '\'' +
                ", userId='" + userId + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return notificationId.equals(that.notificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }
}