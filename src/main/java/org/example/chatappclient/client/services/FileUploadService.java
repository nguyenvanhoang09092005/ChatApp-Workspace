package org.example.chatappclient.client.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

/**
 * Service upload file lên server/cloud (Cloudinary)
 */
public class FileUploadService {

    private static volatile FileUploadService instance;

    // Cloudinary config (should be in config file)
    private static final String CLOUDINARY_CLOUD_NAME = "your-cloud-name";
    private static final String CLOUDINARY_UPLOAD_PRESET = "your-preset";
    private static final String CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME;

    private FileUploadService() {}

    public static FileUploadService getInstance() {
        if (instance == null) {
            synchronized (FileUploadService.class) {
                if (instance == null) {
                    instance = new FileUploadService();
                }
            }
        }
        return instance;
    }

    // ==================== UPLOAD RESULT ====================

    public static class UploadResult {
        private final boolean success;
        private final String url;
        private final String publicId;
        private final String error;

        public UploadResult(boolean success, String url, String publicId, String error) {
            this.success = success;
            this.url = url;
            this.publicId = publicId;
            this.error = error;
        }

        public static UploadResult success(String url, String publicId) {
            return new UploadResult(true, url, publicId, null);
        }

        public static UploadResult failure(String error) {
            return new UploadResult(false, null, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getUrl() { return url; }
        public String getPublicId() { return publicId; }
        public String getError() { return error; }
    }

    // ==================== UPLOAD METHODS ====================

    public UploadResult uploadImage(File file) {
        return uploadToCloudinary(file, "image/upload");
    }

    public UploadResult uploadVideo(File file) {
        return uploadToCloudinary(file, "video/upload");
    }

    public UploadResult uploadAudio(File file) {
        return uploadToCloudinary(file, "video/upload"); // Cloudinary uses video endpoint for audio
    }

    public UploadResult uploadFile(File file) {
        return uploadToCloudinary(file, "raw/upload");
    }

    private UploadResult uploadToCloudinary(File file, String resourceType) {
        try {
            String uploadUrl = CLOUDINARY_URL + "/" + resourceType;
            String boundary = "===" + System.currentTimeMillis() + "===";

            HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

                // Upload preset
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                writer.append(CLOUDINARY_UPLOAD_PRESET).append("\r\n");

                // File
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: ").append(Files.probeContentType(file.toPath())).append("\r\n\r\n");
                writer.flush();

                Files.copy(file.toPath(), os);
                os.flush();

                writer.append("\r\n--").append(boundary).append("--\r\n");
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    // Parse JSON response to get URL
                    String json = response.toString();
                    String url = extractJsonValue(json, "secure_url");
                    String publicId = extractJsonValue(json, "public_id");
                    return UploadResult.success(url, publicId);
                }
            } else {
                return UploadResult.failure("Upload failed: " + responseCode);
            }

        } catch (Exception e) {
            return UploadResult.failure(e.getMessage());
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}