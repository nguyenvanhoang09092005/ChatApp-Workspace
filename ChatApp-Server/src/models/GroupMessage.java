package models;

import java.util.Date;

public class GroupMessage {
    private String messageId;  // Thay đổi từ int thành String
    private String groupId;    // Thay đổi từ int thành String
    private String senderId;   // Thay đổi từ int thành String
    private String content;
    private String messageType; // "text", "image", "file", "sticker", "system"
    private String attachmentUrl;
    private boolean isEdited;
    private boolean isDeleted;
    private Date sentAt;
    private Date editedAt;
    private String metadata; // JSON string for additional data

    // Constructors
    public GroupMessage() {
        this.messageId = java.util.UUID.randomUUID().toString();
        this.sentAt = new Date();
        this.messageType = "text";
        this.isEdited = false;
        this.isDeleted = false;
    }

    public GroupMessage(String groupId, String senderId, String content, String messageType) {
        this();
        this.groupId = groupId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        if (this.isEdited) {
            this.editedAt = new Date();
        }
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
        if (edited) {
            this.editedAt = new Date();
        }
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public boolean isSystemMessage() {
        return "system".equals(messageType);
    }

    @Override
    public String toString() {
        return "GroupMessage{" +
                "messageId='" + messageId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) : "") + '\'' +
                ", sentAt=" + sentAt +
                '}';
    }
}