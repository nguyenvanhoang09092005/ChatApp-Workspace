package utils;

import java.util.regex.Pattern;

public class ValidationUtil {

    // ==================== REGEX PATTERNS ====================

    // Email pattern (RFC 5322 simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Phone pattern (Vietnam format: 0xxxxxxxxx or +84xxxxxxxxx)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$"
    );

    // Username pattern (alphanumeric and underscore, 3-20 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,20}$"
    );

    // Password pattern (at least 8 chars, 1 upper, 1 lower, 1 digit, 1 special)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    // ==================== EMAIL VALIDATION ====================

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // ==================== PHONE VALIDATION ====================

    /**
     * Validate phone number format (Vietnam)
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Normalize phone number to standard format
     */
    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        phone = phone.trim().replaceAll("\\s+", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        return phone;
    }

    // ==================== USERNAME VALIDATION ====================

    /**
     * Validate username format
     */
    public static boolean isValidUsername(String username) {
        if (isEmpty(username)) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    // ==================== PASSWORD VALIDATION ====================

    /**
     * Validate password strength (basic check)
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Validate password strength (strict with regex)
     */
    public static boolean isStrictValidPassword(String password) {
        if (isEmpty(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Get password strength score (0-5)
     */
    public static int getPasswordStrengthScore(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;

        return Math.min(score, 5);
    }

    /**
     * Get password strength description
     */
    public static String getPasswordStrength(String password) {
        int score = getPasswordStrengthScore(password);

        switch (score) {
            case 0:
            case 1:
                return "Very Weak";
            case 2:
                return "Weak";
            case 3:
                return "Medium";
            case 4:
                return "Strong";
            case 5:
                return "Very Strong";
            default:
                return "Unknown";
        }
    }

    // ==================== DISPLAY NAME VALIDATION ====================

    /**
     * Validate display name
     */
    public static boolean isValidDisplayName(String displayName) {
        return !isEmpty(displayName) && displayName.trim().length() <= 100;
    }

    // ==================== GENERAL VALIDATION ====================

    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Sanitize input to prevent SQL injection
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        return input.trim()
                .replaceAll("['\";\\\\]", "")
                .replaceAll("--", "")
                .replaceAll("/\\*", "")
                .replaceAll("\\*/", "");
    }

    /**
     * Validate string length
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) return false;
        int length = str.trim().length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validate IP address format
     */
    public static boolean isValidIP(String ip) {
        if (isEmpty(ip)) return false;

        String ipPattern =
                "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        return ip.matches(ipPattern);
    }

    /**
     * Validate verification code (6 digits)
     */
    public static boolean isValidVerificationCode(String code) {
        return code != null && code.matches("^[0-9]{6}$");
    }

    /**
     * Validate UUID format
     */
    public static boolean isValidUUID(String uuid) {
        if (isEmpty(uuid)) return false;

        String uuidPattern =
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

        return uuid.matches(uuidPattern);
    }
}