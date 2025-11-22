package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.protocol.Protocol;

/**
 * Service xử lý cuộc gọi thoại/video
 */
public class CallService {

    private static volatile CallService instance;
    private final SocketClient socketClient;

    // Callbacks
    private OnIncomingCall onIncomingCall;
    private OnCallStatusChange onCallAnswered;
    private OnCallStatusChange onCallRejected;
    private OnCallStatusChange onCallEnded;
    private OnCallError onCallError;

    @FunctionalInterface
    public interface OnIncomingCall {
        void onCall(String callId, String callerId, String callerName, String callType);
    }

    @FunctionalInterface
    public interface OnCallStatusChange {
        void onChange(String callId);
    }

    @FunctionalInterface
    public interface OnCallError {
        void onError(String callId, String error);
    }

    private CallService() {
        socketClient = SocketClient.getInstance();
    }

    public static CallService getInstance() {
        if (instance == null) {
            synchronized (CallService.class) {
                if (instance == null) {
                    instance = new CallService();
                }
            }
        }
        return instance;
    }

    // ==================== CALL OPERATIONS ====================

    public String startCall(String conversationId, String callerId, String callType) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_START, conversationId, callerId, callType);
        String response = socketClient.sendRequest(request, 15000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return Protocol.getData(response); // Returns callId
    }

    public boolean answerCall(String callId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_ANSWER, callId, userId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        return Protocol.isSuccess(response);
    }

    public void rejectCall(String callId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_REJECT, callId, userId);
        socketClient.sendMessage(request);
    }

    public void endCall(String callId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_END, callId, userId);
        socketClient.sendMessage(request);
    }

    public void setMuted(String callId, boolean muted) {
        // TODO: Implement mute/unmute logic (WebRTC)
    }

    public void setVideoEnabled(String callId, boolean enabled) {
        // TODO: Implement video toggle (WebRTC)
    }

    public void switchCamera(String callId) {
        // TODO: Implement camera switch (WebRTC)
    }

    // ==================== CALLBACKS ====================

    public void setOnIncomingCall(OnIncomingCall callback) {
        this.onIncomingCall = callback;
    }

    public void setOnCallAnswered(OnCallStatusChange callback) {
        this.onCallAnswered = callback;
    }

    public void setOnCallRejected(OnCallStatusChange callback) {
        this.onCallRejected = callback;
    }

    public void setOnCallEnded(OnCallStatusChange callback) {
        this.onCallEnded = callback;
    }

    public void setOnCallError(OnCallError callback) {
        this.onCallError = callback;
    }
}