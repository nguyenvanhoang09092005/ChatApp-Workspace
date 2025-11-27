package org.example.chatappclient.client.services;

import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatService {

    private static volatile ChatService instance;

    private final AuthService authService;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final UserService userService;
    private final CallService callService;

    private User currentUser;
    private final Map<String, Conversation> conversationCache;
    private final Map<String, User> userCache;

    private ChatService() {
        authService = AuthService.getInstance();
        messageService = MessageService.getInstance();
        conversationService = ConversationService.getInstance();
        userService = UserService.getInstance();
        callService = CallService.getInstance();

        conversationCache = new HashMap<>();
        userCache = new HashMap<>();
    }

    public static ChatService getInstance() {
        if (instance == null) {
            synchronized (ChatService.class) {
                if (instance == null) {
                    instance = new ChatService();
                }
            }
        }
        return instance;
    }

    // ==================== LOGIN ====================

    public User login(String username, String password, boolean rememberMe) throws Exception {
        AuthService.LoginResult result =
                authService.login(username, password, rememberMe);

        if (!result.isSuccess()) {
            throw new Exception(result.getMessage());
        }

        this.currentUser = result.getUser();
        loadInitialData();

        return currentUser;
    }

    // ==================== LOGOUT ====================

    public void logout() {
        authService.logout();
        clearCache();
        currentUser = null;
    }

    // ==================== LOAD DATA ====================

    private void loadInitialData() throws Exception {
        if (currentUser == null) return;

        List<Conversation> conversations =
                conversationService.getAllConversations(currentUser.getUserId());

        for (Conversation c : conversations) {
            conversationCache.put(c.getConversationId(), c);
        }
    }

    private void clearCache() {
        conversationCache.clear();
        userCache.clear();
    }

    // ==================== GETTERS ====================

    public User getCurrentUser() {
        return currentUser;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public ConversationService getConversationService() {
        return conversationService;
    }

    public UserService getUserService() {
        return userService;
    }

    public CallService getCallService() {
        return callService;
    }

    // ==================== CACHE ====================

    public Conversation getCachedConversation(String id) {
        return conversationCache.get(id);
    }

    public void cacheConversation(Conversation conv) {
        conversationCache.put(conv.getConversationId(), conv);
    }

    public User getCachedUser(String userId) {
        return userCache.get(userId);
    }

    public void cacheUser(User user) {
        userCache.put(user.getUserId(), user);
    }
}
