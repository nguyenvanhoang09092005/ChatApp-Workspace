package org.example.chatappclient.client.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private String email;
    private String phone;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String gender;
    private LocalDate birthday;
    private String status; // online, offline, away, busy
    private String statusMessage;
    private boolean isActive;
    private boolean isVerified;

    private boolean isOnline;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== CONSTRUCTORS ====================

    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "offline";
        this.isOnline = false;
    }

    public User(String userId, String username, String email, String displayName) {
        this();
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
    }

    // ==================== GETTERS ====================
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = LocalDateTime.now();
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
        this.updatedAt = LocalDateTime.now();
    }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
    public String getStatus() { return status; }
    public boolean isOnline() { return isOnline; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

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

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.updatedAt = LocalDateTime.now();
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void setBio(String bio) {
        this.bio = bio;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
        if (!isOnline) {
            this.lastSeen = LocalDateTime.now();
        }
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

    // ==================== UTILITY METHODS ====================

    public String getDisplayNameOrUsername() {
        return (displayName != null && !displayName.trim().isEmpty())
                ? displayName : username;
    }

    public String getStatusText() {
        if (isOnline) {
            return "Đang hoạt động";
        }
        if (lastSeen != null) {
            return formatLastSeen();
        }
        return "Ngoại tuyến";
    }

    public String getAvatarUrlOrDefault() {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            return avatarUrl;
        }
        return "https://ui-avatars.com/api/?background=0084ff&color=fff&name="
                + getDisplayNameOrUsername().replace(" ", "+");
    }

    private String formatLastSeen() {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(lastSeen, now).toMinutes();

        if (minutes < 1) return "Vừa truy cập";
        if (minutes < 60) return minutes + " phút trước";

        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ trước";

        long days = hours / 24;
        if (days < 7) return days + " ngày trước";

        return lastSeen.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public boolean isValidEmail() {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public boolean isValidPhone() {
        return phone != null && phone.matches("^[0-9]{10,11}$");
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", status='" + status + '\'' +
                ", isOnline=" + isOnline +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

}