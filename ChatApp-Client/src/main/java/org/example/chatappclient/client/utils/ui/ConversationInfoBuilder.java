package org.example.chatappclient.client.utils.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.controllers.main.handlers.ConversationHandler;
import org.example.chatappclient.client.controllers.main.handlers.UIComponentFactory;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.models.Message;
import org.example.chatappclient.client.services.ConversationService;
import org.example.chatappclient.client.services.MessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConversationInfoBuilder {

    private static final String DEFAULT_AVATAR = "https://ui-avatars.com/api/?background=0084ff&color=fff&name=";
    private MainController mainController;
    private MessageService messageService;
    private List<Message> conversationMessages;
    private Conversation currentConversation;

    public ConversationInfoBuilder() {
        this.messageService = MessageService.getInstance();
        this.conversationMessages = new ArrayList<>();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Tạo nội dung info sidebar cho conversation
     */
    public ScrollPane createInfoSidebarContent(Conversation conv) {
        this.currentConversation = conv;
        loadConversationMessages(conv.getConversationId());

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white;");

        VBox mainContent = new VBox();
        mainContent.setStyle("-fx-background-color: white;");

        // Header - Avatar & Name
        VBox header = createHeader(conv);

        // Action Buttons Row
        HBox actionButtons = createActionButtons();

        // Separator
        Region separator1 = createSeparator();

        // Conversation Settings (phân biệt 1-1 và nhóm)
        VBox conversationSettings = createConversationSettings(conv.isGroup());

        // Separator
        Region separator2 = createSeparator();

        // Privacy & Support Section
        VBox privacySection = createPrivacySection();

        // Separator
        Region separator3 = createSeparator();

        // Media Section
        VBox mediaSection = createMediaSection();

        // Separator
        Region separator4 = createSeparator();

        // Group Members (chỉ hiển thị nếu là nhóm)
        VBox membersSection = createMembersSection(conv);

        // Separator (chỉ hiển thị nếu có members section)
        Region separator5 = createSeparator();
        if (!conv.isGroup()) {
            separator5.setVisible(false);
            separator5.setManaged(false);
        }

        // Files Section
        VBox filesSection = createFilesSection();

        // Separator
        Region separator6 = createSeparator();

        // Links Section
        VBox linksSection = createLinksSection();

        // Separator
        Region separator7 = createSeparator();

        // Danger Zone (phân biệt 1-1 và nhóm)
        VBox dangerZone = createDangerZone(conv.isGroup());

        mainContent.getChildren().addAll(
                header,
                actionButtons,
                separator1,
                conversationSettings,
                separator2,
                privacySection,
                separator3,
                mediaSection,
                separator4,
                membersSection,
                separator5,
                filesSection,
                separator6,
                linksSection,
                separator7,
                dangerZone
        );

        scrollPane.setContent(mainContent);
        return scrollPane;
    }

    // ==================== HEADER SECTION ====================

    private VBox createHeader(Conversation conv) {
        VBox header = new VBox(12);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20, 16, 20, 16));
        header.setStyle("-fx-background-color: white;");

        StackPane avatarContainer = new StackPane();
        ImageView avatar = createAvatar(conv.getAvatarUrl(), conv.getName(), 80);
        avatarContainer.getChildren().add(avatar);

        Label name = new Label(conv.getName());
        name.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #081c36;");

        String statusText = formatUserStatus(conv.isActive(), conv.getLastSeenTime());
        Label status = new Label(statusText);
        status.setStyle("-fx-font-size: 13px; -fx-text-fill: " +
                (conv.isActive() ? "#00c853" : "#65676b") + ";");

        header.getChildren().addAll(avatarContainer, name, status);
        return header;
    }

    // ==================== ACTION BUTTONS ====================

    private HBox createActionButtons() {
        HBox actions = new HBox(0);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(16, 16, 16, 16));
        actions.setStyle("-fx-background-color: white;");

        actions.getChildren().addAll(
                createActionButton("🔍", "Tìm kiếm"),
                createActionButton("🔔", "Tắt thông báo"),
                createActionButton("📌", "Ghim hội thoại")
        );

        return actions;
    }

    private VBox createActionButton(String icon, String label) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(110);
        box.setStyle("-fx-cursor: hand;");

        box.setOnMouseEntered(e ->
                box.setStyle("-fx-cursor: hand; -fx-background-color: #f0f2f5; -fx-background-radius: 8;")
        );
        box.setOnMouseExited(e ->
                box.setStyle("-fx-cursor: hand;")
        );

        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-background-color: #f0f2f5; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 50; " +
                        "-fx-min-width: 48px; " +
                        "-fx-min-height: 48px; " +
                        "-fx-alignment: center;"
        );

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #050505; -fx-text-alignment: center;");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(100);

        box.getChildren().addAll(iconLabel, textLabel);
        return box;
    }

    // ==================== SETTINGS SECTIONS ====================

    /**
     * ✅ Conversation settings - Phân biệt 1-1 và nhóm
     */
    private VBox createConversationSettings(boolean isGroup) {
        VBox section = new VBox(0);
        section.setPadding(new Insets(8, 0, 8, 0));
        section.setStyle("-fx-background-color: white;");

        section.getChildren().add(createSettingItem("🎨", "Đổi chủ đề", "", false));
        section.getChildren().add(createSettingItem("😊", "Biểu tượng cảm xúc", "👍", false));

        if (isGroup) {
            // Nhóm chat: Đổi tên đoạn chat
            section.getChildren().add(createSettingItem("✏️", "Đổi tên đoạn chat", "", false));
            section.getChildren().add(createSettingItem("🖼️", "Thay đổi ảnh nhóm", "", false));
        } else {
            // Chat 1-1: Đặt biệt danh
            section.getChildren().add(createSettingItem("✏️", "Đặt biệt danh", "", false));
        }

        return section;
    }

    private VBox createPrivacySection() {
        VBox section = new VBox(0);
        section.setPadding(new Insets(8, 0, 8, 0));
        section.setStyle("-fx-background-color: white;");

        section.getChildren().addAll(
                createSettingItem("⏰", "Tự động xóa tin nhắn", "Không bao giờ", false),
                createSettingItem("🔇", "Ẩn trò chuyện", "", false),
                createSettingItem("⚠️", "Báo xấu", "", false)
        );

        return section;
    }

    // ==================== LOAD MESSAGES ====================

    private void loadConversationMessages(String conversationId) {
        try {
            conversationMessages = messageService.getMessages(conversationId);
            System.out.println("✅ Loaded " + conversationMessages.size() + " messages for info sidebar");
        } catch (Exception e) {
            System.err.println("❌ Error loading messages: " + e.getMessage());
            conversationMessages = new ArrayList<>();
        }
    }

    private List<Message> getMediaMessages() {
        return conversationMessages.stream()
                .filter(msg -> "IMAGE".equalsIgnoreCase(msg.getMessageType()) ||
                        "VIDEO".equalsIgnoreCase(msg.getMessageType()))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(30)
                .collect(Collectors.toList());
    }

    private List<Message> getFileMessages() {
        return conversationMessages.stream()
                .filter(msg -> "FILE".equalsIgnoreCase(msg.getMessageType()))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(20)
                .collect(Collectors.toList());
    }

    private List<String> getLinkMessages() {
        List<String> links = new ArrayList<>();
        String urlRegex = "https?://[^\\s]+";
        for (Message msg : conversationMessages) {
            if (msg.getContent() != null && msg.getContent().matches(".*" + urlRegex + ".*")) {
                String[] words = msg.getContent().split("\\s+");
                for (String word : words) {
                    if (word.matches(urlRegex)) {
                        links.add(word);
                    }
                }
            }
        }
        return links.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    // ==================== MEDIA SECTION ====================

    private VBox createMediaSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white;");

        List<Message> mediaMessages = getMediaMessages();

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Ảnh/Video");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #050505;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewAll = new Button("Xem tất cả");
        viewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #0084ff; -fx-cursor: hand; -fx-font-size: 13px;");
        viewAll.setOnAction(e -> showAllMediaDialog(mediaMessages));

        header.getChildren().addAll(title, spacer, viewAll);

        if (mediaMessages.isEmpty()) {
            Label emptyLabel = new Label("Chưa có ảnh/video");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-padding: 20 0 20 0;");
            section.getChildren().addAll(header, emptyLabel);
            return section;
        }

        // Grid of images (3 cột)
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);

        int displayCount = Math.min(6, mediaMessages.size());

        for (int i = 0; i < displayCount; i++) {
            Message msg = mediaMessages.get(i);
            StackPane mediaBox = createMediaThumbnail(msg);
            grid.add(mediaBox, i % 3, i / 3);
        }

        section.getChildren().addAll(header, grid);
        return section;
    }

    private StackPane createMediaThumbnail(Message message) {
        StackPane container = new StackPane();
        container.setPrefSize(80, 80);
        container.setStyle(
                "-fx-background-color: #f0f2f5; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );

        container.setOnMouseEntered(e ->
                container.setStyle(
                        "-fx-background-color: #e4e6eb; " +
                                "-fx-background-radius: 8; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"
                )
        );
        container.setOnMouseExited(e ->
                container.setStyle(
                        "-fx-background-color: #f0f2f5; " +
                                "-fx-background-radius: 8; " +
                                "-fx-cursor: hand;"
                )
        );

        // ✅ Click to open full media viewer
        container.setOnMouseClicked(e -> openMediaViewer(message));

        try {
            if (message.getMediaUrl() != null && !message.getMediaUrl().isEmpty()) {
                ImageView imageView = new ImageView();
                imageView.setFitWidth(80);
                imageView.setFitHeight(80);
                imageView.setPreserveRatio(false);

                Rectangle clip = new Rectangle(80, 80);
                clip.setArcWidth(8);
                clip.setArcHeight(8);
                imageView.setClip(clip);

                Image image = new Image(message.getMediaUrl(), true);
                imageView.setImage(image);

                container.getChildren().add(imageView);

                if ("VIDEO".equalsIgnoreCase(message.getMessageType())) {
                    Label playIcon = new Label("▶");
                    playIcon.setStyle(
                            "-fx-font-size: 20px; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-color: rgba(0,0,0,0.5); " +
                                    "-fx-padding: 8; " +
                                    "-fx-background-radius: 50;"
                    );
                    container.getChildren().add(playIcon);
                }
            } else {
                Label placeholder = new Label("🖼️");
                placeholder.setStyle("-fx-font-size: 24px;");
                container.getChildren().add(placeholder);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error loading media thumbnail: " + e.getMessage());
            Label errorIcon = new Label("❌");
            errorIcon.setStyle("-fx-font-size: 24px;");
            container.getChildren().add(errorIcon);
        }

        return container;
    }

    /**
     * ✅ Mở media viewer toàn màn hình
     */
    private void openMediaViewer(Message message) {
        if (message.getMediaUrl() == null || message.getMediaUrl().isEmpty()) {
            System.err.println("❌ No media URL available");
            return;
        }

        Stage viewerStage = new Stage();
        viewerStage.initModality(Modality.APPLICATION_MODAL);
        viewerStage.initStyle(StageStyle.UNDECORATED);
        viewerStage.setTitle("Media Viewer");

        // Background overlay
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.95);");

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 50; " +
                        "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> viewerStage.close());

        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(20));

        // Media content
        if ("IMAGE".equalsIgnoreCase(message.getMessageType())) {
            ImageView imageView = new ImageView();
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(1200);
            imageView.setFitHeight(800);

            try {
                Image fullImage = new Image(message.getMediaUrl(), true);
                imageView.setImage(fullImage);

                // Info label
                Label infoLabel = new Label("📷 " + formatDate(message.getTimestamp()));
                infoLabel.setStyle(
                        "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-background-color: rgba(0,0,0,0.6); " +
                                "-fx-padding: 8 16; " +
                                "-fx-background-radius: 4;"
                );
                StackPane.setAlignment(infoLabel, Pos.BOTTOM_CENTER);
                StackPane.setMargin(infoLabel, new Insets(20));

                root.getChildren().addAll(imageView, infoLabel, closeBtn);
            } catch (Exception e) {
                Label errorLabel = new Label("❌ Không thể tải ảnh");
                errorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
                root.getChildren().addAll(errorLabel, closeBtn);
            }

        } else if ("VIDEO".equalsIgnoreCase(message.getMessageType())) {
            // Video player placeholder
            VBox videoContainer = new VBox(20);
            videoContainer.setAlignment(Pos.CENTER);

            Label videoIcon = new Label("🎥");
            videoIcon.setStyle("-fx-font-size: 80px;");

            Label videoLabel = new Label("Video Player");
            videoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");

            Label urlLabel = new Label(message.getMediaUrl());
            urlLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 14px;");
            urlLabel.setMaxWidth(800);

            Button openExternalBtn = new Button("Mở trong trình duyệt");
            openExternalBtn.setStyle(
                    "-fx-background-color: #0084ff; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 16px; " +
                            "-fx-padding: 12 24; " +
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand;"
            );
            openExternalBtn.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(message.getMediaUrl()));
                } catch (Exception ex) {
                    System.err.println("❌ Cannot open video: " + ex.getMessage());
                }
            });

            videoContainer.getChildren().addAll(videoIcon, videoLabel, urlLabel, openExternalBtn);
            root.getChildren().addAll(videoContainer, closeBtn);
        }

        // Close on click outside
        root.setOnMouseClicked(e -> {
            if (e.getTarget() == root) {
                viewerStage.close();
            }
        });

        // ESC to close
        Scene scene = new Scene(root, 1400, 900);
        scene.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ESCAPE")) {
                viewerStage.close();
            }
        });

        viewerStage.setScene(scene);
        viewerStage.show();
    }

    private void showAllMediaDialog(List<Message> mediaMessages) {
        System.out.println("→ Opening all media dialog with " + mediaMessages.size() + " items");
        // TODO: Implement full media gallery
    }

    // ==================== MEMBERS SECTION ====================

    private VBox createMembersSection(Conversation conv) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white;");

        if (!conv.isGroup()) {
            section.setVisible(false);
            section.setManaged(false);
            return section;
        }

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("👥");
        icon.setStyle("-fx-font-size: 20px;");

        int memberCount = conv.getMemberIds() != null ? conv.getMemberIds().size() : 0;
        Label title = new Label(memberCount + " thành viên");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #050505;");

        header.getChildren().addAll(icon, title);

        VBox membersList = new VBox(0);
        if (conv.getMemberIds() != null) {
            int displayCount = Math.min(5, conv.getMemberIds().size());
            for (int i = 0; i < displayCount; i++) {
                String memberId = conv.getMemberIds().get(i);
                membersList.getChildren().add(createMemberItem(memberId, "Member " + (i + 1)));
            }
        }

        Button viewAllMembers = new Button("Xem tất cả thành viên");
        viewAllMembers.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #0084ff; " +
                        "-fx-cursor: hand; " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 8 0 0 0;"
        );

        section.getChildren().addAll(header, membersList, viewAllMembers);
        return section;
    }

    private HBox createMemberItem(String memberId, String name) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 16, 8, 16));
        item.setStyle("-fx-cursor: hand;");

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #f0f2f5; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-cursor: hand;"));

        ImageView avatar = createAvatar(null, name, 40);

        VBox textBox = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #050505;");

        Label roleLabel = new Label("Thành viên");
        roleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");

        textBox.getChildren().addAll(nameLabel, roleLabel);
        item.getChildren().addAll(avatar, textBox);

        return item;
    }

    // ==================== FILES SECTION ====================

    private VBox createFilesSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white;");

        List<Message> fileMessages = getFileMessages();

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("File");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #050505;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewAll = new Button("Xem tất cả");
        viewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #0084ff; -fx-cursor: hand; -fx-font-size: 13px;");
        viewAll.setOnAction(e -> showAllFilesDialog(fileMessages));

        header.getChildren().addAll(title, spacer, viewAll);

        if (fileMessages.isEmpty()) {
            Label emptyLabel = new Label("Chưa có file");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-padding: 20 0 20 0;");
            section.getChildren().addAll(header, emptyLabel);
            return section;
        }

        VBox filesList = new VBox(0);
        int displayCount = Math.min(5, fileMessages.size());

        for (int i = 0; i < displayCount; i++) {
            Message msg = fileMessages.get(i);
            filesList.getChildren().add(
                    createFileItem(
                            msg.getFileName() != null ? msg.getFileName() : "File",
                            formatFileSize(msg.getFileSize()),
                            formatDate(msg.getTimestamp()),
                            msg.getMediaUrl()
                    )
            );
        }

        section.getChildren().addAll(header, filesList);
        return section;
    }

    private HBox createFileItem(String name, String size, String date, String url) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 16, 8, 16));
        item.setStyle("-fx-cursor: hand;");

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #f0f2f5; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-cursor: hand;"));

        item.setOnMouseClicked(e -> {
            if (url != null && !url.isEmpty()) {
                try {
                    System.out.println("→ Opening file: " + url);
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    System.err.println("❌ Cannot open file: " + ex.getMessage());
                }
            }
        });

        String fileIcon = getFileIconByName(name);
        Label icon = new Label(fileIcon);
        icon.setStyle("-fx-font-size: 32px;");

        VBox fileInfo = new VBox(2);
        HBox.setHgrow(fileInfo, Priority.ALWAYS);

        Label fileName = new Label(name);
        fileName.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #050505;");
        fileName.setMaxWidth(200);

        Label fileSize = new Label(size);
        fileSize.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");

        fileInfo.getChildren().addAll(fileName, fileSize);

        Label dateLabel = new Label(date);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");

        item.getChildren().addAll(icon, fileInfo, dateLabel);
        return item;
    }

    private void showAllFilesDialog(List<Message> fileMessages) {
        System.out.println("→ Opening all files dialog with " + fileMessages.size() + " items");
    }

    private String getFileIconByName(String fileName) {
        if (fileName == null) return "📄";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z")) return "📦";
        if (lower.endsWith(".pdf")) return "📕";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "📘";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "📗";
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "📙";
        if (lower.endsWith(".txt")) return "📝";
        if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".gif")) return "🖼️";
        if (lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov")) return "🎥";
        if (lower.endsWith(".mp3") || lower.endsWith(".wav")) return "🎵";
        return "📄";
    }

    // ==================== LINKS SECTION ====================

    private VBox createLinksSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color: white;");

        List<String> links = getLinkMessages();

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Link");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #050505;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewAll = new Button("Xem tất cả");
        viewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #0084ff; -fx-cursor: hand; -fx-font-size: 13px;");
        viewAll.setOnAction(e -> showAllLinksDialog(links));

        header.getChildren().addAll(title, spacer, viewAll);

        if (links.isEmpty()) {
            Label emptyLabel = new Label("Chưa có link");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-padding: 20 0 20 0;");
            section.getChildren().addAll(header, emptyLabel);
            return section;
        }

        VBox linksList = new VBox(0);
        int displayCount = Math.min(5, links.size());

        for (int i = 0; i < displayCount; i++) {
            String link = links.get(i);
            linksList.getChildren().add(createLinkItem(link));
        }

        section.getChildren().addAll(header, linksList);
        return section;
    }

    private HBox createLinkItem(String url) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 16, 8, 16));
        item.setStyle("-fx-cursor: hand;");

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #f0f2f5; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-cursor: hand;"));

        item.setOnMouseClicked(e -> {
            try {
                System.out.println("→ Opening link: " + url);
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                System.err.println("❌ Cannot open link: " + ex.getMessage());
            }
        });

        Label linkIcon = new Label("🔗");
        linkIcon.setStyle("-fx-font-size: 24px;");

        VBox linkInfo = new VBox(2);
        HBox.setHgrow(linkInfo, Priority.ALWAYS);

        String domain = extractDomain(url);
        Label linkTitle = new Label(domain);
        linkTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #050505;");

        Label linkUrl = new Label(truncateUrl(url, 40));
        linkUrl.setStyle("-fx-font-size: 12px; -fx-text-fill: #0084ff;");
        linkUrl.setMaxWidth(250);

        linkInfo.getChildren().addAll(linkTitle, linkUrl);

        item.getChildren().addAll(linkIcon, linkInfo);
        return item;
    }

    private void showAllLinksDialog(List<String> links) {
        System.out.println("→ Opening all links dialog with " + links.size() + " items");
    }

    private String extractDomain(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            return host != null ? host : url;
        } catch (Exception e) {
            return url;
        }
    }

    private String truncateUrl(String url, int maxLength) {
        if (url.length() <= maxLength) return url;
        return url.substring(0, maxLength - 3) + "...";
    }

    // ==================== DANGER ZONE ====================

    /**
     * ✅ Danger Zone - Phân biệt 1-1 và nhóm
     */
    private VBox createDangerZone(boolean isGroup) {
        VBox section = new VBox(0);
        section.setPadding(new Insets(8, 0, 20, 0));
        section.setStyle("-fx-background-color: white;");

        // Xóa lịch sử trò chuyện - Hiển thị cho cả 1-1 và nhóm
        HBox deleteItem = createSettingItem("🗑️", "Xóa lịch sử trò chuyện", "", true);
        deleteItem.setOnMouseClicked(e -> confirmDeleteConversation());
        section.getChildren().add(deleteItem);

        if (isGroup) {
            // Chỉ hiển thị "Rời nhóm" cho nhóm chat
            section.getChildren().add(createSettingItem("📤", "Rời nhóm", "", true));
        } else {
            // Chỉ hiển thị "Chặn người dùng" cho chat 1-1
            section.getChildren().add(createSettingItem("🚫", "Chặn người dùng", "", true));
        }
//
//        // Báo xấu - Hiển thị cho cả 1-1 và nhóm
//        section.getChildren().add(createSettingItem("⚠️", "Báo xấu", "", true));

        return section;
    }

    private void confirmDeleteConversation() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);"
        );

        Label title = new Label("Xóa cuộc trò chuyện?");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        // ✅ Message rõ ràng hơn
        Label desc = new Label(
                "Cuộc trò chuyện sẽ bị xóa khỏi danh sách của bạn.\n" +
                        "Người khác vẫn có thể thấy cuộc trò chuyện này.\n" +
                        "Nếu họ gửi tin nhắn mới, cuộc trò chuyện sẽ xuất hiện lại."
        );
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #65676b;");
        desc.setWrapText(true);
        desc.setAlignment(Pos.CENTER);

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Hủy");
        cancelBtn.setStyle(
                "-fx-background-color: #e4e6eb;" +
                        "-fx-text-fill: #050505;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> dialog.close());

        Button deleteBtn = new Button("Xóa");
        deleteBtn.setStyle(
                "-fx-background-color: #dc3545;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> {
            deleteConversation();
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, deleteBtn);
        root.getChildren().addAll(title, desc, buttons);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.show();
    }

    private void deleteConversation() {
        if (currentConversation == null) return;

        try {
            String conversationId = currentConversation.getConversationId();
            String userId = mainController.getCurrentUser().getUserId();

            System.out.println("🗑️ Deleting conversation for current user only:");
            System.out.println("   ConversationID: " + conversationId);
            System.out.println("   UserID: " + userId);

            ConversationService.getInstance().deleteConversationForCurrentUser(conversationId);

            System.out.println("✅ Conversation deleted for current user");

            currentConversation = null;
            conversationMessages.clear();

            if (mainController != null) {
                Platform.runLater(() -> {
                    // ✅ GỌI callback trong MainController
                    mainController.onConversationDeleted(conversationId);

                    AlertUtil.showToastSuccess("Đã xóa cuộc trò chuyện");
                });
            }

        } catch (Exception e) {
            System.err.println("❌ Delete conversation failed: " + e.getMessage());
            e.printStackTrace();

            Platform.runLater(() -> {
                AlertUtil.showError("Lỗi", "Không thể xóa cuộc trò chuyện: " + e.getMessage());
            });
        }
    }
    // ==================== HELPER METHODS ====================

    private HBox createSettingItem(String icon, String text, String value, boolean danger) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setStyle("-fx-cursor: hand;");

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #f0f2f5; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-cursor: hand;"));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (danger ? "#dc3545" : "#050505") + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox rightSide = new HBox(8);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        if (!value.isEmpty()) {
            Label valueLabel = new Label(value);
            valueLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #65676b;");
            rightSide.getChildren().add(valueLabel);
        }

        Label arrow = new Label("›");
        arrow.setStyle("-fx-font-size: 18px; -fx-text-fill: #65676b;");
        rightSide.getChildren().add(arrow);

        item.getChildren().addAll(iconLabel, textLabel, spacer, rightSide);
        return item;
    }

    private Region createSeparator() {
        Region separator = new Region();
        separator.setPrefHeight(8);
        separator.setStyle("-fx-background-color: #f0f2f5;");
        return separator;
    }

    private ImageView createAvatar(String url, String name, int size) {
        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        loadAvatar(iv, url, name, size);
        return iv;
    }

    private void loadAvatar(ImageView imageView, String url, String name, int size) {
        try {
            String imgUrl = (url != null && !url.isEmpty())
                    ? url
                    : DEFAULT_AVATAR + name.replace(" ", "+") + "&size=" + size;
            imageView.setImage(new Image(imgUrl, true));
        } catch (Exception e) {
            imageView.setImage(new Image(DEFAULT_AVATAR + "U&size=" + size, true));
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatUserStatus(boolean isOnline, LocalDateTime lastSeen) {
        if (isOnline) {
            return "Đang hoạt động";
        }

        if (lastSeen == null) {
            return "Không hoạt động";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = ChronoUnit.MINUTES.between(lastSeen, now);
        long hoursAgo = ChronoUnit.HOURS.between(lastSeen, now);
        long daysAgo = ChronoUnit.DAYS.between(lastSeen, now);

        if (daysAgo >= 1) {
            return "Không hoạt động";
        }

        if (hoursAgo < 1) {
            if (minutesAgo < 1) {
                return "Hoạt động vừa xong";
            }
            return "Hoạt động " + minutesAgo + " phút trước";
        }

        return "Hoạt động " + hoursAgo + " giờ trước";
    }
}