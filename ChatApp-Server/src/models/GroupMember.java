package models;

import java.util.Date;

public class GroupMember {
    private String groupId;    // Thay đổi từ int thành String
    private String userId;     // Thay đổi từ int thành String
    private String role; // "admin", "moderator", "member"
    private Date joinedAt;
    private Date lastSeen;
    private boolean isMuted;
    private boolean isBanned;

    // Constructors
    public GroupMember() {
        this.joinedAt = new Date();
        this.lastSeen = new Date();
        this.role = "member";
        this.isMuted = false;
        this.isBanned = false;
    }

    public GroupMember(String groupId, String userId, String role) {
        this();
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
    }

    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isModerator() {
        return "moderator".equals(role) || isAdmin();
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "groupId='" + groupId + '\'' +
                ", userId='" + userId + '\'' +
                ", role='" + role + '\'' +
                ", joinedAt=" + joinedAt +
                '}';
    }
}