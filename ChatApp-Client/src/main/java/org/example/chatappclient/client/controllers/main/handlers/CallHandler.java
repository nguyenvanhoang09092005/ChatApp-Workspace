package org.example.chatappclient.client.controllers.main.handlers;

import javafx.application.Platform;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.services.CallService;
import org.example.chatappclient.client.utils.ui.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler xử lý cuộc gọi thoại và video - Sử dụng IconLoader
 */
public class CallHandler {

    private final MainController mainController;
    private final CallService callService;
    private final ExecutorService executor;
    private ConversationHandler conversationHandler;

    // Call dialogs
    private AudioCallDialog audioCallDialog;
    private VideoCallDialog videoCallDialog;
    private IncomingCallDialog incomingCallDialog;

    // Call state
    private String currentCallId;
    private boolean isInCall = false;

    public CallHandler(MainController mainController) {
        this.mainController = mainController;
        this.callService = CallService.getInstance();
        this.executor = Executors.newCachedThreadPool();

        // Preload icons khi khởi tạo
        preloadIcons();

        setupCallListener();
    }

    /**
     * Preload tất cả icons cần thiết cho cuộc gọi
     */
    private void preloadIcons() {
        executor.submit(() -> {
            System.out.println("🔄 Preloading call icons...");
            IconLoader.preloadAllIcons();
        });
    }

