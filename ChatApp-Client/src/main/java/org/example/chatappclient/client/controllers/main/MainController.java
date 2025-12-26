package org.example.chatappclient.client.controllers.main;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.example.chatappclient.client.controllers.main.handlers.*;
import org.example.chatappclient.client.models.*;
import org.example.chatappclient.client.services.*;
import org.example.chatappclient.client.utils.ui.ConversationInfoBuilder;
import org.example.chatappclient.client.utils.ui.EmojiStickerDialog;
import org.example.chatappclient.client.utils.data.StickerData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainController {

    // ==================== FXML COMPONENTS ====================

    // Left Sidebar
    @FXML private VBox leftSidebar;
    @FXML private ImageView userAvatar;
    @FXML private Circle onlineIndicator;
    @FXML private Button messagesButton, contactsButton, groupsButton;
    @FXML private Button notificationsButton, cloudStorageButton, settingsButton;
    @FXML private Label notificationBadge;

    // Conversation Panel
    @FXML private VBox conversationPanel;
    @FXML private Label panelTitle;
    @FXML private Button newGroupButton, newChatButton;
    @FXML private TextField searchField;
    @FXML private HBox filterTabs;
    @FXML private Button allTab, unreadTab, groupTab;
    @FXML private ScrollPane conversationScrollPane;
    @FXML private VBox conversationListContainer;

    // Chat Panel
    @FXML private BorderPane chatPanel;
    @FXML private HBox chatHeader;
    @FXML private ImageView chatPartnerAvatar;
    @FXML private Circle partnerOnlineIndicator;
    @FXML private Label chatPartnerName, chatPartnerStatus;
    @FXML private Button searchInChatBtn, audioCallButton, videoCallButton, chatInfoButton;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesContainer;
    @FXML private HBox typingIndicator, replyPreview;
    @FXML private Label typingLabel, replyToName, replyToContent;
    @FXML private Button cancelReplyBtn;

    // Message Input
    @FXML private Button stickerButton, imageButton, attachButton;
    @FXML private TextArea messageInputArea;
    @FXML private Button voiceButton, likeButton, sendButton;

    // Welcome & Info Sidebar
    @FXML private VBox welcomeScreen, infoSidebar;
    @FXML private Button startChatButton;

    private ChatController chatController;
    private EmojiStickerDialog emojiStickerDialog;

    // ==================== HANDLERS ====================
    private NavigationHandler navigationHandler;
    private ConversationHandler conversationHandler;
    private MessageHandler messageHandler;
    private CallHandler callHandler;
    private FileHandler fileHandler;
    private UIComponentFactory uiFactory;

    // ==================== SERVICES ====================
    private AuthService authService;
    private ConversationService conversationService;
    private MessageService messageService;
    private UserService userService;

    // ==================== STATE ====================
    private User currentUser;
    private String currentConversationId;
    private final Set<String> uploadingMessageIds = new HashSet<>();

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        System.out.println("=== MainController Initialize ===");

        // Initialize services
        initServices();

        // Get current user
        currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            navigationHandler = new NavigationHandler(this);
            navigationHandler.navigateToLogin(leftSidebar);
            return;
        }

        // Initialize emoji/sticker dialog
        initEmojiStickerDialog();

        initHandlers();
        setupUI();
        bindEventHandlers();
        loadInitialData();
        showWelcomeScreen();
    }

    private void initServices() {
        authService = AuthService.getInstance();
        conversationService = ConversationService.getInstance();
        messageService = MessageService.getInstance();
        userService = UserService.getInstance();
    }

    /**
     * Khởi tạo Emoji/Sticker Dialog
     */
    private void initEmojiStickerDialog() {
        emojiStickerDialog = new EmojiStickerDialog();

        // Callback khi chọn emoji
        emojiStickerDialog.setOnEmojiSelected(emoji -> {
            System.out.println("😀 Emoji selected: " + emoji);
            insertTextAtCursor(emoji);
        });

        // Callback khi chọn sticker
        emojiStickerDialog.setOnStickerSelected(sticker -> {
            System.out.println("🎨 Sticker selected: " + sticker.getName());
            sendSticker(sticker);
        });
    }

    private void initHandlers() {
        uiFactory = new UIComponentFactory();
        navigationHandler = new NavigationHandler(this);
        conversationHandler = new ConversationHandler(this, conversationService, uiFactory);
        messageHandler = MessageHandler.getInstance();
        messageHandler.setMainController(this);
        callHandler = new CallHandler(this);
        fileHandler = new FileHandler(this);
        chatController = new ChatController(chatMessagesContainer, chatScrollPane, currentUser.getUserId());
        callHandler.setConversationHandler(conversationHandler);

    }

    private void setupUI() {
        // User profile
        uiFactory.loadAvatar(userAvatar, currentUser.getAvatarUrl(), currentUser.getUsername(), 48);
        onlineIndicator.setFill(javafx.scene.paint.Color.web("#31A24C"));

        // Scrollpanes
        chatScrollPane.setFitToWidth(true);
        conversationScrollPane.setFitToWidth(true);

        // Message input toggle send/like button
        messageInputArea.textProperty().addListener((obs, old, newVal) -> {
            boolean hasText = newVal != null && !newVal.trim().isEmpty();
            sendButton.setVisible(hasText);
            sendButton.setManaged(hasText);
            likeButton.setVisible(!hasText);
            likeButton.setManaged(!hasText);
        });
    }

    private void bindEventHandlers() {
        // Navigation
        messagesButton.setOnAction(e -> navigationHandler.showMessages());
        contactsButton.setOnAction(e -> navigationHandler.showContacts());
        groupsButton.setOnAction(e -> navigationHandler.showGroups());
        notificationsButton.setOnAction(e -> navigationHandler.showNotifications());
        cloudStorageButton.setOnAction(e -> navigationHandler.showCloudStorage());
        settingsButton.setOnAction(e -> navigationHandler.showSettings());

        // Conversation
        newChatButton.setOnAction(e -> conversationHandler.showNewChatDialog());
        newGroupButton.setOnAction(e -> conversationHandler.showCreateGroupDialog());
        startChatButton.setOnAction(e -> conversationHandler.showNewChatDialog());

        // Filter tabs
        allTab.setOnAction(e -> conversationHandler.filterConversations("all"));
        unreadTab.setOnAction(e -> conversationHandler.filterConversations("unread"));
        groupTab.setOnAction(e -> conversationHandler.filterConversations("group"));

        // Search
        searchField.textProperty().addListener((obs, old, val) -> conversationHandler.searchConversations(val));

        // Messaging
        sendButton.setOnAction(e -> messageHandler.sendTextMessage());
        likeButton.setOnAction(e -> {
            if (currentConversationId != null) {
                messageHandler.sendLike(currentConversationId);
            }
        });
        cancelReplyBtn.setOnAction(e -> messageHandler.cancelReply());

        messageInputArea.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                messageHandler.sendTextMessage();
            }
        });

        // File & Media
        imageButton.setOnAction(e -> fileHandler.selectImage());
        attachButton.setOnAction(e -> fileHandler.selectFile());

        // Emoji/Sticker button
        stickerButton.setOnAction(e -> showEmojiStickerDialog());

        // Calls
        audioCallButton.setOnAction(e -> callHandler.startAudioCall());
        videoCallButton.setOnAction(e -> callHandler.startVideoCall());

        // Chat info
        chatInfoButton.setOnAction(e -> toggleInfoSidebar());
    }

    /**
     * Hiển thị Emoji/Sticker Dialog
     */
    private void showEmojiStickerDialog() {
        if (currentConversationId == null) {
            System.out.println("⚠️ Chưa chọn conversation");
            return;
        }

        // Lấy vị trí của sticker button
        Bounds bounds = stickerButton.localToScreen(stickerButton.getBoundsInLocal());

        // Hiển thị dialog phía trên button
        double x = bounds.getMinX();
        double y = bounds.getMinY() - 430; // 420 (dialog height) + 10 (spacing)

        emojiStickerDialog.show(stickerButton, x, y);
    }

    /**
     * Chèn emoji vào vị trí con trỏ trong TextArea
     */
    private void insertTextAtCursor(String emoji) {
        Platform.runLater(() -> {
            int caretPosition = messageInputArea.getCaretPosition();
            String currentText = messageInputArea.getText();

            String newText = currentText.substring(0, caretPosition) +
                    emoji +
                    currentText.substring(caretPosition);

            messageInputArea.setText(newText);
            messageInputArea.positionCaret(caretPosition + emoji.length());
            messageInputArea.requestFocus();
        });
    }

    /**
     * Gửi sticker
     */
    private void sendSticker(StickerData.Sticker sticker) {
        if (currentConversationId == null) {
            System.err.println("⚠️ Không thể gửi sticker: chưa chọn conversation");
            return;
        }

        System.out.println("📤 Sending sticker: " + sticker.getName());

        // Tạo Message object cho sticker
        Message stickerMsg = new Message();
        stickerMsg.setMessageId(generateTempMessageId());
        stickerMsg.setConversationId(currentConversationId);
        stickerMsg.setSenderId(currentUser.getUserId());
        stickerMsg.setMessageType("STICKER");
        stickerMsg.setMediaUrl(sticker.getUrl());
        stickerMsg.setFileName(sticker.getName());
        stickerMsg.setTimestamp(java.time.LocalDateTime.now());
        stickerMsg.setDelivered(false);
        stickerMsg.setRead(false);

        // Hiển thị sticker ngay lập tức trong UI
        addMessageToUI(stickerMsg);

        // Gửi sticker qua MessageHandler
        messageHandler.sendSticker(currentConversationId, sticker);
    }

    /**
     * Generate temporary message ID
     */
    private String generateTempMessageId() {
        return "temp_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private void loadInitialData() {
        conversationHandler.loadConversations();
    }

    // ==================== PUBLIC UI METHODS ====================

    public void showWelcomeScreen() {
        welcomeScreen.setVisible(true);
        chatPanel.setVisible(false);
        currentConversationId = null;
    }

    public void showChatPanel() {
        welcomeScreen.setVisible(false);
        chatPanel.setVisible(true);
    }

    public void displayConversations(List<Conversation> conversations) {
        Platform.runLater(() -> {
            conversationListContainer.getChildren().clear();
            if (conversations.isEmpty()) {
                conversationListContainer.getChildren().add(
                        uiFactory.createEmptyState("Chưa có cuộc trò chuyện", "Bắt đầu chat với bạn bè!")
                );
                return;
            }

            for (Conversation conv : conversations) {
                HBox item = uiFactory.createConversationItem(conv, c -> openConversation(c));
                conversationListContainer.getChildren().add(item);
                conversationHandler.cacheConversationItem(conv.getConversationId(), item);
            }
        });
    }

    public void displayMessages(List<Message> messages) {
        chatController.displayMessages(messages);
    }

    public void addMessageToUI(Message msg) {
        chatController.addNewMessage(msg);
    }

    public void updateChatHeader(Conversation conv) {
        Platform.runLater(() -> {
            chatPartnerName.setText(conv.getName());
            chatPartnerStatus.setText(conv.isActive() ? "Đang hoạt động" : "Không hoạt động");
            chatPartnerStatus.getStyleClass().remove("online");
            if (conv.isActive()) chatPartnerStatus.getStyleClass().add("online");
            uiFactory.loadAvatar(chatPartnerAvatar, conv.getAvatarUrl(), conv.getName(), 44);
            partnerOnlineIndicator.setVisible(conv.isActive());
        });
    }

    public void showTypingIndicator(String userName) {
        Platform.runLater(() -> {
            typingLabel.setText(userName + " đang nhập...");
            typingIndicator.setVisible(true);
        });
    }

    public void hideTypingIndicator() {
        Platform.runLater(() -> typingIndicator.setVisible(false));
    }

    public void updateNotificationBadge(int count) {
        Platform.runLater(() -> {
            notificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            notificationBadge.setVisible(count > 0);
        });
    }

    public void setActiveNavButton(Button btn) {
        messagesButton.getStyleClass().remove("active");
        contactsButton.getStyleClass().remove("active");
        groupsButton.getStyleClass().remove("active");
        notificationsButton.getStyleClass().remove("active");
        btn.getStyleClass().add("active");
    }

    public void setActiveFilterTab(String filter) {
        allTab.getStyleClass().remove("active");
        unreadTab.getStyleClass().remove("active");
        groupTab.getStyleClass().remove("active");
        switch (filter) {
            case "all" -> allTab.getStyleClass().add("active");
            case "unread" -> unreadTab.getStyleClass().add("active");
            case "group" -> groupTab.getStyleClass().add("active");
        }
    }

    public void setPanelTitle(String title) {
        panelTitle.setText(title);
    }

    public void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    // ==================== PRIVATE METHODS ====================

    public void openConversation(Conversation conv) {
        currentConversationId = conv.getConversationId();
        showChatPanel();
        updateChatHeader(conv);
        conversationHandler.setActiveConversation(conv.getConversationId());
        messageHandler.loadMessages(conv.getConversationId());
        conversationHandler.markAsRead(conv.getConversationId());
    }

    private void toggleInfoSidebar() {
        boolean visible = !infoSidebar.isVisible();
        infoSidebar.setVisible(visible);
        infoSidebar.setManaged(visible);
        if (visible) {
            buildInfoSidebar();
        }
    }

    private void buildInfoSidebar() {
        infoSidebar.getChildren().clear();
        Conversation conv = conversationHandler.getConversation(currentConversationId);
        if (conv != null) {
            ConversationInfoBuilder builder = new ConversationInfoBuilder();
            builder.setMainController(this);
            ScrollPane content = builder.createInfoSidebarContent(conv);
            infoSidebar.getChildren().add(content);
        }
    }

    // ==================== LOADING MESSAGE MANAGEMENT ====================

    /**
     * Thêm loading indicator vào chat
     */
    public void addLoadingMessageToUI(VBox loadingView) {
        Platform.runLater(() -> {
            int beforeCount = chatMessagesContainer.getChildren().size();
            chatMessagesContainer.getChildren().add(loadingView);
            int afterCount = chatMessagesContainer.getChildren().size();
            System.out.println("➕ ADD LOADING VIEW:");
            System.out.println("   ID: " + loadingView.getId());
            System.out.println("   Children before: " + beforeCount);
            System.out.println("   Children after: " + afterCount);
            System.out.println("   Added successfully: " + (afterCount > beforeCount));
            scrollToBottom();
        });
    }

    /**
     * Xóa loading indicator khỏi chat
     */
    public void removeLoadingMessageFromUI(VBox loadingView) {
        Platform.runLater(() -> {
            int beforeCount = chatMessagesContainer.getChildren().size();
            System.out.println("\n➖ REMOVE LOADING VIEW:");
            System.out.println("   Looking for ID: " + loadingView.getId());
            System.out.println("   Children count before: " + beforeCount);

            boolean exists = chatMessagesContainer.getChildren().contains(loadingView);
            System.out.println("   Exists in container: " + exists);

            if (exists) {
                chatMessagesContainer.getChildren().remove(loadingView);
                int afterCount = chatMessagesContainer.getChildren().size();
                System.out.println("   Children count after: " + afterCount);
                System.out.println("   ✅ Removed successfully");
            } else {
                System.err.println("   ⚠️ Loading view NOT FOUND in container!");
                System.err.println("   Current children in container:");
                for (int i = 0; i < chatMessagesContainer.getChildren().size(); i++) {
                    Node child = chatMessagesContainer.getChildren().get(i);
                    System.err.println("   [" + i + "] " + child.getClass().getSimpleName() + " (ID: " + child.getId() + ")");
                }
            }
        });
    }

    /**
     * Getter cho chatMessagesContainer
     */
    public VBox getChatMessagesContainer() {
        return chatMessagesContainer;
    }

    // ==================== GETTERS ====================

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentConversationId() {
        return currentConversationId;
    }

    public TextArea getMessageInputArea() {
        return messageInputArea;
    }

    public VBox getLeftSidebar() {
        return leftSidebar;
    }

    public HBox getReplyPreview() {
        return replyPreview;
    }

    public Label getReplyToName() {
        return replyToName;
    }

    public Label getReplyToContent() {
        return replyToContent;
    }

    public Button getMessagesButton() {
        return messagesButton;
    }

    public Button getContactsButton() {
        return contactsButton;
    }

    public Button getGroupsButton() {
        return groupsButton;
    }

    public Button getNotificationsButton() {
        return notificationsButton;
    }

    public void markUploading(String messageId) {
        uploadingMessageIds.add(messageId);
    }

    public boolean isUploading(String messageId) {
        return uploadingMessageIds.contains(messageId);
    }

    public void clearUploading(String messageId) {
        uploadingMessageIds.remove(messageId);
    }

    // ==================== CLEANUP ====================

    public void cleanup() {
        if (emojiStickerDialog != null) {
            emojiStickerDialog.hide();
        }
        if (conversationHandler != null) conversationHandler.cleanup();
        if (messageHandler != null) messageHandler.cleanup();
        if (callHandler != null) callHandler.cleanup();
        if (fileHandler != null) fileHandler.cleanup();
    }

    public void onConversationDeleted(String conversationId) {
        System.out.println("🧹 UI handling deleted conversation: " + conversationId);
        Platform.runLater(() -> {
            infoSidebar.setVisible(false);
            infoSidebar.setManaged(false);
            infoSidebar.getChildren().clear();
        });

        chatController.resetChat();

        currentConversationId = null;

        conversationHandler.loadConversations();

        showWelcomeScreen();
    }

}