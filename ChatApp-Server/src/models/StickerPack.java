package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * StickerPack Model - Server side
 */
public class StickerPack implements Serializable {
    private static final long serialVersionUID = 1L;

    private String packId;
    private String name;
    private String thumbnailUrl;
    private String author;
    private boolean isPremium;
    private double price;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int downloadCount;
    private boolean isActive;

    // Constructor
    public StickerPack() {
        this.packId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isPremium = false;
        this.price = 0.0;
        this.downloadCount = 0;
        this.isActive = true;
    }

    public StickerPack(String name, String thumbnailUrl, String author) {
        this();
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.author = author;
    }

    // Getters & Setters
    public String getPackId() { return packId; }
    public void setPackId(String packId) { this.packId = packId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "StickerPack{" +
                "packId='" + packId + '\'' +
                ", name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", isPremium=" + isPremium +
                '}';
    }
}

/**
 * Sticker Model - Server side
 */
class Sticker implements Serializable {
    private static final long serialVersionUID = 1L;

    private String stickerId;
    private String packId;
    private String imageUrl;
    private String name;
    private String tags;
    private int orderIndex;
    private LocalDateTime createdAt;
    private boolean isAnimated;
    private long fileSize;

    // Constructor
    public Sticker() {
        this.stickerId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.isAnimated = false;
        this.orderIndex = 0;
        this.fileSize = 0;
    }

    public Sticker(String packId, String imageUrl, String name) {
        this();
        this.packId = packId;
        this.imageUrl = imageUrl;
        this.name = name;
    }

    // Getters & Setters
    public String getStickerId() { return stickerId; }
    public void setStickerId(String stickerId) { this.stickerId = stickerId; }

    public String getPackId() { return packId; }
    public void setPackId(String packId) { this.packId = packId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAnimated() { return isAnimated; }
    public void setAnimated(boolean animated) { isAnimated = animated; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    @Override
    public String toString() {
        return "Sticker{" +
                "stickerId='" + stickerId + '\'' +
                ", packId='" + packId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

/**
 * UserStickerPack Model - Tracks user ownership of sticker packs
 */
class UserStickerPack implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String packId;
    private LocalDateTime purchasedAt;
    private double pricePaid;
    private String transactionId;

    // Constructor
    public UserStickerPack() {
        this.purchasedAt = LocalDateTime.now();
    }

    public UserStickerPack(String userId, String packId) {
        this();
        this.userId = userId;
        this.packId = packId;
    }

    // Getters & Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPackId() { return packId; }
    public void setPackId(String packId) { this.packId = packId; }

    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }

    public double getPricePaid() { return pricePaid; }
    public void setPricePaid(double pricePaid) { this.pricePaid = pricePaid; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}