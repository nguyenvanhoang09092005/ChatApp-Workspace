package org.example.chatappclient.client.utils.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import org.example.chatappclient.client.utils.data.EmojiData;
import org.example.chatappclient.client.utils.data.StickerData;

import java.util.List;
import java.util.function.Consumer;

/**
 * EmojiStickerDialog - Dialog hiển thị emoji và sticker như Zalo
 */
public class EmojiStickerDialog {

    private Popup popup;
    private Consumer<String> onEmojiSelected;
    private Consumer<StickerData.Sticker> onStickerSelected;

    private TabPane tabPane;
    private VBox emojiPane;
    private VBox stickerPane;

    private static final int EMOJI_SIZE = 32;
    private static final int EMOJIS_PER_ROW = 8;
    private static final int STICKER_SIZE = 80;
    private static final int STICKERS_PER_ROW = 4;

    public EmojiStickerDialog() {
        createUI();
    }

    private void createUI() {
        popup = new Popup();
        popup.setAutoHide(true);

        // Main container
        VBox container = new VBox();
        container.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2); " +
                        "-fx-padding: 0;"
        );
        container.setPrefSize(360, 420);

        // TabPane để chuyển đổi giữa Emoji và Sticker
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-padding: 0;");

        // Tab Emoji
        Tab emojiTab = new Tab("😀 Emoji");
        emojiPane = createEmojiPane();
        emojiTab.setContent(emojiPane);

        // Tab Sticker
        Tab stickerTab = new Tab("🎨 Sticker");
        stickerPane = createStickerPane();
        stickerTab.setContent(stickerPane);

        tabPane.getTabs().addAll(emojiTab, stickerTab);
        container.getChildren().add(tabPane);

        popup.getContent().add(container);
    }

    /**
     * Tạo pane hiển thị emoji
     */
    private VBox createEmojiPane() {
        VBox container = new VBox(8);
        container.setPadding(new Insets(8));

        // Search box
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Tìm emoji...");
        searchField.setStyle(
                "-fx-background-radius: 20; " +
                        "-fx-padding: 8 16 8 16; " +
                        "-fx-background-color: #f0f2f5;"
        );

        // Frequently used
        Label frequentLabel = new Label("⭐ Gần đây");
        frequentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #65676B;");

        FlowPane frequentPane = createEmojiGrid(EmojiData.getFrequentlyUsed());

        // Categories
        ScrollPane categoriesScroll = new ScrollPane();
        categoriesScroll.setFitToWidth(true);
        categoriesScroll.setStyle("-fx-background-color: transparent;");
        categoriesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoriesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox categoriesContainer = new VBox(12);
        categoriesContainer.setPadding(new Insets(8, 0, 8, 0));

        for (String category : EmojiData.getCategoryNames()) {
            Label categoryLabel = new Label(category);
            categoryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #65676B;");

            List<String> emojis = EmojiData.getEmojisForCategory(category);
            FlowPane emojiGrid = createEmojiGrid(emojis);

            categoriesContainer.getChildren().addAll(categoryLabel, emojiGrid);
        }

        categoriesScroll.setContent(categoriesContainer);

        // Search functionality
        searchField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                List<String> results = EmojiData.searchEmojis(newVal);
                categoriesContainer.getChildren().clear();

                Label resultLabel = new Label("Kết quả tìm kiếm");
                resultLabel.setStyle("-fx-font-weight: bold;");
                categoriesContainer.getChildren().addAll(resultLabel, createEmojiGrid(results));
            } else {
                // Reset to all categories
                categoriesContainer.getChildren().clear();
                for (String category : EmojiData.getCategoryNames()) {
                    Label categoryLabel = new Label(category);
                    categoryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                    categoriesContainer.getChildren().addAll(
                            categoryLabel,
                            createEmojiGrid(EmojiData.getEmojisForCategory(category))
                    );
                }
            }
        });

        container.getChildren().addAll(searchField, frequentLabel, frequentPane, categoriesScroll);
        VBox.setVgrow(categoriesScroll, Priority.ALWAYS);

        return container;
    }

    /**
     * Tạo grid hiển thị emoji
     */
    private FlowPane createEmojiGrid(List<String> emojis) {
        FlowPane grid = new FlowPane(4, 4);
        grid.setAlignment(Pos.TOP_LEFT);

        for (String emoji : emojis) {
            Button btn = new Button(emoji);
            btn.setFont(Font.font(EMOJI_SIZE));
            btn.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-cursor: hand; " +
                            "-fx-padding: 4;"
            );
            btn.setPrefSize(EMOJI_SIZE + 16, EMOJI_SIZE + 16);

            btn.setOnMouseEntered(e ->
                    btn.setStyle(
                            "-fx-background-color: #f0f2f5; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-padding: 4;"
                    )
            );
            btn.setOnMouseExited(e ->
                    btn.setStyle(
                            "-fx-background-color: transparent; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-padding: 4;"
                    )
            );

            btn.setOnAction(e -> {
                if (onEmojiSelected != null) {
                    onEmojiSelected.accept(emoji);
                }
                popup.hide();
            });

            grid.getChildren().add(btn);
        }

        return grid;
    }

    /**
     * Tạo pane hiển thị sticker
     */
    private VBox createStickerPane() {
        VBox container = new VBox(8);
        container.setPadding(new Insets(8));

        // Sticker packs tabs
        TabPane packsTabPane = new TabPane();
        packsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        packsTabPane.setStyle("-fx-padding: 0;");

        List<StickerData.StickerPack> packs = StickerData.getAllPacks();

        for (StickerData.StickerPack pack : packs) {
            Tab packTab = new Tab(pack.getName());
            if (pack.isPremium()) {
                packTab.setText(pack.getName() + " 👑");
            }

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent;");

            FlowPane stickerGrid = createStickerGrid(pack.getStickers());
            scrollPane.setContent(stickerGrid);

            packTab.setContent(scrollPane);
            packsTabPane.getTabs().add(packTab);
        }

        container.getChildren().add(packsTabPane);
        VBox.setVgrow(packsTabPane, Priority.ALWAYS);

        return container;
    }

    /**
     * Tạo grid hiển thị sticker
     */
    private FlowPane createStickerGrid(List<StickerData.Sticker> stickers) {
        FlowPane grid = new FlowPane(8, 8);
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setPadding(new Insets(8));

        for (StickerData.Sticker sticker : stickers) {
            VBox stickerBox = new VBox(4);
            stickerBox.setAlignment(Pos.CENTER);
            stickerBox.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-cursor: hand; " +
                            "-fx-padding: 8; " +
                            "-fx-background-radius: 8;"
            );
            stickerBox.setPrefSize(STICKER_SIZE + 16, STICKER_SIZE + 16);

            // Placeholder cho sticker image
            // Trong thực tế, bạn sẽ load image từ URL
            Label stickerLabel = new Label(sticker.getName().substring(0, 1));
            stickerLabel.setStyle(
                    "-fx-font-size: 48px; " +
                            "-fx-background-color: #f0f2f5; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 16;"
            );
            stickerLabel.setPrefSize(STICKER_SIZE, STICKER_SIZE);

            stickerBox.setOnMouseEntered(e ->
                    stickerBox.setStyle(
                            "-fx-background-color: #f0f2f5; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-padding: 8; " +
                                    "-fx-background-radius: 8;"
                    )
            );
            stickerBox.setOnMouseExited(e ->
                    stickerBox.setStyle(
                            "-fx-background-color: transparent; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-padding: 8; " +
                                    "-fx-background-radius: 8;"
                    )
            );

            stickerBox.setOnMouseClicked(e -> {
                if (onStickerSelected != null) {
                    onStickerSelected.accept(sticker);
                }
                popup.hide();
            });

            stickerBox.getChildren().add(stickerLabel);
            grid.getChildren().add(stickerBox);
        }

        return grid;
    }

    /**
     * Hiển thị dialog tại vị trí chỉ định
     */
    public void show(javafx.scene.Node owner, double x, double y) {
        popup.show(owner, x, y);
    }

    /**
     * Đóng dialog
     */
    public void hide() {
        popup.hide();
    }

    /**
     * Set callback khi chọn emoji
     */
    public void setOnEmojiSelected(Consumer<String> callback) {
        this.onEmojiSelected = callback;
    }

    /**
     * Set callback khi chọn sticker
     */
    public void setOnStickerSelected(Consumer<StickerData.Sticker> callback) {
        this.onStickerSelected = callback;
    }
}