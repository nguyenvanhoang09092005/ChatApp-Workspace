package server.handlers;

import config.ServerConfig;
import database.dao.CallHistoryDAO;
import database.dao.CallParticipantDAO;
import database.dao.ConversationDAO;
import database.dao.UserDAO;
import models.CallHistory;
import models.CallParticipant;
import models.Conversation;
import models.User;
import protocol.Protocol;
import server.ClientHandler;
import server.media.UdpMediaServer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side Call Handler - FIXED với Call History tracking
 * Version: 3.0 - Complete IP Detection + Call History
 */
public class CallHandler {

    private final ClientHandler clientHandler;
    private static final Map<String, CallSession> activeCalls = new ConcurrentHashMap<>();
    private static UdpMediaServer mediaServer;

    public CallHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        initializeMediaServer();
    }

    private static synchronized void initializeMediaServer() {
        if (mediaServer == null) {
            try {
                String bindAddress = ServerConfig.getUdpServerIP();
                int basePort = ServerConfig.getUdpBasePort();

                mediaServer = new UdpMediaServer(bindAddress, basePort);
                mediaServer.start();

                System.out.println("✅ UDP Media Server started");
                System.out.println("   Bind Address: " + bindAddress);
                System.out.println("   Base Port: " + basePort);

            } catch (Exception e) {
                System.err.println("❌ Failed to start UDP Media Server: " + e.getMessage());
                e.printStackTrace();
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
        String callType = parts[3];

        try {
            Conversation conv = ConversationDAO.getConversationById(conversationId);
            if (conv == null) {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_NOT_FOUND, "Conversation not found"));
                return;
            }

            String callId = generateCallId();
            int udpPort = allocateUdpPort(callId);

            // Tạo session cuộc gọi
            CallSession session = new CallSession(
                    callId, conversationId, callerId, callType, udpPort
            );
            activeCalls.put(callId, session);

            // ✅ LƯU CALL HISTORY
            String callerName = getUserName(callerId);
            CallHistory callHistory = new CallHistory(
                    callId, conversationId, callType, callerId, callerName
            );
            CallHistoryDAO.createCallHistory(callHistory);

            // ✅ LƯU CALLER PARTICIPANT
            CallParticipant callerParticipant = new CallParticipant(
                    callId, callerId, "caller", "initiated"
            );
            CallParticipantDAO.addParticipant(callerParticipant);
            CallParticipantDAO.setJoinedTime(callId, callerId);

            // Notify members
            List<String> memberIds = conv.getMemberIds();
            for (String memberId : memberIds) {
                if (!memberId.equals(callerId)) {
                    notifyIncomingCall(memberId, session);

                    // ✅ LƯU RECEIVER PARTICIPANT (pending state)
                    CallParticipant receiverParticipant = new CallParticipant(
                            callId, memberId, "receiver", "pending"
                    );
                    CallParticipantDAO.addParticipant(receiverParticipant);
                }
            }

            // Get server IP for client
            String serverIP = getServerIPForClient(clientHandler);

            String responseData = String.format("%s%s%s%s%d",
                    callId, Protocol.DELIMITER,
                    serverIP, Protocol.DELIMITER,
                    udpPort);

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Call started", responseData));

            System.out.println("✅ Call started: " + callId);
            System.out.println("   Caller: " + callerId);
            System.out.println("   Server IP: " + serverIP);
            System.out.println("   UDP Port: " + udpPort);
            System.out.println("   Type: " + callType);
            System.out.println("   ✓ Saved to call_history");

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

        // ✅ CẬP NHẬT PARTICIPANT ACTION
        CallParticipantDAO.updateParticipantAction(callId, userId, "answered");
        CallParticipantDAO.setJoinedTime(callId, userId);

        // Notify caller
        ClientHandler callerHandler = clientHandler.getServer()
                .getClientHandler(session.getCallerId());
        if (callerHandler != null) {
            callerHandler.sendMessage(Protocol.buildRequest(
                    Protocol.CALL_ANSWERED, callId, userId));
        }

        String serverIP = getServerIPForClient(clientHandler);

        String responseData = String.format("%s%s%d%s%s",
                serverIP, Protocol.DELIMITER,
                session.getUdpPort(), Protocol.DELIMITER,
                session.getCallType());

        clientHandler.sendMessage(Protocol.buildSuccessResponse(
                "Call answered", responseData));

        System.out.println("✅ Call answered: " + callId);
        System.out.println("   Answerer: " + userId);
        System.out.println("   ✓ Updated call_participants");
    }

    // ==================== REJECT CALL ====================

    private void handleRejectCall(String[] parts) {
        if (parts.length < 3) return;

        String callId = parts[1];
        String userId = parts[2];

        CallSession session = activeCalls.get(callId);
        if (session == null) return;

        // ✅ CẬP NHẬT PARTICIPANT ACTION
        CallParticipantDAO.updateParticipantAction(callId, userId, "rejected");

        // ✅ CẬP NHẬT CALL HISTORY STATUS
        CallHistoryDAO.endCall(callId, "rejected");

        // Notify caller
        ClientHandler callerHandler = clientHandler.getServer()
                .getClientHandler(session.getCallerId());
        if (callerHandler != null) {
            callerHandler.sendMessage(Protocol.buildRequest(
                    Protocol.CALL_REJECTED, callId, userId));
        }

        releaseUdpPort(callId);
        activeCalls.remove(callId);

        System.out.println("❌ Call rejected: " + callId + " by " + userId);
        System.out.println("   ✓ Updated call_history (status=rejected)");
    }

    // ==================== END CALL ====================

    private void handleEndCall(String[] parts) {
        if (parts.length < 3) return;

        String callId = parts[1];
        String userId = parts[2];

        CallSession session = activeCalls.get(callId);
        if (session == null) return;

        // ✅ SET LEFT TIME cho user kết thúc
        CallParticipantDAO.setLeftTime(callId, userId);
        CallParticipantDAO.updateParticipantAction(callId, userId, "left");

        // Notify all participants
        for (String participantId : session.getParticipants().keySet()) {
            if (!participantId.equals(userId)) {
                ClientHandler handler = clientHandler.getServer()
                        .getClientHandler(participantId);
                if (handler != null) {
                    handler.sendMessage(Protocol.buildRequest(
                            Protocol.CALL_ENDED, callId));
                }

                // ✅ UPDATE LEFT TIME cho các participants khác
                CallParticipantDAO.setLeftTime(callId, participantId);
            }
        }

        // ✅ CẬP NHẬT CALL HISTORY - KẾT THÚC CUỘC GỌI
        CallHistoryDAO.endCall(callId, "completed");

        releaseUdpPort(callId);
        activeCalls.remove(callId);

        System.out.println("✅ Call ended: " + callId);
        System.out.println("   ✓ Updated call_history (status=completed)");
        System.out.println("   ✓ Updated call_participants (left_at)");
        System.out.println("   Remaining active calls: " + activeCalls.size());
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
        int port = ServerConfig.getUdpBasePort() + activeCalls.size();
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

    // ==================== IP DETECTION ====================

    private String getServerIPForClient(ClientHandler handler) {
        try {
            InetAddress clientAddr = handler.getClientAddress();
            InetAddress serverLocalAddr = handler.getServerLocalAddress();

            if (serverLocalAddr != null &&
                    !serverLocalAddr.isLoopbackAddress() &&
                    !serverLocalAddr.isAnyLocalAddress()) {
                return serverLocalAddr.getHostAddress();
            }

            if (clientAddr != null) {
                String bestIP = detectBestNetworkForClient(clientAddr);
                if (bestIP != null) {
                    return bestIP;
                }
            }

        } catch (Exception e) {
            System.err.println("⚠️ Failed to detect server IP: " + e.getMessage());
        }

        return "127.0.0.1";
    }

    private String detectBestNetworkForClient(InetAddress clientAddr) {
        try {
            String clientIP = clientAddr.getHostAddress();

            if (ServerConfig.isZeroTierEnabled() && isZeroTierIP(clientIP)) {
                String ztIP = getZeroTierIP();
                if (ztIP != null) return ztIP;
            }

            if (isPrivateIP(clientIP)) {
                String matchingIP = getMatchingPrivateIP(clientIP);
                if (matchingIP != null) return matchingIP;

                String anyPrivateIP = getAnyPrivateIP();
                if (anyPrivateIP != null) return anyPrivateIP;
            }

            String publicIP = getPublicIP();
            if (publicIP != null) return publicIP;

        } catch (Exception e) {
            System.err.println("⚠️ Error detecting best network: " + e.getMessage());
        }

        return null;
    }

    private boolean isZeroTierIP(String ip) {
        return ip.matches("172\\.(2[2-9]|3[0-1])\\..*");
    }

    private boolean isPrivateIP(String ip) {
        return ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    }

    private String getZeroTierIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;

                String name = iface.getName().toLowerCase();
                String displayName = iface.getDisplayName().toLowerCase();

                if (name.contains("zt") || name.contains("zerotier") ||
                        name.startsWith("feth") || displayName.contains("zerotier")) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr.getAddress().length == 4) {
                            String ip = addr.getHostAddress();
                            if (isZeroTierIP(ip)) return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error getting ZeroTier IP: " + e.getMessage());
        }
        return null;
    }

    private String getMatchingPrivateIP(String clientIP) {
        try {
            String clientSubnet = clientIP.substring(0, clientIP.lastIndexOf('.'));
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getAddress().length != 4) continue;

                    String ip = addr.getHostAddress();
                    String subnet = ip.substring(0, ip.lastIndexOf('.'));

                    if (subnet.equals(clientSubnet)) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error finding matching private IP: " + e.getMessage());
        }
        return null;
    }

    private String getPublicIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getAddress().length != 4) continue;

                    String ip = addr.getHostAddress();
                    if (!isPrivateIP(ip) && !ip.startsWith("127.")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error getting public IP: " + e.getMessage());
        }
        return null;
    }

    private String getAnyPrivateIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<String> ips192 = new ArrayList<>();
            List<String> ips10 = new ArrayList<>();
            List<String> ips172 = new ArrayList<>();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getAddress().length != 4) continue;

                    String ip = addr.getHostAddress();
                    if (ip.startsWith("192.168.")) {
                        ips192.add(ip);
                    } else if (ip.startsWith("10.")) {
                        ips10.add(ip);
                    } else if (ip.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
                        ips172.add(ip);
                    }
                }
            }

            if (!ips192.isEmpty()) return ips192.get(0);
            if (!ips10.isEmpty()) return ips10.get(0);
            if (!ips172.isEmpty()) return ips172.get(0);

        } catch (Exception e) {
            System.err.println("⚠️ Error getting any private IP: " + e.getMessage());
        }
        return null;
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