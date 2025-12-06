package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Message implements Serializable {
    public static final String TYPE_EMOJI = "EMOJI";
    private static final long serialVersionUID = 1L;

    // Message types
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_FILE = "FILE";
    public static final String TYPE_AUDIO = "AUDIO";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_LOCATION = "LOCATION";
    public static final String TYPE_STICKER = "STICKER";
    public static final String TYPE_SYSTEM = "SYSTEM";

    private String messageId;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String messageType;
    private String mediaUrl;
    private String fileName;
    private long fileSize;
    private String thumbnailUrl;
    private int mediaDuration; // seconds
    private LocalDateTime timestamp;
    private boolean isRead;
    private boolean isDelivered;
    private boolean isEdited;
    private boolean isRecalled;
    private String replyToMessageId;

    // ==================== CONSTRUCTORS ====================

    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.messageType = TYPE_TEXT;
        this.isRead = false;
        this.isDelivered = false;
        this.isEdited = false;
        this.isRecalled = false;
    }

    public Message(String conversationId, String senderId, String content) {
        this();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
    }

    // ==================== GETTERS ====================

    public String getMessageId() {
        return messageId;
    }

    public String getConversationId() {
        return conversationId;
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

    public String getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public int getMediaDuration() {
        return mediaDuration;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public boolean isRecalled() {
        return isRecalled;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    // ==================== SETTERS ====================

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
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

    public void setContent(String content) {
        this.content = content;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setMediaDuration(int mediaDuration) {
        this.mediaDuration = mediaDuration;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public void setRecalled(boolean recalled) {
        isRecalled = recalled;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if message is media message
     */
    public boolean isMediaMessage() {
        return TYPE_IMAGE.equals(messageType) ||
                TYPE_AUDIO.equals(messageType) ||
                TYPE_VIDEO.equals(messageType) ||
                TYPE_FILE.equals(messageType);
    }

    /**
     * Check if message is system message
     */
    public boolean isSystemMessage() {
        return TYPE_SYSTEM.equals(messageType);
    }

    /**
     * Mark as delivered
     */
    public void markAsDelivered() {
        this.isDelivered = true;
    }

    /**
     * Mark as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.isDelivered = true;
    }

    /**
     * Edit message content
     */
    public void editContent(String newContent) {
        this.content = newContent;
        this.isEdited = true;
    }

    /**
     * Recall message
     */
    public void recall() {
        this.isRecalled = true;
        this.content = "Tin nhắn đã được thu hồi";
    }

    /**
     * Get display content based on type and status
     */
    public String getDisplayContent() {
        if (isRecalled) return "Tin nhắn đã được thu hồi";
        if (TYPE_IMAGE.equals(messageType)) return "📷 Hình ảnh";
        if (TYPE_FILE.equals(messageType)) return "📎 " + (fileName != null ? fileName : "File");
        if (TYPE_AUDIO.equals(messageType)) return "🎵 Tin nhắn thoại";
        if (TYPE_VIDEO.equals(messageType)) return "🎥 Video";
        if (TYPE_LOCATION.equals(messageType)) return "📍 Vị trí";
        if (TYPE_STICKER.equals(messageType)) return "Sticker";
        return content != null ? content : "";
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", timestamp=" + timestamp +
                ", isRead=" + isRead +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return messageId.equals(message.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}