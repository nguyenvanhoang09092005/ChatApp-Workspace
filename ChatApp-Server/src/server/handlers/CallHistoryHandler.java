package server.handlers;

import database.dao.CallHistoryDAO;
import database.dao.CallParticipantDAO;
import models.CallHistory;
import models.CallParticipant;
import protocol.Protocol;
import server.ClientHandler;

import java.util.List;

/**
 * Handler xử lý các request liên quan đến lịch sử cuộc gọi
 */
public class CallHistoryHandler {

    private final ClientHandler clientHandler;

    public CallHistoryHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void handle(String command, String[] parts) {
        switch (command) {
            case Protocol.CALL_HISTORY_GET:
                handleGetCallHistory(parts);
                break;
            case Protocol.CALL_HISTORY_GET_BY_CONV:
                handleGetCallHistoryByConversation(parts);
                break;
            case Protocol.CALL_HISTORY_GET_MISSED:
                handleGetMissedCalls(parts);
                break;
            case Protocol.CALL_HISTORY_DELETE:
                handleDeleteCall(parts);
                break;
            case Protocol.CALL_HISTORY_CLEAR:
                handleClearCallHistory(parts);
                break;
            case Protocol.CALL_HISTORY_STATS:
                handleGetCallStats(parts);
                break;
            default:
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR, "Unknown call history command"));
        }
    }

    // ==================== GET CALL HISTORY ====================

    private void handleGetCallHistory(String[] parts) {
        if (parts.length < 3) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Invalid request"));
            return;
        }

        String userId = parts[1];
        int limit = Integer.parseInt(parts[2]);

        try {
            List<CallHistory> calls = CallHistoryDAO.getCallsByUser(userId, limit);

            if (calls.isEmpty()) {
                clientHandler.sendMessage(Protocol.buildSuccessResponse(
                        "No call history", "0"));
                return;
            }

            StringBuilder data = new StringBuilder();
            data.append(calls.size());

            for (CallHistory call : calls) {
                List<CallParticipant> participants =
                        CallParticipantDAO.getParticipantsByCall(call.getCallId());

                data.append(Protocol.DELIMITER)
                        .append(serializeCallHistory(call, participants, userId));
            }

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Call history retrieved", data.toString()));

        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Failed to get call history"));
        }
    }

    // ==================== GET BY CONVERSATION ====================

    private void handleGetCallHistoryByConversation(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Invalid request"));
            return;
        }

        String conversationId = parts[1];

        try {
            List<CallHistory> calls = CallHistoryDAO.getCallsByConversation(conversationId);

            if (calls.isEmpty()) {
                clientHandler.sendMessage(Protocol.buildSuccessResponse(
                        "No calls found", "0"));
                return;
            }

            StringBuilder data = new StringBuilder();
            data.append(calls.size());

            for (CallHistory call : calls) {
                data.append(Protocol.DELIMITER)
                        .append(serializeCallHistorySimple(call));
            }

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Calls retrieved", data.toString()));

        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Failed to get calls"));
        }
    }

    // ==================== GET MISSED CALLS ====================

    private void handleGetMissedCalls(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Invalid request"));
            return;
        }

        String userId = parts[1];

        try {
            List<CallHistory> calls = CallHistoryDAO.getMissedCallsByUser(userId);

            if (calls.isEmpty()) {
                clientHandler.sendMessage(Protocol.buildSuccessResponse(
                        "No missed calls", "0"));
                return;
            }

            StringBuilder data = new StringBuilder();
            data.append(calls.size());

            for (CallHistory call : calls) {
                data.append(Protocol.DELIMITER)
                        .append(serializeCallHistorySimple(call));
            }

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Missed calls retrieved", data.toString()));

        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Failed to get missed calls"));
        }
    }

    // ==================== DELETE ====================

    private void handleDeleteCall(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Invalid request"));
            return;
        }

        String callId = parts[1];

        try {
            boolean success = CallHistoryDAO.deleteCall(callId);

            if (success) {
                clientHandler.sendMessage(Protocol.buildSuccessResponse(
                        "Call deleted"));
            } else {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_NOT_FOUND, "Call not found"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Failed to delete call"));
        }
    }

    // ==================== CLEAR ====================

    private void handleClearCallHistory(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Invalid request"));
            return;
        }

        String userId = parts[1];

        try {
            // Xóa tất cả participants của user
            // Sau đó xóa các call history không còn participants
            // (Implement theo business logic của bạn)

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Call history cleared"));

        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Failed to clear call history"));
        }
    }

    // ==================== STATISTICS ====================

    private void handleGetCallStats(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Invalid request"));
            return;
        }

        String userId = parts[1];

        try {
            int totalCalls = CallHistoryDAO.getTotalCallsByUser(userId);
            int totalDuration = CallHistoryDAO.getTotalDurationByUser(userId);

            String data = String.format("%d%s%d",
                    totalCalls, Protocol.DELIMITER,
                    totalDuration);

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "Statistics retrieved", data));

        } catch (Exception e) {
            e.printStackTrace();
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR, "Failed to get statistics"));
        }
    }

    // ==================== SERIALIZATION ====================

    private String serializeCallHistory(CallHistory call,
                                        List<CallParticipant> participants,
                                        String currentUserId) {
        // Format: callId,conversationId,callType,status,callerId,callerName,
        //         startTime,endTime,duration,isIncoming,partnerName,partnerAction

        StringBuilder sb = new StringBuilder();
        sb.append(call.getCallId()).append(",")
                .append(call.getConversationId()).append(",")
                .append(call.getCallType()).append(",")
                .append(call.getStatus()).append(",")
                .append(call.getCallerId()).append(",")
                .append(call.getCallerName()).append(",")
                .append(call.getStartTime() != null ? call.getStartTime().getTime() : 0).append(",")
                .append(call.getEndTime() != null ? call.getEndTime().getTime() : 0).append(",")
                .append(call.getDuration()).append(",");

        // Xác định isIncoming và partner info
        boolean isIncoming = !call.getCallerId().equals(currentUserId);
        sb.append(isIncoming).append(",");

        // Tìm partner (người còn lại trong cuộc gọi)
        String partnerName = "";
        String partnerAction = "";
        for (CallParticipant p : participants) {
            if (!p.getUserId().equals(currentUserId)) {
                partnerName = call.getCallerName(); // hoặc lấy từ database
                partnerAction = p.getAction();
                break;
            }
        }

        sb.append(partnerName).append(",")
                .append(partnerAction);

        return sb.toString();
    }

    private String serializeCallHistorySimple(CallHistory call) {
        // Format đơn giản: callId,callType,status,callerId,callerName,
        //                  startTime,duration

        return String.format("%s,%s,%s,%s,%s,%d,%d",
                call.getCallId(),
                call.getCallType(),
                call.getStatus(),
                call.getCallerId(),
                call.getCallerName(),
                call.getStartTime() != null ? call.getStartTime().getTime() : 0,
                call.getDuration());
    }
}