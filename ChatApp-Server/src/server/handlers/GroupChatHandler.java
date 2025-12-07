package server.handlers;

import database.dao.UserDAO;
import database.dao.*;
import models.*;
import protocol.Protocol;
import server.ClientHandler;
import server.ChatServer;
import server.RequestHandler;

import java.util.List;

/**
 * Handler for Group Chat functionality
 */
public class GroupChatHandler implements RequestHandler {

    private GroupDAO groupDAO;
    private GroupMemberDAO groupMemberDAO;
    private GroupMessageDAO groupMessageDAO;

    public GroupChatHandler() {
        // Initialize DAOs
        this.groupDAO = new GroupDAO();
        this.groupMemberDAO = new GroupMemberDAO();
        this.groupMessageDAO = new GroupMessageDAO();
        // UserDAO là static, không cần khởi tạo
    }

    @Override
    public String handleRequest(String request, ClientHandler client) {
        String[] parts = Protocol.parseMessage(request);
        if (parts.length == 0) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Empty request");
        }

        String command = parts[0];
        String userId = client.getUserId();

        try {
            switch (command) {
                case Protocol.GROUP_CREATE:
                    return handleCreateGroup(parts, userId);
                case Protocol.GROUP_UPDATE:
                    return handleUpdateGroup(parts, userId);
                case Protocol.GROUP_DELETE:
                    return handleDeleteGroup(parts, userId);
                case Protocol.GROUP_GET_INFO:
                    return handleGetGroupInfo(parts, userId);
                case Protocol.GROUP_GET_MEMBERS:
                    return handleGetGroupMembers(parts, userId);
                case Protocol.GROUP_ADD_MEMBER:
                    return handleAddMember(parts, userId);
                case Protocol.GROUP_REMOVE_MEMBER:
                    return handleRemoveMember(parts, userId);
                case Protocol.GROUP_LEAVE:
                    return handleLeaveGroup(parts, userId);
                case Protocol.GROUP_CHANGE_ROLE:
                    return handleChangeRole(parts, userId);
                case Protocol.GROUP_UPDATE_AVATAR:
                    return handleUpdateAvatar(parts, userId);
                case Protocol.GROUP_SEARCH:
                    return handleSearchGroups(parts, userId);
                case Protocol.GROUP_MESSAGE_SEND:
                    return handleSendMessage(parts, userId, client);
                case Protocol.GROUP_MESSAGE_GET_HISTORY:
                    return handleGetMessageHistory(parts, userId);
                case Protocol.GROUP_MESSAGE_DELETE:
                    return handleDeleteMessage(parts, userId);
                case Protocol.GROUP_MESSAGE_EDIT:
                    return handleEditMessage(parts, userId);
                case Protocol.GROUP_MESSAGE_MARK_READ:
                    return handleMarkMessageRead(parts, userId);
                default:
                    return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Unknown command: " + command);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Protocol.buildErrorResponse(Protocol.SERVER_ERROR, "Internal server error: " + e.getMessage());
        }
    }

    // ==================== GROUP MANAGEMENT ====================

