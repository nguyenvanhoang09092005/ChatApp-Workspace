package org.example.chatappclient.client.controllers.main.handlers;

import javafx.application.Platform;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.services.CallService;
import org.example.chatappclient.client.utils.ui.AlertUtil;
import org.example.chatappclient.client.utils.ui.DialogFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler xử lý cuộc gọi thoại và video
 */
public class CallHandler {

    private final MainController mainController;
    private final CallService callService;
    private final ExecutorService executor;

    // Call state
    private String currentCallId;
    private boolean isInCall = false;
    private boolean isMuted = false;
    private boolean isVideoEnabled = true;

    public CallHandler(MainController mainController) {
        this.mainController = mainController;
        this.callService = CallService.getInstance();
        this.executor = Executors.newCachedThreadPool();

        setupCallListener();
    }

    // ==================== START CALLS ====================

    public void startAudioCall() {
        String conversationId = mainController.getCurrentConversationId();
        if (conversationId == null) {
            AlertUtil.showToastWarning("Vui lòng chọn một cuộc trò chuyện");
            return;
        }

        if (isInCall) {
            AlertUtil.showToastWarning("Bạn đang trong một cuộc gọi khác");
            return;
        }

        executor.submit(() -> {
            try {
                Platform.runLater(() -> AlertUtil.showToastInfo("Đang kết nối cuộc gọi thoại..."));

                currentCallId = callService.startCall(
                        conversationId,
                        mainController.getCurrentUser().getUserId(),
                        "audio"
                );

                if (currentCallId != null) {
                    isInCall = true;
                    Platform.runLater(this::showAudioCallDialog);
                } else {
                    Platform.runLater(() -> AlertUtil.showToastError("Không thể bắt đầu cuộc gọi"));
                }

            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Lỗi: " + e.getMessage()));
            }
        });
    }

    public void startVideoCall() {
        String conversationId = mainController.getCurrentConversationId();
        if (conversationId == null) {
            AlertUtil.showToastWarning("Vui lòng chọn một cuộc trò chuyện");
            return;
        }

        if (isInCall) {
            AlertUtil.showToastWarning("Bạn đang trong một cuộc gọi khác");
            return;
        }

        executor.submit(() -> {
            try {
                Platform.runLater(() -> AlertUtil.showToastInfo("Đang kết nối cuộc gọi video..."));

                currentCallId = callService.startCall(
                        conversationId,
                        mainController.getCurrentUser().getUserId(),
                        "video"
                );

                if (currentCallId != null) {
                    isInCall = true;
                    isVideoEnabled = true;
                    Platform.runLater(this::showVideoCallDialog);
                } else {
                    Platform.runLater(() -> AlertUtil.showToastError("Không thể bắt đầu cuộc gọi"));
                }

            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Lỗi: " + e.getMessage()));
            }
        });
    }

    // ==================== CALL ACTIONS ====================

    public void answerCall(String callId, String callType) {
        executor.submit(() -> {
            try {
                boolean success = callService.answerCall(callId, mainController.getCurrentUser().getUserId());

                if (success) {
                    currentCallId = callId;
                    isInCall = true;

                    Platform.runLater(() -> {
                        if ("video".equals(callType)) {
                            showVideoCallDialog();
                        } else {
                            showAudioCallDialog();
                        }
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showToastError("Không thể trả lời cuộc gọi"));
            }
        });
    }

    public void rejectCall(String callId) {
        executor.submit(() -> {
            try {
                callService.rejectCall(callId, mainController.getCurrentUser().getUserId());
            } catch (Exception e) {
                System.err.println("Error rejecting call: " + e.getMessage());
            }
        });
    }

    public void endCall() {
        if (currentCallId == null) return;

        executor.submit(() -> {
            try {
                callService.endCall(currentCallId, mainController.getCurrentUser().getUserId());
                resetCallState();
            } catch (Exception e) {
                System.err.println("Error ending call: " + e.getMessage());
            }
        });
    }

    public void toggleMute() {
        isMuted = !isMuted;
        callService.setMuted(currentCallId, isMuted);
    }

    public void toggleVideo() {
        isVideoEnabled = !isVideoEnabled;
        callService.setVideoEnabled(currentCallId, isVideoEnabled);
    }

    public void switchCamera() {
        callService.switchCamera(currentCallId);
    }

    // ==================== CALL UI ====================

    private void showAudioCallDialog() {
        // Get partner info
        String partnerName = "Đang gọi...";
        Conversation conv = getConversationHandler().getConversation(mainController.getCurrentConversationId());
        if (conv != null) {
            partnerName = conv.getName();
        }

        DialogFactory.showAudioCallDialog(
                partnerName,
                this::toggleMute,
                this::endCall,
                () -> isMuted
        );
    }

    private void showVideoCallDialog() {
        String partnerName = "Đang gọi...";
        Conversation conv = getConversationHandler().getConversation(mainController.getCurrentConversationId());
        if (conv != null) {
            partnerName = conv.getName();
        }

        DialogFactory.showVideoCallDialog(
                partnerName,
                this::toggleMute,
                this::toggleVideo,
                this::switchCamera,
                this::endCall,
                () -> isMuted,
                () -> isVideoEnabled
        );
    }

    private void showIncomingCallDialog(String callerName, String callType, String callId) {
        DialogFactory.showIncomingCallDialog(
                callerName,
                callType,
                () -> answerCall(callId, callType),
                () -> rejectCall(callId)
        );
    }

    // ==================== REALTIME LISTENER ====================

    private void setupCallListener() {
        callService.setOnIncomingCall((callId, callerId, callerName, callType) -> {
            Platform.runLater(() -> showIncomingCallDialog(callerName, callType, callId));
        });

        callService.setOnCallAnswered(callId -> {
            Platform.runLater(() -> AlertUtil.showToastSuccess("Cuộc gọi đã được kết nối"));
        });

        callService.setOnCallRejected(callId -> {
            Platform.runLater(() -> {
                AlertUtil.showToastInfo("Cuộc gọi bị từ chối");
                resetCallState();
            });
        });

        callService.setOnCallEnded(callId -> {
            Platform.runLater(() -> {
                AlertUtil.showToastInfo("Cuộc gọi đã kết thúc");
                resetCallState();
            });
        });

        callService.setOnCallError((callId, error) -> {
            Platform.runLater(() -> {
                AlertUtil.showToastError("Lỗi cuộc gọi: " + error);
                resetCallState();
            });
        });
    }

    // ==================== HELPERS ====================

    private void resetCallState() {
        currentCallId = null;
        isInCall = false;
        isMuted = false;
        isVideoEnabled = true;
    }

    private ConversationHandler getConversationHandler() {
        // This would be injected or accessed via mainController
        return null; // TODO: Implement proper access
    }

    public boolean isInCall() {
        return isInCall;
    }

    // ==================== CLEANUP ====================

    public void cleanup() {
        if (isInCall) {
            endCall();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}