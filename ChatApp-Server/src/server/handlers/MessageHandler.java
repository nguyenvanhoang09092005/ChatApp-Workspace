package server.handlers;

import database.dao.MessageDAO;
import database.dao.ConversationDAO;
import database.dao.UserDAO;
import models.Message;
import models.Conversation;
import models.User;
import protocol.Protocol;
import server.ClientHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handler for message operations
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

        // Get conversation and validate membership
        Conversation conversation = ConversationDAO.findById(conversationId);
        if (conversation == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Conversation not found"
            ));
            return;
        }

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
        message.setMessageType(messageType);
        message.setSenderName(sender.getDisplayName());
        message.setSenderAvatar(sender.getAvatarUrl());
        message.setMediaUrl(mediaUrl);
        message.setFileName(fileName);

        try {
            message.setFileSize(Long.parseLong(fileSize));
        } catch (NumberFormatException e) {
            message.setFileSize(0);
        }

        if (replyToId != null && !replyToId.isEmpty()) {
            message.setReplyToMessageId(replyToId);
        }

        // Save to database
        if (MessageDAO.createMessage(message)) {

            // Build message data
            String messageData = buildMessageData(message);

            // Send to sender (confirmation)
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Message sent",
                    messageData
            ));

            // Broadcast to all members except sender
            broadcastMessage(conversation, message, senderId);

            System.out.println("✓ Message sent: " + conversationId + " from " + senderId);
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to send message"
            ));
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

        List<Message> messages = MessageDAO.getMessagesPaginated(conversationId, offset, limit);

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);
            data.append(buildMessageData(messages.get(i)));
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Message history retrieved",
                data.toString()
        ));
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

        // Mark all messages as read
        MessageDAO.markAllAsRead(conversationId, userId);

        // Notify sender(s) about read status
        List<Message> unreadMessages = MessageDAO.getUnreadMessages(conversationId, userId);
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

        // Create forwarded message
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

        // TODO: Implement reaction storage
        // For now, just broadcast to conversation members

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
                message.getMessageType(),
                Protocol.LIST_DELIMITER,
                message.getMediaUrl() != null ? message.getMediaUrl() : "",
                Protocol.LIST_DELIMITER,
                message.getTimestamp().toString(),
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
                message.getContent(),
                message.getMessageType(),
                message.getMediaUrl() != null ? message.getMediaUrl() : "",
                message.getSenderName(),
                message.getSenderAvatar() != null ? message.getSenderAvatar() : ""
        );

        for (String memberId : conversation.getMemberIds()) {
            if (!memberId.equals(excludeUserId)) {
                ClientHandler handler = clientHandler.getServer().getClientHandler(memberId);
                if (handler != null) {
                    handler.sendMessage(broadcastMsg);
                }
            }
        }
    }
}