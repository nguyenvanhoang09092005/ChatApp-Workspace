package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.CallHistory;
import org.example.chatappclient.client.protocol.Protocol;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service quản lý lịch sử cuộc gọi cho client
 */
public class CallHistoryService {
    private static volatile CallHistoryService instance;
    private final SocketClient socketClient;

    private CallHistoryService() {
        this.socketClient = SocketClient.getInstance();
    }

    public static CallHistoryService getInstance() {
        if (instance == null) {
            synchronized (CallHistoryService.class) {
                if (instance == null) {
                    instance = new CallHistoryService();
                }
            }
        }
        return instance;
    }

    // ==================== GET CALL HISTORY ====================

    /**
     * Lấy lịch sử cuộc gọi của user
     * @param userId ID người dùng
     * @param limit Số lượng cuộc gọi cần lấy
     * @return Danh sách CallHistory
     */
    public List<CallHistory> getCallHistory(String userId, int limit) throws Exception {
        String request = Protocol.buildRequest(
                Protocol.CALL_HISTORY_GET, userId, String.valueOf(limit));
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }

        if (!Protocol.isSuccess(response)) {
            throw new Exception(Protocol.getErrorMessage(response));
        }

        return parseCallHistoryList(response, userId);
    }

    /**
     * Lấy lịch sử cuộc gọi trong một conversation
     */
    public List<CallHistory> getCallHistoryByConversation(String conversationId)
            throws Exception {
        String request = Protocol.buildRequest(
                Protocol.CALL_HISTORY_GET_BY_CONV, conversationId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }

        if (!Protocol.isSuccess(response)) {
            throw new Exception(Protocol.getErrorMessage(response));
        }

        return parseSimpleCallHistoryList(response);
    }

    /**
     * Lấy các cuộc gọi nhỡ
     */
    public List<CallHistory> getMissedCalls(String userId) throws Exception {
        String request = Protocol.buildRequest(
                Protocol.CALL_HISTORY_GET_MISSED, userId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }

        if (!Protocol.isSuccess(response)) {
            throw new Exception(Protocol.getErrorMessage(response));
        }

        return parseSimpleCallHistoryList(response);
    }

    // ==================== DELETE ====================

    /**
     * Xóa một cuộc gọi khỏi lịch sử
     */
    public boolean deleteCall(String callId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_HISTORY_DELETE, callId);
        String response = socketClient.sendRequest(request, 5000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }

        return Protocol.isSuccess(response);
    }

    /**
     * Xóa toàn bộ lịch sử cuộc gọi
     */
    public boolean clearCallHistory(String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_HISTORY_CLEAR, userId);
        String response = socketClient.sendRequest(request, 5000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }

        return Protocol.isSuccess(response);
    }

    // ==================== STATISTICS ====================

    /**
     * Lấy thống kê cuộc gọi
     * @return int[]{totalCalls, totalDuration}
     */
    public int[] getCallStatistics(String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.CALL_HISTORY_STATS, userId);
        String response = socketClient.sendRequest(request, 5000);

        if (response == null) {
            throw new Exception("Server không phản hồi");
        }

        if (!Protocol.isSuccess(response)) {
            throw new Exception(Protocol.getErrorMessage(response));
        }

        String[] parts = Protocol.parseMessage(response);
        if (parts.length >= 4) {
            int totalCalls = Integer.parseInt(parts[2]);
            int totalDuration = Integer.parseInt(parts[3]);
            return new int[]{totalCalls, totalDuration};
        }

        return new int[]{0, 0};
    }

    // ==================== PARSING ====================

    private List<CallHistory> parseCallHistoryList(String response, String currentUserId) {
        List<CallHistory> calls = new ArrayList<>();

        try {
            String[] parts = Protocol.parseMessage(response);

            if (parts.length < 3) return calls;

            int count = Integer.parseInt(parts[2]);

            for (int i = 0; i < count && (i + 3) < parts.length; i++) {
                CallHistory call = parseCallHistory(parts[i + 3], currentUserId);
                if (call != null) {
                    calls.add(call);
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi parse call history: " + e.getMessage());
            e.printStackTrace();
        }

        return calls;
    }

    private CallHistory parseCallHistory(String data, String currentUserId) {
        try {
            // Format: callId,conversationId,callType,status,callerId,callerName,
            //         startTime,endTime,duration,isIncoming,partnerName,partnerAction

            String[] fields = data.split(",", -1);
            if (fields.length < 12) return null;

            CallHistory call = new CallHistory();
            call.setCallId(fields[0]);
            call.setConversationId(fields[1]);
            call.setCallType(fields[2]);
            call.setStatus(fields[3]);
            call.setCallerId(fields[4]);
            call.setCallerName(fields[5]);

            // Parse timestamps
            long startTime = Long.parseLong(fields[6]);
            if (startTime > 0) {
                call.setStartTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(startTime), ZoneId.systemDefault()));
            }

            long endTime = Long.parseLong(fields[7]);
            if (endTime > 0) {
                call.setEndTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(endTime), ZoneId.systemDefault()));
            }

            call.setDuration(Integer.parseInt(fields[8]));
            call.setIncoming(Boolean.parseBoolean(fields[9]));
            call.setPartnerName(fields[10]);

            return call;

        } catch (Exception e) {
            System.err.println("Lỗi parse call history item: " + e.getMessage());
            return null;
        }
    }

    private List<CallHistory> parseSimpleCallHistoryList(String response) {
        List<CallHistory> calls = new ArrayList<>();

        try {
            String[] parts = Protocol.parseMessage(response);

            if (parts.length < 3) return calls;

            int count = Integer.parseInt(parts[2]);

            for (int i = 0; i < count && (i + 3) < parts.length; i++) {
                CallHistory call = parseSimpleCallHistory(parts[i + 3]);
                if (call != null) {
                    calls.add(call);
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi parse simple call history: " + e.getMessage());
        }

        return calls;
    }

    private CallHistory parseSimpleCallHistory(String data) {
        try {
            // Format: callId,callType,status,callerId,callerName,startTime,duration

            String[] fields = data.split(",", -1);
            if (fields.length < 7) return null;

            CallHistory call = new CallHistory();
            call.setCallId(fields[0]);
            call.setCallType(fields[1]);
            call.setStatus(fields[2]);
            call.setCallerId(fields[3]);
            call.setCallerName(fields[4]);

            long startTime = Long.parseLong(fields[5]);
            if (startTime > 0) {
                call.setStartTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(startTime), ZoneId.systemDefault()));
            }

            call.setDuration(Integer.parseInt(fields[6]));

            return call;

        } catch (Exception e) {
            System.err.println("Lỗi parse simple call history: " + e.getMessage());
            return null;
        }
    }
}