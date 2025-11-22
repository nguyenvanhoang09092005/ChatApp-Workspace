package org.example.chatappclient.client.utils.validation;


import org.example.chatappclient.client.config.Constants;
import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(Constants.EMAIL_PATTERN);
    private static final Pattern PHONE_PATTERN = Pattern.compile(Constants.PHONE_PATTERN);
    private static final Pattern USERNAME_PATTERN = Pattern.compile(Constants.USERNAME_PATTERN);

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Validate username format
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String trimmed = username.trim();

        if (trimmed.length() < Constants.MIN_USERNAME_LENGTH ||
                trimmed.length() > Constants.MAX_USERNAME_LENGTH) {
            return false;
        }

        return USERNAME_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validate password strength
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        return password.length() >= Constants.MIN_PASSWORD_LENGTH &&
                password.length() <= Constants.MAX_PASSWORD_LENGTH;
    }

    /**
     * Check password strength (weak, medium, strong)
     */
    public static PasswordStrength checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.WEAK;
        }

        int score = 0;

        // Length check
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;

        // Contains lowercase
        if (password.matches(".*[a-z].*")) score++;

        // Contains uppercase
        if (password.matches(".*[A-Z].*")) score++;

        // Contains digit
        if (password.matches(".*\\d.*")) score++;

        // Contains special character
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;

        if (score <= 2) return PasswordStrength.WEAK;
        if (score <= 4) return PasswordStrength.MEDIUM;
        return PasswordStrength.STRONG;
    }

    /**
     * Validate display name
     */
    public static boolean isValidDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return false;
        }

        String trimmed = displayName.trim();

        return trimmed.length() >= Constants.MIN_DISPLAY_NAME_LENGTH &&
                trimmed.length() <= Constants.MAX_DISPLAY_NAME_LENGTH;
    }

    /**
     * Validate status message
     */
    public static boolean isValidStatusMessage(String statusMessage) {
        if (statusMessage == null) {
            return true; // Status message is optional
        }

        return statusMessage.length() <= Constants.MAX_STATUS_MESSAGE_LENGTH;
    }

    /**
     * Validate message content
     */
    public static boolean isValidMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        return message.length() <= Constants.MAX_MESSAGE_LENGTH;
    }

    /**
     * Check if string is empty or null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if all fields are filled
     */
    public static boolean areFieldsFilled(String... fields) {
        for (String field : fields) {
            if (isEmpty(field)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get email validation error message
     */
    public static String getEmailErrorMessage(String email) {
        if (isEmpty(email)) {
            return "Email không được để trống";
        }
        if (!isValidEmail(email)) {
            return "Email không hợp lệ";
        }
        return null;
    }

    /**
     * Get username validation error message
     */
    public static String getUsernameErrorMessage(String username) {
        if (isEmpty(username)) {
            return "Tên đăng nhập không được để trống";
        }
        if (username.length() < Constants.MIN_USERNAME_LENGTH) {
            return "Tên đăng nhập phải có ít nhất " + Constants.MIN_USERNAME_LENGTH + " ký tự";
        }
        if (username.length() > Constants.MAX_USERNAME_LENGTH) {
            return "Tên đăng nhập không được vượt quá " + Constants.MAX_USERNAME_LENGTH + " ký tự";
        }
        if (!isValidUsername(username)) {
            return "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới";
        }
        return null;
    }

    /**
     * Get password validation error message
     */
    public static String getPasswordErrorMessage(String password) {
        if (isEmpty(password)) {
            return "Mật khẩu không được để trống";
        }
        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return "Mật khẩu phải có ít nhất " + Constants.MIN_PASSWORD_LENGTH + " ký tự";
        }
        if (password.length() > Constants.MAX_PASSWORD_LENGTH) {
            return "Mật khẩu không được vượt quá " + Constants.MAX_PASSWORD_LENGTH + " ký tự";
        }
        return null;
    }

    /**
     * Validate verification code format (6 digits)
     */
    public static boolean isValidVerificationCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return code.trim().matches("^\\d{6}$");
    }

    public enum PasswordStrength {
        WEAK, MEDIUM, STRONG
    }
}