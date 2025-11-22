package org.example.chatappclient.client.utils.helpers;

public class ImageUtil {

    public static boolean isValidImageUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase();
        return lower.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$") ||
                lower.startsWith("data:image/") ||
                lower.contains("ui-avatars.com") ||
                lower.contains("picsum.photos");
    }

    /**
     * Generate avatar URL from name
     */
    public static String generateAvatarUrl(String name) {
        if (name == null || name.isEmpty()) {
            name = "User";
        }
        String encoded = name.replace(" ", "+");
        return "https://ui-avatars.com/api/?background=0084ff&color=fff&name=" + encoded;
    }

    /**
     * Generate random placeholder image URL
     */
    public static String generatePlaceholderUrl(int width, int height) {
        return "https://picsum.photos/" + width + "/" + height;
    }

    /**
     * Get thumbnail URL from image URL
     */
    public static String getThumbnailUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return "";
        // For Cloudinary URLs, add transformation
        if (imageUrl.contains("cloudinary.com")) {
            return imageUrl.replace("/upload/", "/upload/w_200,h_200,c_fill/");
        }
        return imageUrl;
    }
}
