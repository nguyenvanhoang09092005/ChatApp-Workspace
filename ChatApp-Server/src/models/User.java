package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User model for ChatApp Server
 * MUST BE COMPATIBLE WITH CLIENT USER MODEL
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private String salt;
    private String displayName;
    private String avatarUrl;
    private String statusMessage;
    private boolean isOnline;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private boolean isVerified;

    // ==================== CONSTRUCTORS ====================

    public User() {
        this.userId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isOnline = false;
        this.isActive = true;
        this.isVerified = false;
    }

    public User(String username, String email, String passwordHash, String salt) {
        this();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.displayName = username; // Default display name
    }

    // ==================== GETTERS ====================

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isVerified() {
        return isVerified;
    }

    // ==================== SETTERS ====================

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.updatedAt = LocalDateTime.now();
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
        if (!isOnline) {
            this.lastSeen = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get display name or username if display name is null
     */
    public String getDisplayNameOrUsername() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : username;
    }

    /**
     * Check if user has phone number
     */
    public boolean hasPhone() {
        return phone != null && !phone.isEmpty();
    }

    /**
     * Check if user has avatar
     */
    public boolean hasAvatar() {
        return avatarUrl != null && !avatarUrl.isEmpty();
    }

    /**
     * Get status text
     */
    public String getStatusText() {
        if (statusMessage != null && !statusMessage.isEmpty()) {
            return statusMessage;
        }
        return isOnline ? "Đang hoạt động" : "Ngoại tuyến";
    }

    /**
     * Clone user without sensitive data (for sending to clients)
     */
    public User cloneWithoutSensitiveData() {
        User clone = new User();
        clone.setUserId(this.userId);
        clone.setUsername(this.username);
        clone.setEmail(this.email);
        clone.setPhone(this.phone);
        clone.setDisplayName(this.displayName);
        clone.setAvatarUrl(this.avatarUrl);
        clone.setStatusMessage(this.statusMessage);
        clone.setOnline(this.isOnline);
        clone.setLastSeen(this.lastSeen);
        clone.setCreatedAt(this.createdAt);
        clone.setUpdatedAt(this.updatedAt);
        clone.setActive(this.isActive);
        clone.setVerified(this.isVerified);
        // DO NOT copy passwordHash and salt
        return clone;
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", isOnline=" + isOnline +
                ", isVerified=" + isVerified +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}