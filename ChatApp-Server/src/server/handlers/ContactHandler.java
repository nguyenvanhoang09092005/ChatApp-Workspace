package server.handlers;

import database.dao.ContactDAO;
import database.dao.UserDAO;
import database.dao.NotificationDAO;
import models.Contact;
import models.User;
import protocol.Protocol;
import server.ClientHandler;

import java.util.List;

/**
 * Handler for contact operations
 */
public class ContactHandler {

    private final ClientHandler clientHandler;

    public ContactHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void handle(String command, String[] parts) {
        switch (command) {
            case Protocol.CONTACT_GET_ALL:
                handleGetAllContacts(parts);
                break;
            case Protocol.CONTACT_ADD:
                handleAddContact(parts);
                break;
            case Protocol.CONTACT_REMOVE:
                handleRemoveContact(parts);
                break;
            case Protocol.CONTACT_BLOCK:
                handleBlockContact(parts);
                break;
            case Protocol.CONTACT_UNBLOCK:
                handleUnblockContact(parts);
                break;
            case Protocol.CONTACT_GET_BLOCKED:
                handleGetBlockedContacts(parts);
                break;
            case Protocol.CONTACT_REQUEST_SEND:
                handleSendFriendRequest(parts);
                break;
            case Protocol.CONTACT_REQUEST_ACCEPT:
                handleAcceptFriendRequest(parts);
                break;
            case Protocol.CONTACT_REQUEST_REJECT:
                handleRejectFriendRequest(parts);
                break;
            case Protocol.CONTACT_REQUEST_LIST:
                handleGetFriendRequests(parts);
                break;
            default:
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Unknown contact command"
                ));
        }
    }

    // ==================== GET ALL CONTACTS ====================

    private void handleGetAllContacts(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        List<Contact> contacts = ContactDAO.getFriends(userId);

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < contacts.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);

            Contact contact = contacts.get(i);
            User contactUser = UserDAO.findById(contact.getContactUserId());

            if (contactUser != null) {
                data.append(contact.getContactId())
                        .append(Protocol.LIST_DELIMITER)
                        .append(contact.getContactUserId())
                        .append(Protocol.LIST_DELIMITER)
                        .append(contactUser.getUsername())
                        .append(Protocol.LIST_DELIMITER)
                        .append(contactUser.getDisplayName())
                        .append(Protocol.LIST_DELIMITER)
                        .append(contactUser.getAvatarUrl() != null ? contactUser.getAvatarUrl() : "")
                        .append(Protocol.LIST_DELIMITER)
                        .append(contact.getNickname() != null ? contact.getNickname() : "")
                        .append(Protocol.LIST_DELIMITER)
                        .append(contact.isFavorite())
                        .append(Protocol.LIST_DELIMITER)
                        .append(contactUser.isOnline());
            }
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Contacts retrieved",
                data.toString()
        ));
    }

    // ==================== SEND FRIEND REQUEST ====================

    private void handleSendFriendRequest(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String senderId = parts[1];
        String receiverId = parts[2];

        // Check if already friends or request exists
        if (ContactDAO.contactExists(senderId, receiverId)) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.CONFLICT,
                    "Contact relationship already exists"
            ));

            return;
        }

        // Send friend request
        if (ContactDAO.sendFriendRequest(senderId, receiverId)) {
            // Get sender info
            User sender = UserDAO.findById(senderId);
            if (sender != null) {
                // Create notification for receiver
                NotificationDAO.createFriendRequestNotification(
                        receiverId,
                        senderId,
                        sender.getDisplayName()
                );

                // Send notification to receiver if online
                ClientHandler receiverHandler = clientHandler.getServer()
                        .getClientHandler(receiverId);
                if (receiverHandler != null) {
                    receiverHandler.sendMessage(Protocol.buildRequest(
                            Protocol.NOTIFICATION_NEW,
                            "friend_request",
                            sender.getDisplayName() + " sent you a friend request"
                    ));
                }
            }

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Friend request sent"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to send friend request"
            ));
        }
    }

    // ==================== ACCEPT FRIEND REQUEST ====================

    private void handleAcceptFriendRequest(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        String requesterId = parts[2];

        // Accept friend request
        if (ContactDAO.acceptFriendRequest(userId, requesterId)) {
            // Get user info
            User user = UserDAO.findById(userId);
            if (user != null) {
                // Create notification for requester
                NotificationDAO.createFriendAcceptNotification(
                        requesterId,
                        userId,
                        user.getDisplayName()
                );

                // Send notification to requester if online
                ClientHandler requesterHandler = clientHandler.getServer()
                        .getClientHandler(requesterId);
                if (requesterHandler != null) {
                    requesterHandler.sendMessage(Protocol.buildRequest(
                            Protocol.NOTIFICATION_NEW,
                            "friend_accept",
                            user.getDisplayName() + " accepted your friend request"
                    ));
                }
            }

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Friend request accepted"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to accept friend request"
            ));
        }
    }

    // ==================== REJECT FRIEND REQUEST ====================

    private void handleRejectFriendRequest(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        String requesterId = parts[2];

        if (ContactDAO.rejectFriendRequest(userId, requesterId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Friend request rejected"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to reject friend request"
            ));
        }
    }

    // ==================== GET FRIEND REQUESTS ====================

    private void handleGetFriendRequests(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        List<Contact> requests = ContactDAO.getPendingRequests(userId);

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < requests.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);

            Contact request = requests.get(i);
            User requester = UserDAO.findById(request.getContactUserId());

            if (requester != null) {
                data.append(request.getContactId())
                        .append(Protocol.LIST_DELIMITER)
                        .append(requester.getUserId())
                        .append(Protocol.LIST_DELIMITER)
                        .append(requester.getUsername())
                        .append(Protocol.LIST_DELIMITER)
                        .append(requester.getDisplayName())
                        .append(Protocol.LIST_DELIMITER)
                        .append(requester.getAvatarUrl() != null ? requester.getAvatarUrl() : "")
                        .append(Protocol.LIST_DELIMITER)
                        .append(request.getCreatedAt().toString());
            }
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Friend requests retrieved",
                data.toString()
        ));
    }

    // ==================== REMOVE CONTACT ====================

    private void handleRemoveContact(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        String contactUserId = parts[2];

        if (ContactDAO.unfriend(userId, contactUserId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Contact removed"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to remove contact"
            ));
        }
    }

    // ==================== BLOCK CONTACT ====================

    private void handleBlockContact(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        String contactUserId = parts[2];

        if (ContactDAO.blockContact(userId, contactUserId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Contact blocked"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to block contact"
            ));
        }
    }

    // ==================== UNBLOCK CONTACT ====================

    private void handleUnblockContact(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        String contactUserId = parts[2];

        if (ContactDAO.unblockContact(userId, contactUserId)) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Contact unblocked"
            ));
        } else {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to unblock contact"
            ));
        }
    }

    // ==================== GET BLOCKED CONTACTS ====================

    private void handleGetBlockedContacts(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String userId = parts[1];
        List<Contact> blockedContacts = ContactDAO.getBlockedContacts(userId);

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < blockedContacts.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);

            Contact contact = blockedContacts.get(i);
            User blockedUser = UserDAO.findById(contact.getContactUserId());

            if (blockedUser != null) {
                data.append(contact.getContactId())
                        .append(Protocol.LIST_DELIMITER)
                        .append(blockedUser.getUserId())
                        .append(Protocol.LIST_DELIMITER)
                        .append(blockedUser.getUsername())
                        .append(Protocol.LIST_DELIMITER)
                        .append(blockedUser.getDisplayName())
                        .append(Protocol.LIST_DELIMITER)
                        .append(blockedUser.getAvatarUrl() != null ? blockedUser.getAvatarUrl() : "");
            }
        }

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Blocked contacts retrieved",
                data.toString()
        ));
    }

    // ==================== ADD CONTACT ====================

    private void handleAddContact(String[] parts) {
        // Alias for send friend request
        handleSendFriendRequest(parts);
    }
}