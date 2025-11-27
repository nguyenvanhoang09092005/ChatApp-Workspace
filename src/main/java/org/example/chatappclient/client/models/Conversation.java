package org.example.chatappclient.client.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Conversation implements Serializable {
    private static final long serialVersionUID = 1L;

    // Conversation types
    public static final String TYPE_PRIVATE = "private";
    public static final String TYPE_GROUP = "group";

    private String conversationId;
    private String type;
    private String name;
    private String avatarUrl;
    private String description;
    private List<String> memberIds;
    private String creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastMessage;
    private String lastMessageTime;
    private int unreadCount;
    private boolean isActive;
    private boolean isMuted;
    private boolean isPinned;
    private boolean isArchived;
    private Map<String, Object> settings;

    // ==================== CONSTRUCTORS ====================

    public Conversation() {
        this.conversationId = UUID.randomUUID().toString();
        this.type = TYPE_PRIVATE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.memberIds = new ArrayList<>();
        this.isActive = true;
        this.isMuted = false;
        this.isPinned = false;
        this.isArchived = false;
        this.unreadCount = 0;
        this.settings = new HashMap<>();
    }

    public Conversation(String type, String name) {
        this();
        this.type = type;
        this.name = name;
    }

    // ==================== GETTERS ====================

    public String getConversationId() { return conversationId; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getDescription() { return description; }
    public List<String> getMemberIds() { return memberIds; }
    public String getCreatorId() { return creatorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageTime() { return lastMessageTime; }
    public int getUnreadCount() { return unreadCount; }
    public boolean isActive() { return isActive; }
    public boolean isMuted() { return isMuted; }
    public boolean isPinned() { return isPinned; }
    public boolean isArchived() { return isArchived; }
    public Map<String, Object> getSettings() { return settings; }

    // ==================== SETTERS ====================

    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setType(String type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setDescription(String description) { this.description = description; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    public void setMuted(boolean isMuted) { this.isMuted = isMuted; }
    public void setPinned(boolean isPinned) { this.isPinned = isPinned; }
    public void setArchived(boolean isArchived) { this.isArchived = isArchived; }
    public void setSettings(Map<String, Object> settings) { this.settings = settings; }

    // ==================== UTILITY METHODS ====================

    public boolean isGroup() {
        return TYPE_GROUP.equals(type);
    }

    public boolean isPrivate() {
        return TYPE_PRIVATE.equals(type);
    }

    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
            updatedAt = LocalDateTime.now();
        }
    }

    public void removeMember(String userId) {
        if (memberIds.remove(userId)) {
            updatedAt = LocalDateTime.now();
        }
    }

    public boolean hasMember(String userId) {
        return memberIds.contains(userId);
    }

    public int getMemberCount() {
        return memberIds.size();
    }

    public String getAvatarUrlOrDefault() {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            return avatarUrl;
        }
        if (isGroup()) {
            return "https://ui-avatars.com/api/?background=28a745&color=fff&name="
                    + (name != null ? name.replace(" ", "+") : "Group");
        }
        return "https://ui-avatars.com/api/?background=0084ff&color=fff&name=User";
    }

    public String getFormattedLastMessageTime() {
        if (lastMessageTime == null || lastMessageTime.isEmpty()) {
            return "";
        }

        try {
            LocalDateTime msgTime = LocalDateTime.parse(lastMessageTime);
            LocalDateTime now = LocalDateTime.now();

            if (msgTime.toLocalDate().equals(now.toLocalDate())) {
                return msgTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else if (msgTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
                return "Hôm qua";
            } else if (msgTime.toLocalDate().isAfter(now.toLocalDate().minusDays(7))) {
                return msgTime.format(DateTimeFormatter.ofPattern("EEE"));
            } else if (msgTime.getYear() == now.getYear()) {
                return msgTime.format(DateTimeFormatter.ofPattern("dd/MM"));
            } else {
                return msgTime.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
            }
        } catch (Exception e) {
            return lastMessageTime;
        }
    }

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (isGroup()) {
            return "Nhóm chat";
        }
        return "Cuộc trò chuyện";
    }

    public String getUnreadBadge() {
        if (unreadCount == 0) return "";
        if (unreadCount > 99) return "99+";
        return String.valueOf(unreadCount);
    }

    public void incrementUnreadCount() {
        unreadCount++;
    }

    public void resetUnreadCount() {
        unreadCount = 0;
    }

    public void updateLastMessage(String message, LocalDateTime time) {
        this.lastMessage = message;
        this.lastMessageTime = time.toString();
        this.updatedAt = time;
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "Conversation{" +
                "conversationId='" + conversationId + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", memberCount=" + memberIds.size() +
                ", unreadCount=" + unreadCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId);
    }


    public String getLastMessagePreview() {
        if (lastMessage == null || lastMessage.trim().isEmpty()) {
            return "";
        }
        if (lastMessage.length() > 40) {
            return lastMessage.substring(0, 40) + "...";
        }
        return lastMessage;
    }

}