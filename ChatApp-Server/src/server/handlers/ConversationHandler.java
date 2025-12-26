package server.handlers;

import database.dao.ConversationDAO;
import database.dao.ConversationDeletionDAO;
import database.dao.UserDAO;
import database.dao.MessageDAO;
import models.Conversation;
import models.User;
import models.Message;
import protocol.Protocol;
import server.ClientHandler;

import java.util.List;
import java.util.ArrayList;

/**
 * Handler for conversation operations
 * ✅ Fixed version
 */
public class ConversationHandler {

    private final ClientHandler clientHandler;

    public ConversationHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    /**
     * Main handler - route commands to appropriate methods
     */
    public void handle(String command, String[] parts) {
        switch (command) {
            case Protocol.CONVERSATION_GET_ALL:
                handleGetAllConversations(parts);
                break;
            case Protocol.CONVERSATION_GET:
                handleGetConversation(parts);
                break;
            case Protocol.CONVERSATION_GET_BY_ID:
                handleGetConversationById(parts);
                break;
            case Protocol.CONVERSATION_CREATE:
                handleCreateConversation(parts);
                break;
            case Protocol.CONVERSATION_CREATE_GROUP:
                handleCreateGroupConversation(parts);
                break;
            case Protocol.CONVERSATION_DELETE:
                handleDeleteConversation(parts);
                break;
            case Protocol.CONVERSATION_DELETE_FOR_USER:  // ✅ THÊM CASE MỚI
                handleDeleteConversationForUser(parts);
                break;
            case Protocol.CONVERSATION_MUTE:
                handleMuteConversation(parts);
                break;
            case Protocol.CONVERSATION_UNMUTE:
                handleUnmuteConversation(parts);
                break;
            case Protocol.CONVERSATION_PIN:
                handlePinConversation(parts);
                break;
            case Protocol.CONVERSATION_ARCHIVE:
                handleArchiveConversation(parts);
                break;
            case Protocol.CONVERSATION_UNARCHIVE:
                handleUnarchiveConversation(parts);
                break;
            default:
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Unknown conversation command: " + command
                ));
        }
    }

    // ==================== GET ALL CONVERSATIONS ====================

    private void handleGetAllConversations(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing userId"
            ));
            return;
        }

        String userId = parts[1];
        System.out.println("→ Getting all conversations for user: " + userId);

        List<Conversation> conversations = ConversationDAO.getUserConversations(userId);

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < conversations.size(); i++) {
            if (i > 0) {
                data.append(Protocol.FIELD_DELIMITER);
            }

            Conversation conv = conversations.get(i);
            data.append(buildConversationData(conv, userId));
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversations retrieved successfully",
                data.toString()
        ));

        System.out.println("✅ Sent " + conversations.size() + " conversations to user: " + userId);
    }

    // ==================== GET CONVERSATION ====================

    private void handleGetConversation(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing userId or otherUserId"
            ));
            return;
        }

        String userId = parts[1];
        String otherUserId = parts[2];

        System.out.println("→ Getting conversation between " + userId + " and " + otherUserId);

        Conversation conversation = ConversationDAO.findPrivateConversation(userId, otherUserId);

        if (conversation == null) {
            System.out.println("  → Conversation not found, creating new one...");
            conversation = ConversationDAO.createPrivateConversation(userId, otherUserId);
        }

        if (conversation != null) {
            String conversationData = buildConversationData(conversation, userId);
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation retrieved successfully",
                    conversationData
            ));
            System.out.println("✅ Conversation sent: " + conversation.getConversationId());
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to get or create conversation"
            ));
            System.err.println("❌ Failed to get/create conversation");
        }
    }

    // ==================== GET CONVERSATION BY ID ====================

    private void handleGetConversationById(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing conversationId"
            ));
            return;
        }

        String conversationId = parts[1];
        System.out.println("→ Getting conversation by ID: " + conversationId);

        Conversation conversation = ConversationDAO.findById(conversationId);

        if (conversation != null) {
            String conversationData = buildConversationData(conversation, clientHandler.getUserId());
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation retrieved successfully",
                    conversationData
            ));
            System.out.println("✅ Conversation sent: " + conversationId);
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "Conversation not found: " + conversationId
            ));
            System.err.println("❌ Conversation not found: " + conversationId);
        }
    }

    // ==================== CREATE CONVERSATION ====================

    private void handleCreateConversation(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing userId or otherUserId"
            ));
            return;
        }

        String userId = parts[1];
        String otherUserId = parts[2];

        System.out.println("→ Creating conversation between " + userId + " and " + otherUserId);

        Conversation existing = ConversationDAO.findPrivateConversation(userId, otherUserId);
        if (existing != null) {
            System.out.println("  → Conversation already exists: " + existing.getConversationId());

            boolean wasDeleted = ConversationDeletionDAO.isConversationDeletedByUser(
                    existing.getConversationId(), userId
            );

            if (wasDeleted) {
                System.out.println("  → Restoring deleted conversation for user");
                ConversationDeletionDAO.restoreConversationForUser(
                        existing.getConversationId(), userId
                );
            }

            String conversationData = buildConversationData(existing, userId);
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation already exists",
                    conversationData
            ));
            return;
        }

        Conversation conversation = ConversationDAO.createPrivateConversation(userId, otherUserId);

        if (conversation != null) {
            String conversationData = buildConversationData(conversation, userId);
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation created successfully",
                    conversationData
            ));

            ClientHandler otherHandler = clientHandler.getServer().getClientHandler(otherUserId);
            if (otherHandler != null) {
                String otherData = buildConversationData(conversation, otherUserId);
                otherHandler.sendMessage(Protocol.buildRequest(
                        Protocol.CONVERSATION_CREATE,
                        otherData
                ));
                System.out.println("  → Notified other user: " + otherUserId);
            }

            System.out.println("✅ Conversation created: " + conversation.getConversationId());
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to create conversation"
            ));
            System.err.println("❌ Failed to create conversation");
        }
    }

    // ==================== CREATE GROUP CONVERSATION ====================

    private void handleCreateGroupConversation(String[] parts) {
        if (parts.length < 4) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing groupName, creatorId, or memberIds"
            ));
            return;
        }

        String groupName = parts[1];
        String creatorId = parts[2];
        String memberIdsStr = parts[3];

        System.out.println("→ Creating group: " + groupName + " by " + creatorId);

        String[] memberIdArray = memberIdsStr.split(Protocol.LIST_DELIMITER);
        List<String> memberIds = new ArrayList<>();
        for (String memberId : memberIdArray) {
            if (memberId != null && !memberId.trim().isEmpty()) {
                memberIds.add(memberId.trim());
            }
        }

        System.out.println("  → Members: " + memberIds);

        Conversation conversation = ConversationDAO.createGroupConversation(
                groupName, creatorId, memberIds);

        if (conversation != null) {
            String conversationData = buildConversationData(conversation, creatorId);
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Group conversation created successfully",
                    conversationData
            ));

            for (String memberId : conversation.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    ClientHandler memberHandler = clientHandler.getServer()
                            .getClientHandler(memberId);
                    if (memberHandler != null) {
                        String memberData = buildConversationData(conversation, memberId);
                        memberHandler.sendMessage(Protocol.buildRequest(
                                Protocol.CONVERSATION_CREATE_GROUP,
                                memberData
                        ));
                        System.out.println("  → Notified member: " + memberId);
                    }
                }
            }

            System.out.println("✅ Group conversation created: " + groupName);
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to create group conversation"
            ));
            System.err.println("❌ Failed to create group conversation");
        }
    }

    // ==================== DELETE CONVERSATION (ADMIN) ====================

    private void handleDeleteConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing conversationId"
            ));
            return;
        }

        String conversationId = parts[1];
        System.out.println("→ Deleting conversation (admin): " + conversationId);

        if (ConversationDAO.deleteConversation(conversationId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation deleted successfully"
            ));
            System.out.println("✅ Conversation deleted: " + conversationId);

            Conversation conv = ConversationDAO.findById(conversationId);
            if (conv != null && conv.getMemberIds() != null) {
                for (String memberId : conv.getMemberIds()) {
                    ClientHandler memberHandler = clientHandler.getServer()
                            .getClientHandler(memberId);
                    if (memberHandler != null) {
                        memberHandler.sendMessage(Protocol.buildRequest(
                                Protocol.CONVERSATION_DELETE,
                                conversationId
                        ));
                    }
                }
            }
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to delete conversation"
            ));
            System.err.println("❌ Failed to delete conversation: " + conversationId);
        }
    }

    // ==================== DELETE CONVERSATION FOR USER ====================

    /**
     * ✅ Xóa conversation CHỈ cho user cụ thể
     * User khác vẫn thấy conversation bình thường
     */
    private void handleDeleteConversationForUser(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing conversationId or userId"
            ));
            return;
        }

        String conversationId = parts[1];
        String userId = parts[2];

        System.out.println("→ Deleting conversation for user:");
        System.out.println("  → ConversationID: " + conversationId);
        System.out.println("  → UserID: " + userId);

        try {
            // Kiểm tra conversation có tồn tại không
            Conversation conv = ConversationDAO.findById(conversationId);
            if (conv == null) {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_NOT_FOUND,
                        "Conversation not found"
                ));
                System.err.println("❌ Conversation not found: " + conversationId);
                return;
            }

            // Kiểm tra user có phải là member không
            if (!conv.hasMember(userId)) {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.UNAUTHORIZED,
                        "User is not a member of this conversation"
                ));
                System.err.println("❌ User is not a member: " + userId);
                return;
            }

            // Đánh dấu conversation đã bị xóa bởi user này
            boolean success = ConversationDAO.deleteConversationForUser(conversationId, userId);

            if (success) {
                clientHandler.sendMessage(Protocol.buildSuccessResponse(
                        "Conversation deleted successfully"
                ));

                System.out.println("✅ Conversation deleted for user successfully");
                System.out.println("  → Other users are NOT affected");

                // KHÔNG broadcast vì chỉ ảnh hưởng đến user này

            } else {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_DATABASE_ERROR,
                        "Failed to delete conversation"
                ));
                System.err.println("❌ Failed to delete conversation for user");
            }

        } catch (Exception e) {
            System.err.println("❌ Error handling delete conversation for user: " + e.getMessage());
            e.printStackTrace();
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Server error: " + e.getMessage()
            ));
        }
    }

    // ==================== MUTE/UNMUTE/PIN/ARCHIVE ====================

    private void handleMuteConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing conversationId"
            ));
            return;
        }

        String conversationId = parts[1];
        System.out.println("→ Muting conversation: " + conversationId);

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation muted successfully"
        ));
        System.out.println("✅ Conversation muted: " + conversationId);
    }

    private void handleUnmuteConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing conversationId"
            ));
            return;
        }

        String conversationId = parts[1];
        System.out.println("→ Unmuting conversation: " + conversationId);

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation unmuted successfully"
        ));
        System.out.println("✅ Conversation unmuted: " + conversationId);
    }

    private void handlePinConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing conversationId"
            ));
            return;
        }

        String conversationId = parts[1];
        System.out.println("→ Pinning conversation: " + conversationId);

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation pinned successfully"
        ));
        System.out.println("✅ Conversation pinned: " + conversationId);
    }

    private void handleArchiveConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing conversationId"
            ));
            return;
        }

        String conversationId = parts[1];
        System.out.println("→ Archiving conversation: " + conversationId);

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation archived successfully"
        ));
        System.out.println("✅ Conversation archived: " + conversationId);
    }

    private void handleUnarchiveConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request: missing conversationId"
            ));
            return;
        }

        String conversationId = parts[1];
        System.out.println("→ Unarchiving conversation: " + conversationId);

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation unarchived successfully"
        ));
        System.out.println("✅ Conversation unarchived: " + conversationId);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build conversation data string
     * Format: conversationId,type,name,avatar,lastMsg,lastMsgTime,unread,memberCount,isOnline,lastSeen,memberIds
     */
    private String buildConversationData(Conversation conversation, String currentUserId) {
        StringBuilder data = new StringBuilder();

        data.append(conversation.getConversationId())
                .append(Protocol.LIST_DELIMITER);

        data.append(conversation.getType())
                .append(Protocol.LIST_DELIMITER);

        String otherUserId = null;

        if (conversation.isPrivate()) {
            for (String memberId : conversation.getMemberIds()) {
                if (!memberId.equals(currentUserId)) {
                    otherUserId = memberId;
                    break;
                }
            }

            if (otherUserId != null) {
                User otherUser = UserDAO.findById(otherUserId);
                if (otherUser != null) {
                    data.append(otherUser.getDisplayName())
                            .append(Protocol.LIST_DELIMITER);
                    data.append(otherUser.getAvatarUrl() != null ? otherUser.getAvatarUrl() : "");
                } else {
                    data.append("Unknown")
                            .append(Protocol.LIST_DELIMITER)
                            .append("");
                }
            } else {
                data.append("Unknown")
                        .append(Protocol.LIST_DELIMITER)
                        .append("");
            }
        } else {
            data.append(conversation.getName() != null ? conversation.getName() : "Group Chat")
                    .append(Protocol.LIST_DELIMITER);
            data.append(conversation.getAvatarUrl() != null ? conversation.getAvatarUrl() : "");
        }

        data.append(Protocol.LIST_DELIMITER);

        data.append(conversation.getLastMessage() != null ? conversation.getLastMessage() : "")
                .append(Protocol.LIST_DELIMITER);

        data.append(conversation.getLastMessageTime() != null ?
                        conversation.getLastMessageTime().toString() : "")
                .append(Protocol.LIST_DELIMITER);

        int unreadCount = MessageDAO.getUnreadCount(conversation.getConversationId(), currentUserId);
        data.append(unreadCount)
                .append(Protocol.LIST_DELIMITER);

        data.append(conversation.getMemberCount())
                .append(Protocol.LIST_DELIMITER);

        boolean isOnline = false;
        String lastSeenStr = "";

        if (conversation.isPrivate() && otherUserId != null) {
            isOnline = clientHandler.getServer().isClientOnline(otherUserId);

            User otherUser = UserDAO.findById(otherUserId);
            if (otherUser != null) {
                if (!isOnline) {
                    isOnline = otherUser.isOnline();
                }

                if (otherUser.getLastSeen() != null) {
                    lastSeenStr = otherUser.getLastSeen().toString();
                }
            }
        }

        data.append(isOnline)
                .append(Protocol.LIST_DELIMITER);

        data.append(lastSeenStr)
                .append(Protocol.LIST_DELIMITER);

        if (conversation.getMemberIds() != null && !conversation.getMemberIds().isEmpty()) {
            data.append(String.join(";", conversation.getMemberIds()));
        } else {
            data.append("");
        }

        String result = data.toString();
        System.out.println("  → Built conversation data: " + result);

        return result;
    }
}