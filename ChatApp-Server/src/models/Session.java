package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

public class Session implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String userId;
    private String ipAddress;
    private String deviceInfo;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private boolean isActive;

    // ==================== CONSTRUCTORS ====================

    public Session() {
        this.sessionId = UUID.randomUUID().toString();
        this.loginTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.isActive = true;
    }

    public Session(String userId, String ipAddress, String deviceInfo) {
        this();
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
    }

    // ==================== GETTERS ====================

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
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

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Update last activity to current time
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Get session duration in seconds
     */
    public long getSessionDuration() {
        return Duration.between(loginTime, LocalDateTime.now()).getSeconds();
    }

    /**
     * Get idle time in seconds
     */
    public long getIdleTime() {
        return Duration.between(lastActivity, LocalDateTime.now()).getSeconds();
    }

    /**
     * Check if session is expired (idle for more than specified minutes)
     */
    public boolean isExpired(long maxIdleMinutes) {
        long idleMinutes = getIdleTime() / 60;
        return idleMinutes > maxIdleMinutes;
    }

    /**
     * Terminate session
     */
    public void terminate() {
        this.isActive = false;
    }

    /**
     * Get formatted session duration
     */
    public String getFormattedDuration() {
        long seconds = getSessionDuration();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    /**
     * Get formatted idle time
     */
    public String getFormattedIdleTime() {
        long seconds = getIdleTime();
        long minutes = seconds / 60;
        long secs = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds ago", minutes, secs);
        } else {
            return String.format("%ds ago", secs);
        }
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                ", loginTime=" + loginTime +
                ", lastActivity=" + lastActivity +
                ", isActive=" + isActive +
                ", duration='" + getFormattedDuration() + '\'' +
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