package org.example.chatappclient.client.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// ==================== CONTACT MODEL ====================

public class Contact implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_BLOCKED = "blocked";

    private String contactId;
    private String userId;
    private String contactUserId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private User contactUser; // Full user object

    public Contact() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = STATUS_PENDING;
    }

    public Contact(String userId, String contactUserId) {
        this();
        this.userId = userId;
        this.contactUserId = contactUserId;
    }

    // Getters
    public String getContactId() { return contactId; }
    public String getUserId() { return userId; }
    public String getContactUserId() { return contactUserId; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public User getContactUser() { return contactUser; }

    // Setters
    public void setContactId(String contactId) { this.contactId = contactId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setContactUserId(String contactUserId) { this.contactUserId = contactUserId; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setContactUser(User contactUser) { this.contactUser = contactUser; }

    // Utility methods
    public boolean isPending() { return STATUS_PENDING.equals(status); }
    public boolean isAccepted() { return STATUS_ACCEPTED.equals(status); }
    public boolean isBlocked() { return STATUS_BLOCKED.equals(status); }

    public void accept() {
        this.status = STATUS_ACCEPTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void block() {
        this.status = STATUS_BLOCKED;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return Objects.equals(contactId, contact.contactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactId);
    }

    @Override
    public String toString() {
        return "Contact{" +
                "contactId='" + contactId + '\'' +
                ", userId='" + userId + '\'' +
                ", contactUserId='" + contactUserId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
