package org.example.chatappclient.client.controllers.main.handlers;

import javafx.application.Platform;
import org.example.chatappclient.client.controllers.main.MainController;
import org.example.chatappclient.client.models.Conversation;
import org.example.chatappclient.client.services.CallService;
import org.example.chatappclient.client.services.media.UdpMediaClient;
import org.example.chatappclient.client.utils.ui.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ✅ FINAL FIXED: Call Handler với Video Streaming hoàn chỉnh
 * Fix: Đảm bảo callback được register TRƯỚC KHI dialog.setConnected()
 */
public class CallHandler {

    private final MainController mainController;
    private final CallService callService;
    private final ExecutorService executor;
    private ConversationHandler conversationHandler;

    private AudioCallDialog audioCallDialog;
    private VideoCallDialog videoCallDialog;
    private IncomingCallDialog incomingCallDialog;

    private String currentCallId;
    private boolean isInCall = false;

    public CallHandler(MainController mainController) {
        this.mainController = mainController;
        this.callService = CallService.getInstance();
        this.executor = Executors.newCachedThreadPool();

        preloadIcons();
        setupCallListener();
    }

    private void preloadIcons() {
        executor.submit(() -> {
            System.out.println("🔄 Preloading call icons...");
            IconLoader.preloadAllIcons();
        });
    }

    public void setConversationHandler(ConversationHandler conversationHandler) {
        this.conversationHandler = conversationHandler;
        System.out.println("✅ ConversationHandler set for CallHandler");
    }

    // ==================== START VIDEO CALL ====================

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

