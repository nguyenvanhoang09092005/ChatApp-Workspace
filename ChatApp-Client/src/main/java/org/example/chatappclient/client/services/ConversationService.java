// ==================== ConversationService.java ====================
package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Service xử lý các thao tác với Conversation
 */
public class ConversationService {

    private static volatile ConversationService instance;
    private final SocketClient socketClient;

    // Callbacks
    private BiConsumer<String, Message> onNewMessage;
    private BiConsumer<String, Boolean> onUserOnlineStatus;

    private ConversationService() {
        socketClient = SocketClient.getInstance();
    }

    public static ConversationService getInstance() {
        if (instance == null) {
            synchronized (ConversationService.class) {
                if (instance == null) {
                    instance = new ConversationService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD ====================

    public List<Conversation> getAllConversations(String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_GET_ALL, userId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        String data = Protocol.getData(response);
        return parseConversations(data);
    }

    public Conversation getConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_GET_BY_ID, conversationId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    public Conversation findOrCreatePrivateChat(String userId, String targetQuery) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_CREATE, userId, targetQuery, "private");
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    public Conversation createGroup(String userId, String groupName, List<String> memberIds) throws Exception {
        String members = String.join(",", memberIds);
        String request = Protocol.buildRequest(Protocol.CONVERSATION_CREATE_GROUP, userId, groupName, members);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseConversation(Protocol.getData(response));
    }

    public void deleteConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_DELETE, conversationId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    // ==================== ACTIONS ====================

    public void markAsRead(String conversationId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.MESSAGE_MARK_READ, conversationId, userId);
        socketClient.sendMessage(request);
    }

    public void muteConversation(String conversationId, boolean mute) throws Exception {
        String command = mute ? Protocol.CONVERSATION_MUTE : Protocol.CONVERSATION_UNMUTE;
        String request = Protocol.buildRequest(command, conversationId);
        socketClient.sendMessage(request);
    }

    public void pinConversation(String conversationId, boolean pin) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_PIN, conversationId, String.valueOf(pin));
        socketClient.sendMessage(request);
    }

    public void archiveConversation(String conversationId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CONVERSATION_ARCHIVE, conversationId);
        socketClient.sendMessage(request);
    }

    // ==================== PARSING ====================

    private List<Conversation> parseConversations(String data) {
        List<Conversation> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;

        for (String convData : Protocol.parseDataList(data)) {
            Conversation conv = parseConversation(convData);
            if (conv != null) list.add(conv);
        }
        return list;
    }

    private Conversation parseConversation(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] f = Protocol.parseFields(data);
        if (f.length < 4) return null;

        Conversation c = new Conversation();
        c.setConversationId(f[0]);
        c.setType(f[1]);
        c.setName(f[2]);
        c.setAvatarUrl(f[3]);
        if (f.length > 4) c.setLastMessage(f[4]);
        if (f.length > 5) c.setLastMessageTime(f[5]);
        if (f.length > 6) c.setUnreadCount(parseInt(f[6]));
        if (f.length > 7) c.setActive(Boolean.parseBoolean(f[7]));
        return c;
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    // ==================== CALLBACKS ====================

    public void setOnNewMessage(BiConsumer<String, Message> callback) {
        this.onNewMessage = callback;
    }

    public void setOnUserOnlineStatus(BiConsumer<String, Boolean> callback) {
        this.onUserOnlineStatus = callback;
    }
}