    public void setConversationHandler(ConversationHandler conversationHandler) {
        this.conversationHandler = conversationHandler;
        System.out.println("✅ ConversationHandler được set cho CallHandler");
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
                Platform.runLater(() ->
                        AlertUtil.showToastInfo("Đang kết nối cuộc gọi thoại...")
                );

                currentCallId = callService.startCall(
                        conversationId,
                        mainController.getCurrentUser().getUserId(),
                        "audio"
                );

                if (currentCallId != null) {
                    isInCall = true;

                    // Get partner info
                    String partnerName = getPartnerName(conversationId);
                    String avatarUrl = getPartnerAvatar(conversationId);

                    Platform.runLater(() ->
                            showAudioCallDialog(partnerName, avatarUrl)
                    );

                    System.out.println("✅ Cuộc gọi audio bắt đầu: " + currentCallId);
                } else {
                    Platform.runLater(() ->
                            AlertUtil.showToastError("Không thể bắt đầu cuộc gọi")
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showToastError("Lỗi: " + e.getMessage())
                );
                e.printStackTrace();
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
                Platform.runLater(() ->
                        AlertUtil.showToastInfo("Đang kết nối cuộc gọi video...")
                );

                currentCallId = callService.startCall(
                        conversationId,
                        mainController.getCurrentUser().getUserId(),
                        "video"
                );

                if (currentCallId != null) {
                    isInCall = true;
                    String partnerName = getPartnerName(conversationId);

                    Platform.runLater(() ->
                            showVideoCallDialog(partnerName)
                    );

                    System.out.println("✅ Cuộc gọi video bắt đầu: " + currentCallId);
                } else {
                    Platform.runLater(() ->
                            AlertUtil.showToastError("Không thể bắt đầu cuộc gọi")
                    );
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showToastError("Lỗi: " + e.getMessage())
                );
                e.printStackTrace();
            }
        });
    }

    // ==================== CALL ACTIONS ====================

    public void answerCall(String callId, String callType) {
        executor.submit(() -> {
            try {
                boolean success = callService.answerCall(
                        callId,
                        mainController.getCurrentUser().getUserId()
                );

                if (success) {
                    currentCallId = callId;
                    isInCall = true;

                    Platform.runLater(() -> {
                        // Đóng dialog cuộc gọi đến
                        if (incomingCallDialog != null) {
                            incomingCallDialog.close();
                            incomingCallDialog = null;
                        }

                        // Show appropriate call dialog
                        if ("video".equals(callType)) {
                            String partnerName = "Người dùng";
                            showVideoCallDialog(partnerName);
                        } else {
                            String partnerName = "Người dùng";
                            String avatarUrl = null;
                            showAudioCallDialog(partnerName, avatarUrl);
                        }
                    });

                    // Set connected state after 1 second
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        if (audioCallDialog != null) {
                            audioCallDialog.setConnected();
                        }
                        if (videoCallDialog != null) {
                            videoCallDialog.setConnected();
                        }
                    });

                    System.out.println("✅ Đã trả lời cuộc gọi: " + callId);
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showToastError("Không thể trả lời cuộc gọi")
                );
                e.printStackTrace();
            }
        });
    }

    public void rejectCall(String callId) {
        executor.submit(() -> {
            try {
                callService.rejectCall(callId, mainController.getCurrentUser().getUserId());

                Platform.runLater(() -> {
                    if (incomingCallDialog != null) {
                        incomingCallDialog.close();
                        incomingCallDialog = null;
                    }
                    AlertUtil.showToastInfo("Đã từ chối cuộc gọi");
                });

                System.out.println("✅ Đã từ chối cuộc gọi: " + callId);

            } catch (Exception e) {
                System.err.println("❌ Lỗi từ chối cuộc gọi: " + e.getMessage());
            }
        });
    }

    public void endCall() {
        if (currentCallId == null) {
            System.out.println("⚠️ Không có cuộc gọi nào đang hoạt động");
            return;
        }

        final String callIdToEnd = currentCallId;

        executor.submit(() -> {
            try {
                callService.endCall(callIdToEnd, mainController.getCurrentUser().getUserId());

                Platform.runLater(() -> {
                    closeAllDialogs();
                    resetCallState();
                    AlertUtil.showToastInfo("Cuộc gọi đã kết thúc");
                });

                System.out.println("✅ Đã kết thúc cuộc gọi: " + callIdToEnd);

            } catch (Exception e) {
                System.err.println("❌ Lỗi kết thúc cuộc gọi: " + e.getMessage());
                Platform.runLater(() -> {
                    closeAllDialogs();
                    resetCallState();
                });
            }
        });
    }

    public void toggleMute() {
        if (currentCallId != null) {
            boolean isMuted = false;

            if (audioCallDialog != null) {
                isMuted = audioCallDialog.isMuted();
            } else if (videoCallDialog != null) {
                isMuted = videoCallDialog.isMuted();
            }

            callService.setMuted(currentCallId, isMuted);
            System.out.println(isMuted ? "🔇 Đã tắt tiếng" : "🔊 Đã bật tiếng");
        }
    }

    public void toggleVideo() {
        if (currentCallId != null && videoCallDialog != null) {
            boolean isVideoEnabled = videoCallDialog.isVideoEnabled();
            callService.setVideoEnabled(currentCallId, isVideoEnabled);
            System.out.println(isVideoEnabled ? "📹 Đã bật video" : "📷 Đã tắt video");
        }
    }

    public void switchCamera() {
        if (currentCallId != null) {
            callService.switchCamera(currentCallId);
            System.out.println("🔄 Đã chuyển camera");
        }
    }

    // ==================== CALL UI ====================

    private void showAudioCallDialog(String partnerName, String avatarUrl) {
        try {
            audioCallDialog = new AudioCallDialog(partnerName, avatarUrl);

            audioCallDialog.setOnMuteToggle(this::toggleMute);
            audioCallDialog.setOnEndCall(this::endCall);

            audioCallDialog.setRinging();
            audioCallDialog.show();

            System.out.println("✅ Đã hiển thị AudioCallDialog cho: " + partnerName);

        } catch (Exception e) {
            System.err.println("❌ Lỗi hiển thị AudioCallDialog: " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showToastError("Không thể hiển thị giao diện cuộc gọi");
        }
    }

    private void showVideoCallDialog(String partnerName) {
        try {
            videoCallDialog = new VideoCallDialog(partnerName);

            videoCallDialog.setOnMuteToggle(this::toggleMute);
            videoCallDialog.setOnVideoToggle(this::toggleVideo);
            videoCallDialog.setOnSwitchCamera(this::switchCamera);
            videoCallDialog.setOnEndCall(this::endCall);

            videoCallDialog.setRinging();
            videoCallDialog.show();

            System.out.println("✅ Đã hiển thị VideoCallDialog cho: " + partnerName);

        } catch (Exception e) {
            System.err.println("❌ Lỗi hiển thị VideoCallDialog: " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showToastError("Không thể hiển thị giao diện cuộc gọi");
        }
    }

    private void showIncomingCallDialog(String callerName, String callType,
                                        String callId, String avatarUrl) {
        try {
            incomingCallDialog = new IncomingCallDialog(callerName, callType, avatarUrl);

            incomingCallDialog.setOnAccept(() -> answerCall(callId, callType));
            incomingCallDialog.setOnReject(() -> rejectCall(callId));

            incomingCallDialog.show();

            System.out.println("✅ Đã hiển thị IncomingCallDialog từ: " + callerName);

        } catch (Exception e) {
            System.err.println("❌ Lỗi hiển thị IncomingCallDialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeAllDialogs() {
        if (audioCallDialog != null) {
            try {
                audioCallDialog.close();
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi đóng AudioCallDialog: " + e.getMessage());
            }
            audioCallDialog = null;
        }

        if (videoCallDialog != null) {
            try {
                videoCallDialog.close();
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi đóng VideoCallDialog: " + e.getMessage());
            }
            videoCallDialog = null;
        }

        if (incomingCallDialog != null) {
            try {
                incomingCallDialog.close();
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi đóng IncomingCallDialog: " + e.getMessage());
            }
            incomingCallDialog = null;
        }

        System.out.println("🗑️ Đã đóng tất cả dialog cuộc gọi");
    }

    // ==================== REALTIME LISTENER ====================

    private void setupCallListener() {
        // Incoming call
        callService.setOnIncomingCall((callId, callerId, callerName, callType) -> {
            System.out.println("📞 Cuộc gọi đến từ: " + callerName + " (" + callType + ")");

            // Get avatar URL
            String avatarUrl = null;
            try {
                if (conversationHandler != null) {
                    // Có thể cần fetch từ UserService thay vì ConversationHandler
                    avatarUrl = getPartnerAvatar(callerId);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Không thể lấy avatar: " + e.getMessage());
            }

            final String finalAvatarUrl = avatarUrl;
            Platform.runLater(() ->
                    showIncomingCallDialog(callerName, callType, callId, finalAvatarUrl)
            );
        });

        // Call answered
        callService.setOnCallAnswered(callId -> {
            Platform.runLater(() -> {
                AlertUtil.showToastSuccess("Cuộc gọi đã được kết nối");

                if (audioCallDialog != null) {
                    audioCallDialog.setConnected();
                }
                if (videoCallDialog != null) {
                    videoCallDialog.setConnected();
                }

                System.out.println("✅ Cuộc gọi đã kết nối: " + callId);
            });
        });

        // Call rejected
        callService.setOnCallRejected(callId -> {
            Platform.runLater(() -> {
                AlertUtil.showToastInfo("Cuộc gọi bị từ chối");
                closeAllDialogs();
                resetCallState();

                System.out.println("❌ Cuộc gọi bị từ chối: " + callId);
            });
        });

        // Call ended
        callService.setOnCallEnded(callId -> {
            Platform.runLater(() -> {
                AlertUtil.showToastInfo("Cuộc gọi đã kết thúc");
                closeAllDialogs();
                resetCallState();

                System.out.println("✅ Cuộc gọi đã kết thúc: " + callId);
            });
        });

        // Call error
        callService.setOnCallError((callId, error) -> {
            Platform.runLater(() -> {
                AlertUtil.showToastError("Lỗi cuộc gọi: " + error);
                closeAllDialogs();
                resetCallState();

                System.err.println("❌ Lỗi cuộc gọi " + callId + ": " + error);
            });
        });

        System.out.println("✅ Đã thiết lập CallService listeners");
    }

    // ==================== HELPERS ====================

    private String getPartnerName(String conversationId) {
        try {
            if (conversationHandler != null) {
                Conversation conv = conversationHandler.getConversation(conversationId);
                if (conv != null && conv.getName() != null) {
                    return conv.getName();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không thể lấy partner name: " + e.getMessage());
        }
        return "Người dùng";
    }

    private String getPartnerAvatar(String conversationId) {
        try {
            if (conversationHandler != null) {
                Conversation conv = conversationHandler.getConversation(conversationId);
                if (conv != null) {
                    return conv.getAvatarUrl();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không thể lấy partner avatar: " + e.getMessage());
        }
        return null;
    }

    private void resetCallState() {
        currentCallId = null;
        isInCall = false;
        System.out.println("🔄 Call state đã được reset");
    }

    // ==================== PUBLIC GETTERS ====================

    public boolean isInCall() {
        return isInCall;
    }

    public String getCurrentCallId() {
        return currentCallId;
    }

    // ==================== CLEANUP ====================

    public void cleanup() {
        System.out.println("🧹 Đang cleanup CallHandler...");

        if (isInCall) {
            endCall();
        }

        closeAllDialogs();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            System.out.println("✅ Executor đã được shutdown");
        }

        // Clear icon cache nếu cần
        // IconLoader.clearCache();

        System.out.println("✅ CallHandler đã được cleanup hoàn toàn");
    }
}