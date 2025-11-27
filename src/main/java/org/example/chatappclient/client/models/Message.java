package org.example.chatappclient.client.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Message implements Serializable {
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
    private LocalDateTime timestamp;
    private int mediaDuration; // giây


    private boolean isRead;
    private boolean isDelivered;
    private boolean isEdited;
    private boolean isRecalled;
    private String replyToMessageId;
    private Message replyToMessage;
    private List<Reaction> reactions;
    private Map<String, Object> metadata;

    // ==================== CONSTRUCTORS ====================

    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.messageType = TYPE_TEXT;
        this.isRead = false;
        this.isDelivered = false;
        this.isEdited = false;
        this.isRecalled = false;
        this.reactions = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public Message(String conversationId, String senderId, String content) {
        this();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
    }

    // ==================== GETTERS ====================

    public String getMessageId() { return messageId; }
    public String getConversationId() { return conversationId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getSenderAvatar() { return senderAvatar; }
    public String getContent() { return content; }
    public String getMessageType() { return messageType; }
    public String getMediaUrl() { return mediaUrl; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getTimestamp() {
        if (timestamp == null) return "";
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public boolean isRead() { return isRead; }
    public boolean isDelivered() { return isDelivered; }
    public boolean isEdited() { return isEdited; }
    public boolean isRecalled() { return isRecalled; }
    public String getReplyToMessageId() { return replyToMessageId; }
    public Message getReplyToMessage() { return replyToMessage; }
    public List<Reaction> getReactions() { return reactions; }
    public Map<String, Object> getMetadata() { return metadata; }

    // ==================== SETTERS ====================

    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public void setContent(String content) { this.content = content; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean isRead) { this.isRead = isRead; }
    public void setDelivered(boolean isDelivered) { this.isDelivered = isDelivered; }
    public void setEdited(boolean isEdited) { this.isEdited = isEdited; }
    public void setRecalled(boolean isRecalled) { this.isRecalled = isRecalled; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }
    public void setReplyToMessage(Message replyToMessage) { this.replyToMessage = replyToMessage; }
    public void setReactions(List<Reaction> reactions) { this.reactions = reactions; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public int getMediaDuration() {
        return mediaDuration;
    }

    public void setMediaDuration(int mediaDuration) {
        this.mediaDuration = mediaDuration;
    }

    // ==================== UTILITY METHODS ====================

    public boolean isMediaMessage() {
        return TYPE_IMAGE.equals(messageType) ||
                TYPE_AUDIO.equals(messageType) ||
                TYPE_VIDEO.equals(messageType) ||
                TYPE_FILE.equals(messageType);
    }

    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime msgTime = timestamp;

        if (msgTime.toLocalDate().equals(now.toLocalDate())) {
            return msgTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (msgTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "Hôm qua " + msgTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (msgTime.getYear() == now.getYear()) {
            return msgTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        } else {
            return msgTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }

    public String getFormattedFileSize() {
        if (fileSize == 0) return "";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    public void addReaction(String userId, String emoji) {
        removeReaction(userId); // Remove old reaction if exists
        reactions.add(new Reaction(userId, emoji));
    }

    public void removeReaction(String userId) {
        reactions.removeIf(r -> r.getUserId().equals(userId));
    }

    public Map<String, Integer> getReactionCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Reaction r : reactions) {
            counts.merge(r.getEmoji(), 1, Integer::sum);
        }
        return counts;
    }

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

    // ==================== INNER CLASS ====================

    public static class Reaction implements Serializable {
        private String userId;
        private String emoji;
        private LocalDateTime timestamp;

        public Reaction() {}

        public Reaction(String userId, String emoji) {
            this.userId = userId;
            this.emoji = emoji;
            this.timestamp = LocalDateTime.now();
        }

        public String getUserId() { return userId; }
        public String getEmoji() { return emoji; }
        public LocalDateTime getTimestamp() { return timestamp; }

        public void setUserId(String userId) { this.userId = userId; }
        public void setEmoji(String emoji) { this.emoji = emoji; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", content='" + content + '\'' +
                ", messageType='" + messageType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(messageId, message.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}