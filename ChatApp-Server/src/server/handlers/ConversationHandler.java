package server.handlers;

import database.dao.ConversationDAO;
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
 */
public class ConversationHandler {

    private final ClientHandler clientHandler;

    public ConversationHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

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
                        "Unknown conversation command"
                ));
        }
    }

    // ==================== GET ALL CONVERSATIONS ====================

    private void handleGetAllConversations(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        List<Conversation> conversations = ConversationDAO.getUserConversations(userId);

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < conversations.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);

            Conversation conv = conversations.get(i);
            data.append(buildConversationData(conv, userId));
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversations retrieved",
                data.toString()
        ));

        System.out.println("✓ Sent " + conversations.size() + " conversations to user: " + userId);
    }

    // ==================== GET CONVERSATION ====================

    private void handleGetConversation(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        String otherUserId = parts[2];

        // Find or create private conversation
        Conversation conversation = ConversationDAO.findPrivateConversation(userId, otherUserId);

        if (conversation == null) {
            // Create new private conversation
            conversation = ConversationDAO.createPrivateConversation(userId, otherUserId);
        }

        if (conversation != null) {
            String conversationData = buildConversationData(conversation, userId);
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation retrieved",
                    conversationData
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to get or create conversation"
            ));
        }
    }

    // ==================== GET CONVERSATION BY ID ====================

    private void handleGetConversationById(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];
        Conversation conversation = ConversationDAO.findById(conversationId);

        if (conversation != null) {
            String conversationData = buildConversationData(conversation, clientHandler.getUserId());
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation retrieved",
                    conversationData
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "Conversation not found"
            ));
        }
    }

    // ==================== CREATE CONVERSATION ====================

    private void handleCreateConversation(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        String otherUserId = parts[2];

        // Check if conversation already exists
        Conversation existing = ConversationDAO.findPrivateConversation(userId, otherUserId);
        if (existing != null) {
            String conversationData = buildConversationData(existing, userId);
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation already exists",
                    conversationData
            ));
            return;
        }

        // Create new private conversation
        Conversation conversation = ConversationDAO.createPrivateConversation(userId, otherUserId);

        if (conversation != null) {
            String conversationData = buildConversationData(conversation, userId);
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation created",
                    conversationData
            ));

            // Notify other user
            ClientHandler otherHandler = clientHandler.getServer().getClientHandler(otherUserId);
            if (otherHandler != null) {
                otherHandler.sendMessage(Protocol.buildRequest(
                        Protocol.CONVERSATION_CREATE,
                        buildConversationData(conversation, otherUserId)
                ));
            }
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to create conversation"
            ));
        }
    }

    // ==================== CREATE GROUP CONVERSATION ====================

    private void handleCreateGroupConversation(String[] parts) {
        if (parts.length < 4) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String groupName = parts[1];
        String creatorId = parts[2];
        String memberIdsStr = parts[3];

        // Parse member IDs
        String[] memberIdArray = memberIdsStr.split(Protocol.LIST_DELIMITER);
        List<String> memberIds = new ArrayList<>();
        for (String memberId : memberIdArray) {
            if (memberId != null && !memberId.trim().isEmpty()) {
                memberIds.add(memberId.trim());
            }
        }

        // Create group conversation
        Conversation conversation = ConversationDAO.createGroupConversation(
                groupName, creatorId, memberIds);

        if (conversation != null) {
            String conversationData = buildConversationData(conversation, creatorId);
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Group conversation created",
                    conversationData
            ));

            // Notify all members
            for (String memberId : conversation.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    ClientHandler memberHandler = clientHandler.getServer()
                            .getClientHandler(memberId);
                    if (memberHandler != null) {
                        memberHandler.sendMessage(Protocol.buildRequest(
                                Protocol.CONVERSATION_CREATE_GROUP,
                                buildConversationData(conversation, memberId)
                        ));
                    }
                }
            }

            System.out.println("✓ Group conversation created: " + groupName);
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to create group conversation"
            ));
        }
    }

    // ==================== DELETE CONVERSATION ====================

    private void handleDeleteConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];

        if (ConversationDAO.deleteConversation(conversationId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Conversation deleted"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to delete conversation"
            ));
        }
    }

    // ==================== MUTE CONVERSATION ====================

    private void handleMuteConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];

        // TODO: Implement mute functionality in database
        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation muted"
        ));
    }

    // ==================== UNMUTE CONVERSATION ====================

    private void handleUnmuteConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];

        // TODO: Implement unmute functionality in database
        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation unmuted"
        ));
    }

    // ==================== PIN CONVERSATION ====================

    private void handlePinConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];

        // TODO: Implement pin functionality in database
        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation pinned"
        ));
    }

    // ==================== ARCHIVE CONVERSATION ====================

    private void handleArchiveConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];

        // TODO: Implement archive functionality in database
        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation archived"
        ));
    }

    // ==================== UNARCHIVE CONVERSATION ====================

    private void handleUnarchiveConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];

        // TODO: Implement unarchive functionality in database
        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Conversation unarchived"
        ));
    }

    // ==================== HELPER METHODS ====================

