package org.example.chatappclient.client.utils.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class để load và cache icons
 * Giúp tránh load lại icon nhiều lần, tăng hiệu năng
 */
public class IconLoader {

    // Cache để lưu icons đã load
    private static final Map<String, Image> iconCache = new HashMap<>();

    // Icon paths
    public static final String MIC_ON = "/icons/mic_on.png";
    public static final String MIC_OFF = "/icons/mic_off.png";
    public static final String SPEAKER_ON = "/icons/speaker_on.png";
    public static final String SPEAKER_OFF = "/icons/speaker_off.png";
    public static final String VIDEO_ON = "/icons/video_on.png";
    public static final String VIDEO_OFF = "/icons/video_off.png";
    public static final String SWITCH_CAMERA = "/icons/switch_camera.png";
    public static final String HANG_UP = "/icons/hang_up.png";
    public static final String PHONE_ACCEPT = "/icons/phone_accept.png";
    public static final String PHONE_DECLINE = "/icons/phone_decline.png";

    /**
     * Load icon từ resources với cache
     */
    public static Image loadIcon(String iconPath) {
        // Kiểm tra cache trước
        if (iconCache.containsKey(iconPath)) {
            return iconCache.get(iconPath);
        }

        try {
            InputStream stream = IconLoader.class.getResourceAsStream(iconPath);
            if (stream == null) {
                System.err.println("❌ Không tìm thấy icon: " + iconPath);
                return null;
            }

            Image image = new Image(stream);
            iconCache.put(iconPath, image);

            System.out.println("✅ Loaded icon: " + iconPath);
            return image;

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi load icon: " + iconPath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Tạo ImageView từ icon path với kích thước tùy chỉnh
     */
    public static ImageView createIconView(String iconPath, double size) {
        return createIconView(iconPath, size, size);
    }

    /**
     * Tạo ImageView từ icon path với width và height riêng
     */
    public static ImageView createIconView(String iconPath, double width, double height) {
        Image icon = loadIcon(iconPath);

        if (icon == null) {
            // Fallback: tạo placeholder
            ImageView placeholder = new ImageView();
            placeholder.setFitWidth(width);
            placeholder.setFitHeight(height);
            return placeholder;
        }

        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(width);
        iconView.setFitHeight(height);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);

        return iconView;
    }

    /**
     * Preload tất cả icons khi app khởi động
     */
    public static void preloadAllIcons() {
        System.out.println("🔄 Preloading icons...");

        String[] allIcons = {
                MIC_ON, MIC_OFF,
                SPEAKER_ON, SPEAKER_OFF,
                VIDEO_ON, VIDEO_OFF,
                SWITCH_CAMERA,
                HANG_UP,
                PHONE_ACCEPT, PHONE_DECLINE
        };

        int loaded = 0;
        for (String iconPath : allIcons) {
            if (loadIcon(iconPath) != null) {
                loaded++;
            }
        }

        System.out.println(String.format("✅ Preloaded %d/%d icons", loaded, allIcons.length));
    }

    public static void clearCache() {
        iconCache.clear();
        System.out.println("🗑️ Icon cache cleared");
    }

    public static boolean iconExists(String iconPath) {
        InputStream stream = IconLoader.class.getResourceAsStream(iconPath);
        return stream != null;
    }

    public static String getCacheInfo() {
        return String.format("Icon cache: %d items", iconCache.size());
    }
}