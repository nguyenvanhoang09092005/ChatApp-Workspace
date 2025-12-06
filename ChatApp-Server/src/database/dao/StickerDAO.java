package database.dao;

import database.connection.DBConnection;
import models.StickerPack;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Sticker operations
 */
public class StickerDAO {

    // ==================== STICKER PACK OPERATIONS ====================

    /**
     * Get all active sticker packs
     */
    public static List<StickerPack> getAllPacks() {
        String sql = "SELECT * FROM sticker_packs WHERE is_active = TRUE ORDER BY created_at DESC";
        List<StickerPack> packs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                packs.add(mapResultSetToStickerPack(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting sticker packs: " + e.getMessage());
        }

        return packs;
    }

    /**
     * Get sticker pack by ID
     */
    public static StickerPack getPackById(String packId) {
        String sql = "SELECT * FROM sticker_packs WHERE pack_id = ? AND is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, packId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToStickerPack(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting sticker pack: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get free sticker packs
     */
    public static List<StickerPack> getFreePacks() {
        String sql = "SELECT * FROM sticker_packs WHERE is_premium = FALSE AND is_active = TRUE " +
                "ORDER BY download_count DESC, created_at DESC";
        List<StickerPack> packs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                packs.add(mapResultSetToStickerPack(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting free packs: " + e.getMessage());
        }

        return packs;
    }

    /**
     * Get premium sticker packs
     */
    public static List<StickerPack> getPremiumPacks() {
        String sql = "SELECT * FROM sticker_packs WHERE is_premium = TRUE AND is_active = TRUE " +
                "ORDER BY download_count DESC, created_at DESC";
        List<StickerPack> packs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                packs.add(mapResultSetToStickerPack(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting premium packs: " + e.getMessage());
        }

        return packs;
    }

    /**
     * Get user's owned sticker packs
     */
    public static List<StickerPack> getUserPacks(String userId) {
        String sql = "SELECT sp.* FROM sticker_packs sp " +
                "INNER JOIN user_sticker_packs usp ON sp.pack_id = usp.pack_id " +
                "WHERE usp.user_id = ? AND sp.is_active = TRUE " +
                "ORDER BY usp.purchased_at DESC";
        List<StickerPack> packs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                packs.add(mapResultSetToStickerPack(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting user packs: " + e.getMessage());
        }

        return packs;
    }

    /**
     * Check if user owns a pack
     */
    public static boolean userOwnsPack(String userId, String packId) {
        String sql = "SELECT COUNT(*) as count FROM user_sticker_packs " +
                "WHERE user_id = ? AND pack_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, packId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error checking pack ownership: " + e.getMessage());
        }

        return false;
    }

    /**
     * Grant sticker pack to user (purchase or gift)
     */
    public static boolean grantPackToUser(String userId, String packId, double pricePaid, String transactionId) {
        // Check if user already owns the pack
        if (userOwnsPack(userId, packId)) {
            System.out.println("⚠️ User already owns this pack");
            return false;
        }

        String sql = "INSERT INTO user_sticker_packs (user_id, pack_id, price_paid, transaction_id) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, packId);
            ps.setDouble(3, pricePaid);
            ps.setString(4, transactionId);

            int result = ps.executeUpdate();
            System.out.println("✅ Pack granted to user: " + userId + " -> " + packId);
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error granting pack: " + e.getMessage());
            return false;
        }
    }

    // ==================== STICKER OPERATIONS ====================

    /**
     * Get stickers for a pack
     */
    public static List<Map<String, String>> getStickersForPack(String packId) {
        String sql = "SELECT * FROM stickers WHERE pack_id = ? ORDER BY order_index ASC";
        List<Map<String, String>> stickers = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, packId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, String> sticker = new HashMap<>();
                sticker.put("stickerId", rs.getString("sticker_id"));
                sticker.put("packId", rs.getString("pack_id"));
                sticker.put("imageUrl", rs.getString("image_url"));
                sticker.put("name", rs.getString("name"));
                sticker.put("tags", rs.getString("tags"));
                sticker.put("orderIndex", String.valueOf(rs.getInt("order_index")));
                sticker.put("isAnimated", String.valueOf(rs.getBoolean("is_animated")));
                sticker.put("fileSize", String.valueOf(rs.getLong("file_size")));
                stickers.add(sticker);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting stickers: " + e.getMessage());
        }

        return stickers;
    }

    /**
     * Get sticker by ID
     */
    public static Map<String, String> getStickerById(String stickerId) {
        String sql = "SELECT * FROM stickers WHERE sticker_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, stickerId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Map<String, String> sticker = new HashMap<>();
                sticker.put("stickerId", rs.getString("sticker_id"));
                sticker.put("packId", rs.getString("pack_id"));
                sticker.put("imageUrl", rs.getString("image_url"));
                sticker.put("name", rs.getString("name"));
                sticker.put("tags", rs.getString("tags"));
                return sticker;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting sticker: " + e.getMessage());
        }

        return null;
    }

