package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.Session;
import org.example.chatappclient.client.models.User;
import org.example.chatappclient.client.protocol.Protocol;
import org.example.chatappclient.client.utils.storage.PreferencesManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AuthService {
    private static AuthService instance;

    private SocketClient socketClient;
    private PreferencesManager preferencesManager;
    private Session currentSession;
    private User currentUser;

    private static final long REQUEST_TIMEOUT = 30000; // 30 seconds

    private AuthService() {
        this.socketClient = SocketClient.getInstance();
        this.preferencesManager = PreferencesManager.getInstance();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Login with username and password
     */
    public LoginResult login(String username, String password, boolean rememberMe) {
        try {
            System.out.println("=== AUTH SERVICE LOGIN ===");
            System.out.println("Username: " + username);
            System.out.println("Password length: " + password.length());

            String passwordHash = hashPassword(password);
            System.out.println("Password hash: " + passwordHash);

            String request = Protocol.buildRequest(
                    Protocol.LOGIN,
                    username,
                    passwordHash
            );

            System.out.println("Sending request: " + request);

            String response = socketClient.sendRequest(request, REQUEST_TIMEOUT);

            System.out.println("Received response: " + response);

            if (response == null) {
                System.err.println("Response is null!");
                return new LoginResult(false, "Không nhận được phản hồi từ server", null, null);
            }

            String[] parts = Protocol.parseMessage(response);

            System.out.println("Response parts count: " + parts.length);
            for (int i = 0; i < parts.length; i++) {
                System.out.println("Part[" + i + "]: " + parts[i]);
            }

            if (parts.length < 2) {
                System.err.println("Response has less than 2 parts!");
                return new LoginResult(false, "Phản hồi không hợp lệ", null, null);
            }

            String status = parts[0];
            String message = parts[1];

            System.out.println("Status: " + status);
            System.out.println("Message: " + message);

            if (Protocol.SUCCESS.equals(status)) {
                System.out.println("Login status is SUCCESS");

                // Kiểm tra format response từ server
                // Format mong đợi: SUCCESS|||message|||sessionId:::username:::token|||userData
                if (parts.length >= 3) {
                    // Parse session data từ parts[2]
                    String sessionData = parts[2];
                    System.out.println("Session data: " + sessionData);

                    String[] sessionParts = sessionData.split(":::");
                    System.out.println("Session parts count: " + sessionParts.length);

                    if (sessionParts.length >= 3) {
                        String userId = sessionParts[0];
                        String receivedUsername = sessionParts[1];
                        String token = sessionParts[2];

                        System.out.println("Creating session - UserID: " + userId + ", Username: " + receivedUsername + ", Token: " + token);

                        currentSession = new Session(userId, receivedUsername, token);

                        // Parse user data nếu có
                        if (parts.length >= 4) {
                            System.out.println("Parsing user data from parts[3]: " + parts[3]);
                            currentUser = parseUserData(parts[3]);
                        } else {
                            System.out.println("No user data in response, creating basic user");
                            currentUser = new User();
                            currentUser.setUserId(userId);
                            currentUser.setUsername(receivedUsername);
                        }

                        // Save credentials if remember me
                        if (rememberMe) {
                            System.out.println("Saving credentials (remember me enabled)");
                            preferencesManager.saveCredentials(username, password);
                        } else {
                            System.out.println("Clearing credentials (remember me disabled)");
                            preferencesManager.clearCredentials();
                        }

                        System.out.println("Login successful!");
                        return new LoginResult(true, message, currentUser, currentSession);
                    } else {
                        System.err.println("Session data không đủ 3 phần (userId, username, token)");
                        System.err.println("Expected format: userId:::username:::token");
                        System.err.println("Actual: " + sessionData);
                        return new LoginResult(false, "Dữ liệu session không hợp lệ", null, null);
                    }
                } else {
                    System.err.println("Response không có session data (parts.length = " + parts.length + ")");
                    return new LoginResult(false, "Dữ liệu đăng nhập không đầy đủ", null, null);
                }
            } else {
                System.err.println("Login failed with status: " + status);
                return new LoginResult(false, message, null, null);
            }

        } catch (Exception e) {
            System.err.println("Exception in login: " + e.getMessage());
            e.printStackTrace();
            return new LoginResult(false, "Lỗi: " + e.getMessage(), null, null);
        }
    }

    /**
     * Register new user
     */
    public RegisterResult register(String username, String email, String password, String confirmPassword) {
        try {
            if (!password.equals(confirmPassword)) {
                return new RegisterResult(false, "Mật khẩu xác nhận không khớp");
            }

            String passwordHash = hashPassword(password);

            String request = Protocol.buildRequest(
                    Protocol.REGISTER,
                    username,
                    email,
                    passwordHash
            );

            String response = socketClient.sendRequest(request, REQUEST_TIMEOUT);

            if (response == null) {
                return new RegisterResult(false, "Không nhận được phản hồi từ server");
            }

            String[] parts = Protocol.parseMessage(response);

            if (parts.length < 2) {
                return new RegisterResult(false, "Phản hồi không hợp lệ");
            }

            String status = parts[0];
            String message = parts[1];

            boolean success = Protocol.SUCCESS.equals(status);
            return new RegisterResult(success, message);

        } catch (Exception e) {
            return new RegisterResult(false, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Request password reset
     */
    public boolean forgotPassword(String email) {
        try {
            String request = Protocol.buildRequest(
                    Protocol.FORGOT_PASSWORD,
                    email
            );

            String response = socketClient.sendRequest(request, REQUEST_TIMEOUT);

            if (response == null) return false;

            return Protocol.isSuccess(response);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reset password with code
     */
    public boolean resetPassword(String email, String code, String newPassword) {
        try {
            String passwordHash = hashPassword(newPassword);

            String request = Protocol.buildRequest(
                    Protocol.RESET_PASSWORD,
                    email,
                    code,
                    passwordHash
            );

            String response = socketClient.sendRequest(request, REQUEST_TIMEOUT);

            if (response == null) return false;

            return Protocol.isSuccess(response);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verify email with code
     */
    public boolean verifyEmail(String email, String code) {
        try {
            String request = Protocol.buildRequest(
                    Protocol.VERIFY_EMAIL,
                    email,
                    code
            );

            String response = socketClient.sendRequest(request, REQUEST_TIMEOUT);

            if (response == null) return false;

            return Protocol.isSuccess(response);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resend verification code
     */
    public boolean resendVerificationCode(String email) {
        try {
            String request = Protocol.buildRequest(
                    Protocol.RESEND_VERIFICATION,
                    email
            );

            String response = socketClient.sendRequest(request, REQUEST_TIMEOUT);

            if (response == null) return false;

            return Protocol.isSuccess(response);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Logout current user
     */
    public boolean logout() {
        try {
            if (currentSession != null) {
                String request = Protocol.buildRequest(
                        Protocol.LOGOUT,
                        currentSession.getToken()
                );

                socketClient.sendMessage(request);
            }

            currentSession = null;
            currentUser = null;

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hash password using SHA-256
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    /**
     * Parse user data
     */
    private User parseUserData(String userData) {
        try {
            System.out.println("Parsing user data: " + userData);
            String[] fields = userData.split(",");
            System.out.println("User data fields count: " + fields.length);

            User user = new User();
            if (fields.length > 0) user.setUserId(fields[0]);
            if (fields.length > 1) user.setUsername(fields[1]);
            if (fields.length > 2) user.setEmail(fields[2]);
            if (fields.length > 3) user.setDisplayName(fields[3]);
            if (fields.length > 4) user.setAvatarUrl(fields[4]);
            if (fields.length > 5) user.setStatusMessage(fields[5]);
            if (fields.length > 6) user.setOnline(Boolean.parseBoolean(fields[6]));

            System.out.println("User parsed successfully: " + user.getUsername());
            return user;

        } catch (Exception e) {
            System.err.println("Error parsing user data: " + e.getMessage());
            e.printStackTrace();
            return new User();
        }
    }

    public Session getCurrentSession() {
        return currentSession;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentSession != null && currentSession.isValid();
    }

    // ==================== RESULT CLASSES ====================

    public static class LoginResult {
        private boolean success;
        private String message;
        private User user;
        private Session session;

        public LoginResult(boolean success, String message, User user, Session session) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.session = session;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
        public Session getSession() { return session; }
    }

    public static class RegisterResult {
        private boolean success;
        private String message;

        public RegisterResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}