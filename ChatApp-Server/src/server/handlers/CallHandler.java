package server.handlers;

import database.dao.ConversationDAO;
import database.dao.UserDAO;
import models.Conversation;
import models.User;
import protocol.Protocol;
import server.ClientHandler;
import server.media.UdpMediaServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side Call Handler với UDP support
 */
public class CallHandler {

    private final ClientHandler clientHandler;
    private static final Map<String, CallSession> activeCalls = new ConcurrentHashMap<>();
    private static UdpMediaServer mediaServer;

    // UDP Server configuration
    private static final String UDP_SERVER_IP = "0.0.0.0";
    private static final int UDP_BASE_PORT = 50000;

    public CallHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        initializeMediaServer();
    }

    private static synchronized void initializeMediaServer() {
        if (mediaServer == null) {
            try {
                mediaServer = new UdpMediaServer(UDP_SERVER_IP, UDP_BASE_PORT);
                mediaServer.start();
                System.out.println("✅ UDP Media Server started on port " + UDP_BASE_PORT);
            } catch (Exception e) {
                System.err.println("❌ Failed to start UDP Media Server: " + e.getMessage());
            }
        }
    }

    public void handle(String command, String[] parts) {
        switch (command) {
            case Protocol.CALL_START:
                handleStartCall(parts);
                break;
            case Protocol.CALL_ANSWER:
                handleAnswerCall(parts);
                break;
            case Protocol.CALL_REJECT:
                handleRejectCall(parts);
                break;
            case Protocol.CALL_END:
                handleEndCall(parts);
                break;
            default:
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR, "Unknown call command"));
        }
    }

    // ==================== START CALL ====================

    private void handleStartCall(String[] parts) {
        if (parts.length < 4) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Invalid call start data"));
            return;
        }

        String conversationId = parts[1];
        String callerId = parts[2];
        String callType = parts[3]; // "audio" or "video"

        try {
            // Lấy conversation để tìm members
            Conversation conv = ConversationDAO.getConversationById(conversationId);
            if (conv == null) {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_NOT_FOUND, "Conversation not found"));
                return;
            }

            // Tạo call session
            String callId = generateCallId();
            int udpPort = allocateUdpPort(callId);

            CallSession session = new CallSession(
                    callId, conversationId, callerId, callType, udpPort
            );
            activeCalls.put(callId, session);

            // Gửi thông báo cuộc gọi đến các thành viên
            List<String> memberIds = conv.getMemberIds();
            for (String memberId : memberIds) {
                if (!memberId.equals(callerId)) {
                    notifyIncomingCall(memberId, session);
                }
            }

            // Trả về thông tin cho caller
            String responseData = String.format("%s%s%s%s%d",
                    callId, Protocol.DELIMITER,
                    getServerPublicIP(), Protocol.DELIMITER,
                    udpPort);

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Call started", responseData));

            System.out.println("✅ Call started: " + callId + " (Port: " + udpPort + ")");

        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Failed to start call: " + e.getMessage()));
        }
    }

    // ==================== ANSWER CALL ====================

    private void handleAnswerCall(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Invalid answer data"));
            return;
        }

        String callId = parts[1];
        String userId = parts[2];

        CallSession session = activeCalls.get(callId);
        if (session == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND, "Call not found"));
            return;
        }

        session.addParticipant(userId);

        // Thông báo cho caller
        ClientHandler callerHandler = clientHandler.getServer()
                .getClientHandler(session.getCallerId());
        if (callerHandler != null) {
            callerHandler.sendMessage(Protocol.buildRequest(
                    Protocol.CALL_ANSWERED, callId, userId));
        }

        // Trả về thông tin UDP cho answerer
        String responseData = String.format("%s%s%d%s%s",
                getServerPublicIP(), Protocol.DELIMITER,
                session.getUdpPort(), Protocol.DELIMITER,
                session.getCallType());

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Call answered", responseData));

        System.out.println("✅ Call answered: " + callId + " by " + userId);
    }

    // ==================== REJECT CALL ====================

    private void handleRejectCall(String[] parts) {
        if (parts.length < 3) return;

        String callId = parts[1];
        String userId = parts[2];

        CallSession session = activeCalls.get(callId);
        if (session == null) return;

        // Thông báo cho caller
        ClientHandler callerHandler = clientHandler.getServer()
                .getClientHandler(session.getCallerId());
        if (callerHandler != null) {
            callerHandler.sendMessage(Protocol.buildRequest(
                    Protocol.CALL_REJECTED, callId, userId));
        }

        System.out.println("❌ Call rejected: " + callId + " by " + userId);
    }

    // ==================== END CALL ====================

    private void handleEndCall(String[] parts) {
        if (parts.length < 3) return;

        String callId = parts[1];
        String userId = parts[2];

        CallSession session = activeCalls.get(callId);
        if (session == null) return;

        // Thông báo cho tất cả participants
        for (String participantId : session.getParticipants().keySet()) {
            if (!participantId.equals(userId)) {
                ClientHandler handler = clientHandler.getServer()
                        .getClientHandler(participantId);
                if (handler != null) {
                    handler.sendMessage(Protocol.buildRequest(
                            Protocol.CALL_ENDED, callId));
                }
            }
        }

        // Giải phóng UDP port
        releaseUdpPort(callId);

        // Xóa session
        activeCalls.remove(callId);

        System.out.println("✅ Call ended: " + callId);
    }

    // ==================== HELPERS ====================

    private void notifyIncomingCall(String userId, CallSession session) {
        ClientHandler handler = clientHandler.getServer().getClientHandler(userId);
        if (handler != null) {
            String callerName = getUserName(session.getCallerId());
            String message = Protocol.buildRequest(
                    Protocol.CALL_INCOMING,
                    session.getCallId(),
                    session.getCallerId(),
                    callerName,
                    session.getCallType()
            );
            handler.sendMessage(message);
        }
    }

    private String generateCallId() {
        return "CALL_" + System.currentTimeMillis();
    }

    private int allocateUdpPort(String callId) {
        // Simple port allocation - tăng dần từ base port
        int port = UDP_BASE_PORT + activeCalls.size();
        if (mediaServer != null) {
            mediaServer.registerCall(callId, port);
        }
        return port;
    }

    private void releaseUdpPort(String callId) {
        if (mediaServer != null) {
            mediaServer.unregisterCall(callId);
        }
    }

    private String getServerPublicIP() {
        try {
            // Try to get ZeroTier IP first
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String getUserName(String userId) {
        try {
            User user = UserDAO.findById(userId);
            return user != null ? user.getDisplayName() : "Unknown";
        } catch (Exception e) {
            return "User";
        }
    }

    // ==================== CALL SESSION ====================

    private static class CallSession {
        private final String callId;
        private final String conversationId;
        private final String callerId;
        private final String callType;
        private final int udpPort;
        private final ConcurrentHashMap<String, Boolean> participants;

        public CallSession(String callId, String conversationId,
                           String callerId, String callType, int udpPort) {
            this.callId = callId;
            this.conversationId = conversationId;
            this.callerId = callerId;
            this.callType = callType;
            this.udpPort = udpPort;
            this.participants = new ConcurrentHashMap<>();
            this.participants.put(callerId, true);
        }

        public void addParticipant(String userId) {
            participants.put(userId, true);
        }

        public String getCallId() { return callId; }
        public String getConversationId() { return conversationId; }
        public String getCallerId() { return callerId; }
        public String getCallType() { return callType; }
        public int getUdpPort() { return udpPort; }
        public ConcurrentHashMap<String, Boolean> getParticipants() {
            return participants;
        }
    }
}