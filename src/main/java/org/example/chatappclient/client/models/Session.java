package org.example.chatappclient.client.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Session implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String userId;
    private String username;
    private String token;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private LocalDateTime expiryTime;
    private String ipAddress;
    private boolean isActive;

    // ==================== CONSTRUCTORS ====================

    public Session() {
        this.sessionId = UUID.randomUUID().toString();
        this.loginTime = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
        this.isActive = true;
    }

    public Session(String userId, String username, String token) {
        this();
        this.userId = userId;
        this.username = username;
        this.token = token;
        this.expiryTime = LocalDateTime.now().plusHours(1); // Default 1 hour
    }

    public Session(String userId, String username, String token, long expiryMillis) {
        this();
        this.userId = userId;
        this.username = username;
        this.token = token;
        this.expiryTime = LocalDateTime.now().plusSeconds(expiryMillis / 1000);
    }

    // ==================== GETTERS ====================

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isActive() {
        return isActive;
    }

    // ==================== SETTERS ====================

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public void setLastActivityTime(LocalDateTime lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * Update last activity time
     */
    public void updateActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }

    /**
     * Extend session expiry time
     */
    public void extendExpiry(long millis) {
        this.expiryTime = LocalDateTime.now().plusSeconds(millis / 1000);
    }

    /**
     * Invalidate session
     */
    public void invalidate() {
        this.isActive = false;
        this.expiryTime = LocalDateTime.now();
    }

    /**
     * Check if session is valid (active and not expired)
     */
    public boolean isValid() {
        return isActive && !isExpired();
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", loginTime=" + loginTime +
                ", expiryTime=" + expiryTime +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return sessionId.equals(session.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}