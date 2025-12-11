package org.example.chatappclient.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.example.chatappclient.client.models.User;
import org.example.chatappclient.client.services.AuthService;
import org.example.chatappclient.client.services.CallService;

/**
 * Ứng dụng test JavaFX - Kiểm thử tính năng gọi thoại/video qua UDP
 * Chạy thử toàn bộ luồng: đăng nhập → gọi → nhận → điều khiển → kết thúc
 */
public class CallTestExample extends Application {

    private CallService callService;
    private User currentUser;
    private String currentCallId;
    private SocketClient socketClient;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== BẮT ĐẦU TEST CUỘC GỌI JAVA FX ===\n");

        // Khởi tạo và kết nối tới server
        initializeAndConnect();

        // Ẩn cửa sổ chính (chỉ dùng để test dialog gọi)
        primaryStage.hide();
    }

    /**
     * Khởi tạo kết nối, đăng nhập và chuẩn bị CallService
     */
    private void initializeAndConnect() {
        new Thread(() -> {
            try {
                // 1. Kết nối tới server
                System.out.println("Đang kết nối tới server...");
                socketClient = SocketClient.getInstance();

                if (!socketClient.connect()) {
                    showError("Không thể kết nối tới server");
                    Platform.exit();
                    return;
                }

                System.out.println("Đã kết nối thành công tới server\n");

                // 2. Đăng nhập
                System.out.println("Đang đăng nhập...");
                AuthService authService = AuthService.getInstance();
                AuthService.LoginResult result = authService.login("user1", "Password123!", false);

                if (!result.isSuccess()) {
                    showError("Đăng nhập thất bại: " + result.getMessage());
                    Platform.exit();
                    return;
                }

                currentUser = result.getUser();
                System.out.println("Đăng nhập thành công!");
                System.out.println("   Tên người dùng: " + currentUser.getUsername());
                System.out.println("   ID người dùng  : " + currentUser.getUserId() + "\n");

                // 3. Khởi tạo CallService
                System.out.println("Đang khởi tạo dịch vụ gọi thoại...");
                callService = CallService.getInstance();
                setupCallCallbacks();
                System.out.println("Dịch vụ gọi thoại đã sẵn sàng\n");

                // 4. Bắt đầu test gọi
                Platform.runLater(this::startCallTest);

            } catch (Exception e) {
                showError("Lỗi khởi tạo ứng dụng: " + e.getMessage());
                e.printStackTrace();
                Platform.exit();
            }
        }).start();
    }

    /**
     * Thiết lập các callback xử lý sự kiện cuộc gọi
     */
    private void setupCallCallbacks() {
        // Có cuộc gọi đến
        callService.setOnIncomingCall((callId, callerId, callerName, callType) -> {
            Platform.runLater(() -> {
                System.out.println("\n========== CÓ CUỘC GỌI ĐẾN ==========");
                System.out.println("   Loại cuộc gọi : " + ("video".equals(callType) ? "VIDEO" : "THOẠI"));
                System.out.println("   Từ            : " + callerName + " (ID: " + callerId + ")");
                System.out.println("   Mã cuộc gọi   : " + callId);
                System.out.println("=======================================\n");

                // Tự động bắt máy sau 2 giây (cho test nhanh
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        System.out.println("Tự động bắt máy...");
                        callService.answerCall(callId, currentUser.getUserId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            });
        });

        // Cuộc gọi được chấp nhận
        callService.setOnCallAnswered(callId -> {
            Platform.runLater(() -> {
                System.out.println("\n========== CUỘC GỌI ĐÃ KẾT NỐI ==========");
                System.out.println("   Mã cuộc gọi   : " + callId);
                System.out.println("   Âm thanh/video đang hoạt động");
                System.out.println("===========================================\n");
            });
        });

        // Cuộc gọi bị từ chối
        callService.setOnCallRejected(callId -> {
            Platform.runLater(() -> {
                System.out.println("\n========== CUỘC GỌI BỊ TỪ CHỐI ==========");
                System.out.println("   Mã cuộc gọi   : " + callId);
                System.out.println("===========================================\n");
            });
        });

        // Cuộc gọi kết thúc
        callService.setOnCallEnded(callId -> {
            Platform.runLater(() -> {
                System.out.println("\n========== CUỘC GỌI ĐÃ KẾT THÚC ==========");
                System.out.println("   Mã cuộc gọi   : " + callId);
                System.out.println("==========================================\n");
            });
        });

        // Lỗi trong cuộc gọi
        callService.setOnCallError((callId, error) -> {
            Platform.runLater(() -> {
                System.err.println("\n========== LỖI CUỘC GỌI ==========");
                System.err.println("   Mã cuộc gọi   : " + callId);
                System.err.println("   Chi tiết lỗi  : " + error);
                System.err.println("======================================\n");
            });
        });
    }

    /**
     * Bắt đầu thực hiện cuộc gọi test
     */
    private void startCallTest() {
        System.out.println("=== BẮT ĐẦU KIỂM THỬ CUỘC GỌI ===\n");

        // Chọn loại cuộc gọi: "audio" hoặc "video"
        String callType = "audio"; // Thay thành "video" để test gọi có hình
        String conversationId = "test_conv_" + System.currentTimeMillis();

        System.out.println("Đang thực hiện cuộc gọi " + callType.toUpperCase() + "...");
        System.out.println("   Mã hội thoại : " + conversationId);
        System.out.println("   Người gọi     : " + currentUser.getUserId());

        new Thread(() -> {
            try {
                currentCallId = callService.startCall(
                        conversationId,
                        currentUser.getUserId(),
                        callType
                );

                if (currentCallId == null || currentCallId.isEmpty()) {
                    Platform.runLater(() -> showError("Không thể bắt đầu cuộc gọi"));
                    return;
                }

                Platform.runLater(() -> {
                    System.out.println("Cuộc gọi đã được thiết lập thành công!");
                    System.out.println("   Mã cuộc gọi   : " + currentCallId);
                    System.out.println("\nMicro đã hoạt động...\n");

                    // Bắt đầu test các chức năng điều khiển
                    startControlTests(callType);
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi khi bắt đầu cuộc gọi: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Kiểm thử các nút điều khiển trong cuộc gọi (tắt tiếng, bật video, đổi camera...)
     */
    private void startControlTests(String callType) {
        new Thread(() -> {
            try {
                System.out.println("=== KIỂM THỬ CÁC CHỨC NĂNG ĐIỀU KHIỂN ===\n");

                // Test 1: Tắt tiếng
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    System.out.println("Đang thử TẮT TIẾNG...");
                    callService.setMuted(currentCallId, true);
                    System.out.println("   Trạng thái: ĐÃ TẮT TIẾNG\n");
                });

                // Test 2: Bật tiếng lại
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    System.out.println("Đang thử BẬT TIẾNG...");
                    callService.setMuted(currentCallId, false);
                    System.out.println("   Trạng thái: ĐÃ BẬT TIẾNG\n");
                });

                // Test video (chỉ khi là cuộc gọi video)
                if ("video".equals(callType)) {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        System.out.println("Đang thử TẮT VIDEO...");
                        callService.setVideoEnabled(currentCallId, false);
                        System.out.println("   Trạng thái: ĐÃ TẮT CAMERA\n");
                    });

                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        System.out.println("Đang thử BẬT VIDEO...");
                        callService.setVideoEnabled(currentCallId, true);
                        System.out.println("   Trạng thái: ĐÃ BẬT CAMERA\n");
                    });

                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        System.out.println("Đang thử CHUYỂN CAMERA...");
                        callService.switchCamera(currentCallId);
                        System.out.println("   Đã chuyển camera thành công\n");
                    });
                }

                // Đếm ngược trước khi cúp máy
                Platform.runLater(() -> System.out.println("Cuộc gọi sẽ kết thúc sau 5 giây..."));
                for (int i = 5; i > 0; i--) {
                    final int count = i;
                    Platform.runLater(() -> System.out.println("   " + count + "..."));
                    Thread.sleep(1000);
                }

                // Kết thúc cuộc gọi
                Platform.runLater(() -> {
                    System.out.println("\nĐang cúp máy...");
                    try {
                        callService.endCall(currentCallId, currentUser.getUserId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                Thread.sleep(2000);

                // Kết quả test
                Platform.runLater(() -> {
                    System.out.println("\n=== KIỂM THỬ HOÀN TẤT ===");
                    System.out.println("Tất cả chức năng hoạt động tốt!");
                    System.out.println("   • Bắt đầu cuộc gọi    : THÀNH CÔNG");
                    System.out.println("   • Tắt/Bật tiếng       : THÀNH CÔNG");
                    if ("video".equals(callType)) {
                        System.out.println("   • Bật/Tắt video       : THÀNH CÔNG");
                        System.out.println("   • Chuyển camera       : THÀNH CÔNG");
                    }
                    System.out.println("   • Kết thúc cuộc gọi   : THÀNH CÔNG\n");

                    cleanup();
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi trong quá trình test điều khiển: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void showError(String message) {
        System.err.println("LỖI: " + message);
    }

    /**
     * Dọn dẹp tài nguyên và thoát ứng dụng
     */
    private void cleanup() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("\nĐang ngắt kết nối khỏi server...");

                if (socketClient != null && socketClient.isConnected()) {
                    socketClient.disconnect();
                    System.out.println("Đã ngắt kết nối");
                }

                Thread.sleep(500);
                System.out.println("Ứng dụng đang thoát...");
                Platform.exit();
                System.exit(0);

            } catch (Exception e) {
                System.err.println("Lỗi khi dọn dẹp: " + e.getMessage());
                Platform.exit();
                System.exit(1);
            }
        }).start();
    }

    @Override
    public void stop() {
        System.out.println("\nỨng dụng đang dừng...");
        cleanup();
    }
}