    /**
     * Search stickers by keyword
     */
    public static List<Map<String, String>> searchStickers(String keyword, int limit) {
        String sql = "SELECT * FROM stickers " +
                "WHERE MATCH(name, tags) AGAINST(? IN NATURAL LANGUAGE MODE) " +
                "ORDER BY order_index ASC LIMIT ?";
        List<Map<String, String>> stickers = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, keyword);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, String> sticker = new HashMap<>();
                sticker.put("stickerId", rs.getString("sticker_id"));
                sticker.put("packId", rs.getString("pack_id"));
                sticker.put("imageUrl", rs.getString("image_url"));
                sticker.put("name", rs.getString("name"));
                sticker.put("tags", rs.getString("tags"));
                stickers.add(sticker);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching stickers: " + e.getMessage());
        }

        return stickers;
    }

    /**
     * Track sticker usage (analytics)
     */
    public static void trackStickerUsage(String userId, String stickerId) {
        String sql = "INSERT INTO sticker_usage (user_id, sticker_id) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, stickerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("⚠️ Error tracking sticker usage: " + e.getMessage());
            // Non-critical, don't throw
        }
    }

    // ==================== ADMIN OPERATIONS ====================

    /**
     * Create new sticker pack
     */
    public static boolean createPack(StickerPack pack) {
        String sql = "INSERT INTO sticker_packs (pack_id, name, thumbnail_url, author, " +
                "is_premium, price, description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pack.getPackId());
            ps.setString(2, pack.getName());
            ps.setString(3, pack.getThumbnailUrl());
            ps.setString(4, pack.getAuthor());
            ps.setBoolean(5, pack.isPremium());
            ps.setDouble(6, pack.getPrice());
            ps.setString(7, pack.getDescription());
            ps.setBoolean(8, pack.isActive());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error creating pack: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add sticker to pack
     */
    public static boolean addStickerToPack(String stickerId, String packId, String imageUrl,
                                           String name, String tags, int orderIndex) {
        String sql = "INSERT INTO stickers (sticker_id, pack_id, image_url, name, tags, order_index) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, stickerId);
            ps.setString(2, packId);
            ps.setString(3, imageUrl);
            ps.setString(4, name);
            ps.setString(5, tags);
            ps.setInt(6, orderIndex);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error adding sticker: " + e.getMessage());
            return false;
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Map ResultSet to StickerPack object
     */
    private static StickerPack mapResultSetToStickerPack(ResultSet rs) throws SQLException {
        StickerPack pack = new StickerPack();
        pack.setPackId(rs.getString("pack_id"));
        pack.setName(rs.getString("name"));
        pack.setThumbnailUrl(rs.getString("thumbnail_url"));
        pack.setAuthor(rs.getString("author"));
        pack.setPremium(rs.getBoolean("is_premium"));
        pack.setPrice(rs.getDouble("price"));
        pack.setDescription(rs.getString("description"));
        pack.setDownloadCount(rs.getInt("download_count"));
        pack.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            pack.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            pack.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return pack;
    }
}