package org.example.chatappclient.client.controllers.main;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.example.chatappclient.client.controllers.main.handlers.*;
import org.example.chatappclient.client.models.*;
import org.example.chatappclient.client.services.*;

import java.util.List;

/**
 * MainController - Chỉ xử lý hiển thị giao diện chính
 * Các chức năng được delegate sang các Handler tương ứng
 */
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
    @FXML private VBox chatPanel;
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
    @FXML private Button stickerButton, imageButton, attachButton;
    @FXML private TextArea messageInputArea;
    @FXML private Button voiceButton, likeButton, sendButton;

    // Welcome & Info Sidebar
    @FXML private VBox welcomeScreen, infoSidebar;
    @FXML private Button startChatButton;

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

    // ==================== INITIALIZATION ====================
    @FXML
    public void initialize() {
        System.out.println("=== MainController Initialize ===");

        // Initialize services
        initServices();

        // Get current user
        currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            // Create navigation handler first to navigate to login
            navigationHandler = new NavigationHandler(this);
            navigationHandler.navigateToLogin(leftSidebar);
            return;
        }

        // Initialize handlers
        initHandlers();

        // Setup UI
        setupUI();

        // Bind event handlers
        bindEventHandlers();

        // Load initial data
        loadInitialData();

        // Show welcome screen
        showWelcomeScreen();
    }

    private void initServices() {
        authService = AuthService.getInstance();
        conversationService = ConversationService.getInstance();
        messageService = MessageService.getInstance();
        userService = UserService.getInstance();
    }

    private void initHandlers() {
        uiFactory = new UIComponentFactory();
        navigationHandler = new NavigationHandler(this);
        conversationHandler = new ConversationHandler(this, conversationService, uiFactory);
        messageHandler = MessageHandler.getInstance();
        messageHandler.setMainController(this); // IMPORTANT: Inject MainController
        callHandler = new CallHandler(this);
        fileHandler = new FileHandler(this);
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

        // Messaging - FIXED
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
        stickerButton.setOnAction(e -> fileHandler.showStickerPicker());
        voiceButton.setOnAction(e -> fileHandler.recordVoice());

        // Calls
        audioCallButton.setOnAction(e -> callHandler.startAudioCall());
        videoCallButton.setOnAction(e -> callHandler.startVideoCall());

        // Chat info
        chatInfoButton.setOnAction(e -> toggleInfoSidebar());
    }

    private void loadInitialData() {
        conversationHandler.loadConversations();
    }

    // ==================== PUBLIC UI METHODS (được gọi từ Handlers) ====================

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
        Platform.runLater(() -> {
            chatMessagesContainer.getChildren().clear();

            if (messages.isEmpty()) {
                chatMessagesContainer.getChildren().add(
                        uiFactory.createCenteredLabel("Hãy bắt đầu cuộc trò chuyện!")
                );
                return;
            }

            String lastDate = null;
            String lastSender = null;

            for (Message msg : messages) {
                String msgDate = uiFactory.extractDate(msg.getTimestamp());

                // Date separator
                if (!msgDate.equals(lastDate)) {
                    chatMessagesContainer.getChildren().add(uiFactory.createDateSeparator(msgDate));
                    lastDate = msgDate;
                    lastSender = null;
                }

                boolean consecutive = msg.getSenderId().equals(lastSender);
                HBox bubble = uiFactory.createMessageBubble(msg, currentUser.getUserId(), consecutive);
                chatMessagesContainer.getChildren().add(bubble);
                lastSender = msg.getSenderId();
            }

            scrollToBottom();
        });
    }

    public void addMessageToUI(Message msg) {
        Platform.runLater(() -> {
            HBox bubble = uiFactory.createMessageBubble(msg, currentUser.getUserId(), false);
            chatMessagesContainer.getChildren().add(bubble);
            scrollToBottom();
        });
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

    public  void openConversation(Conversation conv) {
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
            infoSidebar.getChildren().add(uiFactory.createInfoSidebarContent(conv));
        }
    }

    // ==================== GETTERS ====================

    public User getCurrentUser() { return currentUser; }
    public String getCurrentConversationId() { return currentConversationId; }
    public TextArea getMessageInputArea() { return messageInputArea; }
    public VBox getLeftSidebar() { return leftSidebar; }
    public HBox getReplyPreview() { return replyPreview; }
    public Label getReplyToName() { return replyToName; }
    public Label getReplyToContent() { return replyToContent; }
    public Button getMessagesButton() { return messagesButton; }
    public Button getContactsButton() { return contactsButton; }
    public Button getGroupsButton() { return groupsButton; }
    public Button getNotificationsButton() { return notificationsButton; }

    // ==================== CLEANUP ====================

    public void cleanup() {
        if (conversationHandler != null) conversationHandler.cleanup();
        if (messageHandler != null) messageHandler.cleanup();
        if (callHandler != null) callHandler.cleanup();
        if (fileHandler != null) fileHandler.cleanup();
    }
}