                    Platform.runLater(() -> {
                        showVideoCallDialog(partnerName);

                        // ✅ Setup video streaming NGAY sau khi dialog được tạo
                        setupVideoStreaming();

                        // Enable speaker sau khi setup xong
                        executor.submit(() -> {
                            try {
                                Thread.sleep(300);
                                callService.setSpeakerEnabled(currentCallId, true);
                                System.out.println("🔊 Speaker ON");
                                System.out.println("CallService: Đã bật loa");
                                System.out.println("✅ Speaker enabled for video caller");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    });

                    System.out.println("✅ Video call started: " + currentCallId);
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

    // ==================== START AUDIO CALL ====================

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

                    String partnerName = getPartnerName(conversationId);
                    String avatarUrl = getPartnerAvatar(conversationId);

                    Platform.runLater(() -> {
                        showAudioCallDialog(partnerName, avatarUrl);

                        executor.submit(() -> {
                            try {
                                Thread.sleep(300);
                                callService.setSpeakerEnabled(currentCallId, true);
                                System.out.println("✅ Speaker enabled for caller");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    });

                    System.out.println("✅ Audio call started: " + currentCallId);
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

    // ==================== ✅ VIDEO STREAMING SETUP - FIXED ====================

    /**
     * ✅ CRITICAL FIX: Setup video streaming với proper callback registration
     */
    private void setupVideoStreaming() {
        if (videoCallDialog == null) {
            System.err.println("❌ VideoCallDialog is null - Cannot setup video streaming");
            return;
        }

        UdpMediaClient mediaClient = callService.getMediaClient();
        if (mediaClient == null) {
            System.err.println("❌ MediaClient is null - Cannot setup video streaming");
            return;
        }

        // ✅ CALLBACK 1: Gửi video từ webcam → server
        videoCallDialog.setOnVideoData(frame -> {
            if (frame != null && mediaClient != null) {
                mediaClient.sendVideoFrame(frame);
            }
        });
        System.out.println("✅ Video send callback registered");

        // ✅ CALLBACK 2: Nhận video từ server → hiển thị lên màn hình
        mediaClient.setOnVideoFrameReceived(frame -> {
            if (frame != null && videoCallDialog != null) {
                Platform.runLater(() -> {
                    videoCallDialog.receiveVideoFrame(frame);
                });
            }
        });
        System.out.println("✅ Video receive callback registered");

        System.out.println("✅ Video streaming setup complete");
        System.out.println("   Webcam → Server: ACTIVE");
        System.out.println("   Server → Display: ACTIVE");
    }

    // ==================== ANSWER CALL ====================

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
                        // Close incoming dialog
                        if (incomingCallDialog != null) {
                            incomingCallDialog.close();
                            incomingCallDialog = null;
                        }

                        // Show appropriate dialog
                        if ("video".equals(callType)) {
                            String partnerName = "Người dùng";
                            showVideoCallDialog(partnerName);

                            // ✅ Setup video streaming TRƯỚC KHI setConnected
                            setupVideoStreaming();

                        } else {
                            String partnerName = "Người dùng";
                            String avatarUrl = null;
                            showAudioCallDialog(partnerName, avatarUrl);
                        }
                    });

                    // Wait for dialog and callbacks to be ready
                    Thread.sleep(500);

                    // Now set connected state - this will start webcam
                    Platform.runLater(() -> {
                        if (audioCallDialog != null) {
                            audioCallDialog.setConnected();
                            callService.setSpeakerEnabled(callId, true);
                            System.out.println("✅ Audio call connected (receiver)");
                        }

                        if (videoCallDialog != null) {
                            videoCallDialog.setConnected();
                            callService.setSpeakerEnabled(callId, true);
                            System.out.println("🔊 Speaker ON");
                            System.out.println("CallService: Đã bật loa");
                            System.out.println("✅ Video call connected (receiver)");
                        }
                    });

                    System.out.println("✅ Call answered: " + callId);
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showToastError("Không thể trả lời cuộc gọi")
                );
                e.printStackTrace();
            }
        });
    }

    // ==================== REJECT/END CALL ====================

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

                System.out.println("✅ Call rejected: " + callId);

            } catch (Exception e) {
                System.err.println("❌ Error rejecting call: " + e.getMessage());
            }
        });
    }

    public void endCall() {
        if (currentCallId == null) {
            System.out.println("⚠️ No active call");
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

                System.out.println("✅ Call ended: " + callIdToEnd);

            } catch (Exception e) {
                System.err.println("❌ Error ending call: " + e.getMessage());
                Platform.runLater(() -> {
                    closeAllDialogs();
                    resetCallState();
                });
            }
        });
    }

    // ==================== CONTROL METHODS ====================

    public void toggleMute() {
        if (currentCallId != null) {
            boolean isMuted = false;

            if (audioCallDialog != null) {
                isMuted = audioCallDialog.isMuted();
            } else if (videoCallDialog != null) {
                isMuted = videoCallDialog.isMuted();
            }

            callService.setMuted(currentCallId, isMuted);
            System.out.println(isMuted ? "🔇 Muted" : "🔊 Unmuted");
        }
    }

    public void toggleVideo() {
        if (currentCallId != null && videoCallDialog != null) {
            boolean isVideoEnabled = videoCallDialog.isVideoEnabled();
            callService.setVideoEnabled(currentCallId, isVideoEnabled);
            System.out.println(isVideoEnabled ? "📹 Video ON" : "📷 Video OFF");
        }
    }

    public void switchCamera() {
        if (currentCallId != null) {
            callService.switchCamera(currentCallId);
            System.out.println("🔄 Switch camera");
            System.out.println("🔄 Camera switched");
        }
    }

    // ==================== SHOW DIALOGS ====================

    private void showVideoCallDialog(String partnerName) {
        try {
            videoCallDialog = new VideoCallDialog(partnerName);

            videoCallDialog.setOnMuteToggle(this::toggleMute);
            videoCallDialog.setOnVideoToggle(this::toggleVideo);
            videoCallDialog.setOnSwitchCamera(this::switchCamera);
            videoCallDialog.setOnEndCall(this::endCall);

            videoCallDialog.setRinging();
            videoCallDialog.show();

            System.out.println("✅ VideoCallDialog shown");

        } catch (Exception e) {
            System.err.println("❌ Error showing VideoCallDialog: " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showToastError("Không thể hiển thị giao diện cuộc gọi");
        }
    }

    private void showAudioCallDialog(String partnerName, String avatarUrl) {
        try {
            audioCallDialog = new AudioCallDialog(partnerName, avatarUrl);

            audioCallDialog.setOnMuteToggle(() -> {
                if (currentCallId != null) {
                    boolean isMuted = audioCallDialog.isMuted();
                    callService.setMuted(currentCallId, isMuted);
                }
            });

            audioCallDialog.setOnSpeakerToggle(() -> {
                if (currentCallId != null) {
                    boolean isSpeakerOn = audioCallDialog.isSpeakerOn();
                    callService.setSpeakerEnabled(currentCallId, isSpeakerOn);
                }
            });

            audioCallDialog.setOnEndCall(this::endCall);

            audioCallDialog.setRinging();
            audioCallDialog.show();

            System.out.println("✅ AudioCallDialog shown");

        } catch (Exception e) {
            System.err.println("❌ Error showing AudioCallDialog: " + e.getMessage());
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

            System.out.println("✅ IncomingCallDialog shown from: " + callerName);

        } catch (Exception e) {
            System.err.println("❌ Error showing IncomingCallDialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeAllDialogs() {
        if (audioCallDialog != null) {
            try {
                audioCallDialog.close();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing AudioCallDialog: " + e.getMessage());
            }
            audioCallDialog = null;
        }

        if (videoCallDialog != null) {
            try {
                videoCallDialog.close();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing VideoCallDialog: " + e.getMessage());
            }
            videoCallDialog = null;
        }

        if (incomingCallDialog != null) {
            try {
                incomingCallDialog.close();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing IncomingCallDialog: " + e.getMessage());
            }
            incomingCallDialog = null;
        }

        System.out.println("🗑️ All dialogs closed");
    }

    // ==================== REALTIME LISTENERS ====================

    private void setupCallListener() {
        callService.setOnIncomingCall((callId, callerId, callerName, callType) -> {
            System.out.println("📞 Incoming call from: " + callerName + " (" + callType + ")");

            String avatarUrl = null;
            try {
                if (conversationHandler != null) {
                    avatarUrl = getPartnerAvatar(callerId);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Cannot get avatar: " + e.getMessage());
            }

            final String finalAvatarUrl = avatarUrl;
            Platform.runLater(() ->
                    showIncomingCallDialog(callerName, callType, callId, finalAvatarUrl)
            );
        });

        callService.setOnCallAnswered(callId -> {
            Platform.runLater(() -> {
                AlertUtil.showToastSuccess("Cuộc gọi đã được kết nối");

                if (audioCallDialog != null) {
                    audioCallDialog.setConnected();
                    callService.setSpeakerEnabled(callId, true);
                }

                if (videoCallDialog != null) {
                    // ✅ Setup video streaming TRƯỚC KHI setConnected
                    setupVideoStreaming();

                    // Delay nhỏ để callback register xong
                    executor.submit(() -> {
                        try {
                            Thread.sleep(300);
                            Platform.runLater(() -> {
                                videoCallDialog.setConnected();
                                callService.setSpeakerEnabled(callId, true);
                                System.out.println("🔊 Speaker ON");
                                System.out.println("CallService: Đã bật loa");
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }

                System.out.println("✅ Call connected: " + callId);
            });
        });

        callService.setOnCallRejected(callId -> {
            Platform.runLater(() -> {
                AlertUtil.showToastInfo("Cuộc gọi bị từ chối");
                closeAllDialogs();
                resetCallState();
            });
        });

        callService.setOnCallEnded(callId -> {
            Platform.runLater(() -> {
                AlertUtil.showToastInfo("Cuộc gọi đã kết thúc");
                closeAllDialogs();
                resetCallState();
            });
        });

        callService.setOnCallError((callId, error) -> {
            Platform.runLater(() -> {
                AlertUtil.showToastError("Lỗi cuộc gọi: " + error);
                closeAllDialogs();
                resetCallState();
            });
        });

        System.out.println("✅ CallService listeners setup complete");
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
            System.err.println("⚠️ Cannot get partner name: " + e.getMessage());
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
            System.err.println("⚠️ Cannot get partner avatar: " + e.getMessage());
        }
        return null;
    }

    private void resetCallState() {
        currentCallId = null;
        isInCall = false;
        System.out.println("🔄 Call state reset");
    }

    public boolean isInCall() {
        return isInCall;
    }

    public String getCurrentCallId() {
        return currentCallId;
    }

    public void cleanup() {
        System.out.println("🧹 Cleaning up CallHandler...");

        if (isInCall) {
            endCall();
        }

        closeAllDialogs();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        System.out.println("✅ CallHandler cleanup complete");
    }
}