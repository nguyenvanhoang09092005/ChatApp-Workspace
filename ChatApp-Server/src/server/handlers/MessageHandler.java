package server.handlers;

import database.dao.MessageDAO;
import database.dao.ConversationDAO;
import database.dao.UserDAO;
import database.dao.StickerDAO;
import models.Message;
import models.Conversation;
import models.User;
import protocol.Protocol;
import server.ClientHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handler for message operations - Complete with Sticker/Emoji support
 */
public class MessageHandler {

    private final ClientHandler clientHandler;

    public MessageHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void handle(String command, String[] parts) {
        switch (command) {
            case Protocol.MESSAGE_SEND:
                handleSendMessage(parts);
                break;
            case Protocol.MESSAGE_GET_HISTORY:
                handleGetHistory(parts);
                break;
            case Protocol.MESSAGE_MARK_READ:
                handleMarkRead(parts);
                break;
            case Protocol.MESSAGE_EDIT:
                handleEditMessage(parts);
                break;
            case Protocol.MESSAGE_RECALL:
                handleRecallMessage(parts);
                break;
            case Protocol.MESSAGE_DELETE:
                handleDeleteMessage(parts);
                break;
            case Protocol.MESSAGE_FORWARD:
                handleForwardMessage(parts);
                break;
            case Protocol.MESSAGE_REACT:
                handleReactMessage(parts);
                break;
            case Protocol.TYPING_START:
                handleTypingStart(parts);
                break;
            case Protocol.TYPING_STOP:
                handleTypingStop(parts);
                break;
            default:
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Unknown message command"
                ));
        }
    }

    // ==================== SEND MESSAGE ====================

    /**
     * Handle MESSAGE_SEND with full support for text, image, file, sticker, emoji
     * Format: MESSAGE_SEND|||conversationId|||senderId|||content|||type|||replyToId|||mediaUrl|||fileName|||fileSize
     */
    private void handleSendMessage(String[] parts) {
        if (parts.length < 5) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid message data"
            ));
            return;
        }

        String conversationId = parts[1];
        String senderId = parts[2];
        String content = parts[3];
        String messageType = parts.length > 4 ? parts[4] : Message.TYPE_TEXT;
        String replyToId = parts.length > 5 ? parts[5] : null;
        String mediaUrl = parts.length > 6 ? parts[6] : null;
        String fileName = parts.length > 7 ? parts[7] : null;
        String fileSize = parts.length > 8 ? parts[8] : "0";

        System.out.println("→ Processing message - Type: " + messageType + ", Conv: " + conversationId);

        // Validate conversation
        Conversation conversation = ConversationDAO.findById(conversationId);
        if (conversation == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Conversation not found"
            ));
            return;
        }

        // Validate membership
        if (!conversation.hasMember(senderId)) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.FORBIDDEN,
                    "You are not a member of this conversation"
            ));
            return;
        }

        // Get sender info
        User sender = UserDAO.findById(senderId);
        if (sender == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Sender not found"
            ));
            return;
        }

        // Create message
        Message message = new Message(conversationId, senderId, content);
        message.setSenderName(sender.getDisplayName());
        message.setSenderAvatar(sender.getAvatarUrl());
        message.setTimestamp(LocalDateTime.now());

        // ==================== HANDLE MESSAGE TYPES ====================

        if ("sticker".equalsIgnoreCase(messageType) || "STICKER".equals(messageType)) {
            // STICKER MESSAGE
            message.setMessageType(Message.TYPE_STICKER);
            message.setMediaUrl(mediaUrl);
            message.setFileName(fileName);
            message.setFileSize(0); // Stickers have no file size

            // Track sticker usage for analytics
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                String stickerId = extractStickerIdFromUrl(mediaUrl);
                if (stickerId != null) {
                    StickerDAO.trackStickerUsage(senderId, stickerId);
                }
            }

            System.out.println("  → Sticker message: " + fileName);

        } else if ("emoji".equalsIgnoreCase(messageType) || "EMOJI".equals(messageType)) {
            // EMOJI MESSAGE (large single emoji)
            message.setMessageType(Message.TYPE_EMOJI);
            System.out.println("  → Emoji message: " + content);

        } else if ("image".equalsIgnoreCase(messageType) || "IMAGE".equals(messageType)) {
            // IMAGE MESSAGE
            message.setMessageType(Message.TYPE_IMAGE);
            message.setMediaUrl(mediaUrl);
            message.setFileName(fileName);
            try {
                message.setFileSize(Long.parseLong(fileSize));
            } catch (NumberFormatException e) {
                message.setFileSize(0);
            }
            System.out.println("  → Image message: " + fileName);

        } else if ("file".equalsIgnoreCase(messageType) || "FILE".equals(messageType)) {
            // FILE MESSAGE
            message.setMessageType(Message.TYPE_FILE);
            message.setMediaUrl(mediaUrl);
            message.setFileName(fileName);
            try {
                message.setFileSize(Long.parseLong(fileSize));
            } catch (NumberFormatException e) {
                message.setFileSize(0);
            }
            System.out.println("  → File message: " + fileName);

        } else if ("like".equalsIgnoreCase(messageType)) {
            // LIKE MESSAGE
            message.setMessageType(Message.TYPE_TEXT);
            message.setContent("👍");
            System.out.println("  → Like message");

        } else {
            // TEXT MESSAGE (default)
            message.setMessageType(Message.TYPE_TEXT);
            System.out.println("  → Text message");
        }

        // Set reply if exists
        if (replyToId != null && !replyToId.isEmpty()) {
            message.setReplyToMessageId(replyToId);
        }

        // Save to database
        if (MessageDAO.createMessage(message)) {
            System.out.println("  ✅ Message saved to DB: " + message.getMessageId());

            // Build message data for response
            String messageData = buildMessageData(message);

            // Send success confirmation to sender
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Message sent",
                    messageData
            ));

            // Broadcast to all conversation members (except sender)
            broadcastMessage(conversation, message, senderId);

            System.out.println("  ✅ Message broadcasted to " + conversation.getMemberIds().size() + " members");
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to send message"
            ));
        }
    }

    /**
     * Extract sticker ID from URL
     * Examples:
     * - https://example.com/stickers/pack_001/s001.png?sticker_id=s001
     * - https://example.com/stickers/s001.png
     */
    private String extractStickerIdFromUrl(String url) {
        try {
            // Method 1: Extract from query parameter
            if (url.contains("sticker_id=")) {
                String[] parts = url.split("sticker_id=");
                if (parts.length > 1) {
                    return parts[1].split("&")[0];
                }
            }

            // Method 2: Extract from filename
            String filename = url.substring(url.lastIndexOf('/') + 1);
            String filenameWithoutExt = filename.split("\\.")[0];

            // Validate format (should start with 's' followed by numbers)
            if (filenameWithoutExt.matches("s\\d+")) {
                return filenameWithoutExt;
            }

            return null;
        } catch (Exception e) {
            System.err.println("⚠️ Error extracting sticker ID: " + e.getMessage());
            return null;
        }
    }

    // ==================== GET HISTORY ====================
    private void handleGetHistory(String[] parts) {
        if (parts.length < 4) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];
        int offset = Integer.parseInt(parts[2]);
        int limit = Integer.parseInt(parts[3]);
        String userId = clientHandler.getUserId();

        List<Message> messages = MessageDAO.getMessagesPaginatedForUser(
                conversationId, offset, limit, userId
        );

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);
            data.append(buildMessageData(messages.get(i)));
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Message history retrieved",
                data.toString()
        ));

        System.out.println("✅ Sent " + messages.size() + " messages (filtered by deletion timestamp)");
    }

    // ==================== MARK READ ====================

    private void handleMarkRead(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String conversationId = parts[1];
        String userId = parts[2];

        // Get unread messages before marking
        List<Message> unreadMessages = MessageDAO.getUnreadMessages(conversationId, userId);

        // Mark all messages as read
        MessageDAO.markAllAsRead(conversationId, userId);

        // Notify senders about read status
        for (Message msg : unreadMessages) {
            ClientHandler senderHandler = clientHandler.getServer()
                    .getClientHandler(msg.getSenderId());

            if (senderHandler != null) {
                senderHandler.sendMessage(Protocol.buildRequest(
                        Protocol.MESSAGE_READ,
                        msg.getMessageId(),
                        userId
                ));
            }
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Messages marked as read"
        ));
    }

    // ==================== EDIT MESSAGE ====================

    private void handleEditMessage(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String messageId = parts[1];
        String newContent = parts[2];

        Message message = MessageDAO.findById(messageId);
        if (message == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "Message not found"
            ));
            return;
        }

        // Check ownership
        if (!message.getSenderId().equals(clientHandler.getUserId())) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.FORBIDDEN,
                    "You can only edit your own messages"
            ));
            return;
        }

        // Cannot edit sticker or emoji messages
        if (Message.TYPE_STICKER.equals(message.getMessageType()) ||
                Message.TYPE_EMOJI.equals(message.getMessageType())) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.FORBIDDEN,
                    "Cannot edit sticker or emoji messages"
            ));
            return;
        }

        // Update message
        if (MessageDAO.editMessage(messageId, newContent)) {
            message.editContent(newContent);

            // Broadcast update to conversation members
            Conversation conversation = ConversationDAO.findById(message.getConversationId());
            if (conversation != null) {
                String updateMsg = Protocol.buildRequest(
                        Protocol.MESSAGE_EDIT,
                        messageId,
                        newContent
                );

                for (String memberId : conversation.getMemberIds()) {
                    ClientHandler handler = clientHandler.getServer().getClientHandler(memberId);
                    if (handler != null) {
                        handler.sendMessage(updateMsg);
                    }
                }
            }

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Message edited"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to edit message"
            ));
        }
    }

    // ==================== RECALL MESSAGE ====================

    private void handleRecallMessage(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String messageId = parts[1];

        Message message = MessageDAO.findById(messageId);
        if (message == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "Message not found"
            ));
            return;
        }

        // Check ownership
        if (!message.getSenderId().equals(clientHandler.getUserId())) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.FORBIDDEN,
                    "You can only recall your own messages"
            ));
            return;
        }

        // Recall message
        if (MessageDAO.recallMessage(messageId)) {
            // Broadcast recall to conversation members
            Conversation conversation = ConversationDAO.findById(message.getConversationId());
            if (conversation != null) {
                String recallMsg = Protocol.buildRequest(
                        Protocol.MESSAGE_RECALL,
                        messageId
                );

                for (String memberId : conversation.getMemberIds()) {
                    ClientHandler handler = clientHandler.getServer().getClientHandler(memberId);
                    if (handler != null) {
                        handler.sendMessage(recallMsg);
                    }
                }
            }

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Message recalled"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to recall message"
            ));
        }
    }

    // ==================== DELETE MESSAGE ====================

    private void handleDeleteMessage(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String messageId = parts[1];

        if (MessageDAO.deleteMessage(messageId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Message deleted"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to delete message"
            ));
        }
    }

    // ==================== FORWARD MESSAGE ====================

    private void handleForwardMessage(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String messageId = parts[1];
        String targetConversationId = parts[2];

        Message originalMessage = MessageDAO.findById(messageId);
        if (originalMessage == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "Message not found"
            ));
            return;
        }

        // Create forwarded message (preserving type and media)
        Message forwardedMessage = new Message(
                targetConversationId,
                clientHandler.getUserId(),
                originalMessage.getContent()
        );
        forwardedMessage.setMessageType(originalMessage.getMessageType());
        forwardedMessage.setMediaUrl(originalMessage.getMediaUrl());
        forwardedMessage.setFileName(originalMessage.getFileName());
        forwardedMessage.setFileSize(originalMessage.getFileSize());

        if (MessageDAO.createMessage(forwardedMessage)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Message forwarded"
            ));

            // Broadcast to target conversation
            Conversation targetConv = ConversationDAO.findById(targetConversationId);
            if (targetConv != null) {
                broadcastMessage(targetConv, forwardedMessage, null);
            }
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to forward message"
            ));
        }
    }

    // ==================== REACT TO MESSAGE ====================

    private void handleReactMessage(String[] parts) {
        if (parts.length < 4) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String messageId = parts[1];
        String userId = parts[2];
        String emoji = parts[3];


        Message message = MessageDAO.findById(messageId);
        if (message != null) {
            Conversation conversation = ConversationDAO.findById(message.getConversationId());
            if (conversation != null) {
                String reactionMsg = Protocol.buildRequest(
                        Protocol.MESSAGE_REACT,
                        messageId,
                        userId,
                        emoji
                );

                for (String memberId : conversation.getMemberIds()) {
                    ClientHandler handler = clientHandler.getServer().getClientHandler(memberId);
                    if (handler != null) {
                        handler.sendMessage(reactionMsg);
                    }
                }
            }
        }
    }

    // ==================== TYPING INDICATORS ====================

    private void handleTypingStart(String[] parts) {
        if (parts.length < 3) return;

        String conversationId = parts[1];
        String userId = parts[2];

        // Broadcast to conversation members
        Conversation conversation = ConversationDAO.findById(conversationId);
        if (conversation != null) {
            String typingMsg = Protocol.buildRequest(
                    Protocol.TYPING_START,
                    conversationId,
                    userId
            );

            for (String memberId : conversation.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    ClientHandler handler = clientHandler.getServer().getClientHandler(memberId);
                    if (handler != null) {
                        handler.sendMessage(typingMsg);
                    }
                }
            }
        }
    }

    private void handleTypingStop(String[] parts) {
        if (parts.length < 3) return;

        String conversationId = parts[1];
        String userId = parts[2];

        // Broadcast to conversation members
        Conversation conversation = ConversationDAO.findById(conversationId);
        if (conversation != null) {
            String typingMsg = Protocol.buildRequest(
                    Protocol.TYPING_STOP,
                    conversationId,
                    userId
            );

            for (String memberId : conversation.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    ClientHandler handler = clientHandler.getServer().getClientHandler(memberId);
                    if (handler != null) {
                        handler.sendMessage(typingMsg);
                    }
                }
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private String buildMessageData(Message message) {
        return String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s",
                message.getMessageId(),
                Protocol.LIST_DELIMITER,
                message.getSenderId(),
                Protocol.LIST_DELIMITER,
                message.getSenderName() != null ? message.getSenderName() : "",
                Protocol.LIST_DELIMITER,
                message.getContent() != null ? message.getContent() : "",
                Protocol.LIST_DELIMITER,
                message.getMessageType() != null ? message.getMessageType() : Message.TYPE_TEXT,
                Protocol.LIST_DELIMITER,
                message.getMediaUrl() != null ? message.getMediaUrl() : "",
                Protocol.LIST_DELIMITER,
                message.getTimestamp() != null ? message.getTimestamp().toString() : "",
                Protocol.LIST_DELIMITER,
                message.isRead(),
                Protocol.LIST_DELIMITER,
                message.getSenderAvatar() != null ? message.getSenderAvatar() : "",
                Protocol.LIST_DELIMITER,
                message.getFileName() != null ? message.getFileName() : "",
                Protocol.LIST_DELIMITER,
                message.getFileSize()
        );
    }

    private void broadcastMessage(Conversation conversation, Message message, String excludeUserId) {
        String broadcastMsg = Protocol.buildRequest(
                Protocol.MESSAGE_RECEIVE,
                message.getMessageId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getContent() != null ? message.getContent() : "",
                message.getMessageType() != null ? message.getMessageType() : Message.TYPE_TEXT,
                message.getMediaUrl() != null ? message.getMediaUrl() : "",
                message.getSenderName() != null ? message.getSenderName() : "",
                message.getSenderAvatar() != null ? message.getSenderAvatar() : "",
                message.getFileName() != null ? message.getFileName() : "",
                String.valueOf(message.getFileSize())
        );

        System.out.println("  → Broadcasting: " + broadcastMsg);

        for (String memberId : conversation.getMemberIds()) {
            if (excludeUserId == null || !memberId.equals(excludeUserId)) {
                ClientHandler handler = clientHandler.getServer().getClientHandler(memberId);
                if (handler != null) {
                    handler.sendMessage(broadcastMsg);
                    System.out.println("    ✓ Sent to: " + memberId);
                }
            }
        }
    }
}