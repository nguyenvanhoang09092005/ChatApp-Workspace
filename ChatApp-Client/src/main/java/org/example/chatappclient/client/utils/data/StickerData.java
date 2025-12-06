package org.example.chatappclient.client.utils.data;

import java.util.*;

/**
 * StickerData - Quản lý dữ liệu sticker packs
 * Bạn có thể thay thế URLs bằng sticker thật từ server
 */
public class StickerData {

    // ==================== STICKER PACKS ====================

    public static final Map<String, StickerPack> STICKER_PACKS = new LinkedHashMap<>();

    static {
        // Pack 1: Cute Animals
        StickerPack cuteAnimals = new StickerPack();
        cuteAnimals.setPackId("pack_001");
        cuteAnimals.setName("Cute Animals");
        cuteAnimals.setThumbnail("https://example.com/stickers/cute-animals/thumb.png");
        cuteAnimals.setAuthor("Sticker Team");
        cuteAnimals.setPremium(false);
        cuteAnimals.setStickers(Arrays.asList(
                createSticker("s001", "pack_001", "https://example.com/stickers/cute-animals/01.png", "Cat Happy"),
                createSticker("s002", "pack_001", "https://example.com/stickers/cute-animals/02.png", "Dog Smile"),
                createSticker("s003", "pack_001", "https://example.com/stickers/cute-animals/03.png", "Bear Hug"),
                createSticker("s004", "pack_001", "https://example.com/stickers/cute-animals/04.png", "Rabbit Love"),
                createSticker("s005", "pack_001", "https://example.com/stickers/cute-animals/05.png", "Panda Cry"),
                createSticker("s006", "pack_001", "https://example.com/stickers/cute-animals/06.png", "Fox Laugh"),
                createSticker("s007", "pack_001", "https://example.com/stickers/cute-animals/07.png", "Koala Sleep"),
                createSticker("s008", "pack_001", "https://example.com/stickers/cute-animals/08.png", "Penguin Dance")
        ));
        STICKER_PACKS.put("pack_001", cuteAnimals);

        // Pack 2: Funny Faces
        StickerPack funnyFaces = new StickerPack();
        funnyFaces.setPackId("pack_002");
        funnyFaces.setName("Funny Faces");
        funnyFaces.setThumbnail("https://example.com/stickers/funny-faces/thumb.png");
        funnyFaces.setAuthor("Emoji Art");
        funnyFaces.setPremium(false);
        funnyFaces.setStickers(Arrays.asList(
                createSticker("s009", "pack_002", "https://example.com/stickers/funny-faces/01.png", "LOL"),
                createSticker("s010", "pack_002", "https://example.com/stickers/funny-faces/02.png", "OMG"),
                createSticker("s011", "pack_002", "https://example.com/stickers/funny-faces/03.png", "Angry"),
                createSticker("s012", "pack_002", "https://example.com/stickers/funny-faces/04.png", "Shocked"),
                createSticker("s013", "pack_002", "https://example.com/stickers/funny-faces/05.png", "Cool"),
                createSticker("s014", "pack_002", "https://example.com/stickers/funny-faces/06.png", "Thinking"),
                createSticker("s015", "pack_002", "https://example.com/stickers/funny-faces/07.png", "Crying"),
                createSticker("s016", "pack_002", "https://example.com/stickers/funny-faces/08.png", "Party")
        ));
        STICKER_PACKS.put("pack_002", funnyFaces);

        // Pack 3: Love & Hearts
        StickerPack loveHearts = new StickerPack();
        loveHearts.setPackId("pack_003");
        loveHearts.setName("Love & Hearts");
        loveHearts.setThumbnail("https://example.com/stickers/love-hearts/thumb.png");
        loveHearts.setAuthor("Love Studio");
        loveHearts.setPremium(false);
        loveHearts.setStickers(Arrays.asList(
                createSticker("s017", "pack_003", "https://example.com/stickers/love-hearts/01.png", "I Love You"),
                createSticker("s018", "pack_003", "https://example.com/stickers/love-hearts/02.png", "Kiss"),
                createSticker("s019", "pack_003", "https://example.com/stickers/love-hearts/03.png", "Heart Eyes"),
                createSticker("s020", "pack_003", "https://example.com/stickers/love-hearts/04.png", "Flying Hearts"),
                createSticker("s021", "pack_003", "https://example.com/stickers/love-hearts/05.png", "Broken Heart"),
                createSticker("s022", "pack_003", "https://example.com/stickers/love-hearts/06.png", "Cupid"),
                createSticker("s023", "pack_003", "https://example.com/stickers/love-hearts/07.png", "Rose"),
                createSticker("s024", "pack_003", "https://example.com/stickers/love-hearts/08.png", "Together")
        ));
        STICKER_PACKS.put("pack_003", loveHearts);

        // Pack 4: Memes
        StickerPack memes = new StickerPack();
        memes.setPackId("pack_004");
        memes.setName("Popular Memes");
        memes.setThumbnail("https://example.com/stickers/memes/thumb.png");
        memes.setAuthor("Meme Central");
        memes.setPremium(true);
        memes.setStickers(Arrays.asList(
                createSticker("s025", "pack_004", "https://example.com/stickers/memes/01.png", "Stonks"),
                createSticker("s026", "pack_004", "https://example.com/stickers/memes/02.png", "This is Fine"),
                createSticker("s027", "pack_004", "https://example.com/stickers/memes/03.png", "Doge"),
                createSticker("s028", "pack_004", "https://example.com/stickers/memes/04.png", "Drake No Yes"),
                createSticker("s029", "pack_004", "https://example.com/stickers/memes/05.png", "Surprised Pikachu"),
                createSticker("s030", "pack_004", "https://example.com/stickers/memes/06.png", "Woman Yelling Cat"),
                createSticker("s031", "pack_004", "https://example.com/stickers/memes/07.png", "Success Kid"),
                createSticker("s032", "pack_004", "https://example.com/stickers/memes/08.png", "Distracted Boyfriend")
        ));
        STICKER_PACKS.put("pack_004", memes);

        // Pack 5: Food & Drinks
        StickerPack foodDrinks = new StickerPack();
        foodDrinks.setPackId("pack_005");
        foodDrinks.setName("Yummy Food");
        foodDrinks.setThumbnail("https://example.com/stickers/food/thumb.png");
        foodDrinks.setAuthor("Foodie Art");
        foodDrinks.setPremium(false);
        foodDrinks.setStickers(Arrays.asList(
                createSticker("s033", "pack_005", "https://example.com/stickers/food/01.png", "Pizza"),
                createSticker("s034", "pack_005", "https://example.com/stickers/food/02.png", "Burger"),
                createSticker("s035", "pack_005", "https://example.com/stickers/food/03.png", "Sushi"),
                createSticker("s036", "pack_005", "https://example.com/stickers/food/04.png", "Coffee"),
                createSticker("s037", "pack_005", "https://example.com/stickers/food/05.png", "Ice Cream"),
                createSticker("s038", "pack_005", "https://example.com/stickers/food/06.png", "Donut"),
                createSticker("s039", "pack_005", "https://example.com/stickers/food/07.png", "Cake"),
                createSticker("s040", "pack_005", "https://example.com/stickers/food/08.png", "Ramen")
        ));
        STICKER_PACKS.put("pack_005", foodDrinks);
    }

