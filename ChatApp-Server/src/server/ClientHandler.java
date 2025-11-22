package server;

import database.dao.UserDAO;
import models.User;
import protocol.Protocol;
import utils.EncryptionUtil;
import utils.ValidationUtil;
import utils.EmailUtil;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String userId;
    private boolean isConnected;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.isConnected = true;
    }

    @Override
    public void run() {
        try {
            // Initialize streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Handle client messages
            String message;
            while (isConnected && (message = in.readLine()) != null) {
                handleMessage(message);
            }

        } catch (IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Handle incoming messages from client
     */
    private void handleMessage(String message) {
        System.out.println("← Received from client: " + message);
        String[] parts = Protocol.parseMessage(message);
        if (parts.length == 0) {
            System.out.println("→ Empty message received");
            return;
        }

        String messageType = parts[0];
        System.out.println("→ Message type: " + messageType);

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

            // ==================== CONVERSATION ====================
            case Protocol.CONVERSATION_GET_ALL:
                handleConversationGetAll(parts);
                break;

            case Protocol.CONVERSATION_GET:
                handleConversationGet(parts);
                break;

            case Protocol.CONVERSATION_CREATE:
                handleConversationCreate(parts);
                break;

            case Protocol.CONVERSATION_DELETE:
                handleConversationDelete(parts);
                break;

            // ==================== MESSAGE ====================
            case Protocol.MESSAGE_SEND:
                handleMessageSend(parts);
                break;

            case Protocol.MESSAGE_GET_HISTORY:
                handleMessageGetHistory(parts);
                break;

            case Protocol.MESSAGE_MARK_READ:
                handleMessageMarkRead(parts);
                break;

            case Protocol.TYPING_START:
                handleTypingStart(parts);
                break;

            case Protocol.TYPING_STOP:
                handleTypingStop(parts);
                break;

            // ==================== CONTACT ====================
            case Protocol.CONTACT_GET_ALL:
                handleContactGetAll(parts);
                break;

            case Protocol.CONTACT_ADD:
                handleContactAdd(parts);
                break;

            case Protocol.CONTACT_REMOVE:
                handleContactRemove(parts);
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
        System.out.println("→ Handling register: " + String.join("|", parts));

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
        System.out.println("→ UserDAO.createUser returned: " + created);

        if (created) {
            // Generate and send verification code
            String code = EncryptionUtil.generateVerificationCode();
            // TODO: Save code to DB with expiry
            EmailUtil.sendVerificationCode(email, code, username);

            sendMessage(Protocol.buildSuccessResponse(
                    "Registration successful. Please check your email for verification code.",
                    user.getUserId()
            ));

            System.out.println("✓ New user registered: " + username);
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

        // Update online status
        UserDAO.updateOnlineStatus(user.getUserId(), true);

        // Register client
        this.userId = user.getUserId();
        server.addClient(userId, this);

        // Send success response with user data
        String userData = String.format("%s%s%s%s%s%s%s",
                user.getUserId(),
                Protocol.ARRAY_SEPARATOR,
                user.getUsername(),
                Protocol.ARRAY_SEPARATOR,
                user.getEmail(),
                Protocol.ARRAY_SEPARATOR,
                user.getDisplayName()
        );

        sendMessage(Protocol.buildSuccessResponse("Login successful", userData));

        System.out.println("✓ User logged in: " + user.getUsername());
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

                System.out.println("✓ Email verified: " + email);
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
            UserDAO.updateOnlineStatus(userId, false);
            server.removeClient(userId);
            System.out.println("✓ User logged out: " + userId);
        }
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
        // TODO: Implement profile update
        sendMessage(Protocol.buildSuccessResponse("Profile updated"));
    }

    /**
     * Handle get profile
     */
    private void handleGetProfile(String[] parts) {
        // TODO: Implement get profile
        sendMessage(Protocol.buildSuccessResponse("Profile data", "{}"));
    }

    /**
     * Handle user search
     */
    private void handleUserSearch(String[] parts) {
        // TODO: Implement user search
        sendMessage(Protocol.buildSuccessResponse("Search results", "[]"));
    }

    /**
     * Handle get online status
     */
    private void handleGetOnlineStatus(String[] parts) {
        // TODO: Implement get online status
        sendMessage(Protocol.buildSuccessResponse("Online status", "[]"));
    }

    // ==================== CONVERSATION HANDLERS ====================

    /**
     * Handle get all conversations
     */
    private void handleConversationGetAll(String[] parts) {
        if (userId == null) {
            sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "User not authenticated"
            ));
            return;
        }

        // TODO: Implement get all conversations from database
        // For now, return empty list
        sendMessage(Protocol.buildSuccessResponse(
                "Conversations retrieved",
                "[]"
        ));

        System.out.println("✓ Sent empty conversation list to user: " + userId);
    }

    /**
     * Handle get conversation
     */
    private void handleConversationGet(String[] parts) {
        // TODO: Implement get specific conversation
        sendMessage(Protocol.buildSuccessResponse("Conversation data", "{}"));
    }

    /**
     * Handle create conversation
     */
    private void handleConversationCreate(String[] parts) {
        // TODO: Implement create conversation
        sendMessage(Protocol.buildSuccessResponse("Conversation created", "{}"));
    }

    /**
     * Handle delete conversation
     */
    private void handleConversationDelete(String[] parts) {
        // TODO: Implement delete conversation
        sendMessage(Protocol.buildSuccessResponse("Conversation deleted"));
    }

    // ==================== MESSAGE HANDLERS ====================

    /**
     * Handle send message
     */
    private void handleMessageSend(String[] parts) {
        // TODO: Implement send message
        sendMessage(Protocol.buildSuccessResponse("Message sent", "{}"));
    }

    /**
     * Handle get message history
     */
    private void handleMessageGetHistory(String[] parts) {
        // TODO: Implement get message history
        sendMessage(Protocol.buildSuccessResponse("Message history", "[]"));
    }

    /**
     * Handle mark message as read
     */
    private void handleMessageMarkRead(String[] parts) {
        // TODO: Implement mark message as read
        sendMessage(Protocol.buildSuccessResponse("Message marked as read"));
    }

    /**
     * Handle typing start
     */
    private void handleTypingStart(String[] parts) {
        // TODO: Broadcast typing status to other users
        System.out.println("User " + userId + " started typing");
    }

    /**
     * Handle typing stop
     */
    private void handleTypingStop(String[] parts) {
        // TODO: Broadcast typing status to other users
        System.out.println("User " + userId + " stopped typing");
    }

    // ==================== CONTACT HANDLERS ====================

    /**
     * Handle get all contacts
     */
    private void handleContactGetAll(String[] parts) {
        // TODO: Implement get all contacts
        sendMessage(Protocol.buildSuccessResponse("Contacts retrieved", "[]"));
    }

    /**
     * Handle add contact
     */
    private void handleContactAdd(String[] parts) {
        // TODO: Implement add contact
        sendMessage(Protocol.buildSuccessResponse("Contact added"));
    }

    /**
     * Handle remove contact
     */
    private void handleContactRemove(String[] parts) {
        // TODO: Implement remove contact
        sendMessage(Protocol.buildSuccessResponse("Contact removed"));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Send message to client
     */
    public boolean sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush();
            System.out.println("→ Sent to client: " + message);
            return true;
        }
        return false;
    }

    /**
     * Disconnect client
     */
    public void disconnect() {
        isConnected = false;

        if (userId != null) {
            UserDAO.updateOnlineStatus(userId, false);
            server.removeClient(userId);
        }

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }

    public String getUserId() {
        return userId;
    }
}