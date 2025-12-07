package models;

import java.util.Date;

public class Group {
    private String groupId;
    private String groupName;
    private String groupAvatar;
    private String description;
    private String creatorId;  // Thay đổi từ int thành String
    private Date createdAt;
    private Date updatedAt;
    private boolean isActive;

    // Constructors
    public Group() {
        this.groupId = java.util.UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isActive = true;
    }

    public Group(String groupName, String creatorId) {
        this();
        this.groupName = groupName;
        this.creatorId = creatorId;
    }

    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
        this.updatedAt = new Date();
    }

    public String getGroupAvatar() {
        return groupAvatar;
    }

    public void setGroupAvatar(String groupAvatar) {
        this.groupAvatar = groupAvatar;
        this.updatedAt = new Date();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = new Date();
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = new Date();
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupId='" + groupId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", creatorId='" + creatorId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}