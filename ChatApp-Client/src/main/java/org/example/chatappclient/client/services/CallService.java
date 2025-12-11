package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.protocol.Protocol;
import org.example.chatappclient.client.services.media.UdpMediaClient;

/**
 * Service xử lý cuộc gọi thoại/video qua UDP - ĐÃ HOÀN CHỈNH
 */
public class CallService {
    private static volatile CallService instance;
    private final SocketClient socketClient;
    private UdpMediaClient mediaClient;

    // Các callback thông báo sự kiện cuộc gọi
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
        setupCallHandlers();
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

    // ==================== CÀI ĐẶT CÁC HANDLER ====================
    private void setupCallHandlers() {
        System.out.println("Đang cài đặt các handler cho CallService...");

        // Cuộc gọi đến
        socketClient.registerHandler(Protocol.CALL_INCOMING, message -> {
            try {
                String[] parts = Protocol.parseMessage(message);
                if (parts.length >= 5) {
                    String callId = parts[1];
                    String callerId = parts[2];
                    String callerName = parts[3];
                    String callType = parts[4];

                    System.out.println("CUỘC GỌI ĐẾN:");
                    System.out.println(" ID cuộc gọi: " + callId);
                    System.out.println(" Người gọi: " + callerName);
                    System.out.println(" Loại: " + callType);

                    if (onIncomingCall != null) {
                        onIncomingCall.onCall(callId, callerId, callerName, callType);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý cuộc gọi đến: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Cuộc gọi được chấp nhận
        socketClient.registerHandler(Protocol.CALL_ANSWERED, message -> {
            try {
                String[] parts = Protocol.parseMessage(message);
                if (parts.length >= 2) {
                    String callId = parts[1];
                    System.out.println("CUỘC GỌI ĐÃ ĐƯỢC CHẤP NHẬN: " + callId);
                    if (onCallAnswered != null) {
                        onCallAnswered.onChange(callId);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý sự kiện chấp nhận cuộc gọi: " + e.getMessage());
            }
        });

        // Cuộc gọi bị từ chối
        socketClient.registerHandler(Protocol.CALL_REJECTED, message -> {
            try {
                String[] parts = Protocol.parseMessage(message);
                if (parts.length >= 2) {
                    String callId = parts[1];
                    System.out.println("CUỘC GỌI BỊ TỪ CHỐI: " + callId);
                    stopMediaClient();
                    if (onCallRejected != null) {
                        onCallRejected.onChange(callId);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý từ chối cuộc gọi: " + e.getMessage());
            }
        });

        // Cuộc gọi kết thúc
        socketClient.registerHandler(Protocol.CALL_ENDED, message -> {
            try {
                String[] parts = Protocol.parseMessage(message);
                if (parts.length >= 2) {
                    String callId = parts[1];
                    System.out.println("CUỘC GỌI ĐÃ KẾT THÚC: " + callId);
                    stopMediaClient();
                    if (onCallEnded != null) {
                        onCallEnded.onChange(callId);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý kết thúc cuộc gọi: " + e.getMessage());
            }
        });

        // Lỗi cuộc gọi
        socketClient.registerHandler(Protocol.CALL_ERROR, message -> {
            try {
                String[] parts = Protocol.parseMessage(message);
                if (parts.length >= 3) {
                    String callId = parts[1];
                    String error = parts[2];
                    System.err.println("LỖI CUỘC GỌI: " + error);
                    stopMediaClient();
                    if (onCallError != null) {
                        onCallError.onError(callId, error);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý lỗi cuộc gọi: " + e.getMessage());
            }
        });

        System.out.println("Đã đăng ký thành công các handler của CallService");
    }

    // ==================== THAO TÁC CUỘC GỌI ====================
    public String startCall(String conversationId, String callerId, String callType) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_START, conversationId, callerId, callType);
        String response = socketClient.sendRequest(request, 15000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }
        if (!Protocol.isSuccess(response)) {
            throw new Exception(Protocol.getErrorMessage(response));
        }

        // Phân tích phản hồi: SUCCESS|||message|||callId|||serverIP|||udpPort
        String[] parts = Protocol.parseMessage(response);
        if (parts.length >= 5) {
            String callId = parts[2];
            String serverIP = parts[3];
            int udpPort = Integer.parseInt(parts[4]);

            System.out.println("Bắt đầu cuộc gọi thành công:");
            System.out.println(" ID cuộc gọi: " + callId);
            System.out.println(" Server media: " + serverIP + ":" + udpPort);

            initializeMediaClient(serverIP, udpPort, callType);
            return callId;
        }
        throw new Exception("Định dạng phản hồi không hợp lệ");
    }

    public boolean answerCall(String callId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_ANSWER, callId, userId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }
        if (!Protocol.isSuccess(response)) {
            throw new Exception(Protocol.getErrorMessage(response));
        }

        // Phân tích thông tin UDP: SUCCESS|||message|||serverIP|||udpPort|||callType
        String[] parts = Protocol.parseMessage(response);
        if (parts.length >= 5) {
            String serverIP = parts[2];
            int udpPort = Integer.parseInt(parts[3]);
            String callType = parts[4];

            System.out.println("Chấp nhận cuộc gọi thành công:");
            System.out.println(" Server media: " + serverIP + ":" + udpPort);
            System.out.println(" Loại cuộc gọi: " + callType);

            initializeMediaClient(serverIP, udpPort, callType);
            return true;
        }
        return false;
    }

    public void rejectCall(String callId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_REJECT, callId, userId);
        socketClient.sendMessage(request);
        stopMediaClient();
        System.out.println("Đã từ chối cuộc gọi: " + callId);
    }

    public void endCall(String callId, String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_END, callId, userId);
        socketClient.sendMessage(request);
        stopMediaClient();
        System.out.println("Đã kết thúc cuộc gọi: " + callId);
    }

    public void setMuted(String callId, boolean muted) {
        if (mediaClient != null) {
            mediaClient.setMuted(muted);
        }
    }

    public void setVideoEnabled(String callId, boolean enabled) {
        if (mediaClient != null) {
            mediaClient.setVideoEnabled(enabled);
        }
    }

    public void switchCamera(String callId) {
        if (mediaClient != null) {
            mediaClient.switchCamera();
        }
    }

    // ==================== QUẢN LÝ MEDIA CLIENT ====================
    private void initializeMediaClient(String serverIP, int udpPort, String callType) {
        try {
            // Dừng client cũ nếu đang tồn tại
            stopMediaClient();

            boolean isVideo = "video".equals(callType);
            mediaClient = new UdpMediaClient(serverIP, udpPort, isVideo);
            mediaClient.start();

            System.out.println("Đã khởi động UDP Media Client:");
            System.out.println(" Server: " + serverIP + ":" + udpPort);
            System.out.println(" Loại: " + (isVideo ? "VIDEO" : "AUDIO"));
        } catch (Exception e) {
            System.err.println("Không thể khởi động media client: " + e.getMessage());
            e.printStackTrace();
            if (onCallError != null) {
                onCallError.onError(null, "Không thể khởi tạo media: " + e.getMessage());
            }
        }
    }

    private void stopMediaClient() {
        if (mediaClient != null) {
            try {
                mediaClient.stop();
                System.out.println("Đã dừng UDP Media Client");
            } catch (Exception e) {
                System.err.println("Lỗi khi dừng media client: " + e.getMessage());
            } finally {
                mediaClient = null;
            }
        }
    }

    // ==================== ĐĂNG KÝ CALLBACK ====================
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

    public UdpMediaClient getMediaClient() {
        return mediaClient;
    }

    // ==================== DỌN DẸP ====================
    public void cleanup() {
        stopMediaClient();
        System.out.println("Đã dọn dẹp CallService");
    }
}