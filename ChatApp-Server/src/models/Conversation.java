package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Conversation model for ChatApp Server
 * MUST BE COMPATIBLE WITH CLIENT CONVERSATION MODEL
 */
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
    private LocalDateTime lastMessageTime;
    private boolean isActive;
    private Map<String, Object> settings;

    // ==================== CONSTRUCTORS ====================

    public Conversation() {
        this.conversationId = UUID.randomUUID().toString();
        this.type = TYPE_PRIVATE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.memberIds = new ArrayList<>();
        this.isActive = true;
        this.settings = new HashMap<>();
    }

    public Conversation(String type, String name, String creatorId) {
        this();
        this.type = type;
        this.name = name;
        this.creatorId = creatorId;
    }

    // ==================== GETTERS ====================

    public String getConversationId() {
        return conversationId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    // ==================== SETTERS ====================

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = LocalDateTime.now();
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if conversation is group
     */
    public boolean isGroup() {
        return TYPE_GROUP.equals(type);
    }

    /**
     * Check if conversation is private
     */
    public boolean isPrivate() {
        return TYPE_PRIVATE.equals(type);
    }

    /**
     * Add member to conversation
     */
    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Remove member from conversation
     */
    public void removeMember(String userId) {
        if (memberIds.remove(userId)) {
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Check if user is member
     */
    public boolean hasMember(String userId) {
        return memberIds.contains(userId);
    }

    /**
     * Get member count
     */
    public int getMemberCount() {
        return memberIds.size();
    }

    /**
     * Update last message info
     */
    public void updateLastMessage(String message, LocalDateTime time) {
        this.lastMessage = message;
        this.lastMessageTime = time;
        this.updatedAt = time;
    }

    /**
     * Check if user is creator
     */
    public boolean isCreator(String userId) {
        return creatorId != null && creatorId.equals(userId);
    }

    /**
     * Get member IDs as comma-separated string (for database storage)
     */
    public String getMemberIdsAsString() {
        return String.join(",", memberIds);
    }

    /**
     * Set member IDs from comma-separated string (from database)
     */
    public void setMemberIdsFromString(String memberIdsStr) {
        if (memberIdsStr != null && !memberIdsStr.trim().isEmpty()) {
            this.memberIds = new ArrayList<>(Arrays.asList(memberIdsStr.split(",")));
        } else {
            this.memberIds = new ArrayList<>();
        }
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "Conversation{" +
                "conversationId='" + conversationId + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", memberCount=" + memberIds.size() +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return conversationId.equals(that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId);
    }
}