package org.example.chatappclient.client.controllers.main.handlers;

import javafx.application.Platform;
import javafx.scene.layout.HBox;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.services.ConversationService;
import org.example.chatappclient.client.utils.ui.AlertUtil;
import org.example.chatappclient.client.utils.ui.DialogFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Handler xử lý danh sách hội thoại, tìm kiếm, lọc
 */
public class ConversationHandler {

    private final MainController mainController;
    private final ConversationService conversationService;
    private final UIComponentFactory uiFactory;
    private final ExecutorService executor;

    // Cache data
    private final Map<String, Conversation> conversationsMap = new ConcurrentHashMap<>();
    private final Map<String, HBox> conversationItemsMap = new ConcurrentHashMap<>();
    private String currentFilter = "all";
    private String activeConversationId = null;

    public ConversationHandler(MainController mainController,
                               ConversationService conversationService,
                               UIComponentFactory uiFactory) {
        this.mainController = mainController;
        this.conversationService = conversationService;
        this.uiFactory = uiFactory;
        this.executor = Executors.newCachedThreadPool();

        // Setup realtime listener
        setupRealtimeListener();
    }

    // ==================== LOAD DATA ====================

    public void loadConversations() {
        executor.submit(() -> {
            try {
                List<Conversation> conversations = conversationService.getAllConversations(
                        mainController.getCurrentUser().getUserId()
                );

                // Cache conversations
                conversationsMap.clear();
                for (Conversation conv : conversations) {
                    conversationsMap.put(conv.getConversationId(), conv);
                }

                // Sort by last message time
                conversations.sort((a, b) -> {
                    if (a.getLastMessageTime() == null) return 1;
                    if (b.getLastMessageTime() == null) return -1;
                    return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                });

                // Update UI
                mainController.displayConversations(conversations);
                updateNotificationBadge();

            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Không thể tải cuộc trò chuyện"));
            }
        });
    }

    // ==================== FILTER & SEARCH ====================

    public void filterConversations(String filter) {
        currentFilter = filter;
        mainController.setActiveFilterTab(filter);

        List<Conversation> filtered = conversationsMap.values().stream()
                .filter(conv -> matchFilter(conv, filter))
                .sorted((a, b) -> compareByTime(a, b))
                .collect(Collectors.toList());

        mainController.displayConversations(filtered);
    }

    public void searchConversations(String query) {
        if (query == null || query.trim().isEmpty()) {
            filterConversations(currentFilter);
            return;
        }

        String q = query.toLowerCase().trim();
        List<Conversation> filtered = conversationsMap.values().stream()
                .filter(conv -> conv.getName().toLowerCase().contains(q))
                .sorted((a, b) -> compareByTime(a, b))
                .collect(Collectors.toList());

        mainController.displayConversations(filtered);
    }

    private boolean matchFilter(Conversation conv, String filter) {
        return switch (filter) {
            case "unread" -> conv.getUnreadCount() > 0;
            case "group" -> "group".equalsIgnoreCase(conv.getType());
            default -> true;
        };
    }

    private int compareByTime(Conversation a, Conversation b) {
        // Pinned first
        if (a.isPinned() && !b.isPinned()) return -1;
        if (!a.isPinned() && b.isPinned()) return 1;

        // Then by time
        if (a.getLastMessageTime() == null) return 1;
        if (b.getLastMessageTime() == null) return -1;
        return b.getLastMessageTime().compareTo(a.getLastMessageTime());
    }

    // ==================== CONVERSATION ACTIONS ====================

    public void showNewChatDialog() {
        DialogFactory.showNewChatDialog(result -> {
            if (result != null && !result.isEmpty()) {
                startNewChat(result);
            }
        });
    }

    public void showCreateGroupDialog() {
        DialogFactory.showCreateGroupDialog((name, members) -> {
            if (name != null && !name.isEmpty()) {
                createGroup(name, members);
            }
        });
    }

