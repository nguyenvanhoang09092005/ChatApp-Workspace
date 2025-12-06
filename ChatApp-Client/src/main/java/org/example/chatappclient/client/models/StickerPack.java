
package org.example.chatappclient.client.models;

import java.util.ArrayList;
import java.util.List;

public class StickerPack {
    private String packId;
    private String name;
    private String thumbnail;
    private String author;
    private boolean isPremium;
    private List<Sticker> stickers;

    public StickerPack() {
        this.stickers = new ArrayList<>();
    }

    public StickerPack(String packId, String name, String thumbnail) {
        this();
        this.packId = packId;
        this.name = name;
        this.thumbnail = thumbnail;
    }

    // Getters & Setters
    public String getPackId() { return packId; }
    public void setPackId(String packId) { this.packId = packId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public List<Sticker> getStickers() { return stickers; }
    public void setStickers(List<Sticker> stickers) { this.stickers = stickers; }
}

// ==================== Sticker.java ====================
class Sticker {
    private String stickerId;
    private String packId;
    private String imageUrl;
    private String name;
    private List<String> tags;

    public Sticker() {
        this.tags = new ArrayList<>();
    }

    public Sticker(String stickerId, String packId, String imageUrl) {
        this();
        this.stickerId = stickerId;
        this.packId = packId;
        this.imageUrl = imageUrl;
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

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}

// ==================== EmojiCategory.java ====================
class EmojiCategory {
    private String categoryId;
    private String name;
    private String icon;
    private List<EmojiItem> emojis;

    public EmojiCategory() {
        this.emojis = new ArrayList<>();
    }

    public EmojiCategory(String categoryId, String name, String icon) {
        this();
        this.categoryId = categoryId;
        this.name = name;
        this.icon = icon;
    }

    // Getters & Setters
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public List<EmojiItem> getEmojis() { return emojis; }
    public void setEmojis(List<EmojiItem> emojis) { this.emojis = emojis; }
}

// ==================== EmojiItem.java ====================
class EmojiItem {
    private String emojiId;
    private String emoji;
    private String name;
    private String category;
    private List<String> keywords;

    public EmojiItem() {
        this.keywords = new ArrayList<>();
    }

    public EmojiItem(String emojiId, String emoji, String name) {
        this();
        this.emojiId = emojiId;
        this.emoji = emoji;
        this.name = name;
    }

    // Getters & Setters
    public String getEmojiId() { return emojiId; }
    public void setEmojiId(String emojiId) { this.emojiId = emojiId; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
}