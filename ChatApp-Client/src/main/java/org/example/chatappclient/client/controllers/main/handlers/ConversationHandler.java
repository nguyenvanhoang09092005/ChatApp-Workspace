package org.example.chatappclient.client.controllers.main.handlers;

import javafx.application.Platform;
import javafx.scene.layout.HBox;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.User;
import org.example.chatappclient.client.services.ConversationService;
import org.example.chatappclient.client.utils.ui.AlertUtil;
import org.example.chatappclient.client.utils.ui.DialogFactory;
import org.example.chatappclient.client.protocol.Protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Handler xử lý danh sách hội thoại, tìm kiếm, lọc + Handle CONVERSATION_RESTORED
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

        setupRealtimeListener();
    }

    // ==================== LOAD DATA ====================

    public void loadConversations() {
        executor.submit(() -> {
            try {
                List<Conversation> conversations = conversationService.getAllConversations(
                        mainController.getCurrentUser().getUserId()
                );

                conversationsMap.clear();
                for (Conversation conv : conversations) {
                    conversationsMap.put(conv.getConversationId(), conv);
                }

                conversations.sort((a, b) -> {
                    if (a.getLastMessageTime() == null) return 1;
                    if (b.getLastMessageTime() == null) return -1;
                    return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                });

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

    public void showNewChatDialog() {
        DialogFactory.showUserSearchDialog(user -> {
            if (user != null) {
                startNewChatWithUser(user);
            }
        });
    }

    private void startNewChatWithUser(User user) {
        executor.submit(() -> {
            try {
                Conversation conv = conversationService.findOrCreatePrivateChat(
                        mainController.getCurrentUser().getUserId(),
                        user.getUserId()
                );

                if (conv != null) {
                    conversationsMap.put(conv.getConversationId(), conv);
                    Platform.runLater(() -> {
                        loadConversations();
                        mainController.openConversation(conv);
                        AlertUtil.showToastSuccess("Đã tạo cuộc trò chuyện với " + user.getDisplayName());
                    });
                } else {
                    Platform.runLater(() ->
                            AlertUtil.showToastError("Không thể tạo cuộc trò chuyện")
                    );
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showToastError("Lỗi: " + e.getMessage())
                );
            }
        });
    }

    private boolean matchFilter(Conversation conv, String filter) {
        return switch (filter) {
            case "unread" -> conv.getUnreadCount() > 0;
            case "group" -> "group".equalsIgnoreCase(conv.getType());
            default -> true;
        };
    }

    private int compareByTime(Conversation a, Conversation b) {
        if (a.isPinned() && !b.isPinned()) return -1;
        if (!a.isPinned() && b.isPinned()) return 1;

        if (a.getLastMessageTime() == null) return 1;
        if (b.getLastMessageTime() == null) return -1;
        return b.getLastMessageTime().compareTo(a.getLastMessageTime());
    }

    // ==================== CONVERSATION ACTIONS ====================

    public void showCreateGroupDialog() {
        DialogFactory.showCreateGroupDialog((name, members) -> {
            if (name != null && !name.isEmpty()) {
                createGroup(name, members);
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
        if (activeConversationId != null) {
            HBox prevItem = conversationItemsMap.get(activeConversationId);
            if (prevItem != null) {
                prevItem.getStyleClass().remove("active");
            }
        }

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

    public void updateLastMessage(String conversationId, String message, String timestampStr) {
        Conversation conv = conversationsMap.get(conversationId);
        if (conv != null) {
            conv.setLastMessage(message);

            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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
        System.out.println("→ Thiết lập realtime listeners...");

        // Lắng nghe tin nhắn mới
        conversationService.setOnNewMessage((conversationId, message) -> {
            System.out.println("✅ Callback nhận tin nhắn mới: " + message.getContent());
            System.out.println("   ConversationID: " + conversationId);
            System.out.println("   Current conversation: " + mainController.getCurrentConversationId());

            Platform.runLater(() -> {
                Conversation conv = conversationsMap.get(conversationId);
                if (conv != null) {
                    conv.setLastMessage(message.getContent());
                    LocalDateTime time = message.getTimestamp();
                    conv.setLastMessageTime(time);

                    if (!conversationId.equals(mainController.getCurrentConversationId())) {
                        conv.setUnreadCount(conv.getUnreadCount() + 1);
                    }
                }

                if (conversationId.equals(mainController.getCurrentConversationId())) {
                    System.out.println("→ Đang mở conversation này, thêm tin nhắn vào UI");
                    mainController.addMessageToUI(message);
                } else {
                    System.out.println("→ Không mở conversation này, chỉ cập nhật danh sách");
                }

                filterConversations(currentFilter);
                updateNotificationBadge();
            });
        });

        // ✅ NEW: Lắng nghe conversation restored
        conversationService.setOnConversationRestored(this::handleConversationRestored);

        // Lắng nghe thay đổi trạng thái online
        conversationService.setOnUserOnlineStatus((userId, isOnline, lastSeenStr) -> {
            System.out.println("→ Nhận thay đổi trạng thái user: " + userId + " - " +
                    (isOnline ? "ONLINE" : "OFFLINE"));
            System.out.println("  → Last seen: " + lastSeenStr);

            int updatedCount = 0;

            for (Conversation conv : conversationsMap.values()) {
                if (conv.isPrivate()) {
                    System.out.println("  → Kiểm tra conversation: " + conv.getConversationId());
                    System.out.println("    → Member IDs: " + conv.getMemberIds());

                    if (conv.getMemberIds() != null && conv.getMemberIds().contains(userId)) {
                        System.out.println("    → FOUND! Cập nhật conversation này");

                        conv.setActive(isOnline);
                        updatedCount++;

                        if (lastSeenStr != null && !lastSeenStr.isEmpty() && !lastSeenStr.equals("null")) {
                            try {
                                LocalDateTime lastSeen = LocalDateTime.parse(
                                        lastSeenStr,
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                );
                                conv.setLastSeenTime(lastSeen);
                                System.out.println("    → Đã cập nhật: isActive=" + isOnline + ", lastSeen=" + lastSeen);
                            } catch (Exception e) {
                                System.err.println("    ⚠️ Lỗi parse last seen: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            if (updatedCount > 0) {
                System.out.println("  ✅ Đã cập nhật " + updatedCount + " conversations");
                Platform.runLater(() -> {
                    filterConversations(currentFilter);
                });
            } else {
                System.out.println("  ⚠️ Không tìm thấy conversation nào để cập nhật");
            }
        });

        System.out.println("✅ Realtime listeners đã được thiết lập");
    }

    /**
     * ✅ NEW: Xử lý khi conversation bị xóa được restore lại
     */
    private void handleConversationRestored(String message) {
        try {
            System.out.println("📥 CONVERSATION_RESTORED received: " + message);

            String[] parts = Protocol.parseMessage(message);
            if (parts.length < 2) {
                System.err.println("❌ Invalid CONVERSATION_RESTORED format");
                return;
            }

            // Parse conversation data
            String conversationData = parts[1];
            String[] convParts = conversationData.split(Protocol.LIST_DELIMITER);

            if (convParts.length < 11) {
                System.err.println("❌ Invalid conversation data");
                return;
            }

            String conversationId = convParts[0];
            System.out.println("  → ConversationID: " + conversationId);

            // Build conversation object
            Conversation restoredConv = new Conversation();
            restoredConv.setConversationId(conversationId);
            restoredConv.setType(convParts[1]);
            restoredConv.setName(convParts[2]);
            restoredConv.setAvatarUrl(convParts[3]);
            restoredConv.setLastMessage(convParts[4]);

            if (!convParts[5].isEmpty()) {
                try {
                    restoredConv.setLastMessageTime(LocalDateTime.parse(convParts[5]));
                } catch (Exception e) {
                    System.err.println("Error parsing last message time: " + e.getMessage());
                }
            }

            try {
                restoredConv.setUnreadCount(Integer.parseInt(convParts[6]));
            } catch (Exception e) {
                restoredConv.setUnreadCount(0);
            }



            restoredConv.setActive("true".equals(convParts[8]));

            if (!convParts[9].isEmpty()) {
                try {
                    restoredConv.setLastSeenTime(LocalDateTime.parse(convParts[9]));
                } catch (Exception ignored) {}
            }

            if (convParts.length > 10 && !convParts[10].isEmpty()) {
                List<String> memberIds = Arrays.asList(convParts[10].split(";"));
                restoredConv.setMemberIds(memberIds);
            }

            Platform.runLater(() -> {
                // Add to conversationsMap
                conversationsMap.put(conversationId, restoredConv);

                // Refresh conversation list to show restored conversation
                filterConversations(currentFilter);

                // Show notification
                AlertUtil.showToastInfo("Có tin nhắn mới từ " + restoredConv.getName());

                System.out.println("✅ Conversation restored and added to list: " + conversationId);
            });

        } catch (Exception e) {
            System.err.println("❌ Error handling conversation restored: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Callback interface for user status change
     */
    public interface OnUserStatusChangeListener {
        void onStatusChange(String userId, boolean isOnline, String lastSeen);
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