    private void startNewChat(String searchQuery) {
        executor.submit(() -> {
            try {
                Conversation conv = conversationService.findOrCreatePrivateChat(
                        mainController.getCurrentUser().getUserId(),
                        searchQuery
                );

                if (conv != null) {
                    conversationsMap.put(conv.getConversationId(), conv);
                    Platform.runLater(() -> {
                        loadConversations();
                        // Open the new conversation
                    });
                } else {
                    Platform.runLater(() -> AlertUtil.showToastError("Không tìm thấy người dùng"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Lỗi: " + e.getMessage()));
            }
        });
    }

    private void createGroup(String name, List<String> memberIds) {
        executor.submit(() -> {
            try {
                Conversation group = conversationService.createGroup(
                        mainController.getCurrentUser().getUserId(),
                        name,
                        memberIds
                );

                if (group != null) {
                    conversationsMap.put(group.getConversationId(), group);
                    Platform.runLater(() -> {
                        loadConversations();
                        AlertUtil.showToastSuccess("Đã tạo nhóm: " + name);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Không thể tạo nhóm"));
            }
        });
    }

    public void markAsRead(String conversationId) {
        executor.submit(() -> {
            try {
                conversationService.markAsRead(conversationId, mainController.getCurrentUser().getUserId());

                Conversation conv = conversationsMap.get(conversationId);
                if (conv != null) {
                    conv.setUnreadCount(0);
                    Platform.runLater(this::updateNotificationBadge);
                }
            } catch (Exception e) {
                System.err.println("Error marking as read: " + e.getMessage());
            }
        });
    }

    public void pinConversation(String conversationId) {
        Conversation conv = conversationsMap.get(conversationId);
        if (conv != null) {
            conv.setPinned(!conv.isPinned());
            filterConversations(currentFilter);
            AlertUtil.showToastSuccess(conv.isPinned() ? "Đã ghim" : "Đã bỏ ghim");
        }
    }

    public void muteConversation(String conversationId) {
        Conversation conv = conversationsMap.get(conversationId);
        if (conv != null) {
            conv.setMuted(!conv.isMuted());
            AlertUtil.showToastSuccess(conv.isMuted() ? "Đã tắt thông báo" : "Đã bật thông báo");
        }
    }

    public void archiveConversation(String conversationId) {
        Conversation conv = conversationsMap.get(conversationId);
        if (conv != null) {
            conv.setArchived(true);
            conversationsMap.remove(conversationId);
            filterConversations(currentFilter);
            AlertUtil.showToastSuccess("Đã lưu trữ");
        }
    }

    public void deleteConversation(String conversationId) {
        boolean confirm = AlertUtil.showConfirmation("Xóa hội thoại",
                "Bạn có chắc muốn xóa cuộc trò chuyện này?");

        if (confirm) {
            executor.submit(() -> {
                try {
                    conversationService.deleteConversation(conversationId);
                    conversationsMap.remove(conversationId);

                    Platform.runLater(() -> {
                        filterConversations(currentFilter);
                        if (conversationId.equals(mainController.getCurrentConversationId())) {
                            mainController.showWelcomeScreen();
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtil.showToastError("Không thể xóa"));
                }
            });
        }
    }

    // ==================== UI HELPERS ====================

    public void setActiveConversation(String conversationId) {
        // Remove active from previous
        if (activeConversationId != null) {
            HBox prevItem = conversationItemsMap.get(activeConversationId);
            if (prevItem != null) {
                prevItem.getStyleClass().remove("active");
            }
        }

        // Set new active
        activeConversationId = conversationId;
        HBox item = conversationItemsMap.get(conversationId);
        if (item != null) {
            item.getStyleClass().add("active");
            item.getStyleClass().remove("unread");
        }
    }

    public void cacheConversationItem(String conversationId, HBox item) {
        conversationItemsMap.put(conversationId, item);
    }

    public Conversation getConversation(String conversationId) {
        return conversationsMap.get(conversationId);
    }

    public void updateLastMessage(String conversationId, String message, String timestamp) {
        Conversation conv = conversationsMap.get(conversationId);
        if (conv != null) {
            conv.setLastMessage(message);
            conv.setLastMessageTime(timestamp);
            Platform.runLater(() -> filterConversations(currentFilter));
        }
    }

    private void updateNotificationBadge() {
        int total = conversationsMap.values().stream()
                .mapToInt(Conversation::getUnreadCount)
                .sum();
        mainController.updateNotificationBadge(total);
    }

    // ==================== REALTIME ====================

    private void setupRealtimeListener() {
        conversationService.setOnNewMessage((conversationId, message) -> {
            Conversation conv = conversationsMap.get(conversationId);
            if (conv != null) {
                conv.setLastMessage(message.getContent());
                conv.setLastMessageTime(message.getTimestamp());

                // Increment unread if not current conversation
                if (!conversationId.equals(mainController.getCurrentConversationId())) {
                    conv.setUnreadCount(conv.getUnreadCount() + 1);
                }

                Platform.runLater(() -> {
                    filterConversations(currentFilter);
                    updateNotificationBadge();
                });
            }
        });

        conversationService.setOnUserOnlineStatus((userId, isOnline) -> {
            // Update online status for private chats
            for (Conversation conv : conversationsMap.values()) {
                if (conv.isPrivate()) {
                    // Check if this user is the partner
                    conv.setActive(isOnline);
                }
            }
            Platform.runLater(() -> filterConversations(currentFilter));
        });
    }

    // ==================== CLEANUP ====================

    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        conversationsMap.clear();
        conversationItemsMap.clear();
    }
}