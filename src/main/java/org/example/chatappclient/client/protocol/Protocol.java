package org.example.chatappclient.client.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Protocol {

    // ==================== AUTH COMMANDS ====================
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String LOGOUT = "LOGOUT";
    public static final String FORGOT_PASSWORD = "FORGOT_PASSWORD";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
    public static final String RESEND_VERIFICATION = "RESEND_VERIFICATION";
    public static final String AUTH_CHECK_SESSION = "CHECK_SESSION";
    public static final String AUTH_REFRESH_TOKEN = "REFRESH_TOKEN";

    // ==================== USER COMMANDS ====================
    public static final String USER_GET_PROFILE = "USER_GET_PROFILE";
    public static final String USER_UPDATE_PROFILE = "USER_UPDATE_PROFILE";
    public static final String USER_UPDATE_AVATAR = "USER_UPDATE_AVATAR";
    public static final String USER_UPDATE_STATUS = "USER_UPDATE_STATUS";
    public static final String USER_CHANGE_PASSWORD = "USER_CHANGE_PASSWORD";
    public static final String USER_SEARCH = "USER_SEARCH";
    public static final String USER_GET_ONLINE_STATUS = "USER_GET_ONLINE_STATUS";

    // ==================== CONTACT COMMANDS ====================
    public static final String CONTACT_GET_ALL = "CONTACT_GET_ALL";
    public static final String CONTACT_ADD = "CONTACT_ADD";
    public static final String CONTACT_REMOVE = "CONTACT_REMOVE";
    public static final String CONTACT_BLOCK = "CONTACT_BLOCK";
    public static final String CONTACT_UNBLOCK = "CONTACT_UNBLOCK";
    public static final String CONTACT_GET_BLOCKED = "CONTACT_GET_BLOCKED";
    public static final String CONTACT_REQUEST_SEND = "CONTACT_REQUEST_SEND";
    public static final String CONTACT_REQUEST_ACCEPT = "CONTACT_REQUEST_ACCEPT";
    public static final String CONTACT_REQUEST_REJECT = "CONTACT_REQUEST_REJECT";
    public static final String CONTACT_REQUEST_LIST = "CONTACT_REQUEST_LIST";

    // ==================== MESSAGE COMMANDS ====================
    public static final String MESSAGE_SEND = "MESSAGE_SEND";
    public static final String MESSAGE_RECEIVE = "MESSAGE_RECEIVE";
    public static final String MESSAGE_DELETE = "MESSAGE_DELETE";
    public static final String MESSAGE_EDIT = "MESSAGE_EDIT";
    public static final String MESSAGE_RECALL = "MESSAGE_RECALL";
    public static final String MESSAGE_FORWARD = "MESSAGE_FORWARD";
    public static final String MESSAGE_REACT = "MESSAGE_REACT";
    public static final String MESSAGE_GET_HISTORY = "MESSAGE_GET_HISTORY";
    public static final String MESSAGE_MARK_READ = "MESSAGE_MARK_READ";
    public static final String MESSAGE_DELIVERED = "MESSAGE_DELIVERED";
    public static final String MESSAGE_READ = "MESSAGE_READ";
    public static final String TYPING_START = "TYPING_START";
    public static final String TYPING_STOP = "TYPING_STOP";

    // ==================== CONVERSATION COMMANDS ====================
    public static final String CONVERSATION_GET_ALL = "CONVERSATION_GET_ALL";
    public static final String CONVERSATION_GET = "CONVERSATION_GET";
    public static final String CONVERSATION_GET_BY_ID = "CONVERSATION_GET_BY_ID";
    public static final String CONVERSATION_CREATE = "CONVERSATION_CREATE";
    public static final String CONVERSATION_CREATE_GROUP = "CONVERSATION_CREATE_GROUP";
    public static final String CONVERSATION_DELETE = "CONVERSATION_DELETE";
    public static final String CONVERSATION_MUTE = "CONVERSATION_MUTE";
    public static final String CONVERSATION_UNMUTE = "CONVERSATION_UNMUTE";
    public static final String CONVERSATION_PIN = "CONVERSATION_PIN";
    public static final String CONVERSATION_ARCHIVE = "CONVERSATION_ARCHIVE";
    public static final String CONVERSATION_UNARCHIVE = "CONVERSATION_UNARCHIVE";

    // ==================== CALL COMMANDS ====================
    public static final String CALL_START = "CALL_START";
    public static final String CALL_ANSWER = "CALL_ANSWER";
    public static final String CALL_REJECT = "CALL_REJECT";
    public static final String CALL_END = "CALL_END";
    public static final String CALL_INCOMING = "CALL_INCOMING";

    // ==================== FILE COMMANDS ====================
    public static final String FILE_UPLOAD = "FILE_UPLOAD";
    public static final String FILE_DOWNLOAD = "FILE_DOWNLOAD";
    public static final String FILE_DELETE = "FILE_DELETE";
    public static final String FILE_GET_INFO = "FILE_GET_INFO";

    // ==================== NOTIFICATION COMMANDS ====================
    public static final String NOTIFICATION_GET_ALL = "NOTIFICATION_GET_ALL";
    public static final String NOTIFICATION_MARK_READ = "NOTIFICATION_MARK_READ";
    public static final String NOTIFICATION_DELETE = "NOTIFICATION_DELETE";
    public static final String NOTIFICATION_CLEAR_ALL = "NOTIFICATION_CLEAR_ALL";
    public static final String NOTIFICATION_NEW = "NOTIFICATION_NEW";

    // ==================== RESPONSE STATUS ====================
    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String SERVER_ERROR = "SERVER_ERROR";

    // ==================== MESSAGE DELIMITERS ====================
    public static final String DELIMITER = "|||";
    public static final String FIELD_DELIMITER = "::";
    public static final String LIST_DELIMITER = ",";
    public static final String END_OF_MESSAGE = "\n";

    // ==================== UTILITY METHODS ====================

    /**
     * Build request message
     */
    public static String buildRequest(String command, String... params) {
        StringBuilder sb = new StringBuilder(command);
        for (String param : params) {
            sb.append(DELIMITER).append(param != null ? param : "");
        }
        return sb.toString();
    }

    /**
     * Build response message
     */
    public static String buildResponse(String status, String message, String... data) {
        StringBuilder sb = new StringBuilder(status);
        sb.append(DELIMITER).append(message != null ? message : "");
        for (String item : data) {
            sb.append(DELIMITER).append(item != null ? item : "");
        }
        return sb.toString();
    }

    /**
     * Parse message parts
     */
    public static String[] parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return new String[0];
        }
        return message.split(Pattern.quote(DELIMITER), -1);
    }

    /**
     * Get command from message
     */
    public static String getCommand(String message) {
        String[] parts = parseMessage(message);
        return parts.length > 0 ? parts[0] : "";
    }

    /**
     * Get status from response
     */
    public static String getStatus(String response) {
        String[] parts = parseMessage(response);
        return parts.length > 0 ? parts[0] : "";
    }

    /**
     * Check if response is successful
     */
    public static boolean isSuccess(String response) {
        return SUCCESS.equals(getStatus(response));
    }

    /**
     * Check if response is error
     */
    public static boolean isError(String response) {
        return ERROR.equals(getStatus(response));
    }

    /**
     * Get error message from response
     */
    public static String getErrorMessage(String response) {
        String[] parts = parseMessage(response);
        return parts.length > 1 ? parts[1] : "Unknown error";
    }

    /**
     * Get data from response (3rd part onwards)
     */
    public static String getData(String response) {
        String[] parts = parseMessage(response);
        return parts.length > 2 ? parts[2] : "";
    }

    /**
     * Parse data list (items separated by FIELD_DELIMITER)
     */
    public static List<String> parseDataList(String data) {
        List<String> list = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return list;
        }

        String[] items = data.split(Pattern.quote(FIELD_DELIMITER));
        for (String item : items) {
            if (item != null && !item.trim().isEmpty()) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Parse fields within a data item (comma-separated)
     */
    public static String[] parseFields(String data) {
        if (data == null || data.isEmpty()) {
            return new String[0];
        }
        return data.split(Pattern.quote(LIST_DELIMITER), -1);
    }

    /**
     * Encode special characters in data
     */
    public static String encode(String data) {
        if (data == null) return "";
        return data.replace(DELIMITER, "\\d")
                .replace(FIELD_DELIMITER, "\\f")
                .replace(LIST_DELIMITER, "\\c");
    }

    /**
     * Decode special characters in data
     */
    public static String decode(String data) {
        if (data == null) return "";
        return data.replace("\\d", DELIMITER)
                .replace("\\f", FIELD_DELIMITER)
                .replace("\\c", LIST_DELIMITER);
    }

    /**
     * Validate message format
     */
    public static boolean isValidMessage(String message) {
        return message != null && !message.isEmpty() && message.contains(DELIMITER);
    }

    /**
     * Extract all data parts from response
     */
    public static List<String> getAllDataParts(String response) {
        String[] parts = parseMessage(response);
        List<String> dataParts = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            dataParts.add(parts[i]);
        }
        return dataParts;
    }

    // Private constructor to prevent instantiation
    private Protocol() {
        throw new AssertionError("Cannot instantiate Protocol class");
    }
}