package server.handlers;

import database.dao.StickerDAO;
import models.StickerPack;
import protocol.Protocol;
import server.ClientHandler;

import java.util.List;
import java.util.Map;

/**
 * StickerHandler - Xử lý các yêu cầu liên quan đến sticker
 */
public class StickerHandler {

    private final ClientHandler clientHandler;

    public StickerHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    /**
     * Route các command sticker đến handler tương ứng
     */
    public void handle(String command, String[] parts) {
        System.out.println("→ StickerHandler: " + command);

        switch (command) {
            case Protocol.STICKER_GET_PACKS:
                handleGetPacks(parts);
                break;

            case Protocol.STICKER_GET_PACK:
                handleGetPack(parts);
                break;

            case Protocol.STICKER_PURCHASE:
                handlePurchasePack(parts);
                break;

            case Protocol.STICKER_GET_USER_PACKS:
                handleGetUserPacks(parts);
                break;

            default:
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Unknown sticker command: " + command
                ));
        }
    }

    /**
     * Get all sticker packs
     * Format: STICKER_GET_PACKS|||filterType
     * filterType: "all", "free", "premium"
     */
    private void handleGetPacks(String[] parts) {
        System.out.println("→ Getting sticker packs");

        String filterType = parts.length > 1 ? parts[1] : "all";
        List<StickerPack> packs;

        switch (filterType.toLowerCase()) {
            case "free":
                packs = StickerDAO.getFreePacks();
                break;
            case "premium":
                packs = StickerDAO.getPremiumPacks();
                break;
            default:
                packs = StickerDAO.getAllPacks();
        }

        if (packs.isEmpty()) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "No sticker packs available",
                    ""
            ));
            return;
        }

        // Build response data
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < packs.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);
            data.append(buildPackData(packs.get(i)));
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Sticker packs retrieved: " + packs.size(),
                data.toString()
        ));

        System.out.println("✅ Sent " + packs.size() + " sticker packs");
    }

    /**
     * Get stickers for a specific pack
     * Format: STICKER_GET_PACK|||packId
     */
    private void handleGetPack(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Missing pack ID"
            ));
            return;
        }

        String packId = parts[1];
        System.out.println("→ Getting stickers for pack: " + packId);

        // Get pack info
        StickerPack pack = StickerDAO.getPackById(packId);
        if (pack == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "Sticker pack not found"
            ));
            return;
        }

        // Get stickers
        List<Map<String, String>> stickers = StickerDAO.getStickersForPack(packId);

        if (stickers.isEmpty()) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Pack has no stickers",
                    buildPackData(pack)
            ));
            return;
        }

        // Build response: pack info + stickers
        StringBuilder data = new StringBuilder();
        data.append(buildPackData(pack));
        data.append(Protocol.FIELD_DELIMITER);

        for (int i = 0; i < stickers.size(); i++) {
            if (i > 0) data.append(Protocol.ARRAY_SEPARATOR);
            data.append(buildStickerData(stickers.get(i)));
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Stickers retrieved: " + stickers.size(),
                data.toString()
        ));

        System.out.println("✅ Sent " + stickers.size() + " stickers");
    }

    /**
     * Purchase/acquire sticker pack
     * Format: STICKER_PURCHASE|||userId|||packId|||transactionId
     */
    private void handlePurchasePack(String[] parts) {
        if (parts.length < 4) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid purchase request"
            ));
            return;
        }

        String userId = parts[1];
        String packId = parts[2];
        String transactionId = parts[3];

        System.out.println("→ Processing pack purchase: " + packId + " for user: " + userId);

        // Verify user
        if (!userId.equals(clientHandler.getUserId())) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "User ID mismatch"
            ));
            return;
        }

        // Check if pack exists
        StickerPack pack = StickerDAO.getPackById(packId);
        if (pack == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "Sticker pack not found"
            ));
            return;
        }

        // Check if user already owns pack
        if (StickerDAO.userOwnsPack(userId, packId)) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "You already own this pack"
            ));
            return;
        }

        // Grant pack to user
        boolean granted = StickerDAO.grantPackToUser(userId, packId, pack.getPrice(), transactionId);

        if (granted) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Pack acquired successfully",
                    buildPackData(pack)
            ));
            System.out.println("✅ Pack granted: " + packId + " -> " + userId);
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to acquire pack"
            ));
        }
    }

    /**
     * Get user's owned sticker packs
     * Format: STICKER_GET_USER_PACKS|||userId
     */
    private void handleGetUserPacks(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Missing user ID"
            ));
            return;
        }

        String userId = parts[1];
        System.out.println("→ Getting packs for user: " + userId);

        List<StickerPack> packs = StickerDAO.getUserPacks(userId);

        if (packs.isEmpty()) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "No sticker packs owned",
                    ""
            ));
            return;
        }

        // Build response data
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < packs.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);
            data.append(buildPackData(packs.get(i)));
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "User packs retrieved: " + packs.size(),
                data.toString()
        ));

        System.out.println("✅ Sent " + packs.size() + " user packs");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build pack data string
     * Format: packId,name,thumbnailUrl,author,isPremium,price,description,downloadCount
     */
    private String buildPackData(StickerPack pack) {
        return String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s",
                pack.getPackId(),
                Protocol.LIST_DELIMITER,
                pack.getName(),
                Protocol.LIST_DELIMITER,
                pack.getThumbnailUrl() != null ? pack.getThumbnailUrl() : "",
                Protocol.LIST_DELIMITER,
                pack.getAuthor() != null ? pack.getAuthor() : "",
                Protocol.LIST_DELIMITER,
                pack.isPremium(),
                Protocol.LIST_DELIMITER,
                pack.getPrice(),
                Protocol.LIST_DELIMITER,
                pack.getDescription() != null ? pack.getDescription() : "",
                Protocol.LIST_DELIMITER,
                pack.getDownloadCount()
        );
    }

    /**
     * Build sticker data string
     * Format: stickerId,packId,imageUrl,name,tags,orderIndex,isAnimated
     */
    private String buildStickerData(Map<String, String> sticker) {
        return String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s",
                sticker.get("stickerId"),
                Protocol.LIST_DELIMITER,
                sticker.get("packId"),
                Protocol.LIST_DELIMITER,
                sticker.get("imageUrl"),
                Protocol.LIST_DELIMITER,
                sticker.get("name"),
                Protocol.LIST_DELIMITER,
                sticker.get("tags"),
                Protocol.LIST_DELIMITER,
                sticker.get("orderIndex"),
                Protocol.LIST_DELIMITER,
                sticker.get("isAnimated")
        );
    }
}