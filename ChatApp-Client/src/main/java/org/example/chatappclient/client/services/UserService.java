package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.User;
import org.example.chatappclient.client.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý user profile, tìm kiếm
 */
public class UserService {

    private static volatile UserService instance;
    private final SocketClient socketClient;

    private UserService() {
        socketClient = SocketClient.getInstance();
    }

    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) {
                if (instance == null) {
                    instance = new UserService();
                }
            }
        }
        return instance;
    }

    // ==================== USER OPERATIONS ====================

    public User getProfile(String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_GET_PROFILE, userId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseUser(Protocol.getData(response));
    }

    public User updateProfile(String userId, String displayName, String bio) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_UPDATE_PROFILE, userId, displayName, bio);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseUser(Protocol.getData(response));
    }

    public String updateAvatar(String userId, String avatarUrl) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_UPDATE_AVATAR, userId, avatarUrl);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return Protocol.getData(response); // Returns new avatar URL
    }

    public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_CHANGE_PASSWORD, userId, oldPassword, newPassword);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    public List<User> searchUsers(String query) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_SEARCH, query);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        return parseUsers(Protocol.getData(response));
    }

    public void updateOnlineStatus(String userId, String status) {
        String request = Protocol.buildRequest(Protocol.USER_UPDATE_STATUS, userId, status);
        socketClient.sendMessage(request);
    }

    // ==================== PARSING ====================

    private List<User> parseUsers(String data) {
        List<User> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;

        for (String userData : Protocol.parseDataList(data)) {
            User user = parseUser(userData);
            if (user != null) list.add(user);
        }
        return list;
    }

    private User parseUser(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] f = Protocol.parseFields(data);
        if (f.length < 3) return null;

        User u = new User();
        u.setUserId(f[0]);
        u.setUsername(f[1]);
        u.setEmail(f[2]);
        if (f.length > 3) u.setDisplayName(f[3]);
        if (f.length > 4) u.setAvatarUrl(f[4]);
        if (f.length > 5) u.setPhone(f[5]);
        if (f.length > 6) u.setBio(f[6]);
        if (f.length > 7) u.setStatus(f[7]);
        if (f.length > 8) u.setOnline(Boolean.parseBoolean(f[8]));
        return u;
    }
}