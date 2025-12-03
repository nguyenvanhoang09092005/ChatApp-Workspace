package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Contact model for ChatApp Server
 * MUST BE COMPATIBLE WITH CLIENT CONTACT MODEL
 */
public class Contact implements Serializable {
    private static final long serialVersionUID = 1L;

    // Contact status constants
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_BLOCKED = "blocked";

    private String contactId;
    private String userId;
    private String contactUserId;
    private String status;
    private String nickname; // Custom nickname for contact
    private boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== CONSTRUCTORS ====================

    public Contact() {
        this.contactId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = STATUS_PENDING;
        this.isFavorite = false;
    }

    public Contact(String userId, String contactUserId) {
        this();
        this.userId = userId;
        this.contactUserId = contactUserId;
    }

    public Contact(String userId, String contactUserId, String status) {
        this(userId, contactUserId);
        this.status = status;
    }

    // ==================== GETTERS ====================

    public String getContactId() {
        return contactId;
    }

    public String getUserId() {
        return userId;
    }

    public String getContactUserId() {
        return contactUserId;
    }

    public String getStatus() {
        return status;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ==================== SETTERS ====================

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setContactUserId(String contactUserId) {
        this.contactUserId = contactUserId;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = LocalDateTime.now();
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
        this.updatedAt = LocalDateTime.now();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if contact is pending
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /**
     * Check if contact is accepted
     */
    public boolean isAccepted() {
        return STATUS_ACCEPTED.equals(status);
    }

    /**
     * Check if contact is blocked
     */
    public boolean isBlocked() {
        return STATUS_BLOCKED.equals(status);
    }

    /**
     * Accept contact request
     */
    public void accept() {
        this.status = STATUS_ACCEPTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Block contact
     */
    public void block() {
        this.status = STATUS_BLOCKED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Unblock contact
     */
    public void unblock() {
        this.status = STATUS_ACCEPTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Validate status
     */
    public boolean isValidStatus() {
        return STATUS_PENDING.equals(status) ||
                STATUS_ACCEPTED.equals(status) ||
                STATUS_BLOCKED.equals(status);
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "Contact{" +
                "contactId='" + contactId + '\'' +
                ", userId='" + userId + '\'' +
                ", contactUserId='" + contactUserId + '\'' +
                ", status='" + status + '\'' +
                ", isFavorite=" + isFavorite +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return contactId.equals(contact.contactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactId);
    }
}