    private String handleCreateGroup(String[] parts, String userId) {
        if (parts.length < 2) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String groupName = parts[1];
        String memberIdsStr = parts.length > 2 ? parts[2] : "";

        // Validate group name
        if (groupName == null || groupName.trim().isEmpty()) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_EMPTY_NAME, "Group name cannot be empty");
        }

        // Check if group name already exists
        if (groupDAO.existsByName(groupName.trim())) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_NAME_EXISTS, "Group name already exists");
        }

        // Create group
        Group group = new Group();
        group.setGroupName(groupName.trim());
        group.setCreatorId(userId);

        boolean success = groupDAO.createGroup(group);
        if (!success) {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to create group");
        }

        // Add creator as admin
        groupMemberDAO.addMember(group.getGroupId(), userId, "admin");

        // Add other members
        if (!memberIdsStr.isEmpty()) {
            String[] memberIds = Protocol.parseFields(memberIdsStr);
            for (String memberId : memberIds) {
                try {
                    // Check if user exists - ĐÃ SỬA: UserDAO.findById()
                    User user = UserDAO.findById(memberId);
                    if (user != null && !groupMemberDAO.isMember(group.getGroupId(), memberId)) {
                        groupMemberDAO.addMember(group.getGroupId(), memberId, "member");
                    }
                } catch (Exception e) {
                    // Continue with other members
                }
            }
        }

        // Notify members
        notifyGroupMembers(group.getGroupId(), Protocol.GROUP_MEMBER_JOINED, userId);

        return Protocol.buildSuccessResponse("Group created successfully",
                group.getGroupId(), groupName);
    }

    private String handleUpdateGroup(String[] parts, String userId) {
        if (parts.length < 3) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String groupId = parts[1];
        String newName = parts[2];

        // Check permissions (only admin can update group)
        if (!groupMemberDAO.isAdmin(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "Only admin can update group");
        }

        // Update group
        Group group = groupDAO.getGroupById(groupId);
        if (group == null) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_NOT_FOUND, "Group not found");
        }

        group.setGroupName(newName);
        boolean success = groupDAO.updateGroup(group);

        if (success) {
            notifyGroupMembers(groupId, Protocol.GROUP_UPDATED, userId, newName);
            return Protocol.buildSuccessResponse("Group updated successfully");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to update group");
        }
    }

    private String handleDeleteGroup(String[] parts, String userId) {
        if (parts.length < 2) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing group ID");
        }

        String groupId = parts[1];

        // Check if user is admin
        if (!groupMemberDAO.isAdmin(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "Only admin can delete group");
        }

        boolean success = groupDAO.deleteGroup(groupId);

        if (success) {
            // Notify members before deleting
            notifyGroupMembers(groupId, "GROUP_DELETED", userId);
            return Protocol.buildSuccessResponse("Group deleted successfully");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to delete group");
        }
    }

    private String handleGetGroupInfo(String[] parts, String userId) {
        if (parts.length < 2) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing group ID");
        }

        String groupId = parts[1];

        // Check if user is member
        if (!groupMemberDAO.isMember(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_ACCESS_DENIED,
                    "You are not a member of this group");
        }

        Group group = groupDAO.getGroupById(groupId);
        if (group == null) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_NOT_FOUND, "Group not found");
        }

        // Format group info
        String groupInfo = String.format("%s::%s::%s::%s",
                group.getGroupId(),
                group.getGroupName(),
                group.getGroupAvatar() != null ? group.getGroupAvatar() : "",
                group.getCreatorId());

        return Protocol.buildSuccessResponse("Group info retrieved", groupInfo);
    }

    // ==================== MEMBER MANAGEMENT ====================

    private String handleGetGroupMembers(String[] parts, String userId) {
        if (parts.length < 2) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing group ID");
        }

        String groupId = parts[1];

        // Check if user is member
        if (!groupMemberDAO.isMember(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_ACCESS_DENIED,
                    "You are not a member of this group");
        }

        List<GroupMember> members = groupMemberDAO.getGroupMembers(groupId);
        StringBuilder membersData = new StringBuilder();

        for (GroupMember member : members) {
            User user = UserDAO.findById(member.getUserId());  // ĐÃ SỬA: UserDAO.findById()
            if (user != null) {
                membersData.append(String.format("%s:%s:%s:%s,",
                        user.getUserId(),
                        user.getUsername(),
                        user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                        member.getRole()));
            }
        }

        // Remove trailing comma
        if (membersData.length() > 0) {
            membersData.setLength(membersData.length() - 1);
        }

        return Protocol.buildSuccessResponse("Group members retrieved", membersData.toString());
    }

    private String handleAddMember(String[] parts, String userId) {
        if (parts.length < 3) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String groupId = parts[1];
        String newMemberId = parts[2];

        // Check if requester is admin
        if (!groupMemberDAO.isAdmin(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "Only admin can add members");
        }

        // Check if user exists - ĐÃ SỬA: UserDAO.findById()
        User user = UserDAO.findById(newMemberId);
        if (user == null) {
            return Protocol.buildErrorResponse(Protocol.ERR_NOT_FOUND, "User not found");
        }

        // Check if already member
        if (groupMemberDAO.isMember(groupId, newMemberId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_MEMBER_EXISTS,
                    "User is already a member");
        }

        // Add member
        boolean success = groupMemberDAO.addMember(groupId, newMemberId, "member");

        if (success) {
            notifyGroupMembers(groupId, Protocol.GROUP_MEMBER_JOINED, newMemberId);
            return Protocol.buildSuccessResponse("Member added successfully");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to add member");
        }
    }

    private String handleRemoveMember(String[] parts, String userId) {
        if (parts.length < 3) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String groupId = parts[1];
        String memberToRemoveId = parts[2];

        // Check if requester is admin
        if (!groupMemberDAO.isAdmin(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "Only admin can remove members");
        }

        // Cannot remove self as admin (must transfer admin first or leave group)
        if (memberToRemoveId.equals(userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "Admin cannot remove themselves. Transfer admin role first or leave group.");
        }

        boolean success = groupMemberDAO.removeMember(groupId, memberToRemoveId);

        if (success) {
            notifyGroupMembers(groupId, Protocol.GROUP_MEMBER_REMOVED,
                    memberToRemoveId, userId);
            return Protocol.buildSuccessResponse("Member removed successfully");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to remove member");
        }
    }

    private String handleLeaveGroup(String[] parts, String userId) {
        if (parts.length < 2) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing group ID");
        }

        String groupId = parts[1];

        // Check if user is admin
        GroupMember member = groupMemberDAO.getMember(groupId, userId);
        if (member != null && "admin".equals(member.getRole())) {
            // Admin cannot leave without transferring admin role first
            // Check if there are other admins
            List<GroupMember> admins = groupMemberDAO.getGroupAdmins(groupId);
            if (admins.size() <= 1) {
                return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                        "Admin cannot leave without transferring admin role to another member");
            }
        }

        boolean success = groupMemberDAO.removeMember(groupId, userId);

        if (success) {
            notifyGroupMembers(groupId, Protocol.GROUP_MEMBER_LEFT, userId);
            return Protocol.buildSuccessResponse("Left group successfully");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to leave group");
        }
    }

    private String handleChangeRole(String[] parts, String userId) {
        if (parts.length < 4) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String groupId = parts[1];
        String targetUserId = parts[2];
        String newRole = parts[3];

        // Check if requester is admin
        if (!groupMemberDAO.isAdmin(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "Only admin can change roles");
        }

        // Validate role
        if (!"admin".equals(newRole) && !"member".equals(newRole)) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Invalid role");
        }

        boolean success = groupMemberDAO.updateRole(groupId, targetUserId, newRole);

        if (success) {
            notifyGroupMembers(groupId, "GROUP_ROLE_CHANGED",
                    targetUserId, newRole);
            return Protocol.buildSuccessResponse("Role updated successfully");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to update role");
        }
    }

    // ==================== MESSAGING ====================

    private String handleSendMessage(String[] parts, String userId, ClientHandler client) {
        if (parts.length < 4) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String groupId = parts[1];
        String messageType = parts[2];
        String content = parts[3];

        // Check if user is member
        if (!groupMemberDAO.isMember(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_ACCESS_DENIED,
                    "You are not a member of this group");
        }

        // Check if user is muted
        GroupMember member = groupMemberDAO.getMember(groupId, userId);
        if (member != null && member.isMuted()) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "You are muted in this group");
        }

        // Create message
        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(userId);
        message.setMessageType(messageType);
        message.setContent(content);

        boolean success = groupMessageDAO.saveMessage(message);
        if (!success) {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to send message");
        }

        // Broadcast to all group members
        broadcastToGroup(groupId, Protocol.GROUP_MESSAGE_RECEIVE,
                message.getMessageId(),
                userId,
                messageType,
                content,
                String.valueOf(System.currentTimeMillis()));

        return Protocol.buildSuccessResponse("Message sent", message.getMessageId());
    }

    private String handleGetMessageHistory(String[] parts, String userId) {
        if (parts.length < 2) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing group ID");
        }

        String groupId = parts[1];
        int limit = parts.length > 2 ? Integer.parseInt(parts[2]) : 50;
        int offset = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;

        // Check if user is member
        if (!groupMemberDAO.isMember(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_ACCESS_DENIED,
                    "You are not a member of this group");
        }

        List<GroupMessage> messages = groupMessageDAO.getMessages(groupId, limit, offset);
        StringBuilder messagesData = new StringBuilder();

        for (GroupMessage message : messages) {
            messagesData.append(String.format("%s:%s:%s:%s:%d|",
                    message.getMessageId(),
                    message.getSenderId(),
                    message.getMessageType(),
                    message.getContent(),
                    message.getSentAt().getTime()));
        }

        // Remove trailing pipe
        if (messagesData.length() > 0) {
            messagesData.setLength(messagesData.length() - 1);
        }

        return Protocol.buildSuccessResponse("Message history retrieved", messagesData.toString());
    }

    // ==================== HELPER METHODS ====================

    private void notifyGroupMembers(String groupId, String event, String... data) {
        List<GroupMember> members = groupMemberDAO.getGroupMembers(groupId);
        String notification = Protocol.buildRequest(event, data);

        for (GroupMember member : members) {
            ClientHandler client = getClientHandler(member.getUserId());
            if (client != null && client.isConnected()) {
                client.sendMessage(notification);
            }
        }
    }

    private void broadcastToGroup(String groupId, String command, String... data) {
        List<GroupMember> members = groupMemberDAO.getGroupMembers(groupId);
        String message = Protocol.buildRequest(command, data);

        for (GroupMember member : members) {
            ClientHandler client = getClientHandler(member.getUserId());
            if (client != null && client.isConnected()) {
                client.sendMessage(message);
            }
        }
    }

    private ClientHandler getClientHandler(String userId) {
        // Implementation depends on how ChatServer manages clients
        // This is a placeholder - you need to implement this based on your architecture
        // Example: return ChatServer.getInstance().getClientHandler(userId);
        return null; // Replace with actual implementation
    }

    // ==================== NOT YET IMPLEMENTED METHODS ====================

    private String handleUpdateAvatar(String[] parts, String userId) {
        if (parts.length < 3) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String groupId = parts[1];
        String avatarUrl = parts[2];

        // Check if user is admin
        if (!groupMemberDAO.isAdmin(groupId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "Only admin can update group avatar");
        }

        boolean success = groupDAO.updateGroupAvatar(groupId, avatarUrl);
        if (success) {
            notifyGroupMembers(groupId, Protocol.GROUP_UPDATED, userId);
            return Protocol.buildSuccessResponse("Group avatar updated");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to update avatar");
        }
    }

    private String handleSearchGroups(String[] parts, String userId) {
        if (parts.length < 2) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing search keyword");
        }

        String keyword = parts[1];
        int limit = parts.length > 2 ? Integer.parseInt(parts[2]) : 20;

        List<Group> groups = groupDAO.searchGroups(keyword, limit);
        StringBuilder groupsData = new StringBuilder();

        for (Group group : groups) {
            // Only include groups that are public or user is a member of
            if (groupMemberDAO.isMember(group.getGroupId(), userId)) {
                groupsData.append(String.format("%s:%s:%s:%d,",
                        group.getGroupId(),
                        group.getGroupName(),
                        group.getDescription() != null ? group.getDescription() : "",
                        groupMemberDAO.countMembers(group.getGroupId())));
            }
        }

        // Remove trailing comma
        if (groupsData.length() > 0) {
            groupsData.setLength(groupsData.length() - 1);
        }

        return Protocol.buildSuccessResponse("Groups found", groupsData.toString());
    }

    private String handleDeleteMessage(String[] parts, String userId) {
        if (parts.length < 2) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing message ID");
        }

        String messageId = parts[1];

        GroupMessage message = groupMessageDAO.getMessageById(messageId);
        if (message == null) {
            return Protocol.buildErrorResponse(Protocol.ERR_NOT_FOUND, "Message not found");
        }

        // Check permissions
        if (!message.getSenderId().equals(userId) && !groupMemberDAO.isAdmin(message.getGroupId(), userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "You don't have permission to delete this message");
        }

        boolean success = groupMessageDAO.markAsDeleted(messageId);
        if (success) {
            notifyGroupMembers(message.getGroupId(), Protocol.GROUP_MESSAGE_DELETE, messageId, userId);
            return Protocol.buildSuccessResponse("Message deleted");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to delete message");
        }
    }

    private String handleEditMessage(String[] parts, String userId) {
        if (parts.length < 3) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String messageId = parts[1];
        String newContent = parts[2];

        GroupMessage message = groupMessageDAO.getMessageById(messageId);
        if (message == null) {
            return Protocol.buildErrorResponse(Protocol.ERR_NOT_FOUND, "Message not found");
        }

        // Check if user can edit the message
        if (!groupMessageDAO.canEditMessage(messageId, userId)) {
            return Protocol.buildErrorResponse(Protocol.ERR_GROUP_PERMISSION_DENIED,
                    "You cannot edit this message");
        }

        boolean success = groupMessageDAO.updateMessageContent(messageId, newContent);
        if (success) {
            notifyGroupMembers(message.getGroupId(), Protocol.GROUP_MESSAGE_EDIT,
                    messageId, newContent, userId);
            return Protocol.buildSuccessResponse("Message edited");
        } else {
            return Protocol.buildErrorResponse(Protocol.ERR_DATABASE_ERROR, "Failed to edit message");
        }
    }

    private String handleMarkMessageRead(String[] parts, String userId) {
        if (parts.length < 3) {
            return Protocol.buildErrorResponse(Protocol.INVALID_REQUEST, "Missing parameters");
        }

        String groupId = parts[1];
        String lastReadMessageId = parts[2];

        // Update user's last read message in the group
        // Implementation depends on how you track read messages
        // This could be stored in a separate table or in group_members table

        return Protocol.buildSuccessResponse("Message marked as read");
    }
}