package server.media;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP Media Server - Relay audio/video giữa các client trong call - FIXED
 */
public class UdpMediaServer {

    private final String bindAddress;
    private final int basePort;
    private DatagramSocket socket;

    private final ExecutorService executor;
    private final AtomicBoolean running;

    // Map: callId -> CallMediaSession
    private final Map<String, CallMediaSession> activeSessions;

    // Map: "ip:port" -> callId for quick lookup
    private final Map<String, String> addressToCallId;

    private static final int MAX_PACKET_SIZE = 65507;

    public UdpMediaServer(String bindAddress, int basePort) {
        this.bindAddress = bindAddress;
        this.basePort = basePort;
        this.executor = Executors.newCachedThreadPool();
        this.running = new AtomicBoolean(false);
        this.activeSessions = new ConcurrentHashMap<>();
        this.addressToCallId = new ConcurrentHashMap<>();
    }

    // ==================== START/STOP ====================

    public void start() throws Exception {
        if (running.get()) {
            System.out.println("⚠️ UDP Media Server already running");
            return;
        }

        socket = new DatagramSocket(basePort, InetAddress.getByName(bindAddress));
        socket.setReuseAddress(true);
        running.set(true);

        // Bắt đầu receive loop
        executor.submit(this::receiveLoop);

        System.out.println("✅ UDP Media Server listening on " + bindAddress + ":" + basePort);
    }

    public void stop() {
        if (!running.get()) return;

        System.out.println("🛑 Stopping UDP Media Server...");
        running.set(false);

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        executor.shutdownNow();
        activeSessions.clear();
        addressToCallId.clear();

        System.out.println("✅ UDP Media Server stopped");
    }

    // ==================== CALL MANAGEMENT ====================

    public void registerCall(String callId, int port) {
        CallMediaSession session = new CallMediaSession(callId, port);
        activeSessions.put(callId, session);
        System.out.println("📞 Registered call: " + callId + " on port " + port);
        System.out.println("   Active sessions: " + activeSessions.size());
    }

    public void unregisterCall(String callId) {
        CallMediaSession session = activeSessions.remove(callId);
        if (session != null) {
            // Remove address mappings
            for (Participant p : session.getParticipants()) {
                String key = p.address.getHostAddress() + ":" + p.port;
                addressToCallId.remove(key);
            }
            System.out.println("📞 Unregistered call: " + callId);
            System.out.println("   Remaining sessions: " + activeSessions.size());
        }
    }

    // ==================== RECEIVE & RELAY ====================

    private void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        int packetCount = 0;

        System.out.println("🔊 UDP receive loop started");

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                packetCount++;
                if (packetCount % 100 == 0) {
                    System.out.println("📊 Received " + packetCount + " UDP packets");
                }

                // Process packet in separate thread
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                DatagramPacket copiedPacket = new DatagramPacket(
                        data, data.length, packet.getAddress(), packet.getPort()
                );

                executor.submit(() -> processPacket(copiedPacket));

            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("⚠️ Error receiving UDP packet: " + e.getMessage());
                }
            }
        }

        System.out.println("🔊 UDP receive loop stopped. Total packets: " + packetCount);
    }

    private void processPacket(DatagramPacket receivedPacket) {
        try {
            InetAddress senderAddress = receivedPacket.getAddress();
            int senderPort = receivedPacket.getPort();
            String senderKey = senderAddress.getHostAddress() + ":" + senderPort;

            byte[] data = receivedPacket.getData();
            int length = receivedPacket.getLength();

            // Validate packet
            if (length < 13) {
                System.err.println("⚠️ Invalid packet (too short): " + length + " bytes");
                return;
            }

            // Find call session
            CallMediaSession session = findOrCreateSessionForSender(senderAddress, senderPort);

            if (session != null) {
                // Add sender to session if not already there
                session.addParticipant(senderAddress, senderPort);
                addressToCallId.put(senderKey, session.getCallId());

                // Relay packet to other participants
                relayPacket(session, data, length, senderAddress, senderPort);
            } else {
                System.err.println("⚠️ No session found for " + senderKey);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error processing packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void relayPacket(CallMediaSession session, byte[] data, int length,
                             InetAddress senderAddr, int senderPort) {
        int relayCount = 0;

        for (Participant p : session.getParticipants()) {
            // Không gửi lại cho sender
            if (p.address.equals(senderAddr) && p.port == senderPort) {
                continue;
            }

            try {
                DatagramPacket packet = new DatagramPacket(
                        data, length, p.address, p.port
                );
                socket.send(packet);
                relayCount++;
            } catch (Exception e) {
                System.err.println("⚠️ Failed to relay to " +
                        p.address.getHostAddress() + ":" + p.port);
            }
        }

        if (relayCount > 0) {
            // Log every 50 relays
            if (session.packetCount.incrementAndGet() % 50 == 0) {
                System.out.println("📤 Relayed " + session.packetCount.get() +
                        " packets for call " + session.getCallId());
            }
        }
    }

    private CallMediaSession findOrCreateSessionForSender(InetAddress address, int port) {
        String key = address.getHostAddress() + ":" + port;

        // Check if we already know this address
        String callId = addressToCallId.get(key);
        if (callId != null) {
            return activeSessions.get(callId);
        }

        // Find by existing participant
        for (CallMediaSession session : activeSessions.values()) {
            if (session.hasParticipant(address, port)) {
                addressToCallId.put(key, session.getCallId());
                return session;
            }
        }

        // Assign to first available session (if client hasn't been registered yet)
        if (!activeSessions.isEmpty()) {
            CallMediaSession firstSession = activeSessions.values().iterator().next();
            System.out.println("📍 Assigning " + key + " to call " + firstSession.getCallId());
            return firstSession;
        }

        return null;
    }

    // ==================== CALL MEDIA SESSION ====================

    private static class CallMediaSession {
        private final String callId;
        private final int port;
        private final ConcurrentHashMap<String, Participant> participants;
        private final java.util.concurrent.atomic.AtomicInteger packetCount;

        public CallMediaSession(String callId, int port) {
            this.callId = callId;
            this.port = port;
            this.participants = new ConcurrentHashMap<>();
            this.packetCount = new java.util.concurrent.atomic.AtomicInteger(0);
        }

        public void addParticipant(InetAddress address, int port) {
            String key = address.getHostAddress() + ":" + port;
            if (!participants.containsKey(key)) {
                participants.put(key, new Participant(address, port));
                System.out.println("➕ Participant added to call " + callId + ": " + key);
                System.out.println("   Total participants: " + participants.size());
            }
        }

        public boolean hasParticipant(InetAddress address, int port) {
            String key = address.getHostAddress() + ":" + port;
            return participants.containsKey(key);
        }

        public Iterable<Participant> getParticipants() {
            return participants.values();
        }

        public String getCallId() { return callId; }
        public int getPort() { return port; }
    }

    private static class Participant {
        public final InetAddress address;
        public final int port;

        public Participant(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public String toString() {
            return address.getHostAddress() + ":" + port;
        }
    }
}