//    private String buildConversationData(Conversation conversation, String currentUserId) {
//        StringBuilder data = new StringBuilder();
//
//        data.append(conversation.getConversationId())
//                .append(Protocol.LIST_DELIMITER)
//                .append(conversation.getType())
//                .append(Protocol.LIST_DELIMITER);
//
//        // For private conversations, use other user's info
//        if (conversation.isPrivate()) {
//            String otherUserId = null;
//            for (String memberId : conversation.getMemberIds()) {
//                if (!memberId.equals(currentUserId)) {
//                    otherUserId = memberId;
//                    break;
//                }
//            }
//
//            if (otherUserId != null) {
//                User otherUser = UserDAO.findById(otherUserId);
//                if (otherUser != null) {
//                    data.append(otherUser.getDisplayName())
//                            .append(Protocol.LIST_DELIMITER)
//                            .append(otherUser.getAvatarUrl() != null ? otherUser.getAvatarUrl() : "");
//                } else {
//                    data.append("Unknown")
//                            .append(Protocol.LIST_DELIMITER)
//                            .append("");
//                }
//            } else {
//                data.append("Unknown")
//                        .append(Protocol.LIST_DELIMITER)
//                        .append("");
//            }
//        } else {
//            // For group conversations, use group info
//            data.append(conversation.getName() != null ? conversation.getName() : "Group Chat")
//                    .append(Protocol.LIST_DELIMITER)
//                    .append(conversation.getAvatarUrl() != null ? conversation.getAvatarUrl() : "");
//        }
//
//        data.append(Protocol.LIST_DELIMITER)
//                .append(conversation.getLastMessage() != null ? conversation.getLastMessage() : "")
//                .append(Protocol.LIST_DELIMITER)
//                .append(conversation.getLastMessageTime() != null ?
//                        conversation.getLastMessageTime().toString() : "")
//                .append(Protocol.LIST_DELIMITER);
//
//        // Get unread count
//        int unreadCount = MessageDAO.getUnreadCount(conversation.getConversationId(), currentUserId);
//        data.append(unreadCount)
//                .append(Protocol.LIST_DELIMITER)
//                .append(conversation.getMemberCount())
//                .append(Protocol.LIST_DELIMITER)
//                .append(conversation.isActive());
//
//        return data.toString();
//    }

    private String buildConversationData(Conversation conversation, String currentUserId) {
        StringBuilder data = new StringBuilder();

        data.append(conversation.getConversationId())
                .append(Protocol.LIST_DELIMITER)
                .append(conversation.getType())
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
                            .append(Protocol.LIST_DELIMITER)
                            .append(otherUser.getAvatarUrl() != null ? otherUser.getAvatarUrl() : "");
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
                    .append(Protocol.LIST_DELIMITER)
                    .append(conversation.getAvatarUrl() != null ? conversation.getAvatarUrl() : "");
        }

        data.append(Protocol.LIST_DELIMITER)
                .append(conversation.getLastMessage() != null ? conversation.getLastMessage() : "")
                .append(Protocol.LIST_DELIMITER)
                .append(conversation.getLastMessageTime() != null ?
                        conversation.getLastMessageTime().toString() : "")
                .append(Protocol.LIST_DELIMITER);

        int unreadCount = MessageDAO.getUnreadCount(conversation.getConversationId(), currentUserId);
        data.append(unreadCount)
                .append(Protocol.LIST_DELIMITER)
                .append(conversation.getMemberCount())
                .append(Protocol.LIST_DELIMITER);

        // Add online status
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
                .append(Protocol.LIST_DELIMITER)
                .append(lastSeenStr)
                .append(Protocol.LIST_DELIMITER);

        // **SỬA: Dùng dấu ; để phân tách memberIds thay vì ,**
        if (conversation.getMemberIds() != null && !conversation.getMemberIds().isEmpty()) {
            data.append(String.join(";", conversation.getMemberIds())); // DÙNG ; thay vì ,
        } else {
            data.append("");
        }

        String result = data.toString();
        System.out.println("  → Đã build conversation data: " + result);

        return result;
    }
}