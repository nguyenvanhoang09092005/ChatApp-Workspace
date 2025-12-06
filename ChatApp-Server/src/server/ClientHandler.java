package server;

import database.dao.UserDAO;
import models.User;
import protocol.Protocol;
import server.handlers.*;
import utils.EncryptionUtil;
import utils.ValidationUtil;
import utils.EmailUtil;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * ClientHandler - Xử lý kết nối và yêu cầu từ client
 * Updated with online status management
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private ChatServer server;
    private BufferedReader in;
    private PrintWriter out;

    private InputStream rawInputStream;
    private OutputStream rawOutputStream;
    private String userId;
    private boolean isConnected;

    // Handlers
    private ContactHandler contactHandler;
    private ConversationHandler conversationHandler;
    private MessageHandler messageHandler;
    private NotificationHandler notificationHandler;
    private FileHandler fileHandler;

    private StickerHandler stickerHandler;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.isConnected = true;

        // Initialize handlers
        this.contactHandler = new ContactHandler(this);
        this.conversationHandler = new ConversationHandler(this);
        this.messageHandler = new MessageHandler(this);
        this.notificationHandler = new NotificationHandler(this);
        this.fileHandler = new FileHandler(this);
        this.stickerHandler = new StickerHandler(this);
    }

    @Override
    public void run() {
        try {
            // === TÁCH RIÊNG STREAM CHO TEXT VÀ BINARY ===
            InputStream socketInputStream = socket.getInputStream();
            OutputStream socketOutputStream = socket.getOutputStream();

            // Stream cho text (protocol, tin nhắn văn bản)
            this.in = new BufferedReader(new InputStreamReader(socketInputStream));
            this.out = new PrintWriter(socketOutputStream, true);

            // Lưu lại stream gốc để FileHandler dùng cho file binary
            this.rawInputStream = socketInputStream;
            this.rawOutputStream = socketOutputStream;

            System.out.println("Client connected from: " + socket.getInetAddress().getHostAddress());

            String message;
            while (isConnected && (message = in.readLine()) != null) {
                handleMessage(message);
            }

        } catch (IOException e) {
            if (isConnected) {
                System.err.println("Client handler error: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    /**
     * Handle incoming messages from client
     */
    private void handleMessage(String message) {
        System.out.println("← Received: " + message);

        String[] parts = Protocol.parseMessage(message);
        if (parts.length == 0) {
            System.out.println("⚠️ Empty message received");
            return;
        }

        String messageType = parts[0];
        System.out.println("→ Processing: " + messageType);

        // Route to appropriate handler
        if (messageType.startsWith("CONTACT_")) {
            contactHandler.handle(messageType, parts);
        } else if (messageType.startsWith("CONVERSATION_")) {
            conversationHandler.handle(messageType, parts);
        } else if (messageType.startsWith("MESSAGE_") || messageType.startsWith("TYPING_")) {
            messageHandler.handle(messageType, parts);
        } else if (messageType.startsWith("NOTIFICATION_")) {
            notificationHandler.handle(messageType, parts);
        } else if (messageType.startsWith("FILE_")) {
            fileHandler.handle(messageType, parts);
        }else if (messageType.startsWith("STICKER_") || messageType.startsWith("EMOJI_")) {
            stickerHandler.handle(messageType, parts);
        } else {
            // Handle auth and user commands directly
            handleDirectCommands(messageType, parts);
        }
    }

    /**
     * Handle auth and user commands directly
     */
    private void handleDirectCommands(String messageType, String[] parts) {
        switch (messageType) {
            // ==================== AUTH ====================
            case Protocol.REGISTER:
                handleRegister(parts);
                break;

            case Protocol.LOGIN:
                handleLogin(parts);
                break;

            case Protocol.VERIFY_EMAIL:
                handleVerifyEmail(parts);
                break;

            case Protocol.RESEND_VERIFICATION:
                handleResendCode(parts);
                break;

            case Protocol.FORGOT_PASSWORD:
                handleForgotPassword(parts);
                break;

            case Protocol.RESET_PASSWORD:
                handleResetPassword(parts);
                break;

            case Protocol.LOGOUT:
                handleLogout();
                break;

            // ==================== USER ====================
            case Protocol.USER_CHANGE_PASSWORD:
                handleChangePassword(parts);
                break;

            case Protocol.USER_UPDATE_PROFILE:
                handleUpdateProfile(parts);
                break;

            case Protocol.USER_GET_PROFILE:
                handleGetProfile(parts);
                break;

            case Protocol.USER_SEARCH:
                handleUserSearch(parts);
                break;

            case Protocol.USER_GET_ONLINE_STATUS:
                handleGetOnlineStatus(parts);
                break;

            default:
                sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Unknown message type: " + messageType
                ));
        }
    }

    // ==================== AUTH HANDLERS ====================

    /**
     * Handle user registration
     */
    private void handleRegister(String[] parts) {
        System.out.println("→ Handling register");

        if (parts.length < 4) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid registration data"
            ));
            return;
        }

        String username = parts[1];
        String email = parts[2];
        String password = parts[3];
        String phone = parts.length > 4 ? parts[4] : null;

        // Validate input
        if (!ValidationUtil.isValidUsername(username)) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid username format"
            ));
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid email format"
            ));
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_WEAK_PASSWORD,
                    "Password must be at least 8 characters with uppercase, lowercase, digit and special character"
            ));
            return;
        }

        // Check username/email exists
        if (UserDAO.usernameExists(username)) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_USERNAME_EXISTS,
                    "Username already exists"
            ));
            return;
        }

        if (UserDAO.emailExists(email)) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_EMAIL_EXISTS,
                    "Email already exists"
            ));
            return;
        }

        // Create user
        String salt = EncryptionUtil.generateSalt();
        String passwordHash = EncryptionUtil.hashPassword(password, salt);

        User user = new User(username, email, passwordHash, salt);
        user.setPhone(phone);
        user.setDisplayName(username);

        boolean created = UserDAO.createUser(user);

        if (created) {
            // Generate and send verification code
            String code = EncryptionUtil.generateVerificationCode();
            // TODO: Save code to DB with expiry
            EmailUtil.sendVerificationCode(email, code, username);

            sendMessage(Protocol.buildSuccessResponse(
                    "Registration successful. Please check your email for verification code.",
                    user.getUserId()
            ));

            System.out.println("✅ New user registered: " + username);
        } else {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to create account"
            ));
        }
    }

    /**
     * Handle user login
     */
    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid login data"
            ));
            return;
        }

        String usernameOrEmail = parts[1];
        String password = parts[2];

        // Find user by username or email
        User user = UserDAO.findByUsername(usernameOrEmail);
        if (user == null) {
            user = UserDAO.findByEmail(usernameOrEmail);
        }

        // Verify user exists
        if (user == null) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_INVALID_CREDENTIALS,
                    "Invalid username or password"
            ));
            return;
        }

        // Check if account is verified
        if (!user.isVerified()) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_ACCOUNT_NOT_VERIFIED,
                    "Account not verified. Please verify your email."
            ));
            return;
        }

        // Check if account is active
        if (!user.isActive()) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_ACCOUNT_INACTIVE,
                    "Account is inactive"
            ));
            return;
        }

        // Verify password
        if (!EncryptionUtil.verifyPassword(password, user.getPasswordHash(), user.getSalt())) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_INVALID_CREDENTIALS,
                    "Invalid username or password"
            ));
            return;
        }

        // ========== UPDATE ONLINE STATUS ==========
        // Update user status to online in database
        UserDAO.updateOnlineStatus(user.getUserId(), true);

        // Register client with server
        this.userId = user.getUserId();
        server.addClient(userId, this);

        // Broadcast online status to all other connected clients
        server.broadcastUserStatus(userId, true);

        System.out.println("✅ User logged in: " + user.getUsername() + " (ID: " + userId + ")");

        // Send success response with user data
        String userData = String.format("%s%s%s%s%s%s%s%s%s%s%s",
                user.getUserId(),
                Protocol.ARRAY_SEPARATOR,
                user.getUsername(),
                Protocol.ARRAY_SEPARATOR,
                user.getEmail(),
                Protocol.ARRAY_SEPARATOR,
                user.getDisplayName(),
                Protocol.ARRAY_SEPARATOR,
                user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                Protocol.ARRAY_SEPARATOR,
                user.getBio() != null ? user.getBio() : ""
        );

        sendMessage(Protocol.buildSuccessResponse("Login successful", userData));
    }

    /**
     * Handle email verification
     */
    private void handleVerifyEmail(String[] parts) {
        if (parts.length < 3) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid verification data"
            ));
            return;
        }

        String email = parts[1];
        String code = parts[2];

        // TODO: Verify code from database
        // For now, simulate verification

        User user = UserDAO.findByEmail(email);
        if (user != null) {
            if (UserDAO.verifyUser(user.getUserId())) {
                // Send welcome email
                EmailUtil.sendWelcomeEmail(email, user.getUsername());

                sendMessage(Protocol.buildSuccessResponse(
                        "Email verified successfully"
                ));

                System.out.println("✅ Email verified: " + email);
            } else {
                sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_DATABASE_ERROR,
                        "Failed to verify email"
                ));
            }
        } else {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "User not found"
            ));
        }
    }

    /**
     * Handle resend verification code
     */
    private void handleResendCode(String[] parts) {
        if (parts.length < 2) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String email = parts[1];
        User user = UserDAO.findByEmail(email);

        if (user != null) {
            String code = EncryptionUtil.generateVerificationCode();
            // TODO: Save new code to database

            EmailUtil.sendVerificationCode(email, code, user.getUsername());

            sendMessage(Protocol.buildSuccessResponse(
                    "Verification code sent"
            ));
        } else {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "User not found"
            ));
        }
    }

    /**
     * Handle forgot password
     */
    private void handleForgotPassword(String[] parts) {
        if (parts.length < 2) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String email = parts[1];
        User user = UserDAO.findByEmail(email);

        if (user != null) {
            String token = EncryptionUtil.generateToken();
            // TODO: Save token to database with expiry

            EmailUtil.sendPasswordResetEmail(email, token, user.getUsername());

            sendMessage(Protocol.buildSuccessResponse(
                    "Password reset email sent"
            ));
        } else {
            // Don't reveal if email exists or not for security
            sendMessage(Protocol.buildSuccessResponse(
                    "If email exists, password reset link will be sent"
            ));
        }
    }

    /**
     * Handle password reset
     */
    private void handleResetPassword(String[] parts) {
        // TODO: Implement password reset with token verification
        sendMessage(Protocol.buildSuccessResponse("Password reset successful"));
    }

    /**
     * Handle logout
     */
    private void handleLogout() {
        if (userId != null) {
            System.out.println("→ User logging out: " + userId);

            // Update online status to offline and update last_seen
            UserDAO.updateOnlineStatus(userId, false);

            // Broadcast offline status to all other clients
            server.broadcastUserStatus(userId, false);

            // Remove from server's connected clients
            server.removeClient(userId);

            System.out.println("✅ User logged out: " + userId);
        }

        // Disconnect the client
        disconnect();
    }

    // ==================== USER HANDLERS ====================

    /**
     * Handle password change
     */
    private void handleChangePassword(String[] parts) {
        if (parts.length < 3 || userId == null) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String oldPassword = parts[1];
        String newPassword = parts[2];

        User user = UserDAO.findById(userId);
        if (user == null) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "User not found"
            ));
            return;
        }

        // Verify old password
        if (!EncryptionUtil.verifyPassword(oldPassword, user.getPasswordHash(), user.getSalt())) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_INVALID_CREDENTIALS,
                    "Incorrect old password"
            ));
            return;
        }

        // Validate new password
        if (!ValidationUtil.isValidPassword(newPassword)) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_WEAK_PASSWORD,
                    "New password does not meet requirements"
            ));
            return;
        }

        // Update password
        String newSalt = EncryptionUtil.generateSalt();
        String newHash = EncryptionUtil.hashPassword(newPassword, newSalt);

        if (UserDAO.updatePassword(userId, newHash, newSalt)) {
            sendMessage(Protocol.buildSuccessResponse("Password changed successfully"));
            System.out.println("✅ Password changed for user: " + userId);
        } else {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_DATABASE_ERROR,
                    "Failed to change password"
            ));
        }
    }

    /**
     * Handle profile update
     */
    private void handleUpdateProfile(String[] parts) {
        if (parts.length < 2 || userId == null) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        // TODO: Parse and update profile data
        // Format: USER_UPDATE_PROFILE|||displayName|||bio|||avatarUrl|||...

        sendMessage(Protocol.buildSuccessResponse("Profile updated"));
    }

    /**
     * Handle get profile
     */
    private void handleGetProfile(String[] parts) {
        if (parts.length < 2) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String targetUserId = parts[1];
        User user = UserDAO.findById(targetUserId);

        if (user != null) {
            String userData = buildUserData(user);
            sendMessage(Protocol.buildSuccessResponse("Profile data", userData));
        } else {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "User not found"
            ));
        }
    }

    /**
     * Handle user search
     */
    private void handleUserSearch(String[] parts) {
        if (parts.length < 2) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid search query"
            ));
            return;
        }

        String query = parts[1].trim();

        if (query.isEmpty()) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Search query cannot be empty"
            ));
            return;
        }

        System.out.println("→ Searching for user: " + query);

        // Try exact match first (email or phone)
        User user = UserDAO.findByEmailOrPhone(query);

        if (user != null) {
            // Found exact match
            String userData = buildUserData(user);
            sendMessage(Protocol.buildSuccessResponse(
                    "User found",
                    userData
            ));
            System.out.println("✅ Found user by email/phone: " + user.getUsername());
            return;
        }

        // If no exact match, try keyword search
        List<User> users = UserDAO.searchByKeyword(query, 10);

        if (users.isEmpty()) {
            sendMessage(Protocol.buildSuccessResponse(
                    "No users found",
                    ""
            ));
            System.out.println("→ No users found for query: " + query);
            return;
        }

        // Build response with multiple users
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) data.append(Protocol.FIELD_DELIMITER);
            data.append(buildUserData(users.get(i)));
        }

        sendMessage(Protocol.buildSuccessResponse(
                "Users found: " + users.size(),
                data.toString()
        ));

        System.out.println("✅ Found " + users.size() + " users for query: " + query);
    }

    /**
     * Build user data string for response
     * Format: userId,username,email,displayName,avatarUrl,phone,statusMessage,isOnline,statusText
     */
    private String buildUserData(User user) {
        return String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s",
                user.getUserId(),
                Protocol.LIST_DELIMITER,
                user.getUsername(),
                Protocol.LIST_DELIMITER,
                user.getEmail(),
                Protocol.LIST_DELIMITER,
                user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                Protocol.LIST_DELIMITER,
                user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                Protocol.LIST_DELIMITER,
                user.getPhone() != null ? user.getPhone() : "",
                Protocol.LIST_DELIMITER,
                user.getStatusMessage() != null ? user.getStatusMessage() : "",
                Protocol.LIST_DELIMITER,
                user.isOnline(),
                Protocol.LIST_DELIMITER,
                user.getStatusText() // "Đang hoạt động" or "Hoạt động X phút/giờ trước" or "Không hoạt động"
        );
    }

    /**
     * Handle get online status - Lấy trạng thái online của user
     */
    private void handleGetOnlineStatus(String[] parts) {
        if (parts.length < 2) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Invalid request"
            ));
            return;
        }

        String targetUserId = parts[1];
        User user = UserDAO.findById(targetUserId);

        if (user != null) {
            StringBuilder data = new StringBuilder();
            data.append(user.getUserId())
                    .append(Protocol.LIST_DELIMITER)
                    .append(user.isOnline())
                    .append(Protocol.LIST_DELIMITER)
                    .append(user.getStatusText())
                    .append(Protocol.LIST_DELIMITER)
                    .append(user.getLastSeen() != null ? user.getLastSeen().toString() : "");

            sendMessage(Protocol.buildSuccessResponse("Online status", data.toString()));
        } else {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "User not found"
            ));
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Send message to client
     */
    public boolean sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            try {
                out.println(message);
                out.flush();
                System.out.println("→ Sent: " + message);
                return true;
            } catch (Exception e) {
                System.err.println("⚠️ Error sending message: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Disconnect client - Cleanup and update status
     */
    public void disconnect() {
        if (!isConnected) {
            return; // Already disconnected
        }

        isConnected = false;

        // Update user status if logged in
        if (userId != null) {
            System.out.println("→ Disconnecting user: " + userId);

            // Update online status to offline and set last_seen
            UserDAO.updateOnlineStatus(userId, false);

            // Broadcast offline status to all other clients
            server.broadcastUserStatus(userId, false);

            // Remove from server's connected clients
            server.removeClient(userId);

            System.out.println("✅ User disconnected: " + userId);
            userId = null;
        }

        // Close all connections
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error closing client connection: " + e.getMessage());
        }
    }

    // ==================== GETTERS ====================

    public String getUserId() {
        return userId;
    }

    public ChatServer getServer() {
        return server;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // Thêm vào cuối phần GETTERS trong class ClientHandler
    public InputStream getRawInputStream() {
        return rawInputStream;
    }

    public OutputStream getRawOutputStream() {
        return rawOutputStream;
    }
}