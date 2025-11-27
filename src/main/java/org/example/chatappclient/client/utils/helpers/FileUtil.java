package org.example.chatappclient.client.utils.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileUtil {

    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * Get file name without extension
     */
    public static String getNameWithoutExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return fileName;
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * Format file size
     */
    public static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Check if file is image
     */
    public static boolean isImage(String fileName) {
        String ext = getExtension(fileName).toLowerCase();
        return ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp)");
    }

    /**
     * Check if file is video
     */
    public static boolean isVideo(String fileName) {
        String ext = getExtension(fileName).toLowerCase();
        return ext.matches("\\.(mp4|avi|mov|mkv|flv|wmv)");
    }

    /**
     * Check if file is audio
     */
    public static boolean isAudio(String fileName) {
        String ext = getExtension(fileName).toLowerCase();
        return ext.matches("\\.(mp3|wav|ogg|m4a|flac)");
    }

    /**
     * Check if file is document
     */
    public static boolean isDocument(String fileName) {
        String ext = getExtension(fileName).toLowerCase();
        return ext.matches("\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt)");
    }

    /**
     * Validate file size
     */
    public static boolean isValidSize(long size, long maxSize) {
        return size > 0 && size <= maxSize;
    }

    /**
     * Validate file type
     */
    public static boolean isValidType(String fileName, String[] allowedTypes) {
        String ext = getExtension(fileName).toLowerCase();
        for (String type : allowedTypes) {
            if (ext.equals(type.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Read file to byte array
     */
    public static byte[] readFileToBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Write bytes to file
     */
    public static void writeBytesToFile(byte[] bytes, File file) throws IOException {
        Files.write(file.toPath(), bytes);
    }

    /**
     * Copy file
     */
    public static void copyFile(File source, File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Delete file
     */
    public static boolean deleteFile(File file) {
        return file.exists() && file.delete();
    }

    /**
     * Create directory if not exists
     */
    public static void ensureDirectoryExists(String path) throws IOException {
        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }

    /**
     * Get safe file name (remove special characters)
     */
    public static String getSafeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Generate unique file name
     */
    public static String generateUniqueFileName(String originalName) {
        String ext = getExtension(originalName);
        String name = getNameWithoutExtension(originalName);
        long timestamp = System.currentTimeMillis();
        return name + "_" + timestamp + ext;
    }
}