    // ==================== HELPER METHODS ====================

    private static Sticker createSticker(String id, String packId, String url, String name) {
        Sticker sticker = new Sticker();
        sticker.setStickerId(id);
        sticker.setPackId(packId);
        sticker.setImageUrl(url);
        sticker.setName(name);
        return sticker;
    }

    // ==================== PUBLIC METHODS ====================

    public static List<StickerPack> getAllPacks() {
        return new ArrayList<>(STICKER_PACKS.values());
    }

    public static List<StickerPack> getFreePacks() {
        List<StickerPack> free = new ArrayList<>();
        for (StickerPack pack : STICKER_PACKS.values()) {
            if (!pack.isPremium()) {
                free.add(pack);
            }
        }
        return free;
    }

    public static List<StickerPack> getPremiumPacks() {
        List<StickerPack> premium = new ArrayList<>();
        for (StickerPack pack : STICKER_PACKS.values()) {
            if (pack.isPremium()) {
                premium.add(pack);
            }
        }
        return premium;
    }

    public static StickerPack getPack(String packId) {
        return STICKER_PACKS.get(packId);
    }

    public static List<Sticker> getStickersForPack(String packId) {
        StickerPack pack = STICKER_PACKS.get(packId);
        return pack != null ? pack.getStickers() : new ArrayList<>();
    }

    // ==================== INNER CLASSES ====================

    public static class StickerPack {
        private String packId;
        private String name;
        private String thumbnail;
        private String author;
        private boolean isPremium;
        private List<Sticker> stickers = new ArrayList<>();

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

    public static class Sticker {
        private String stickerId;
        private String packId;
        private String imageUrl;
        private String name;

        // Getters & Setters
        public String getStickerId() { return stickerId; }
        public void setStickerId(String stickerId) { this.stickerId = stickerId; }

        public String getPackId() { return packId; }
        public void setPackId(String packId) { this.packId = packId; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUrl() {
            return imageUrl;
        }
    }
}