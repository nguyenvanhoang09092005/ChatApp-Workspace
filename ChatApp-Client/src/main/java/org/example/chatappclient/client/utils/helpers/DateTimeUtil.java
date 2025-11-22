package org.example.chatappclient.client.utils.helpers;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

// ==================== DATE TIME UTILITY ====================

public class DateTimeUtil {

    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATETIME_FORMAT = "dd/MM/yyyy HH:mm";
    public static final String FULL_DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    /**
     * Format LocalDateTime to string
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) return "";
        try {
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            return dateTime.toString();
        }
    }

    /**
     * Format to date only
     */
    public static String formatDate(LocalDateTime dateTime) {
        return format(dateTime, DATE_FORMAT);
    }

    /**
     * Format to time only
     */
    public static String formatTime(LocalDateTime dateTime) {
        return format(dateTime, TIME_FORMAT);
    }

    /**
     * Format to date and time
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return format(dateTime, DATETIME_FORMAT);
    }

    /**
     * Get relative time (vừa xong, 5 phút trước, etc.)
     */
    public static String getRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long seconds = java.time.Duration.between(dateTime, now).getSeconds();

        if (seconds < 60) return "Vừa xong";
        if (seconds < 3600) return (seconds / 60) + " phút trước";
        if (seconds < 86400) return (seconds / 3600) + " giờ trước";
        if (seconds < 604800) return (seconds / 86400) + " ngày trước";

        return format(dateTime, DATE_FORMAT);
    }

    /**
     * Get chat time format (Today, Yesterday, or date)
     */
    public static String getChatTimeFormat(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        LocalDateTime now = LocalDateTime.now();

        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return formatTime(dateTime);
        } else if (dateTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "Hôm qua";
        } else if (dateTime.getYear() == now.getYear()) {
            return format(dateTime, "dd/MM");
        } else {
            return format(dateTime, "dd/MM/yy");
        }
    }

    /**
     * Parse string to LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Check if date is today
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        return dateTime.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }

    /**
     * Check if date is yesterday
     */
    public static boolean isYesterday(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        return dateTime.toLocalDate().equals(LocalDateTime.now().toLocalDate().minusDays(1));
    }
}
